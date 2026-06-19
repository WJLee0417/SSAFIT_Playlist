package com.ssafy.ssafit.spotify.service;

import java.time.LocalDateTime;

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
                throw new CustomException(ErrorCode.SPOTIFY_AUTH_DENIED);
            }
            throw new CustomException(ErrorCode.SPOTIFY_OAUTH_CALLBACK_ERROR);
        }
        if (!StringUtils.hasText(authorizationCode)) {
            throw new CustomException(ErrorCode.SPOTIFY_AUTH_REQUIRED);
        }

        SpotifyToken token = spotifyApiClient.exchangeAuthorizationCode(
                authorizationCode.trim());
        validateToken(token);
        SpotifyUserProfile profile = spotifyApiClient.getCurrentUser(
                token.getAccessToken());
        if (profile == null || !StringUtils.hasText(profile.getId())) {
            throw new CustomException(ErrorCode.SPOTIFY_API_ERROR);
        }

        SpotifyConnection connection = new SpotifyConnection();
        connection.setMemberId(loginMemberId);
        connection.setSpotifyUserId(profile.getId());
        connection.setAccessToken(token.getAccessToken());
        connection.setRefreshToken(token.getRefreshToken());
        connection.setTokenExpiresAt(expiresAt(token));
        spotifyConnectionMapper.upsert(connection);
        return connection;
    }

    public SpotifyConnection getConnection(Long loginMemberId) {
        validateAuthenticated(loginMemberId);
        return spotifyConnectionMapper.selectByMemberId(loginMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPOTIFY_AUTH_REQUIRED));
    }

    @Transactional
    public SpotifyConnection getValidConnection(Long loginMemberId) {
        SpotifyConnection connection = getConnection(loginMemberId);
        if (connection.getTokenExpiresAt() != null
                && connection.getTokenExpiresAt()
                        .isAfter(LocalDateTime.now().plusSeconds(TOKEN_EXPIRY_SAFETY_SECONDS))) {
            return connection;
        }
        return refreshConnection(connection);
    }

    private SpotifyConnection refreshConnection(SpotifyConnection connection) {
        if (!StringUtils.hasText(connection.getRefreshToken())) {
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_EXPIRED);
        }
        SpotifyToken refreshed = spotifyApiClient.refreshAccessToken(
                connection.getRefreshToken());
        validateToken(refreshed);
        connection.setAccessToken(refreshed.getAccessToken());
        if (StringUtils.hasText(refreshed.getRefreshToken())) {
            connection.setRefreshToken(refreshed.getRefreshToken());
        }
        connection.setTokenExpiresAt(expiresAt(refreshed));
        spotifyConnectionMapper.updateToken(connection);
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
            throw new CustomException(ErrorCode.SPOTIFY_TOKEN_EXPIRED);
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
}
