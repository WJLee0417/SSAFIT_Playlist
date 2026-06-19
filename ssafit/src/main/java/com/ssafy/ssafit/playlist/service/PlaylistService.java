package com.ssafy.ssafit.playlist.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private static final int MAX_TITLE_LENGTH = 200;

    private final PlaylistMapper playlistMapper;

    @Transactional
    public Playlist create(Long loginMemberId, Playlist playlist) {
        validateAuthenticated(loginMemberId);
        validateTitle(playlist);

        playlist.setOwnerId(loginMemberId);
        playlist.setPlaylistType(PlaylistType.USER);
        playlist.setSourceType(PlaylistSourceType.MANUAL);
        playlist.setVisibility(defaultVisibility(playlist.getVisibility()));
        playlist.setSpotifyPlaylistId(null);
        playlist.setSpotifyPlaylistUrl(null);
        playlist.setSpotifyExportStatus(SpotifyExportStatus.NOT_EXPORTED);
        playlist.setSpotifyExportedAt(null);
        playlist.setDeletedAt(null);
        playlistMapper.insert(playlist);
        return playlist;
    }

    public List<Playlist> getList(Long viewerMemberId, Long ownerId) {
        return playlistMapper.selectList(viewerMemberId, ownerId);
    }

    public Playlist getDetail(Long viewerMemberId, Long playlistId) {
        Playlist playlist = getPlaylist(playlistId);
        validateViewAccess(viewerMemberId, playlist);
        return playlist;
    }

    @Transactional
    public Playlist update(Long loginMemberId, Playlist changes) {
        validateAuthenticated(loginMemberId);
        if (changes == null || changes.getId() == null) {
            throw new CustomException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        validateTitle(changes);

        Playlist existing = getPlaylist(changes.getId());
        validateOwner(loginMemberId, existing);

        changes.setOwnerId(existing.getOwnerId());
        changes.setPlaylistType(existing.getPlaylistType());
        changes.setSourceType(existing.getSourceType());
        changes.setVisibility(defaultVisibility(changes.getVisibility()));
        changes.setSpotifyPlaylistId(existing.getSpotifyPlaylistId());
        changes.setSpotifyPlaylistUrl(existing.getSpotifyPlaylistUrl());
        changes.setSpotifyExportStatus(existing.getSpotifyExportStatus());
        changes.setSpotifyExportedAt(existing.getSpotifyExportedAt());
        changes.setCreatedAt(existing.getCreatedAt());
        changes.setDeletedAt(existing.getDeletedAt());
        playlistMapper.update(changes);
        return changes;
    }

    @Transactional
    public void delete(Long loginMemberId, Long playlistId) {
        validateAuthenticated(loginMemberId);
        Playlist playlist = getPlaylist(playlistId);
        validateOwner(loginMemberId, playlist);
        playlistMapper.softDelete(playlistId);
    }

    public Playlist getExportTarget(Long loginMemberId, Long playlistId) {
        validateAuthenticated(loginMemberId);
        Playlist playlist = playlistMapper.selectExportTargetPlaylist(playlistId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYLIST_NOT_FOUND));
        validateOwner(loginMemberId, playlist);
        return playlist;
    }

    @Transactional
    public void updateSpotifyExportStatus(
            Long loginMemberId,
            Long playlistId,
            SpotifyExportStatus status) {
        Playlist playlist = getExportTarget(loginMemberId, playlistId);
        if (status == null || status == SpotifyExportStatus.SUCCESS) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        playlistMapper.updateSpotifyExportStatus(playlist.getId(), status);
    }

    @Transactional
    public Playlist saveSpotifyExportResult(
            Long loginMemberId,
            Long playlistId,
            String spotifyPlaylistId,
            String spotifyPlaylistUrl) {
        Playlist playlist = getExportTarget(loginMemberId, playlistId);
        if (isBlank(spotifyPlaylistId) || isBlank(spotifyPlaylistUrl)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        playlist.setSpotifyPlaylistId(spotifyPlaylistId.trim());
        playlist.setSpotifyPlaylistUrl(spotifyPlaylistUrl.trim());
        playlist.setSpotifyExportStatus(SpotifyExportStatus.SUCCESS);
        playlistMapper.updateSpotifyExportResult(playlist);
        return playlist;
    }

    private Playlist getPlaylist(Long playlistId) {
        if (playlistId == null) {
            throw new CustomException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        return playlistMapper.selectDetail(playlistId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYLIST_NOT_FOUND));
    }

    private void validateViewAccess(Long viewerMemberId, Playlist playlist) {
        if (playlist.getPlaylistType() == PlaylistType.OFFICIAL
                || playlist.getVisibility() == PlaylistVisibility.PUBLIC) {
            return;
        }
        if (viewerMemberId == null || !viewerMemberId.equals(playlist.getOwnerId())) {
            throw new CustomException(ErrorCode.PRIVATE_PLAYLIST);
        }
    }

    private void validateOwner(Long loginMemberId, Playlist playlist) {
        if (playlist.getPlaylistType() == PlaylistType.OFFICIAL
                || playlist.getOwnerId() == null
                || !loginMemberId.equals(playlist.getOwnerId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateAuthenticated(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
    }

    private void validateTitle(Playlist playlist) {
        if (playlist == null
                || isBlank(playlist.getTitle())
                || playlist.getTitle().trim().length() > MAX_TITLE_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_PLAYLIST_TITLE);
        }
        playlist.setTitle(playlist.getTitle().trim());
    }

    private PlaylistVisibility defaultVisibility(PlaylistVisibility visibility) {
        return visibility == null ? PlaylistVisibility.PRIVATE : visibility;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
