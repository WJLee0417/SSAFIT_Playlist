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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.util.UriComponentsBuilder;

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
    private SpotifyOAuthStateManager stateManager;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        properties = new SpotifyProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRedirectUri("http://localhost/callback");
        properties.setAuthorizationBaseUrl("https://accounts.spotify.com");
        properties.setScopes(List.of("user-read-private", "playlist-modify-private"));
        stateManager = new SpotifyOAuthStateManager();
        session = new MockHttpSession();
        spotifyOAuthService = new SpotifyOAuthService(
                spotifyApiClient,
                spotifyConnectionMapper,
                properties,
                stateManager);
    }

    @Test
    void authorizationUrlCreatesAndStoresState() {
        String url = spotifyOAuthService.buildAuthorizationUrl(session);
        String state = UriComponentsBuilder.fromUriString(url)
                .build()
                .getQueryParams()
                .getFirst("state");

        assertThat(url)
                .contains("client_id=client-id")
                .contains("response_type=code")
                .contains("redirect_uri=http://localhost/callback");
        assertThat(state).isNotBlank();
        assertThat(session.getAttribute(SpotifyOAuthStateManager.SESSION_ATTRIBUTE))
                .isEqualTo(state);
    }

    @Test
    void callbackValidatesStateConsumesItAndStoresConnection() {
        String state = issueState();
        when(spotifyApiClient.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(new SpotifyToken("access", "refresh", 3600));
        when(spotifyApiClient.getCurrentUser("access"))
                .thenReturn(new SpotifyUserProfile("spotify-user"));

        SpotifyConnection connection = spotifyOAuthService.handleCallback(
                10L,
                "authorization-code",
                null,
                state,
                session);

        assertThat(connection.getMemberId()).isEqualTo(10L);
        assertThat(connection.getSpotifyUserId()).isEqualTo("spotify-user");
        assertThat(connection.getAccessToken()).isEqualTo("access");
        assertThat(connection.getTokenExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(session.getAttribute(SpotifyOAuthStateManager.SESSION_ATTRIBUTE))
                .isNull();
        verify(spotifyConnectionMapper).upsert(connection);
    }

    @Test
    void callbackRejectsDeniedAuthorization() {
        String state = issueState();
        assertError(
                ErrorCode.SPOTIFY_AUTH_DENIED,
                () -> spotifyOAuthService.handleCallback(
                        10L,
                        null,
                        "access_denied",
                        state,
                        session));

        verify(spotifyApiClient, never())
                .exchangeAuthorizationCode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void callbackRejectsStateMismatch() {
        issueState();
        assertError(
                ErrorCode.SPOTIFY_OAUTH_STATE_MISMATCH,
                () -> spotifyOAuthService.handleCallback(
                        10L,
                        "authorization-code",
                        null,
                        "different-state",
                        session));

        assertThat(session.getAttribute(SpotifyOAuthStateManager.SESSION_ATTRIBUTE))
                .isNull();
        verify(spotifyApiClient, never())
                .exchangeAuthorizationCode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void callbackRejectsSpotifyErrorResponse() {
        String state = issueState();

        assertError(
                ErrorCode.SPOTIFY_OAUTH_CALLBACK_ERROR,
                () -> spotifyOAuthService.handleCallback(
                        10L,
                        null,
                        "temporarily_unavailable",
                        state,
                        session));
    }

    @Test
    void missingConfigurationDoesNotExposeSensitiveValues() {
        properties.setClientSecret("");

        assertThatThrownBy(() -> spotifyOAuthService.buildAuthorizationUrl(session))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.SPOTIFY_CONFIGURATION_ERROR);
                            assertThat(exception.getMessage())
                                    .doesNotContain("client-id")
                                    .doesNotContain("client-secret")
                                    .doesNotContain("http://localhost/callback");
                        });
    }

    @Test
    void authorizationUrlFailureUsesSafeErrorAndDiscardsState() {
        properties.setScopes(null);

        assertError(
                ErrorCode.SPOTIFY_AUTH_URL_ERROR,
                () -> spotifyOAuthService.buildAuthorizationUrl(session));

        assertThat(session.getAttribute(SpotifyOAuthStateManager.SESSION_ATTRIBUTE))
                .isNull();
    }

    @Test
    void stateFailureDoesNotExposeAuthorizationCodeOrState() {
        issueState();

        assertThatThrownBy(() -> spotifyOAuthService.handleCallback(
                10L,
                "sensitive-authorization-code",
                null,
                "sensitive-returned-state",
                session))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getMessage())
                                .doesNotContain("sensitive-authorization-code")
                                .doesNotContain("sensitive-returned-state")
                                .doesNotContain("client-secret"));
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

    private String issueState() {
        spotifyOAuthService.buildAuthorizationUrl(session);
        return (String) session.getAttribute(SpotifyOAuthStateManager.SESSION_ATTRIBUTE);
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
