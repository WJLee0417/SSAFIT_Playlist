package com.ssafy.ssafit.playlist.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.mapper.PlaylistItemMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoritePlaylistService {

    public static final String FAVORITE_PLAYLIST_TITLE = "좋아요한 노래";
    private static final String FAVORITE_PLAYLIST_DESCRIPTION =
            "좋아요를 누른 노래가 자동으로 모이는 플레이리스트";

    private final PlaylistMapper playlistMapper;
    private final PlaylistItemMapper playlistItemMapper;

    @Transactional
    public Playlist ensureForMember(Long memberId) {
        validateAuthenticated(memberId);
        return playlistMapper.selectFavoriteByOwner(memberId)
                .orElseGet(() -> createFavoritePlaylist(memberId));
    }

    @Transactional
    public void syncLikedMusic(Long memberId, Long musicId, boolean liked) {
        validateAuthenticated(memberId);
        if (musicId == null) {
            throw new CustomException(ErrorCode.MUSIC_NOT_FOUND);
        }
        if (liked) {
            addLikedMusic(memberId, musicId);
        } else {
            removeLikedMusic(memberId, musicId);
        }
    }

    private void addLikedMusic(Long memberId, Long musicId) {
        Playlist playlist = ensureForMember(memberId);
        if (playlistItemMapper.existsByPlaylistAndMusic(playlist.getId(), musicId)) {
            return;
        }

        PlaylistItem item = new PlaylistItem();
        item.setPlaylistId(playlist.getId());
        item.setMusicId(musicId);
        item.setOrderNo(playlistItemMapper.selectNextOrderNo(playlist.getId()));
        try {
            playlistItemMapper.insert(item);
        } catch (DuplicateKeyException exception) {
            retryIfOrderConflict(item);
        }
    }

    private void retryIfOrderConflict(PlaylistItem item) {
        if (playlistItemMapper.existsByPlaylistAndMusic(
                item.getPlaylistId(),
                item.getMusicId())) {
            return;
        }
        item.setOrderNo(playlistItemMapper.selectNextOrderNo(item.getPlaylistId()));
        try {
            playlistItemMapper.insert(item);
        } catch (DuplicateKeyException ignored) {
            // 같은 좋아요 이벤트가 동시에 처리된 경우에는 최종 동기화 시 다시 보정된다.
        }
    }

    private void removeLikedMusic(Long memberId, Long musicId) {
        playlistMapper.selectFavoriteByOwner(memberId)
                .ifPresent(playlist -> playlistItemMapper.deleteByPlaylistAndMusic(
                        playlist.getId(),
                        musicId));
    }

    private Playlist createFavoritePlaylist(Long memberId) {
        Playlist playlist = new Playlist();
        playlist.setOwnerId(memberId);
        playlist.setPlaylistType(PlaylistType.USER);
        playlist.setSourceType(PlaylistSourceType.MANUAL);
        playlist.setVisibility(PlaylistVisibility.PRIVATE);
        playlist.setTitle(FAVORITE_PLAYLIST_TITLE);
        playlist.setDescription(FAVORITE_PLAYLIST_DESCRIPTION);
        playlist.setSpotifyExportStatus(SpotifyExportStatus.NOT_EXPORTED);
        if (playlistMapper.insert(playlist) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return playlist;
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
    }
}
