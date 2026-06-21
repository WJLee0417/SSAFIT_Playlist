package com.ssafy.ssafit.spotify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class PlaylistExportServiceTest {

    @Mock
    private PlaylistService playlistService;

    @Mock
    private PlaylistItemService playlistItemService;

    @Mock
    private SpotifyOAuthService spotifyOAuthService;

    @Mock
    private SpotifyApiClient spotifyApiClient;

    private PlaylistExportService playlistExportService;

    @BeforeEach
    void setUp() {
        playlistExportService = new PlaylistExportService(
                playlistService,
                playlistItemService,
                spotifyOAuthService,
                spotifyApiClient);
    }

    @Test
    void exportCreatesPlaylistAddsTracksAndStoresResult() {
        Playlist playlist = playlist();
        SpotifyConnection connection = connection();
        Playlist exported = playlist();
        exported.setSpotifyPlaylistId("spotify-playlist");
        exported.setSpotifyPlaylistUrl(
                "https://open.spotify.com/playlist/spotify-playlist");
        exported.setSpotifyExportStatus(SpotifyExportStatus.SUCCESS);
        exported.setSpotifyExportedAt(java.time.LocalDateTime.now());
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist);
        when(spotifyOAuthService.getValidConnection(10L)).thenReturn(connection);
        when(playlistItemService.getSpotifyUris(10L, 1L))
                .thenReturn(List.of("spotify:track:a", "spotify:track:b"));
        when(spotifyApiClient.createPlaylist(
                "access",
                "Running Playlist",
                "description",
                false))
                .thenReturn(new SpotifyPlaylistResult(
                        "spotify-playlist",
                        "https://open.spotify.com/playlist/spotify-playlist"));
        when(playlistService.saveSpotifyExportResult(
                10L,
                1L,
                "spotify-playlist",
                "https://open.spotify.com/playlist/spotify-playlist"))
                .thenReturn(exported);

        Playlist result = playlistExportService.export(10L, 1L);

        assertThat(result).isSameAs(exported);
        assertThat(result.getSpotifyPlaylistId()).isEqualTo("spotify-playlist");
        assertThat(result.getSpotifyPlaylistUrl())
                .isEqualTo("https://open.spotify.com/playlist/spotify-playlist");
        assertThat(result.getSpotifyExportStatus())
                .isEqualTo(SpotifyExportStatus.SUCCESS);
        assertThat(result.getSpotifyExportedAt()).isNotNull();
        verify(playlistService).updateSpotifyExportStatus(
                10L,
                1L,
                SpotifyExportStatus.PENDING);
        verify(playlistService).saveSpotifyExportPending(
                10L,
                1L,
                "spotify-playlist",
                "https://open.spotify.com/playlist/spotify-playlist");
        verify(spotifyApiClient).addTracks(
                "access",
                "spotify-playlist",
                List.of("spotify:track:a", "spotify:track:b"));
    }

    @Test
    void exportFailureStoresFailedStatus() {
        Playlist playlist = playlist();
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist);
        when(spotifyOAuthService.getValidConnection(10L)).thenReturn(connection());
        when(playlistItemService.getSpotifyUris(10L, 1L))
                .thenReturn(List.of("spotify:track:a"));
        when(spotifyApiClient.createPlaylist(
                "access",
                "Running Playlist",
                "description",
                false))
                .thenReturn(new SpotifyPlaylistResult("spotify-playlist", "url"));
        doThrow(new CustomException(ErrorCode.SPOTIFY_API_ERROR))
                .when(spotifyApiClient)
                .addTracks("access", "spotify-playlist", List.of("spotify:track:a"));

        assertError(
                ErrorCode.SPOTIFY_PLAYLIST_ITEM_ADD_FAILED,
                () -> playlistExportService.export(10L, 1L));

        verify(playlistService).saveSpotifyExportFailure(
                10L,
                1L,
                "spotify-playlist",
                "url",
                ErrorCode.SPOTIFY_PLAYLIST_ITEM_ADD_FAILED);
    }

    @Test
    void alreadyExportedPlaylistIsRejected() {
        Playlist playlist = playlist();
        playlist.setSpotifyExportStatus(SpotifyExportStatus.SUCCESS);
        playlist.setSpotifyPlaylistId("existing");
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist);

        assertError(
                ErrorCode.SPOTIFY_ALREADY_EXPORTED,
                () -> playlistExportService.export(10L, 1L));

        verify(spotifyOAuthService, never()).getValidConnection(10L);
    }

    @Test
    void exportRejectsPlaylistWithoutSpotifyUris() {
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist());
        when(spotifyOAuthService.getValidConnection(10L)).thenReturn(connection());
        when(playlistItemService.getSpotifyUris(10L, 1L))
                .thenThrow(new CustomException(ErrorCode.SPOTIFY_URI_NOT_FOUND));

        assertError(
                ErrorCode.SPOTIFY_URI_NOT_FOUND,
                () -> playlistExportService.export(10L, 1L));

        verify(playlistService, never()).updateSpotifyExportStatus(
                10L,
                1L,
                SpotifyExportStatus.PENDING);
    }

    @Test
    void ownerValidationFailureStopsBeforeSpotifyConnection() {
        when(playlistService.getExportTarget(20L, 1L))
                .thenThrow(new CustomException(ErrorCode.ACCESS_DENIED));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> playlistExportService.export(20L, 1L));

        verify(spotifyOAuthService, never()).getValidConnection(20L);
    }

    @Test
    void missingSpotifyConnectionStopsBeforePendingState() {
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist());
        when(spotifyOAuthService.getValidConnection(10L))
                .thenThrow(new CustomException(ErrorCode.SPOTIFY_AUTH_REQUIRED));

        assertError(
                ErrorCode.SPOTIFY_AUTH_REQUIRED,
                () -> playlistExportService.export(10L, 1L));

        verify(playlistService, never()).updateSpotifyExportStatus(
                10L,
                1L,
                SpotifyExportStatus.PENDING);
    }

    @Test
    void refreshedAccessTokenIsUsedForExport() {
        Playlist playlist = playlist();
        SpotifyConnection refreshed = connection();
        refreshed.setAccessToken("refreshed-access");
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist);
        when(spotifyOAuthService.getValidConnection(10L)).thenReturn(refreshed);
        when(playlistItemService.getSpotifyUris(10L, 1L))
                .thenReturn(List.of("spotify:track:a"));
        when(spotifyApiClient.createPlaylist(
                "refreshed-access",
                "Running Playlist",
                "description",
                false))
                .thenReturn(new SpotifyPlaylistResult("spotify-playlist", "url"));
        when(playlistService.saveSpotifyExportResult(
                10L, 1L, "spotify-playlist", "url"))
                .thenReturn(playlist);

        playlistExportService.export(10L, 1L);

        verify(spotifyApiClient).addTracks(
                "refreshed-access",
                "spotify-playlist",
                List.of("spotify:track:a"));
    }

    @Test
    void playlistCreationFailureStoresFailedWithoutExternalIdentity() {
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist());
        when(spotifyOAuthService.getValidConnection(10L)).thenReturn(connection());
        when(playlistItemService.getSpotifyUris(10L, 1L))
                .thenReturn(List.of("spotify:track:a"));
        when(spotifyApiClient.createPlaylist(
                "access",
                "Running Playlist",
                "description",
                false))
                .thenThrow(new CustomException(ErrorCode.SPOTIFY_API_ERROR));

        assertError(
                ErrorCode.SPOTIFY_PLAYLIST_CREATE_FAILED,
                () -> playlistExportService.export(10L, 1L));

        verify(playlistService).saveSpotifyExportFailure(
                10L,
                1L,
                null,
                null,
                ErrorCode.SPOTIFY_PLAYLIST_CREATE_FAILED);
        verify(spotifyApiClient, never()).addTracks(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tracksAreAddedInSpotifySafeBatches() {
        List<String> uris = java.util.stream.IntStream.range(0, 201)
                .mapToObj(index -> "spotify:track:" + index)
                .toList();
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist());
        when(spotifyOAuthService.getValidConnection(10L)).thenReturn(connection());
        when(playlistItemService.getSpotifyUris(10L, 1L)).thenReturn(uris);
        when(spotifyApiClient.createPlaylist(
                "access",
                "Running Playlist",
                "description",
                false))
                .thenReturn(new SpotifyPlaylistResult("spotify-playlist", "url"));
        when(playlistService.saveSpotifyExportResult(
                10L, 1L, "spotify-playlist", "url"))
                .thenReturn(playlist());

        playlistExportService.export(10L, 1L);

        verify(spotifyApiClient, times(3)).addTracks(
                org.mockito.ArgumentMatchers.eq("access"),
                org.mockito.ArgumentMatchers.eq("spotify-playlist"),
                org.mockito.ArgumentMatchers.anyList());
        verify(spotifyApiClient).addTracks(
                "access", "spotify-playlist", uris.subList(0, 100));
        verify(spotifyApiClient).addTracks(
                "access", "spotify-playlist", uris.subList(100, 200));
        verify(spotifyApiClient).addTracks(
                "access", "spotify-playlist", uris.subList(200, 201));
    }

    @Test
    void laterBatchFailurePreservesExternalIdentityAsFailed() {
        List<String> uris = java.util.stream.IntStream.range(0, 101)
                .mapToObj(index -> "spotify:track:" + index)
                .toList();
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist());
        when(spotifyOAuthService.getValidConnection(10L)).thenReturn(connection());
        when(playlistItemService.getSpotifyUris(10L, 1L)).thenReturn(uris);
        when(spotifyApiClient.createPlaylist(
                "access",
                "Running Playlist",
                "description",
                false))
                .thenReturn(new SpotifyPlaylistResult("spotify-playlist", "url"));
        doThrow(new CustomException(ErrorCode.SPOTIFY_API_ERROR))
                .when(spotifyApiClient)
                .addTracks("access", "spotify-playlist", uris.subList(100, 101));

        assertError(
                ErrorCode.SPOTIFY_PLAYLIST_ITEM_ADD_FAILED,
                () -> playlistExportService.export(10L, 1L));

        verify(spotifyApiClient).addTracks(
                "access", "spotify-playlist", uris.subList(0, 100));
        verify(playlistService).saveSpotifyExportFailure(
                10L,
                1L,
                "spotify-playlist",
                "url",
                ErrorCode.SPOTIFY_PLAYLIST_ITEM_ADD_FAILED);
    }

    @Test
    void failedExportWithExternalIdentityIsNotAutomaticallyRetried() {
        Playlist playlist = playlist();
        playlist.setSpotifyExportStatus(SpotifyExportStatus.FAILED);
        playlist.setSpotifyPlaylistId("partial-playlist");
        playlist.setSpotifyPlaylistUrl("url");
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist);

        assertError(
                ErrorCode.SPOTIFY_EXPORT_FAILED,
                () -> playlistExportService.export(10L, 1L));

        verify(spotifyOAuthService, never()).getValidConnection(10L);
    }

    @Test
    void failedExportWithoutExternalIdentityCanRetry() {
        Playlist playlist = playlist();
        playlist.setSpotifyExportStatus(SpotifyExportStatus.FAILED);
        when(playlistService.getExportTarget(10L, 1L)).thenReturn(playlist);
        when(spotifyOAuthService.getValidConnection(10L)).thenReturn(connection());
        when(playlistItemService.getSpotifyUris(10L, 1L))
                .thenReturn(List.of("spotify:track:a"));
        when(spotifyApiClient.createPlaylist(
                "access",
                "Running Playlist",
                "description",
                false))
                .thenReturn(new SpotifyPlaylistResult("spotify-playlist", "url"));
        when(playlistService.saveSpotifyExportResult(
                10L, 1L, "spotify-playlist", "url"))
                .thenReturn(playlist);

        playlistExportService.export(10L, 1L);

        verify(spotifyApiClient).createPlaylist(
                "access",
                "Running Playlist",
                "description",
                false);
    }

    private Playlist playlist() {
        Playlist playlist = new Playlist();
        playlist.setId(1L);
        playlist.setTitle("Running Playlist");
        playlist.setDescription("description");
        playlist.setVisibility(PlaylistVisibility.PRIVATE);
        playlist.setSpotifyExportStatus(SpotifyExportStatus.NOT_EXPORTED);
        return playlist;
    }

    private SpotifyConnection connection() {
        SpotifyConnection connection = new SpotifyConnection();
        connection.setAccessToken("access");
        connection.setSpotifyUserId("spotify-user");
        return connection;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
