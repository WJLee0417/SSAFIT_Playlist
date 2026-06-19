package com.ssafy.ssafit.spotify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void searchTracksRejectsEmptyResult() {
        when(spotifyApiClient.issueClientAccessToken()).thenReturn("client-token");
        when(spotifyApiClient.searchTracks("client-token", "missing", 10))
                .thenReturn(List.of());

        assertError(
                ErrorCode.SPOTIFY_TRACK_NOT_FOUND,
                () -> spotifyService.searchTracks("missing", null));
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
