package com.ssafy.ssafit.music.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.exception.GlobalExceptionHandler;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.service.MusicService;
import com.ssafy.ssafit.musicreview.service.MusicReviewService;
import com.ssafy.ssafit.spotify.service.SpotifyService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class MusicApiControllerTest {

    @Mock
    private MusicService musicService;

    @Mock
    private MusicReviewService musicReviewService;

    @Mock
    private SpotifyService spotifyService;

    @Mock
    private SessionMemberResolver sessionMemberResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MusicApiController controller = new MusicApiController(
                musicService,
                musicReviewService,
                spotifyService,
                sessionMemberResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void spotifyImportUsesSessionMemberAndReturnsSafeDto() throws Exception {
        Music music = new Music();
        music.setId(7L);
        music.setTitle("Run");
        music.setArtist("SSAFIT");
        music.setSourceType(MusicSourceType.SPOTIFY);
        music.setSpotifyTrackId("track-1");
        music.setSpotifyUri("spotify:track:track-1");

        when(sessionMemberResolver.requireMemberId(
                org.mockito.ArgumentMatchers.any(HttpSession.class)))
                .thenReturn(10L);
        when(spotifyService.saveSelectedTrack(
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(music);

        mockMvc.perform(post("/api/music/from-spotify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spotifyTrackId": "track-1",
                                  "spotifyUri": "spotify:track:track-1",
                                  "title": "Run",
                                  "artist": "SSAFIT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.spotifyTrackId").value("track-1"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void listQueryBuildsPageResponseFromSeparateServiceCalls() throws Exception {
        when(musicService.getMusicList(
                org.mockito.ArgumentMatchers.any(MusicSearchCondition.class)))
                .thenReturn(java.util.List.of());
        when(musicService.countMusicList(
                org.mockito.ArgumentMatchers.any(MusicSearchCondition.class)))
                .thenReturn(11L);

        mockMvc.perform(get("/api/music")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalCount").value(11))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }
}
