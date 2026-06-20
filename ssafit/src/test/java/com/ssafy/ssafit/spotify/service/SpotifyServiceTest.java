package com.ssafy.ssafit.spotify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.SpotifyTrackMetadata;
import com.ssafy.ssafit.music.service.MusicService;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.domain.SpotifyTrack;

@ExtendWith(MockitoExtension.class)
class SpotifyServiceTest {

    @Mock
    private SpotifyApiClient spotifyApiClient;

    @Mock
    private MusicService musicService;

    private SpotifyService spotifyService;

    @BeforeEach
    void setUp() {
        spotifyService = new SpotifyService(spotifyApiClient, musicService);
    }

    @Test
    void searchTracksConvertsSpotifyMetadata() {
        when(spotifyApiClient.issueClientAccessToken()).thenReturn("client-token");
        when(spotifyApiClient.searchTracks("client-token", "running", 10))
                .thenReturn(List.of(new SpotifyTrack(
                        "track-id",
                        "spotify:track:track-id",
                        "https://open.spotify.com/track/track-id",
                        "Running Song",
                        "Runner",
                        "Running Album",
                        "https://image.test/cover.jpg")));

        List<SpotifyTrackMetadata> result = spotifyService.searchTracks(" running ", 10);

        assertThat(result).singleElement().satisfies(track -> {
            assertThat(track.getSpotifyTrackId()).isEqualTo("track-id");
            assertThat(track.getSpotifyUri()).isEqualTo("spotify:track:track-id");
            assertThat(track.getTitle()).isEqualTo("Running Song");
            assertThat(track.getBpm()).isNull();
        });
    }

    @Test
    void searchTracksReturnsEmptyListForEmptyResult() {
        when(spotifyApiClient.issueClientAccessToken()).thenReturn("client-token");
        when(spotifyApiClient.searchTracks("client-token", "missing", 10))
                .thenReturn(List.of());

        assertThat(spotifyService.searchTracks("missing", null)).isEmpty();
    }

    @Test
    void searchTracksConvertsMissingOptionalFieldsWithoutFailure() {
        when(spotifyApiClient.issueClientAccessToken()).thenReturn("client-token");
        when(spotifyApiClient.searchTracks("client-token", "partial", 10))
                .thenReturn(List.of(new SpotifyTrack(
                        "track-id",
                        "spotify:track:track-id",
                        null,
                        null,
                        null,
                        null,
                        null)));

        List<SpotifyTrackMetadata> result = spotifyService.searchTracks("partial", 10);

        assertThat(result).singleElement().satisfies(track -> {
            assertThat(track.getSpotifyTrackId()).isEqualTo("track-id");
            assertThat(track.getSpotifyUri()).isEqualTo("spotify:track:track-id");
            assertThat(track.getTitle()).isNull();
            assertThat(track.getArtist()).isNull();
            assertThat(track.getAlbumName()).isNull();
            assertThat(track.getSpotifyExternalUrl()).isNull();
            assertThat(track.getImageUrl()).isNull();
        });
    }

    @Test
    void searchTracksExcludesResultsWithoutTrackIdentity() {
        when(spotifyApiClient.issueClientAccessToken()).thenReturn("client-token");
        when(spotifyApiClient.searchTracks("client-token", "mixed", 10))
                .thenReturn(List.of(
                        new SpotifyTrack(
                                null,
                                "spotify:track:missing-id",
                                null,
                                "Missing ID",
                                "Artist",
                                null,
                                null),
                        new SpotifyTrack(
                                "missing-uri",
                                " ",
                                null,
                                "Missing URI",
                                "Artist",
                                null,
                                null),
                        new SpotifyTrack(
                                "valid-id",
                                "spotify:track:valid-id",
                                null,
                                "Valid",
                                "Artist",
                                null,
                                null)));

        List<SpotifyTrackMetadata> result = spotifyService.searchTracks("mixed", 10);

        assertThat(result).singleElement()
                .extracting(SpotifyTrackMetadata::getSpotifyTrackId)
                .isEqualTo("valid-id");
    }

    @Test
    void searchTracksNormalizesApiFailure() {
        when(spotifyApiClient.issueClientAccessToken())
                .thenThrow(new CustomException(ErrorCode.SPOTIFY_API_ERROR));

        assertError(
                ErrorCode.SPOTIFY_SEARCH_FAILED,
                () -> spotifyService.searchTracks("running", 10));
    }

    @Test
    void searchTracksCapsExcessiveLimit() {
        when(spotifyApiClient.issueClientAccessToken()).thenReturn("client-token");
        when(spotifyApiClient.searchTracks("client-token", "running", 10))
                .thenReturn(List.of());

        assertThat(spotifyService.searchTracks("running", 100)).isEmpty();

        verify(spotifyApiClient).searchTracks("client-token", "running", 10);
    }

    @Test
    void searchTracksRejectsBlankKeywordBeforeApiCall() {
        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> spotifyService.searchTracks("   ", 10));

        verifyNoInteractions(spotifyApiClient);
    }

    @Test
    void selectedTrackIsSavedThroughMusicService() {
        SpotifyTrackMetadata metadata = new SpotifyTrackMetadata();
        metadata.setSpotifyTrackId("track-id");
        Music music = new Music();
        music.setId(100L);
        when(musicService.saveSelectedSpotifyTrack(metadata)).thenReturn(music);

        assertThat(spotifyService.saveSelectedTrack(metadata)).isSameAs(music);
        verify(musicService).saveSelectedSpotifyTrack(metadata);
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
