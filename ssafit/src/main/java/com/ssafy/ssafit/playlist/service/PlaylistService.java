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
import com.ssafy.ssafit.playlist.mapper.PlaylistItemMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private static final int MAX_TITLE_LENGTH = 200;

    private final PlaylistMapper playlistMapper;
    private final PlaylistItemMapper playlistItemMapper;

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
        playlist.setSpotifyExportErrorCode(null);
        playlist.setSpotifyExportedAt(null);
        playlist.setDeletedAt(null);
        if (playlistMapper.insert(playlist) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
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
        changes.setSpotifyExportErrorCode(existing.getSpotifyExportErrorCode());
        changes.setSpotifyExportedAt(existing.getSpotifyExportedAt());
        changes.setCreatedAt(existing.getCreatedAt());
        changes.setDeletedAt(existing.getDeletedAt());
        if (playlistMapper.update(changes) != 1) {
            throw new CustomException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        return changes;
    }

    @Transactional
    public void delete(Long loginMemberId, Long playlistId) {
        validateAuthenticated(loginMemberId);
        Playlist playlist = getPlaylist(playlistId);
        validateOwner(loginMemberId, playlist);
        if (playlistMapper.softDelete(playlistId) != 1) {
            throw new CustomException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
    }

    public Playlist getExportTarget(Long loginMemberId, Long playlistId) {
        validateAuthenticated(loginMemberId);
        Playlist playlist = playlistMapper.selectExportTargetPlaylist(playlistId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYLIST_NOT_FOUND));
        validateOwner(loginMemberId, playlist);
        return playlist;
    }

    @Transactional
    public Playlist prepareExportTarget(Long loginMemberId, Long playlistId) {
        validateAuthenticated(loginMemberId);
        Playlist playlist = getPlaylist(playlistId);
        validateViewAccess(loginMemberId, playlist);
        if (playlist.getPlaylistType() == PlaylistType.USER
                && playlist.getOwnerId() != null
                && loginMemberId.equals(playlist.getOwnerId())) {
            return playlist;
        }

        Playlist copy = new Playlist();
        copy.setOwnerId(loginMemberId);
        copy.setPlaylistType(PlaylistType.USER);
        copy.setSourceType(PlaylistSourceType.CURATION_COPY);
        copy.setVisibility(PlaylistVisibility.PRIVATE);
        copy.setTitle(playlist.getTitle());
        copy.setDescription(playlist.getDescription());
        copy.setSpotifyExportStatus(SpotifyExportStatus.NOT_EXPORTED);
        if (playlistMapper.insert(copy) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        playlistItemMapper.copyItems(playlist.getId(), copy.getId());
        return copy;
    }

    @Transactional
    public void updateSpotifyExportStatus(
            Long loginMemberId,
            Long playlistId,
            SpotifyExportStatus status) {
        Playlist playlist = getExportTarget(loginMemberId, playlistId);
        if (status == null
                || status == SpotifyExportStatus.SUCCESS
                || status == SpotifyExportStatus.PARTIAL
                || status == SpotifyExportStatus.FAILED) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        int updated = playlistMapper.updateSpotifyExportStatus(playlist.getId(), status);
        if (updated != 1) {
            throw new CustomException(ErrorCode.SPOTIFY_EXPORT_FAILED);
        }
    }

    @Transactional
    public Playlist saveSpotifyExportPending(
            Long loginMemberId,
            Long playlistId,
            String spotifyPlaylistId,
            String spotifyPlaylistUrl) {
        Playlist playlist = getExportTarget(loginMemberId, playlistId);
        validateSpotifyPlaylistIdentity(spotifyPlaylistId, spotifyPlaylistUrl);
        playlist.setSpotifyPlaylistId(spotifyPlaylistId.trim());
        playlist.setSpotifyPlaylistUrl(spotifyPlaylistUrl.trim());
        playlist.setSpotifyExportStatus(SpotifyExportStatus.PENDING);
        playlist.setSpotifyExportErrorCode(null);
        playlist.setSpotifyExportedAt(null);
        if (playlistMapper.updateSpotifyExportPending(playlist) != 1) {
            throw new CustomException(ErrorCode.SPOTIFY_EXPORT_FAILED);
        }
        return playlist;
    }

    @Transactional
    public Playlist saveSpotifyExportFailure(
            Long loginMemberId,
            Long playlistId,
            String spotifyPlaylistId,
            String spotifyPlaylistUrl,
            ErrorCode errorCode) {
        Playlist playlist = getExportTarget(loginMemberId, playlistId);
        playlist.setSpotifyPlaylistId(trimToNull(spotifyPlaylistId));
        playlist.setSpotifyPlaylistUrl(trimToNull(spotifyPlaylistUrl));
        playlist.setSpotifyExportStatus(SpotifyExportStatus.FAILED);
        playlist.setSpotifyExportErrorCode(
                errorCode == null
                        ? ErrorCode.SPOTIFY_EXPORT_FAILED.name()
                        : errorCode.name());
        playlist.setSpotifyExportedAt(null);
        if (playlistMapper.updateSpotifyExportFailure(playlist) != 1) {
            throw new CustomException(ErrorCode.SPOTIFY_EXPORT_FAILED);
        }
        return playlist;
    }

    @Transactional
    public Playlist saveSpotifyExportResult(
            Long loginMemberId,
            Long playlistId,
            String spotifyPlaylistId,
            String spotifyPlaylistUrl) {
        Playlist playlist = getExportTarget(loginMemberId, playlistId);
        validateSpotifyPlaylistIdentity(spotifyPlaylistId, spotifyPlaylistUrl);

        playlist.setSpotifyPlaylistId(spotifyPlaylistId.trim());
        playlist.setSpotifyPlaylistUrl(spotifyPlaylistUrl.trim());
        playlist.setSpotifyExportStatus(SpotifyExportStatus.SUCCESS);
        playlist.setSpotifyExportErrorCode(null);
        playlist.setSpotifyExportedAt(java.time.LocalDateTime.now());
        if (playlistMapper.updateSpotifyExportResult(playlist) != 1) {
            throw new CustomException(ErrorCode.SPOTIFY_EXPORT_FAILED);
        }
        return playlist;
    }

    @Transactional
    public Playlist saveSpotifyExportPartial(
            Long loginMemberId,
            Long playlistId,
            String spotifyPlaylistId,
            String spotifyPlaylistUrl,
            ErrorCode errorCode) {
        Playlist playlist = getExportTarget(loginMemberId, playlistId);
        validateSpotifyPlaylistIdentity(spotifyPlaylistId, spotifyPlaylistUrl);
        playlist.setSpotifyPlaylistId(spotifyPlaylistId.trim());
        playlist.setSpotifyPlaylistUrl(spotifyPlaylistUrl.trim());
        playlist.setSpotifyExportStatus(SpotifyExportStatus.PARTIAL);
        playlist.setSpotifyExportErrorCode(
                errorCode == null
                        ? ErrorCode.SPOTIFY_PLAYLIST_ITEM_ADD_FAILED.name()
                        : errorCode.name());
        playlist.setSpotifyExportedAt(java.time.LocalDateTime.now());
        if (playlistMapper.updateSpotifyExportPartial(playlist) != 1) {
            throw new CustomException(ErrorCode.SPOTIFY_EXPORT_FAILED);
        }
        return playlist;
    }

    private void validateSpotifyPlaylistIdentity(
            String spotifyPlaylistId,
            String spotifyPlaylistUrl) {
        if (isBlank(spotifyPlaylistId) || isBlank(spotifyPlaylistUrl)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
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
