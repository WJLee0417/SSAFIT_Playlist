package com.ssafy.ssafit.playlist.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.mapper.PlaylistItemMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistItemService {

    private final PlaylistItemMapper playlistItemMapper;
    private final PlaylistMapper playlistMapper;
    private final MusicMapper musicMapper;

    @Transactional
    public PlaylistItem add(Long loginMemberId, PlaylistItem playlistItem) {
        validateAuthenticated(loginMemberId);
        validateItemInput(playlistItem);
        validateOwnedPlaylist(loginMemberId, playlistItem.getPlaylistId());

        Music music = musicMapper.selectMusicDetail(playlistItem.getMusicId())
                .orElseThrow(() -> new CustomException(ErrorCode.MUSIC_NOT_FOUND));
        validateSpotifyMusicUri(music);
        validateNoDuplicate(
                playlistItem.getPlaylistId(),
                playlistItem.getMusicId(),
                playlistItem.getOrderNo(),
                null);

        try {
            playlistItemMapper.insert(playlistItem);
        } catch (DuplicateKeyException exception) {
            throw resolveDuplicateError(playlistItem, null);
        }
        return playlistItem;
    }

    @Transactional
    public void delete(Long loginMemberId, Long playlistItemId) {
        validateAuthenticated(loginMemberId);
        PlaylistItem playlistItem = getItem(playlistItemId);
        validateOwnedPlaylist(loginMemberId, playlistItem.getPlaylistId());
        playlistItemMapper.delete(playlistItemId);
    }

    @Transactional
    public PlaylistItem updateOrder(
            Long loginMemberId,
            Long playlistItemId,
            Integer orderNo) {
        validateAuthenticated(loginMemberId);
        validateOrder(orderNo);

        PlaylistItem playlistItem = getItem(playlistItemId);
        validateOwnedPlaylist(loginMemberId, playlistItem.getPlaylistId());
        if (playlistItemMapper.existsByPlaylistAndOrder(
                playlistItem.getPlaylistId(),
                orderNo,
                playlistItemId)) {
            throw new CustomException(ErrorCode.DUPLICATE_PLAYLIST_ORDER);
        }

        try {
            playlistItemMapper.updateOrder(playlistItemId, orderNo);
        } catch (DuplicateKeyException exception) {
            throw new CustomException(ErrorCode.DUPLICATE_PLAYLIST_ORDER);
        }
        playlistItem.setOrderNo(orderNo);
        return playlistItem;
    }

    public List<PlaylistItem> getItems(Long viewerMemberId, Long playlistId) {
        Playlist playlist = getPlaylist(playlistId);
        validateViewAccess(viewerMemberId, playlist);
        return playlistItemMapper.selectByPlaylistId(playlistId);
    }

    public List<String> getSpotifyUris(Long loginMemberId, Long playlistId) {
        validateAuthenticated(loginMemberId);
        validateOwnedPlaylist(loginMemberId, playlistId);
        int itemCount = playlistItemMapper.countByPlaylistId(playlistId);
        int spotifyUriCount =
                playlistItemMapper.countSpotifyUrisByPlaylistId(playlistId);
        if (itemCount == 0 || itemCount != spotifyUriCount) {
            throw new CustomException(ErrorCode.SPOTIFY_URI_NOT_FOUND);
        }
        return playlistItemMapper.selectSpotifyUrisByPlaylistId(playlistId);
    }

    private void validateNoDuplicate(
            Long playlistId,
            Long musicId,
            Integer orderNo,
            Long excludeItemId) {
        if (playlistItemMapper.existsByPlaylistAndMusic(playlistId, musicId)) {
            throw new CustomException(ErrorCode.DUPLICATE_PLAYLIST_ITEM);
        }
        if (playlistItemMapper.existsByPlaylistAndOrder(
                playlistId,
                orderNo,
                excludeItemId)) {
            throw new CustomException(ErrorCode.DUPLICATE_PLAYLIST_ORDER);
        }
    }

    private CustomException resolveDuplicateError(
            PlaylistItem playlistItem,
            Long excludeItemId) {
        if (playlistItemMapper.existsByPlaylistAndOrder(
                playlistItem.getPlaylistId(),
                playlistItem.getOrderNo(),
                excludeItemId)) {
            return new CustomException(ErrorCode.DUPLICATE_PLAYLIST_ORDER);
        }
        return new CustomException(ErrorCode.DUPLICATE_PLAYLIST_ITEM);
    }

    private PlaylistItem getItem(Long playlistItemId) {
        if (playlistItemId == null) {
            throw new CustomException(ErrorCode.PLAYLIST_ITEM_NOT_FOUND);
        }
        return playlistItemMapper.selectById(playlistItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYLIST_ITEM_NOT_FOUND));
    }

    private Playlist getPlaylist(Long playlistId) {
        if (playlistId == null) {
            throw new CustomException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        return playlistMapper.selectDetail(playlistId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYLIST_NOT_FOUND));
    }

    private Playlist validateOwnedPlaylist(Long loginMemberId, Long playlistId) {
        Playlist playlist = getPlaylist(playlistId);
        if (playlist.getPlaylistType() == PlaylistType.OFFICIAL
                || playlist.getOwnerId() == null
                || !loginMemberId.equals(playlist.getOwnerId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        return playlist;
    }

    private void validateViewAccess(Long viewerMemberId, Playlist playlist) {
        if (playlist.getPlaylistType() == PlaylistType.OFFICIAL
                || playlist.getVisibility()
                        == com.ssafy.ssafit.playlist.domain.PlaylistVisibility.PUBLIC) {
            return;
        }
        if (viewerMemberId == null || !viewerMemberId.equals(playlist.getOwnerId())) {
            throw new CustomException(ErrorCode.PRIVATE_PLAYLIST);
        }
    }

    private void validateItemInput(PlaylistItem playlistItem) {
        if (playlistItem == null || playlistItem.getPlaylistId() == null) {
            throw new CustomException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        if (playlistItem.getMusicId() == null) {
            throw new CustomException(ErrorCode.MUSIC_NOT_FOUND);
        }
        validateOrder(playlistItem.getOrderNo());
    }

    private void validateOrder(Integer orderNo) {
        if (orderNo == null || orderNo < 1) {
            throw new CustomException(ErrorCode.INVALID_PLAYLIST_ORDER);
        }
    }

    private void validateSpotifyMusicUri(Music music) {
        if (music.getSourceType() == MusicSourceType.SPOTIFY
                && (music.getSpotifyUri() == null || music.getSpotifyUri().isBlank())) {
            throw new CustomException(ErrorCode.SPOTIFY_URI_NOT_FOUND);
        }
    }

    private void validateAuthenticated(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
    }
}
