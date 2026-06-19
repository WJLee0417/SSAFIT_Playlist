package com.ssafy.ssafit.spotify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.config.SpotifyProperties;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
import com.ssafy.ssafit.spotify.domain.SpotifyToken;
import com.ssafy.ssafit.spotify.domain.SpotifyUserProfile;
import com.ssafy.ssafit.spotify.mapper.SpotifyConnectionMapper;

@ExtendWith(MockitoExtension.class)
class SpotifyOAuthServiceTest {

    @Mock
    private SpotifyApiClient spotifyApiClient;

    @Mock
    private SpotifyConnectionMapper spotifyConnectionMapper;

    private SpotifyProperties properties;
    private SpotifyOAuthService spotifyOAuthService;

    @BeforeEach
    void setUp() {
        properties = new SpotifyProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRedirectUri("http://localhost/callback");
        properties.setAuthorizationBaseUrl("https://accounts.spotify.com");
        properties.setScopes(List.of("user-read-private", "playlist-modify-private"));
        spotifyOAuthService = new SpotifyOAuthService(
                spotifyApiClient,
                spotifyConnectionMapper,
                properties);
    }

    @Test
    void authorizationUrlContainsConfiguredValues() {
        String url = spotifyOAuthService.buildAuthorizationUrl("csrf-state");

        assertThat(url)
                .contains("client_id=client-id")
                .contains("response_type=code")
                .contains("redirect_uri=http://localhost/callback")
                .contains("state=csrf-state");
    }

    @Test
    void callbackStoresTokenAndSpotifyUser() {
        when(spotifyApiClient.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(new SpotifyToken("access", "refresh", 3600));
        when(spotifyApiClient.getCurrentUser("access"))
                .thenReturn(new SpotifyUserProfile("spotify-user"));

        SpotifyConnection connection = spotifyOAuthService.handleCallback(
                10L,
                "authorization-code",
                null,
                "state",
                "state");

        assertThat(connection.getMemberId()).isEqualTo(10L);
        assertThat(connection.getSpotifyUserId()).isEqualTo("spotify-user");
        assertThat(connection.getAccessToken()).isEqualTo("access");
        assertThat(connection.getTokenExpiresAt()).isAfter(LocalDateTime.now());
        verify(spotifyConnectionMapper).upsert(connection);
    }

    @Test
    void callbackRejectsDeniedAuthorization() {
        assertError(
                ErrorCode.SPOTIFY_AUTH_DENIED,
                () -> spotifyOAuthService.handleCallback(
                        10L,
                        null,
                        "access_denied",
                        "state",
                        "state"));

        verify(spotifyApiClient, never())
                .exchangeAuthorizationCode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void callbackRejectsStateMismatch() {
        assertError(
                ErrorCode.SPOTIFY_AUTH_DENIED,
                () -> spotifyOAuthService.handleCallback(
                        10L,
                        "authorization-code",
                        null,
                        "expected",
                        "returned"));

        verify(spotifyApiClient, never())
                .exchangeAuthorizationCode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void expiredConnectionRefreshesAccessToken() {
        SpotifyConnection connection = connection(LocalDateTime.now().minusMinutes(1));
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(connection));
        when(spotifyApiClient.refreshAccessToken("old-refresh"))
                .thenReturn(new SpotifyToken("new-access", null, 3600));

        SpotifyConnection result = spotifyOAuthService.getValidConnection(10L);

        assertThat(result.getAccessToken()).isEqualTo("new-access");
        assertThat(result.getRefreshToken()).isEqualTo("old-refresh");
        verify(spotifyConnectionMapper).updateToken(connection);
    }

    @Test
    void missingConnectionRequiresSpotifyAuthentication() {
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.empty());

        assertError(
                ErrorCode.SPOTIFY_AUTH_REQUIRED,
                () -> spotifyOAuthService.getValidConnection(10L));
    }

    private SpotifyConnection connection(LocalDateTime expiresAt) {
        SpotifyConnection connection = new SpotifyConnection();
        connection.setMemberId(10L);
        connection.setSpotifyUserId("spotify-user");
        connection.setAccessToken("old-access");
        connection.setRefreshToken("old-refresh");
        connection.setTokenExpiresAt(expiresAt);
        return connection;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
