package com.ssafy.ssafit.spotify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
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
import com.ssafy.ssafit.common.auth.PasswordHashService;
import com.ssafy.ssafit.member.mapper.MemberMapper;
import com.ssafy.ssafit.member.service.AuthService;
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

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PasswordHashService passwordHashService;

    @Mock
    private AuthService authService;

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
        lenient().when(spotifyConnectionMapper.selectBySpotifyUserId(any()))
                .thenReturn(Optional.empty());
        spotifyOAuthService = new SpotifyOAuthService(
                spotifyApiClient,
                spotifyConnectionMapper,
                memberMapper,
                passwordHashService,
                authService,
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
    void firstCallbackInsertsConnection() {
        String state = issueState();
        when(spotifyApiClient.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(new SpotifyToken("access", "refresh", 3600));
        when(spotifyApiClient.getCurrentUser("access"))
                .thenReturn(new SpotifyUserProfile("spotify-user"));
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.empty());
        when(spotifyConnectionMapper.insert(any())).thenReturn(1);

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
        verify(spotifyConnectionMapper).insert(connection);
        verify(spotifyConnectionMapper, never()).updateConnection(connection);
    }

    @Test
    void reconnectUpdatesConnectionAndKeepsExistingRefreshToken() {
        String state = issueState();
        SpotifyConnection existing = connection(LocalDateTime.now().minusMinutes(1));
        when(spotifyApiClient.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(new SpotifyToken("reconnected-access", null, 3600));
        when(spotifyApiClient.getCurrentUser("reconnected-access"))
                .thenReturn(new SpotifyUserProfile("spotify-user"));
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(existing));
        when(spotifyConnectionMapper.updateConnection(existing)).thenReturn(1);

        SpotifyConnection result = spotifyOAuthService.handleCallback(
                10L,
                "authorization-code",
                null,
                state,
                session);

        assertThat(result).isSameAs(existing);
        assertThat(result.getSpotifyUserId()).isEqualTo("spotify-user");
        assertThat(result.getAccessToken()).isEqualTo("reconnected-access");
        assertThat(result.getRefreshToken()).isEqualTo("old-refresh");
        verify(spotifyConnectionMapper).updateConnection(existing);
        verify(spotifyConnectionMapper, never()).insert(existing);
    }

    @Test
    void reconnectToDifferentAccountRequiresNewRefreshToken() {
        String state = issueState();
        SpotifyConnection existing = connection(LocalDateTime.now().minusMinutes(1));
        when(spotifyApiClient.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(new SpotifyToken("reconnected-access", null, 3600));
        when(spotifyApiClient.getCurrentUser("reconnected-access"))
                .thenReturn(new SpotifyUserProfile("different-user"));
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.SPOTIFY_AUTHORIZATION_FAILED,
                () -> spotifyOAuthService.handleCallback(
                        10L,
                        "authorization-code",
                        null,
                        state,
                        session));

        assertThat(existing.getSpotifyUserId()).isEqualTo("spotify-user");
        assertThat(existing.getAccessToken()).isEqualTo("old-access");
        assertThat(existing.getRefreshToken()).isEqualTo("old-refresh");
        verify(spotifyConnectionMapper, never()).updateConnection(existing);
    }

    @Test
    void callbackRejectsDeniedAuthorization() {
        String state = issueState();
        assertError(
                ErrorCode.SPOTIFY_AUTHORIZATION_CANCELLED,
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
                ErrorCode.SPOTIFY_STATE_MISMATCH,
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
                ErrorCode.SPOTIFY_AUTHORIZATION_FAILED,
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
        when(spotifyConnectionMapper.updateToken(connection)).thenReturn(1);

        SpotifyConnection result = spotifyOAuthService.getValidConnection(10L);

        assertThat(result.getAccessToken()).isEqualTo("new-access");
        assertThat(result.getRefreshToken()).isEqualTo("old-refresh");
        assertThat(result.getTokenExpiresAt()).isAfter(LocalDateTime.now());
        verify(spotifyConnectionMapper).updateToken(connection);
    }

    @Test
    void validConnectionReusesAccessTokenWithoutRefresh() {
        SpotifyConnection connection = connection(LocalDateTime.now().plusMinutes(10));
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(connection));

        SpotifyConnection result = spotifyOAuthService.getValidConnection(10L);

        assertThat(result).isSameAs(connection);
        verify(spotifyApiClient, never())
                .refreshAccessToken(org.mockito.ArgumentMatchers.any());
        verify(spotifyConnectionMapper, never())
                .updateToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingExpiresAtRequiresSpotifyAuthentication() {
        SpotifyConnection connection = connection(null);
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(connection));

        assertError(
                ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED,
                () -> spotifyOAuthService.getValidConnection(10L));

        verify(spotifyApiClient, never())
                .refreshAccessToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void expiredConnectionWithoutRefreshTokenFailsWithoutDbUpdate() {
        SpotifyConnection connection = connection(LocalDateTime.now().minusMinutes(1));
        connection.setRefreshToken(null);
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(connection));

        assertError(
                ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED,
                () -> spotifyOAuthService.getValidConnection(10L));

        verify(spotifyApiClient, never())
                .refreshAccessToken(org.mockito.ArgumentMatchers.any());
        verify(spotifyConnectionMapper, never())
                .updateToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refreshApiFailureKeepsStoredValuesAndDoesNotUpdateDb() {
        SpotifyConnection connection = connection(LocalDateTime.now().minusMinutes(1));
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(connection));
        when(spotifyApiClient.refreshAccessToken("old-refresh"))
                .thenThrow(new CustomException(ErrorCode.SPOTIFY_API_ERROR));

        assertError(
                ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED,
                () -> spotifyOAuthService.getValidConnection(10L));

        assertThat(connection.getAccessToken()).isEqualTo("old-access");
        assertThat(connection.getRefreshToken()).isEqualTo("old-refresh");
        verify(spotifyConnectionMapper, never()).updateToken(connection);
    }

    @Test
    void refreshedRefreshTokenIsSavedImmediately() {
        SpotifyConnection connection = connection(LocalDateTime.now().minusMinutes(1));
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(connection));
        when(spotifyApiClient.refreshAccessToken("old-refresh"))
                .thenReturn(new SpotifyToken("new-access", "new-refresh", 3600));
        when(spotifyConnectionMapper.updateToken(connection)).thenReturn(1);

        spotifyOAuthService.getValidConnection(10L);

        assertThat(connection.getAccessToken()).isEqualTo("new-access");
        assertThat(connection.getRefreshToken()).isEqualTo("new-refresh");
        verify(spotifyConnectionMapper, times(1)).updateToken(connection);
    }

    @Test
    void disconnectDeletesOnlyLoggedInMembersConnection() {
        when(spotifyConnectionMapper.deleteByMemberId(10L)).thenReturn(1);
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.empty());

        spotifyOAuthService.disconnect(10L);

        verify(spotifyConnectionMapper).deleteByMemberId(10L);
        assertError(
                ErrorCode.SPOTIFY_NOT_CONNECTED,
                () -> spotifyOAuthService.getConnection(10L));
    }

    @Test
    void disconnectWithoutConnectionRequiresSpotifyAuthentication() {
        when(spotifyConnectionMapper.deleteByMemberId(10L)).thenReturn(0);

        assertError(
                ErrorCode.SPOTIFY_NOT_CONNECTED,
                () -> spotifyOAuthService.disconnect(10L));
    }

    @Test
    void existingConnectionCanBeQueriedWithoutTokenRefresh() {
        SpotifyConnection connection = connection(LocalDateTime.now().plusMinutes(10));
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(connection));

        SpotifyConnection result = spotifyOAuthService.getConnection(10L);

        assertThat(result).isSameAs(connection);
        verify(spotifyApiClient, never())
                .refreshAccessToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingConnectionRequiresSpotifyAuthentication() {
        when(spotifyConnectionMapper.selectByMemberId(10L))
                .thenReturn(Optional.empty());

        assertError(
                ErrorCode.SPOTIFY_NOT_CONNECTED,
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
