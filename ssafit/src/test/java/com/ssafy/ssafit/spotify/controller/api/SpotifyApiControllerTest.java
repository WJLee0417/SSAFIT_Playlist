package com.ssafy.ssafit.spotify.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.common.exception.GlobalExceptionHandler;
import com.ssafy.ssafit.spotify.service.SpotifyOAuthService;
import com.ssafy.ssafit.spotify.service.SpotifyService;

@ExtendWith(MockitoExtension.class)
class SpotifyApiControllerTest {

    @Mock
    private SpotifyService spotifyService;

    @Mock
    private SpotifyOAuthService spotifyOAuthService;

    @Mock
    private SessionMemberResolver sessionMemberResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SpotifyApiController controller = new SpotifyApiController(
                spotifyService,
                spotifyOAuthService,
                sessionMemberResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingSpotifyConnectionIsAdaptedToConnectedFalse() throws Exception {
        when(sessionMemberResolver.requireMemberId(
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(10L);
        when(spotifyOAuthService.getConnection(10L))
                .thenThrow(new CustomException(ErrorCode.SPOTIFY_AUTH_REQUIRED));

        mockMvc.perform(get("/api/spotify/me/connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.connected").value(false))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }
}
