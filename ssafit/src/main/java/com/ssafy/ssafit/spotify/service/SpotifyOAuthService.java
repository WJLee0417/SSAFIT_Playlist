package com.ssafy.ssafit.spotify.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.ssafy.ssafit.common.auth.PasswordHashService;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.mapper.MemberMapper;
import com.ssafy.ssafit.member.service.AuthService;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.config.SpotifyProperties;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
import com.ssafy.ssafit.spotify.domain.SpotifyPlaylistSummary;
import com.ssafy.ssafit.spotify.domain.SpotifyToken;
import com.ssafy.ssafit.spotify.domain.SpotifyUserProfile;
import com.ssafy.ssafit.spotify.mapper.SpotifyConnectionMapper;
import com.ssafy.ssafit.spotify.service.SpotifyOAuthStateManager.OAuthState;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotifyOAuthService {

    private static final long TOKEN_EXPIRY_SAFETY_SECONDS = 30;
    private static final String CONNECT_FLOW = "CONNECT";
    private static final String LOGIN_FLOW = "LOGIN";

    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyConnectionMapper spotifyConnectionMapper;
    private final MemberMapper memberMapper;
    private final PasswordHashService passwordHashService;
    private final AuthService authService;
    private final SpotifyProperties properties;
    private final SpotifyOAuthStateManager stateManager;

    public String buildAuthorizationUrl(HttpSession session) {
        return buildAuthorizationUrl(session, CONNECT_FLOW, null);
    }

    public String buildAuthorizationUrl(HttpSession session, String returnTo) {
        return buildAuthorizationUrl(session, CONNECT_FLOW, returnTo);
    }

    public String buildLoginAuthorizationUrl(HttpSession session, String returnTo) {
        return buildAuthorizationUrl(session, LOGIN_FLOW, returnTo);
    }

    private String buildAuthorizationUrl(
            HttpSession session,
            String flow,
            String returnTo) {
        validateConfiguration();
        String state = stateManager.createAndStore(session, flow, returnTo);
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
        return handleConnectCallback(
                loginMemberId,
                authorizationCode,
                error,
                returnedState,
                session).connection();
    }

    @Transactional
    public SpotifyConnectResult handleConnectCallback(
            Long loginMemberId,
            String authorizationCode,
            String error,
            String returnedState,
            HttpSession session) {
        validateAuthenticated(loginMemberId);
        OAuthState oauthState = stateManager.validateAndConsume(
                session,
                returnedState);
        validateCallbackParameters(authorizationCode, error);

        CallbackIdentity callbackIdentity = exchangeAndResolveIdentity(
                authorizationCode);
        SpotifyToken token = callbackIdentity.token();
        SpotifyUserProfile profile = callbackIdentity.profile();
        String spotifyUserId = requireStableSpotifyUserId(profile);

        findExistingSpotifyConnection(profile)
                .filter(connection -> !loginMemberId.equals(connection.getMemberId()))
                .ifPresent(connection -> {
                    throw new CustomException(
                            ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
                });

        SpotifyConnection connection = saveConnection(loginMemberId, spotifyUserId, token);
        return new SpotifyConnectResult(connection, oauthState.returnTo());
    }

    @Transactional
    public SpotifyLoginResult handleLoginCallback(
            String authorizationCode,
            String error,
            String returnedState,
            HttpSession session) {
        OAuthState oauthState = stateManager.validateAndConsume(
                session,
                returnedState);
        if (!LOGIN_FLOW.equals(oauthState.flow())) {
            throw new CustomException(ErrorCode.SPOTIFY_STATE_MISMATCH);
        }
        validateCallbackParameters(authorizationCode, error);

        CallbackIdentity callbackIdentity = exchangeAndResolveIdentity(
                authorizationCode);
        SpotifyToken token = callbackIdentity.token();
        SpotifyUserProfile profile = callbackIdentity.profile();
        String spotifyUserId = requireStableSpotifyUserId(profile);

        Optional<SpotifyConnection> existingConnection =
                findExistingSpotifyConnection(profile);
        Member member = existingConnection
                .map(connection -> memberMapper.selectById(connection.getMemberId())
                        .orElseThrow(() -> new CustomException(
                                ErrorCode.MEMBER_NOT_FOUND)))
                .orElseGet(() -> createSpotifyMember(spotifyUserId, profile));

        saveConnection(member.getId(), spotifyUserId, token);
        authService.login(session, member);
        return new SpotifyLoginResult(member, oauthState.returnTo());
    }

    private CallbackIdentity exchangeAndResolveIdentity(String authorizationCode) {
        SpotifyToken token;
        try {
            token = spotifyApiClient.exchangeAuthorizationCode(
                    authorizationCode.trim());
            validateToken(token);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
        }
        return resolveCallbackIdentityWithSingleRetry(token);
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

    public List<SpotifyPlaylistSummary> getCurrentUserPlaylists(
            Long loginMemberId,
            Integer offset,
            Integer limit) {
        int safeOffset = offset == null || offset < 0 ? 0 : offset;
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 50));
        return executeWithValidConnection(
                loginMemberId,
                connection -> spotifyApiClient.getCurrentUserPlaylists(
                        connection.getAccessToken(),
                        safeOffset,
                        safeLimit));
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

    private void validateCallbackParameters(
            String authorizationCode,
            String error) {
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
    }

    private String requireStableSpotifyUserId(SpotifyUserProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.stableIdentifier())) {
            throw new CustomException(ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
        }
        return profile.stableIdentifier();
    }

    private Optional<SpotifyConnection> findExistingSpotifyConnection(
            SpotifyUserProfile profile) {
        String stableId = profile.stableIdentifier();
        Optional<SpotifyConnection> connection =
                spotifyConnectionMapper.selectBySpotifyUserId(stableId);
        if (connection.isPresent()
                || !StringUtils.hasText(profile.getId())
                || stableId.equals(profile.getId())) {
            return connection;
        }
        return spotifyConnectionMapper.selectBySpotifyUserId(profile.getId());
    }

    private Member createSpotifyMember(
            String spotifyUserId,
            SpotifyUserProfile profile) {
        String suffix = spotifyIdentitySuffix(spotifyUserId);
        Member member = new Member();
        member.setLoginId("spotify_" + suffix);
        member.setEmail("spotify_" + suffix + "@spotify.local");
        member.setNickname(defaultNickname(profile));
        member.setPasswordHash(passwordHashService.encode(
                UUID.randomUUID().toString()));
        member.setStatus(MemberStatus.ACTIVE);
        if (memberMapper.insert(member) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return member;
    }

    private String spotifyIdentitySuffix(String spotifyUserId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    spotifyUserId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes)
                    .substring(0, 22);
        } catch (NoSuchAlgorithmException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String defaultNickname(SpotifyUserProfile profile) {
        String displayName = profile == null ? null : profile.getDisplayName();
        if (!StringUtils.hasText(displayName)) {
            return "Spotify User";
        }
        String normalized = displayName.trim();
        return normalized.length() <= 100
                ? normalized
                : normalized.substring(0, 100);
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

    public record SpotifyLoginResult(
            Member member,
            String returnTo) {
    }

    public record SpotifyConnectResult(
            SpotifyConnection connection,
            String returnTo) {
    }
}
