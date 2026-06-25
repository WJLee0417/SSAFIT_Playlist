package com.ssafy.ssafit.spotify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistExportTrack;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.service.PlaylistItemService;
import com.ssafy.ssafit.playlist.service.PlaylistService;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
import com.ssafy.ssafit.spotify.domain.SpotifyExportResult;
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

    private PlaylistExportService service;
    private SpotifyConnection connection;

    @BeforeEach
    void setUp() {
        service = new PlaylistExportService(
                playlistService,
                playlistItemService,
                spotifyOAuthService,
                spotifyApiClient);
        connection = new SpotifyConnection();
        connection.setAccessToken("access");
        org.mockito.Mockito.lenient()
                .when(spotifyOAuthService.getValidConnection(10L))
                .thenReturn(connection);
        org.mockito.Mockito.lenient()
                .when(spotifyOAuthService.executeWithValidConnection(
                        eq(10L),
                        any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<SpotifyConnection, Object> operation =
                            invocation.getArgument(1);
                    return operation.apply(connection);
                });
    }

    @Test
    void exportsEligibleTracksAndStoresSpotifyIdentity() {
        preparePlaylist();
        when(playlistItemService.getExportTracks(10L, 1L))
                .thenReturn(List.of(track(1L), track(2L)));
        prepareSpotifyPlaylist();
        Playlist success = exported(SpotifyExportStatus.SUCCESS);
        when(playlistService.saveSpotifyExportResult(
                10L, 1L, "spotify-playlist", spotifyUrl()))
                .thenReturn(success);

        SpotifyExportResult result = service.exportWithResult(10L, 1L);

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.exportedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.spotifyPlaylistId()).isEqualTo("spotify-playlist");
        verify(spotifyApiClient).addTracks(
                "access",
                "spotify-playlist",
                List.of("spotify:track:1", "spotify:track:2"));
    }

    @Test
    void rejectsExportBeforeStateChangeWhenSpotifyIsNotConnected() {
        when(playlistService.prepareExportTarget(10L, 1L)).thenReturn(playlist());
        when(spotifyOAuthService.getValidConnection(10L))
                .thenThrow(new CustomException(ErrorCode.SPOTIFY_NOT_CONNECTED));

        assertError(
                ErrorCode.SPOTIFY_NOT_CONNECTED,
                () -> service.exportWithResult(10L, 1L));

        verify(playlistService, never()).updateSpotifyExportStatus(
                any(), any(), any());
    }

    @Test
    void skipsTracksMissingSpotifyIdentityWithoutFailingExport() {
        preparePlaylist();
        when(playlistItemService.getExportTracks(10L, 1L))
                .thenReturn(List.of(
                        track(1L),
                        new PlaylistExportTrack(2L, null, null)));
        prepareSpotifyPlaylist();
        when(playlistService.saveSpotifyExportResult(
                10L, 1L, "spotify-playlist", spotifyUrl()))
                .thenReturn(exported(SpotifyExportStatus.SUCCESS));

        SpotifyExportResult result = service.exportWithResult(10L, 1L);

        assertThat(result.exportedCount()).isEqualTo(1);
        assertThat(result.skippedMusicIds()).containsExactly(2L);
        verify(spotifyApiClient).addTracks(
                "access",
                "spotify-playlist",
                List.of("spotify:track:1"));
    }

    @Test
    void chunksMoreThanOneHundredTracks() {
        preparePlaylist();
        List<PlaylistExportTrack> tracks = IntStream.rangeClosed(1, 101)
                .mapToObj(index -> track((long) index))
                .toList();
        when(playlistItemService.getExportTracks(10L, 1L)).thenReturn(tracks);
        prepareSpotifyPlaylist();
        when(playlistService.saveSpotifyExportResult(
                10L, 1L, "spotify-playlist", spotifyUrl()))
                .thenReturn(exported(SpotifyExportStatus.SUCCESS));

        SpotifyExportResult result = service.exportWithResult(10L, 1L);

        assertThat(result.exportedCount()).isEqualTo(101);
        verify(spotifyApiClient, times(2))
                .addTracks(eq("access"), eq("spotify-playlist"), any());
    }

    @Test
    void reportsFailedChunkAndPreservesSuccessfulChunk() {
        preparePlaylist();
        List<PlaylistExportTrack> tracks = IntStream.rangeClosed(1, 101)
                .mapToObj(index -> track((long) index))
                .toList();
        when(playlistItemService.getExportTracks(10L, 1L)).thenReturn(tracks);
        prepareSpotifyPlaylist();
        doAnswer(invocation -> {
            List<?> chunk = invocation.getArgument(2);
            if (chunk.size() == 1) {
                throw new CustomException(ErrorCode.SPOTIFY_RATE_LIMITED);
            }
            return null;
        }).when(spotifyApiClient).addTracks(
                eq("access"),
                eq("spotify-playlist"),
                any());
        when(playlistService.saveSpotifyExportPartial(
                10L,
                1L,
                "spotify-playlist",
                spotifyUrl(),
                ErrorCode.SPOTIFY_RATE_LIMITED))
                .thenReturn(exported(SpotifyExportStatus.PARTIAL));

        SpotifyExportResult result = service.exportWithResult(10L, 1L);

        assertThat(result.exportedCount()).isEqualTo(100);
        assertThat(result.failedChunkIndexes()).containsExactly(1);
        assertThat(result.spotifyExportStatus()).isEqualTo(SpotifyExportStatus.PARTIAL);
    }

    @Test
    void preventsOverwriteAfterPartialExport() {
        Playlist playlist = playlist();
        playlist.setSpotifyExportStatus(SpotifyExportStatus.PARTIAL);
        playlist.setSpotifyPlaylistId("existing");
        playlist.setSpotifyPlaylistUrl(spotifyUrl());
        when(playlistService.prepareExportTarget(10L, 1L)).thenReturn(playlist);

        assertError(
                ErrorCode.SPOTIFY_ALREADY_EXPORTED,
                () -> service.exportWithResult(10L, 1L));
    }

    private void preparePlaylist() {
        when(playlistService.prepareExportTarget(10L, 1L)).thenReturn(playlist());
        when(playlistService.saveSpotifyExportPending(
                10L, 1L, "spotify-playlist", spotifyUrl()))
                .thenReturn(exported(SpotifyExportStatus.PENDING));
    }

    private void prepareSpotifyPlaylist() {
        when(spotifyApiClient.createPlaylist(
                "access",
                "Running Playlist",
                "description",
                false))
                .thenReturn(new SpotifyPlaylistResult(
                        "spotify-playlist",
                        spotifyUrl()));
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

    private Playlist exported(SpotifyExportStatus status) {
        Playlist playlist = playlist();
        playlist.setSpotifyPlaylistId("spotify-playlist");
        playlist.setSpotifyPlaylistUrl(spotifyUrl());
        playlist.setSpotifyExportStatus(status);
        playlist.setSpotifyExportedAt(LocalDateTime.now());
        if (status == SpotifyExportStatus.PARTIAL) {
            playlist.setSpotifyExportErrorCode(ErrorCode.SPOTIFY_RATE_LIMITED.name());
        }
        return playlist;
    }

    private PlaylistExportTrack track(Long musicId) {
        return new PlaylistExportTrack(
                musicId,
                "track-" + musicId,
                "spotify:track:" + musicId);
    }

    private String spotifyUrl() {
        return "https://open.spotify.com/playlist/spotify-playlist";
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expected));
    }
}
