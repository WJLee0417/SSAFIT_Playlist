package com.ssafy.ssafit.spotify.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistExportTrack;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.service.PlaylistItemService;
import com.ssafy.ssafit.playlist.service.PlaylistService;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.domain.SpotifyExportResult;
import com.ssafy.ssafit.spotify.domain.SpotifyPlaylistResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaylistExportService {

    private static final int SPOTIFY_TRACK_BATCH_SIZE = 100;

    private final PlaylistService playlistService;
    private final PlaylistItemService playlistItemService;
    private final SpotifyOAuthService spotifyOAuthService;
    private final SpotifyApiClient spotifyApiClient;

    public Playlist export(Long loginMemberId, Long playlistId) {
        SpotifyExportResult result = exportWithResult(loginMemberId, playlistId);
        Playlist playlist = new Playlist();
        playlist.setId(result.playlistId());
        playlist.setSpotifyPlaylistId(result.spotifyPlaylistId());
        playlist.setSpotifyPlaylistUrl(result.spotifyPlaylistUrl());
        playlist.setSpotifyExportStatus(result.spotifyExportStatus());
        playlist.setSpotifyExportErrorCode(result.spotifyExportErrorCode());
        playlist.setSpotifyExportedAt(result.exportedAt());
        return playlist;
    }

    public SpotifyExportResult exportWithResult(
            Long loginMemberId,
            Long playlistId) {
        Playlist playlist = playlistService.prepareExportTarget(
                loginMemberId,
                playlistId);
        validateExportable(playlist);
        Long exportPlaylistId = playlist.getId();

        // Connection validation happens before changing the playlist export state.
        spotifyOAuthService.getValidConnection(loginMemberId);
        List<PlaylistExportTrack> requestedTracks =
                playlistItemService.getExportTracks(loginMemberId, exportPlaylistId);
        List<Long> skippedMusicIds = requestedTracks.stream()
                .filter(track -> !track.exportable())
                .map(PlaylistExportTrack::getMusicId)
                .toList();
        List<String> exportableUris = requestedTracks.stream()
                .filter(PlaylistExportTrack::exportable)
                .map(PlaylistExportTrack::getSpotifyUri)
                .map(String::trim)
                .toList();

        playlistService.updateSpotifyExportStatus(
                loginMemberId,
                exportPlaylistId,
                SpotifyExportStatus.PENDING);

        SpotifyPlaylistResult spotifyPlaylist = null;
        try {
            spotifyPlaylist = spotifyOAuthService.executeWithValidConnection(
                    loginMemberId,
                    connection -> spotifyApiClient.createPlaylist(
                            connection.getAccessToken(),
                            playlist.getTitle(),
                            playlist.getDescription(),
                            playlist.getVisibility() == PlaylistVisibility.PUBLIC));
            validateSpotifyPlaylist(spotifyPlaylist);
            playlistService.saveSpotifyExportPending(
                    loginMemberId,
                    exportPlaylistId,
                    spotifyPlaylist.getId(),
                    spotifyPlaylist.getExternalUrl());
        } catch (RuntimeException exception) {
            ErrorCode errorCode = createErrorCode(exception);
            playlistService.saveSpotifyExportFailure(
                    loginMemberId,
                    exportPlaylistId,
                    spotifyPlaylist == null ? null : spotifyPlaylist.getId(),
                    spotifyPlaylist == null ? null : spotifyPlaylist.getExternalUrl(),
                    errorCode);
            throw new CustomException(errorCode);
        }

        ChunkExportResult chunkResult = exportChunks(
                loginMemberId,
                spotifyPlaylist.getId(),
                exportableUris);
        Playlist savedPlaylist;
        if (chunkResult.failedChunkIndexes().isEmpty()) {
            savedPlaylist = playlistService.saveSpotifyExportResult(
                    loginMemberId,
                    exportPlaylistId,
                    spotifyPlaylist.getId(),
                    spotifyPlaylist.getExternalUrl());
        } else if (chunkResult.exportedCount() > 0) {
            savedPlaylist = playlistService.saveSpotifyExportPartial(
                    loginMemberId,
                    exportPlaylistId,
                    spotifyPlaylist.getId(),
                    spotifyPlaylist.getExternalUrl(),
                    chunkResult.errorCode());
        } else {
            savedPlaylist = playlistService.saveSpotifyExportFailure(
                    loginMemberId,
                    exportPlaylistId,
                    spotifyPlaylist.getId(),
                    spotifyPlaylist.getExternalUrl(),
                    chunkResult.errorCode());
        }

        return new SpotifyExportResult(
                exportPlaylistId,
                requestedTracks.size(),
                chunkResult.exportedCount(),
                skippedMusicIds.size(),
                skippedMusicIds,
                chunkResult.failedChunkIndexes(),
                spotifyPlaylist.getId(),
                spotifyPlaylist.getExternalUrl(),
                savedPlaylist.getSpotifyExportStatus(),
                savedPlaylist.getSpotifyExportErrorCode(),
                savedPlaylist.getSpotifyExportedAt());
    }

    private ChunkExportResult exportChunks(
            Long loginMemberId,
            String spotifyPlaylistId,
            List<String> uris) {
        int exportedCount = 0;
        List<Integer> failedChunkIndexes = new ArrayList<>();
        ErrorCode firstErrorCode = null;
        int chunkIndex = 0;
        for (int start = 0; start < uris.size(); start += SPOTIFY_TRACK_BATCH_SIZE) {
            int end = Math.min(start + SPOTIFY_TRACK_BATCH_SIZE, uris.size());
            List<String> chunk = List.copyOf(uris.subList(start, end));
            try {
                spotifyOAuthService.executeWithValidConnection(
                        loginMemberId,
                        connection -> {
                            spotifyApiClient.addTracks(
                                    connection.getAccessToken(),
                                    spotifyPlaylistId,
                                    chunk);
                            return null;
                        });
                exportedCount += chunk.size();
            } catch (RuntimeException exception) {
                failedChunkIndexes.add(chunkIndex);
                if (firstErrorCode == null) {
                    firstErrorCode = chunkErrorCode(exception);
                }
            }
            chunkIndex++;
        }
        return new ChunkExportResult(
                exportedCount,
                failedChunkIndexes,
                firstErrorCode == null
                        ? ErrorCode.SPOTIFY_PLAYLIST_ITEM_ADD_FAILED
                        : firstErrorCode);
    }

    private void validateExportable(Playlist playlist) {
        if (playlist.getSpotifyExportStatus() == SpotifyExportStatus.SUCCESS
                || playlist.getSpotifyExportStatus() == SpotifyExportStatus.PARTIAL) {
            throw new CustomException(ErrorCode.SPOTIFY_ALREADY_EXPORTED);
        }
        if (playlist.getSpotifyExportStatus() == SpotifyExportStatus.PENDING
                || playlist.getSpotifyPlaylistId() != null
                || playlist.getSpotifyPlaylistUrl() != null) {
            throw new CustomException(ErrorCode.SPOTIFY_EXPORT_FAILED);
        }
    }

    private void validateSpotifyPlaylist(SpotifyPlaylistResult spotifyPlaylist) {
        if (spotifyPlaylist == null
                || spotifyPlaylist.getId() == null
                || spotifyPlaylist.getId().isBlank()
                || spotifyPlaylist.getExternalUrl() == null
                || spotifyPlaylist.getExternalUrl().isBlank()) {
            throw new CustomException(ErrorCode.SPOTIFY_PLAYLIST_CREATE_FAILED);
        }
    }

    private ErrorCode createErrorCode(RuntimeException exception) {
        ErrorCode errorCode = customErrorCode(exception);
        return switch (errorCode) {
            case SPOTIFY_NOT_CONNECTED,
                    SPOTIFY_TOKEN_REFRESH_FAILED,
                    SPOTIFY_FORBIDDEN,
                    SPOTIFY_RATE_LIMITED,
                    SPOTIFY_AUTHORIZATION_FAILED -> errorCode;
            default -> ErrorCode.SPOTIFY_PLAYLIST_CREATE_FAILED;
        };
    }

    private ErrorCode chunkErrorCode(RuntimeException exception) {
        ErrorCode errorCode = customErrorCode(exception);
        return switch (errorCode) {
            case SPOTIFY_TOKEN_REFRESH_FAILED,
                    SPOTIFY_FORBIDDEN,
                    SPOTIFY_RATE_LIMITED,
                    SPOTIFY_AUTHORIZATION_FAILED -> errorCode;
            default -> ErrorCode.SPOTIFY_PLAYLIST_ITEM_ADD_FAILED;
        };
    }

    private ErrorCode customErrorCode(RuntimeException exception) {
        return exception instanceof CustomException customException
                ? customException.getErrorCode()
                : ErrorCode.SPOTIFY_EXPORT_FAILED;
    }

    private record ChunkExportResult(
            int exportedCount,
            List<Integer> failedChunkIndexes,
            ErrorCode errorCode
    ) {
    }
}
