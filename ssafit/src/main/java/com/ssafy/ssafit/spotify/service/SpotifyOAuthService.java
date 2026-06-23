package com.ssafy.ssafit.spotify.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.config.SpotifyProperties;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
import com.ssafy.ssafit.spotify.domain.SpotifyToken;
import com.ssafy.ssafit.spotify.domain.SpotifyUserProfile;
import com.ssafy.ssafit.spotify.mapper.SpotifyConnectionMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotifyOAuthService {

    private static final long TOKEN_EXPIRY_SAFETY_SECONDS = 30;

    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyConnectionMapper spotifyConnectionMapper;
    private final SpotifyProperties properties;
    private final SpotifyOAuthStateManager stateManager;

    public String buildAuthorizationUrl(HttpSession session) {
        validateConfiguration();
        String state = stateManager.createAndStore(session);
        try {
            return UriComponentsBuilder
                    .fromUriString(properties.getAuthorizationBaseUrl())
                    .path("/authorize")
                    .queryParam("client_id", properties.getClientId())
                    .queryParam("response_type", "code")
                    .queryParam("redirect_uri", properties.getRedirectUri())
                    .queryParam("scope", String.join(" ", properties.getScopes()))
                    .queryParam("state", state)
                    .build()
                    .encode()
                    .toUriString();
        } catch (RuntimeException exception) {
            stateManager.discard(session);
            throw new CustomException(ErrorCode.SPOTIFY_AUTH_URL_ERROR);
        }
    }

    @Transactional
    public SpotifyConnection handleCallback(
            Long loginMemberId,
            String authorizationCode,
            String error,
            String returnedState,
            HttpSession session) {
        validateAuthenticated(loginMemberId);
        stateManager.validateAndConsume(session, returnedState);
        if (StringUtils.hasText(error)) {
            if ("access_denied".equals(error)) {
                throw new CustomException(
                        ErrorCode.SPOTIFY_AUTHORIZATION_CANCELLED);
            }
            throw new CustomException(ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
        }
        if (!StringUtils.hasText(authorizationCode)) {
            throw new CustomException(ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
        }

        SpotifyToken token;
        try {
            token = spotifyApiClient.exchangeAuthorizationCode(
                    authorizationCode.trim());
            validateToken(token);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
        }
        CallbackIdentity callbackIdentity =
                resolveCallbackIdentityWithSingleRetry(token);
        token = callbackIdentity.token();
        SpotifyUserProfile profile = callbackIdentity.profile();
        if (profile == null || !StringUtils.hasText(profile.getId())) {
            throw new CustomException(ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
        }

        return saveConnection(loginMemberId, profile.getId(), token);
    }

    private CallbackIdentity resolveCallbackIdentityWithSingleRetry(
            SpotifyToken token) {
        try {
            return new CallbackIdentity(
                    token,
                    spotifyApiClient.getCurrentUser(token.getAccessToken()));
        } catch (CustomException exception) {
            if (exception.getErrorCode()
                    != ErrorCode.SPOTIFY_AUTHORIZATION_FAILED) {
                throw exception;
            }
        }

        if (!StringUtils.hasText(token.getRefreshToken())) {
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
        }

        SpotifyToken refreshed;
        try {
            refreshed = spotifyApiClient.refreshAccessToken(
                    token.getRefreshToken());
            validateToken(refreshed);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
        }
        if (!StringUtils.hasText(refreshed.getRefreshToken())) {
            refreshed.setRefreshToken(token.getRefreshToken());
        }

        try {
            return new CallbackIdentity(
                    refreshed,
                    spotifyApiClient.getCurrentUser(
                            refreshed.getAccessToken()));
        } catch (CustomException exception) {
            if (exception.getErrorCode()
                    == ErrorCode.SPOTIFY_AUTHORIZATION_FAILED) {
                throw new CustomException(
                        ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
            }
            throw exception;
        }
    }

    public SpotifyConnection getConnection(Long loginMemberId) {
        validateAuthenticated(loginMemberId);
        return spotifyConnectionMapper.selectByMemberId(loginMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPOTIFY_NOT_CONNECTED));
    }

    @Transactional
    public SpotifyConnection getValidConnection(Long loginMemberId) {
        SpotifyConnection connection = getConnection(loginMemberId);
        if (connection.getTokenExpiresAt() == null) {
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
        }
        if (StringUtils.hasText(connection.getAccessToken())
                && connection.getTokenExpiresAt()
                        .isAfter(LocalDateTime.now().plusSeconds(TOKEN_EXPIRY_SAFETY_SECONDS))) {
            return connection;
        }
        return refreshConnection(connection);
    }

    /**
     * 사용자 토큰이 필요한 Spotify API를 실행한다.
     * 첫 호출이 401 성격의 인증 실패이면 refresh 후 동일 작업을 정확히 한 번만 재시도한다.
     */
    @Transactional
    public <T> T executeWithValidConnection(
            Long loginMemberId,
            Function<SpotifyConnection, T> operation) {
        SpotifyConnection connection = getValidConnection(loginMemberId);
        try {
            return operation.apply(connection);
        } catch (CustomException exception) {
            if (exception.getErrorCode()
                    != ErrorCode.SPOTIFY_AUTHORIZATION_FAILED) {
                throw exception;
            }
        }

        SpotifyConnection refreshed = refreshConnection(connection);
        try {
            return operation.apply(refreshed);
        } catch (CustomException exception) {
            if (exception.getErrorCode()
                    == ErrorCode.SPOTIFY_AUTHORIZATION_FAILED) {
                throw new CustomException(
                        ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
            }
            throw exception;
        }
    }

    @Transactional
    public void disconnect(Long loginMemberId) {
        validateAuthenticated(loginMemberId);
        if (spotifyConnectionMapper.deleteByMemberId(loginMemberId) != 1) {
            throw new CustomException(ErrorCode.SPOTIFY_NOT_CONNECTED);
        }
    }

    private SpotifyConnection saveConnection(
            Long loginMemberId,
            String spotifyUserId,
            SpotifyToken token) {
        Optional<SpotifyConnection> existing =
                spotifyConnectionMapper.selectByMemberId(loginMemberId);
        if (existing.isEmpty()) {
            if (!StringUtils.hasText(token.getRefreshToken())) {
                throw new CustomException(
                        ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
            }
            SpotifyConnection connection = connection(
                    loginMemberId,
                    spotifyUserId,
                    token.getAccessToken(),
                    token.getRefreshToken(),
                    expiresAt(token));
            if (spotifyConnectionMapper.insert(connection) != 1) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            return connection;
        }

        SpotifyConnection connection = existing.get();
        boolean accountChanged = !spotifyUserId.equals(connection.getSpotifyUserId());
        if (accountChanged && !StringUtils.hasText(token.getRefreshToken())) {
            throw new CustomException(ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
        }
        connection.setSpotifyUserId(spotifyUserId);
        connection.setAccessToken(token.getAccessToken());
        if (StringUtils.hasText(token.getRefreshToken())) {
            connection.setRefreshToken(token.getRefreshToken());
        }
        connection.setTokenExpiresAt(expiresAt(token));
        if (spotifyConnectionMapper.updateConnection(connection) != 1) {
            throw new CustomException(ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
        }
        return connection;
    }

    private SpotifyConnection refreshConnection(SpotifyConnection connection) {
        if (!StringUtils.hasText(connection.getRefreshToken())) {
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
        }
        SpotifyToken refreshed;
        try {
            refreshed = spotifyApiClient.refreshAccessToken(
                    connection.getRefreshToken());
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
        }
        validateToken(refreshed);

        String accessToken = refreshed.getAccessToken();
        String refreshToken = StringUtils.hasText(refreshed.getRefreshToken())
                ? refreshed.getRefreshToken()
                : connection.getRefreshToken();
        LocalDateTime tokenExpiresAt = expiresAt(refreshed);

        connection.setAccessToken(accessToken);
        connection.setRefreshToken(refreshToken);
        connection.setTokenExpiresAt(tokenExpiresAt);
        if (spotifyConnectionMapper.updateToken(connection) != 1) {
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
        }
        return connection;
    }

    private SpotifyConnection connection(
            Long memberId,
            String spotifyUserId,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt) {
        SpotifyConnection connection = new SpotifyConnection();
        connection.setMemberId(memberId);
        connection.setSpotifyUserId(spotifyUserId);
        connection.setAccessToken(accessToken);
        connection.setRefreshToken(refreshToken);
        connection.setTokenExpiresAt(tokenExpiresAt);
        return connection;
    }

    private LocalDateTime expiresAt(SpotifyToken token) {
        int seconds = token.getExpiresInSeconds() == null
                ? 3600
                : token.getExpiresInSeconds();
        return LocalDateTime.now().plusSeconds(seconds);
    }

    private void validateToken(SpotifyToken token) {
        if (token == null || !StringUtils.hasText(token.getAccessToken())) {
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getClientId())
                || !StringUtils.hasText(properties.getClientSecret())
                || !StringUtils.hasText(properties.getRedirectUri())) {
            throw new CustomException(ErrorCode.SPOTIFY_CONFIGURATION_ERROR);
        }
    }

    private void validateAuthenticated(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
    }

    private record CallbackIdentity(
            SpotifyToken token,
            SpotifyUserProfile profile) {
    }
}
