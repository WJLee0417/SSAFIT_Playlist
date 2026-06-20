package com.ssafy.ssafit.spotify.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.service.PlaylistItemService;
import com.ssafy.ssafit.playlist.service.PlaylistService;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
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
        Playlist playlist = playlistService.getExportTarget(loginMemberId, playlistId);
        validateExportable(playlist);
        SpotifyConnection connection = spotifyOAuthService.getValidConnection(loginMemberId);
        List<String> uris = playlistItemService.getSpotifyUris(loginMemberId, playlistId);

        playlistService.updateSpotifyExportStatus(
                loginMemberId,
                playlistId,
                SpotifyExportStatus.PENDING);
        SpotifyPlaylistResult spotifyPlaylist = null;
        try {
            spotifyPlaylist = spotifyApiClient.createPlaylist(
                    connection.getAccessToken(),
                    playlist.getTitle(),
                    playlist.getDescription(),
                    playlist.getVisibility() == PlaylistVisibility.PUBLIC);
            validateSpotifyPlaylist(spotifyPlaylist);
            playlistService.saveSpotifyExportPending(
                    loginMemberId,
                    playlistId,
                    spotifyPlaylist.getId(),
                    spotifyPlaylist.getExternalUrl());
            addTracksInBatches(
                    connection.getAccessToken(),
                    spotifyPlaylist.getId(),
                    uris);
            return playlistService.saveSpotifyExportResult(
                    loginMemberId,
                    playlistId,
                    spotifyPlaylist.getId(),
                    spotifyPlaylist.getExternalUrl());
        } catch (RuntimeException exception) {
            playlistService.saveSpotifyExportFailure(
                    loginMemberId,
                    playlistId,
                    spotifyPlaylist == null ? null : spotifyPlaylist.getId(),
                    spotifyPlaylist == null ? null : spotifyPlaylist.getExternalUrl(),
                    ErrorCode.SPOTIFY_EXPORT_FAILED);
            throw new CustomException(ErrorCode.SPOTIFY_EXPORT_FAILED);
        }
    }

    private void addTracksInBatches(
            String accessToken,
            String spotifyPlaylistId,
            List<String> uris) {
        for (int start = 0; start < uris.size(); start += SPOTIFY_TRACK_BATCH_SIZE) {
            int end = Math.min(start + SPOTIFY_TRACK_BATCH_SIZE, uris.size());
            spotifyApiClient.addTracks(
                    accessToken,
                    spotifyPlaylistId,
                    uris.subList(start, end));
        }
    }

    private void validateExportable(Playlist playlist) {
        if (playlist.getSpotifyExportStatus() == SpotifyExportStatus.SUCCESS) {
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
            throw new CustomException(ErrorCode.SPOTIFY_EXPORT_FAILED);
        }
    }
}
