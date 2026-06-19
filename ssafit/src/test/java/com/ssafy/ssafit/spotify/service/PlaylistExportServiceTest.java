package com.ssafy.ssafit.spotify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
        exported.setSpotifyExportStatus(SpotifyExportStatus.SUCCESS);
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
        verify(playlistService).updateSpotifyExportStatus(
                10L,
                1L,
                SpotifyExportStatus.PENDING);
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
                ErrorCode.SPOTIFY_EXPORT_FAILED,
                () -> playlistExportService.export(10L, 1L));

        verify(playlistService).updateSpotifyExportStatus(
                10L,
                1L,
                SpotifyExportStatus.FAILED);
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
        when(playlistItemService.getSpotifyUris(10L, 1L)).thenReturn(List.of());

        assertError(
                ErrorCode.SPOTIFY_URI_NOT_FOUND,
                () -> playlistExportService.export(10L, 1L));
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
