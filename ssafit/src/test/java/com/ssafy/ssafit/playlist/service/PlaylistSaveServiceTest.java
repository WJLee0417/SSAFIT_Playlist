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
import com.ssafy.ssafit.playlist.domain.PlaylistSave;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistSaveMapper;

@ExtendWith(MockitoExtension.class)
class PlaylistSaveServiceTest {

    @Mock
    private PlaylistSaveMapper playlistSaveMapper;

    @Mock
    private PlaylistMapper playlistMapper;

    private PlaylistSaveService playlistSaveService;

    @BeforeEach
    void setUp() {
        playlistSaveService = new PlaylistSaveService(
                playlistSaveMapper,
                playlistMapper);
    }

    @Test
    void publicPlaylistCanBeSaved() {
        Playlist playlist = playlist(1L, 20L, PlaylistType.USER, PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));
        when(playlistSaveMapper.existsByMemberAndPlaylist(10L, 1L)).thenReturn(false);

        PlaylistSave saved = playlistSaveService.save(10L, 1L);

        assertThat(saved.getMemberId()).isEqualTo(10L);
        assertThat(saved.getPlaylistId()).isEqualTo(1L);
        verify(playlistSaveMapper).insert(saved);
    }

    @Test
    void officialPlaylistCanBeSaved() {
        Playlist playlist = playlist(
                1L,
                null,
                PlaylistType.OFFICIAL,
                PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));
        when(playlistSaveMapper.existsByMemberAndPlaylist(10L, 1L)).thenReturn(false);

        PlaylistSave saved = playlistSaveService.save(10L, 1L);

        verify(playlistSaveMapper).insert(saved);
    }

    @Test
    void duplicateSaveIsRejected() {
        Playlist playlist = playlist(1L, 20L, PlaylistType.USER, PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));
        when(playlistSaveMapper.existsByMemberAndPlaylist(10L, 1L)).thenReturn(true);

        assertError(
                ErrorCode.DUPLICATE_SAVE,
                () -> playlistSaveService.save(10L, 1L));

        verify(playlistSaveMapper, never()).insert(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void otherMemberCannotSavePrivatePlaylist() {
        Playlist playlist = playlist(1L, 20L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));

        assertError(
                ErrorCode.PRIVATE_PLAYLIST,
                () -> playlistSaveService.save(10L, 1L));

        verify(playlistSaveMapper, never())
                .existsByMemberAndPlaylist(10L, 1L);
    }

    @Test
    void privatePlaylistOwnerCanSaveReference() {
        Playlist playlist = playlist(1L, 10L, PlaylistType.USER, PlaylistVisibility.PRIVATE);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));
        when(playlistSaveMapper.existsByMemberAndPlaylist(10L, 1L)).thenReturn(false);

        PlaylistSave saved = playlistSaveService.save(10L, 1L);

        verify(playlistSaveMapper).insert(saved);
    }

    @Test
    void ownerOfSaveRelationCanCancel() {
        Playlist playlist = playlist(1L, 20L, PlaylistType.USER, PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));
        when(playlistSaveMapper.existsByMemberAndPlaylist(10L, 1L)).thenReturn(true);

        playlistSaveService.cancel(10L, 1L);

        verify(playlistSaveMapper).delete(10L, 1L);
    }

    @Test
    void memberCannotDeleteAnotherMembersSaveRelation() {
        Playlist playlist = playlist(1L, 20L, PlaylistType.USER, PlaylistVisibility.PUBLIC);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));
        when(playlistSaveMapper.existsByMemberAndPlaylist(10L, 1L)).thenReturn(false);

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> playlistSaveService.cancel(10L, 1L));

        verify(playlistSaveMapper, never()).delete(10L, 1L);
    }

    @Test
    void savedListIsReturnedForCurrentMember() {
        Playlist first = playlist(1L, null, PlaylistType.OFFICIAL, PlaylistVisibility.PUBLIC);
        Playlist second = playlist(2L, 20L, PlaylistType.USER, PlaylistVisibility.PUBLIC);
        when(playlistSaveMapper.selectSavedList(10L))
                .thenReturn(List.of(first, second));

        List<Playlist> result = playlistSaveService.getMySavedList(10L);

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void missingPlaylistCannotBeSaved() {
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.PLAYLIST_NOT_FOUND,
                () -> playlistSaveService.save(10L, 1L));
    }

    @Test
    void unauthenticatedMemberCannotUseSavedList() {
        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> playlistSaveService.getMySavedList(null));
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
        playlist.setVisibility(visibility);
        playlist.setTitle("저장 대상");
        return playlist;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
