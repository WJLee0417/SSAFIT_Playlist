package com.ssafy.ssafit.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistMapper playlistMapper;

    private PlaylistService playlistService;

    @BeforeEach
    void setUp() {
        playlistService = new PlaylistService(playlistMapper);
    }

    @Test
    void createBuildsUserManualPlaylist() {
        Playlist input = playlist(null, null, PlaylistType.OFFICIAL, PlaylistVisibility.PUBLIC);
        input.setTitle("  러닝 목록  ");
        when(playlistMapper.insert(input)).thenAnswer(invocation -> {
            input.setId(100L);
            return 1;
        });

        Playlist created = playlistService.create(10L, input);

        assertThat(created.getId()).isEqualTo(100L);
        assertThat(created.getOwnerId()).isEqualTo(10L);
        assertThat(created.getPlaylistType()).isEqualTo(PlaylistType.USER);
        assertThat(created.getSourceType()).isEqualTo(PlaylistSourceType.MANUAL);
        assertThat(created.getTitle()).isEqualTo("러닝 목록");
        assertThat(created.getSpotifyExportStatus())
                .isEqualTo(SpotifyExportStatus.NOT_EXPORTED);
    }

    @Test
    void createRequiresAuthentication() {
        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> playlistService.create(null, playlist(null, null, null, null)));
    }

    @Test
    void listDelegatesVisiblePolicyToMapper() {
        Playlist visible = playlist(1L, 20L, PlaylistType.USER, PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectList(10L, null)).thenReturn(List.of(visible));

        assertThat(playlistService.getList(10L, null)).containsExactly(visible);
    }

    @Test
    void publicDetailCanBeViewedByOtherMember() {
        Playlist playlist = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));

        assertThat(playlistService.getDetail(20L, 1L)).isSameAs(playlist);
    }

    @Test
    void privateDetailRejectsOtherMember() {
        Playlist playlist = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));

        assertError(
                ErrorCode.PRIVATE_PLAYLIST,
                () -> playlistService.getDetail(20L, 1L));
    }

    @Test
    void ownerCanUpdatePlaylist() {
        Playlist existing = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        existing.setSourceType(PlaylistSourceType.MANUAL);
        Playlist changes = playlist(1L, 999L, PlaylistType.OFFICIAL, PlaylistVisibility.PUBLIC);
        changes.setTitle("변경");
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(existing));

        Playlist updated = playlistService.update(10L, changes);

        assertThat(updated.getOwnerId()).isEqualTo(10L);
        assertThat(updated.getPlaylistType()).isEqualTo(PlaylistType.USER);
        assertThat(updated.getSourceType()).isEqualTo(PlaylistSourceType.MANUAL);
        verify(playlistMapper).update(changes);
    }

    @Test
    void otherMemberCannotUpdatePlaylist() {
        Playlist existing = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        Playlist changes = playlist(1L, null, null, PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(existing));

        assertError(ErrorCode.ACCESS_DENIED, () -> playlistService.update(20L, changes));
        verify(playlistMapper, never()).update(changes);
    }

    @Test
    void ownerCanSoftDeletePlaylist() {
        Playlist playlist = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));

        playlistService.delete(10L, 1L);

        verify(playlistMapper).softDelete(1L);
    }

    @Test
    void officialPlaylistCannotBeDeletedByMember() {
        Playlist playlist = playlist(1L, null, PlaylistType.OFFICIAL, PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));

        assertError(ErrorCode.ACCESS_DENIED, () -> playlistService.delete(10L, 1L));
        verify(playlistMapper, never()).softDelete(1L);
    }

    @Test
    void exportStatusCanBeChangedToPending() {
        Playlist playlist = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        when(playlistMapper.selectExportTargetPlaylist(1L))
                .thenReturn(Optional.of(playlist));

        playlistService.updateSpotifyExportStatus(
                10L,
                1L,
                SpotifyExportStatus.PENDING);

        verify(playlistMapper)
                .updateSpotifyExportStatus(1L, SpotifyExportStatus.PENDING);
    }

    @Test
    void successStatusMustUseExportResultMethod() {
        Playlist playlist = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        when(playlistMapper.selectExportTargetPlaylist(1L))
                .thenReturn(Optional.of(playlist));

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> playlistService.updateSpotifyExportStatus(
                        10L,
                        1L,
                        SpotifyExportStatus.SUCCESS));
    }

    @Test
    void exportResultIsStoredAsSuccess() {
        Playlist playlist = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        when(playlistMapper.selectExportTargetPlaylist(1L))
                .thenReturn(Optional.of(playlist));

        Playlist result = playlistService.saveSpotifyExportResult(
                10L,
                1L,
                "spotify-id",
                "https://open.spotify.com/playlist/spotify-id");

        assertThat(result.getSpotifyExportStatus()).isEqualTo(SpotifyExportStatus.SUCCESS);
        verify(playlistMapper).updateSpotifyExportResult(playlist);
    }

    private Playlist playlist(
            Long id,
            Long ownerId,
            PlaylistType type,
            PlaylistVisibility visibility) {
        Playlist playlist = new Playlist();
        playlist.setId(id);
        playlist.setOwnerId(ownerId);
        playlist.setPlaylistType(type);
        playlist.setSourceType(PlaylistSourceType.MANUAL);
        playlist.setVisibility(visibility);
        playlist.setTitle("플레이리스트");
        playlist.setSpotifyExportStatus(SpotifyExportStatus.NOT_EXPORTED);
        return playlist;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
