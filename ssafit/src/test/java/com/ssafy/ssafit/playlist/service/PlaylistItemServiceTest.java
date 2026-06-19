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
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.mapper.PlaylistItemMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;

@ExtendWith(MockitoExtension.class)
class PlaylistItemServiceTest {

    @Mock
    private PlaylistItemMapper playlistItemMapper;

    @Mock
    private PlaylistMapper playlistMapper;

    @Mock
    private MusicMapper musicMapper;

    private PlaylistItemService playlistItemService;

    @BeforeEach
    void setUp() {
        playlistItemService = new PlaylistItemService(
                playlistItemMapper,
                playlistMapper,
                musicMapper);
    }

    @Test
    void ownerCanAddMusic() {
        PlaylistItem item = item(null, 1L, 100L, 1);
        stubOwnedPlaylist();
        when(musicMapper.selectMusicDetail(100L)).thenReturn(Optional.of(seedMusic()));
        when(playlistItemMapper.existsByPlaylistAndMusic(1L, 100L)).thenReturn(false);
        when(playlistItemMapper.existsByPlaylistAndOrder(1L, 1, null)).thenReturn(false);
        when(playlistItemMapper.insert(item)).thenAnswer(invocation -> {
            item.setId(1000L);
            return 1;
        });

        PlaylistItem created = playlistItemService.add(10L, item);

        assertThat(created.getId()).isEqualTo(1000L);
        verify(playlistItemMapper).insert(item);
    }

    @Test
    void duplicateMusicIsRejected() {
        PlaylistItem item = item(null, 1L, 100L, 1);
        stubOwnedPlaylist();
        when(musicMapper.selectMusicDetail(100L)).thenReturn(Optional.of(seedMusic()));
        when(playlistItemMapper.existsByPlaylistAndMusic(1L, 100L)).thenReturn(true);

        assertError(
                ErrorCode.DUPLICATE_PLAYLIST_ITEM,
                () -> playlistItemService.add(10L, item));
        verify(playlistItemMapper, never()).insert(item);
    }

    @Test
    void duplicateOrderIsRejected() {
        PlaylistItem item = item(null, 1L, 100L, 1);
        stubOwnedPlaylist();
        when(musicMapper.selectMusicDetail(100L)).thenReturn(Optional.of(seedMusic()));
        when(playlistItemMapper.existsByPlaylistAndMusic(1L, 100L)).thenReturn(false);
        when(playlistItemMapper.existsByPlaylistAndOrder(1L, 1, null)).thenReturn(true);

        assertError(
                ErrorCode.DUPLICATE_PLAYLIST_ORDER,
                () -> playlistItemService.add(10L, item));
    }

    @Test
    void spotifyMusicWithoutUriIsRejected() {
        PlaylistItem item = item(null, 1L, 100L, 1);
        Music spotifyMusic = new Music();
        spotifyMusic.setSourceType(MusicSourceType.SPOTIFY);
        stubOwnedPlaylist();
        when(musicMapper.selectMusicDetail(100L)).thenReturn(Optional.of(spotifyMusic));

        assertError(
                ErrorCode.SPOTIFY_URI_NOT_FOUND,
                () -> playlistItemService.add(10L, item));
    }

    @Test
    void otherMemberCannotAddMusic() {
        PlaylistItem item = item(null, 1L, 100L, 1);
        when(playlistMapper.selectDetail(1L))
                .thenReturn(Optional.of(userPlaylist(1L, 10L, PlaylistVisibility.PRIVATE)));

        assertError(ErrorCode.ACCESS_DENIED, () -> playlistItemService.add(20L, item));
        verify(musicMapper, never()).selectMusicDetail(100L);
    }

    @Test
    void ownerCanDeleteItem() {
        PlaylistItem item = item(1000L, 1L, 100L, 1);
        when(playlistItemMapper.selectById(1000L)).thenReturn(Optional.of(item));
        stubOwnedPlaylist();

        playlistItemService.delete(10L, 1000L);

        verify(playlistItemMapper).delete(1000L);
    }

    @Test
    void ownerCanChangeOrder() {
        PlaylistItem item = item(1000L, 1L, 100L, 1);
        when(playlistItemMapper.selectById(1000L)).thenReturn(Optional.of(item));
        stubOwnedPlaylist();
        when(playlistItemMapper.existsByPlaylistAndOrder(1L, 2, 1000L))
                .thenReturn(false);

        PlaylistItem updated = playlistItemService.updateOrder(10L, 1000L, 2);

        assertThat(updated.getOrderNo()).isEqualTo(2);
        verify(playlistItemMapper).updateOrder(1000L, 2);
    }

    @Test
    void changingToOccupiedOrderIsRejected() {
        PlaylistItem item = item(1000L, 1L, 100L, 1);
        when(playlistItemMapper.selectById(1000L)).thenReturn(Optional.of(item));
        stubOwnedPlaylist();
        when(playlistItemMapper.existsByPlaylistAndOrder(1L, 2, 1000L))
                .thenReturn(true);

        assertError(
                ErrorCode.DUPLICATE_PLAYLIST_ORDER,
                () -> playlistItemService.updateOrder(10L, 1000L, 2));
    }

    @Test
    void privateItemsRejectOtherMember() {
        when(playlistMapper.selectDetail(1L))
                .thenReturn(Optional.of(userPlaylist(1L, 10L, PlaylistVisibility.PRIVATE)));

        assertError(
                ErrorCode.PRIVATE_PLAYLIST,
                () -> playlistItemService.getItems(20L, 1L));
    }

    @Test
    void publicItemsCanBeViewedByOtherMember() {
        Playlist playlist = userPlaylist(1L, 10L, PlaylistVisibility.PUBLIC);
        PlaylistItem item = item(1000L, 1L, 100L, 1);
        when(playlistMapper.selectDetail(1L)).thenReturn(Optional.of(playlist));
        when(playlistItemMapper.selectByPlaylistId(1L)).thenReturn(List.of(item));

        assertThat(playlistItemService.getItems(20L, 1L)).containsExactly(item);
    }

    @Test
    void spotifyUrisAreReturnedInMapperOrder() {
        stubOwnedPlaylist();
        when(playlistItemMapper.selectSpotifyUrisByPlaylistId(1L))
                .thenReturn(List.of("spotify:track:a", "spotify:track:b"));

        List<String> uris = playlistItemService.getSpotifyUris(10L, 1L);

        assertThat(uris).containsExactly("spotify:track:a", "spotify:track:b");
    }

    @Test
    void unauthenticatedMemberCannotMutateItems() {
        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> playlistItemService.delete(null, 1000L));
    }

    private void stubOwnedPlaylist() {
        when(playlistMapper.selectDetail(1L))
                .thenReturn(Optional.of(userPlaylist(1L, 10L, PlaylistVisibility.PRIVATE)));
    }

    private Playlist userPlaylist(Long id, Long ownerId, PlaylistVisibility visibility) {
        Playlist playlist = new Playlist();
        playlist.setId(id);
        playlist.setOwnerId(ownerId);
        playlist.setPlaylistType(PlaylistType.USER);
        playlist.setVisibility(visibility);
        return playlist;
    }

    private PlaylistItem item(
            Long id,
            Long playlistId,
            Long musicId,
            Integer orderNo) {
        PlaylistItem item = new PlaylistItem();
        item.setId(id);
        item.setPlaylistId(playlistId);
        item.setMusicId(musicId);
        item.setOrderNo(orderNo);
        return item;
    }

    private Music seedMusic() {
        Music music = new Music();
        music.setSourceType(MusicSourceType.SEED);
        return music;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
