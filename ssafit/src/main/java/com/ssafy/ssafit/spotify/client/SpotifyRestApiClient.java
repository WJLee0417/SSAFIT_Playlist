package com.ssafy.ssafit.spotify.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.spotify.config.SpotifyProperties;
import com.ssafy.ssafit.spotify.domain.SpotifyPlaylistResult;
import com.ssafy.ssafit.spotify.domain.SpotifyPlaylistSummary;
import com.ssafy.ssafit.spotify.domain.SpotifyToken;
import com.ssafy.ssafit.spotify.domain.SpotifyTrack;
import com.ssafy.ssafit.spotify.domain.SpotifyUserProfile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class SpotifyRestApiClient implements SpotifyApiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final int MAX_LOGGED_RESPONSE_BODY_LENGTH = 2_000;

    private final RestClient.Builder restClientBuilder;
    private final SpotifyProperties properties;

    @Override
    public String issueClientAccessToken() {
        Map<String, Object> response = postToken("grant_type=client_credentials");
        return requiredString(response, "access_token");
    }

    @Override
    public List<SpotifyTrack> searchTracks(
            String accessToken,
            String query,
            int offset,
            int limit) {
        URI uri = UriComponentsBuilder
                .fromUriString(properties.getApiBaseUrl())
                .path("/search")
                .queryParam("q", query)
                .queryParam("type", "track")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .build()
                .encode()
                .toUri();
        Map<String, Object> response = get(uri, accessToken);
        Map<String, Object> tracks = map(response.get("tracks"));
        List<Object> items = list(tracks.get("items"));
        List<SpotifyTrack> result = new ArrayList<>(items.size());
        for (Object value : items) {
            Map<String, Object> item = map(value);
            Map<String, Object> album = map(item.get("album"));
            List<Object> artists = list(item.get("artists"));
            Map<String, Object> firstArtist = artists.isEmpty()
                    ? Map.of()
                    : map(artists.getFirst());
            List<Object> images = list(album.get("images"));
            result.add(new SpotifyTrack(
                    string(item.get("id")),
                    string(item.get("uri")),
                    string(map(item.get("external_urls")).get("spotify")),
                    string(item.get("name")),
                    string(firstArtist.get("id")),
                    string(firstArtist.get("name")),
                    string(album.get("name")),
                    images.isEmpty() ? null : string(map(images.getFirst()).get("url")),
                    integer(item.get("duration_ms"))));
        }
        return result;
    }

    @Override
    public Integer getTrackTempoBpm(String accessToken, String trackId) {
        URI uri = URI.create(properties.getApiBaseUrl()
                + "/audio-features/"
                + encodePath(trackId));
        Map<String, Object> response = get(uri, accessToken);
        Object tempo = response.get("tempo");
        if (tempo instanceof Number number) {
            return Math.round(number.floatValue());
        }
        return null;
    }

    @Override
    public List<String> getArtistGenres(String accessToken, String artistId) {
        URI uri = URI.create(properties.getApiBaseUrl()
                + "/artists/"
                + encodePath(artistId));
        Map<String, Object> response = get(uri, accessToken);
        return list(response.get("genres")).stream()
                .map(this::string)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    @Override
    public SpotifyToken exchangeAuthorizationCode(String code) {
        validateOAuthConfiguration();
        String body = "grant_type=authorization_code"
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(properties.getRedirectUri());
        return toToken(postToken(body));
    }

    @Override
    public SpotifyToken refreshAccessToken(String refreshToken) {
        return toToken(postToken(
                "grant_type=refresh_token&refresh_token=" + encode(refreshToken)));
    }

    @Override
    public SpotifyUserProfile getCurrentUser(String accessToken) {
        Map<String, Object> response = get(
                URI.create(properties.getApiBaseUrl() + "/me"),
                accessToken);
        return new SpotifyUserProfile(
                string(response.get("account_id")),
                requiredString(response, "id"),
                string(response.get("display_name")));
    }

    @Override
    public SpotifyPlaylistResult createPlaylist(
            String accessToken,
            String name,
            String description,
            boolean isPublic) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("description", description);
        request.put("public", isPublic);
        Map<String, Object> response = postJson(
                URI.create(properties.getApiBaseUrl() + "/me/playlists"),
                accessToken,
                request);
        return new SpotifyPlaylistResult(
                requiredString(response, "id"),
                requiredString(map(response.get("external_urls")), "spotify"));
    }

    @Override
    public void addTracks(
            String accessToken,
            String spotifyPlaylistId,
            List<String> uris) {
        postJson(
                URI.create(properties.getApiBaseUrl()
                        + "/playlists/"
                        + encodePath(spotifyPlaylistId)
                        + "/items"),
                accessToken,
                Map.of("uris", uris));
    }

    @Override
    public List<SpotifyPlaylistSummary> getCurrentUserPlaylists(
            String accessToken,
            int offset,
            int limit) {
        URI uri = UriComponentsBuilder
                .fromUriString(properties.getApiBaseUrl())
                .path("/me/playlists")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .build()
                .encode()
                .toUri();
        Map<String, Object> response = get(uri, accessToken);
        List<Object> items = list(response.get("items"));
        List<SpotifyPlaylistSummary> result = new ArrayList<>(items.size());
        for (Object value : items) {
            Map<String, Object> item = map(value);
            Map<String, Object> owner = map(item.get("owner"));
            Map<String, Object> tracks = map(item.get("tracks"));
            result.add(new SpotifyPlaylistSummary(
                    requiredString(item, "id"),
                    requiredString(item, "name"),
                    string(map(item.get("external_urls")).get("spotify")),
                    string(owner.get("id")),
                    bool(item.get("public")),
                    integer(tracks.get("total"))));
        }
        return result;
    }

    @Override
    public List<SpotifyTrack> getPlaylistTracks(
            String accessToken,
            String spotifyPlaylistId,
            int offset,
            int limit) {
        URI uri = UriComponentsBuilder
                .fromUriString(properties.getApiBaseUrl())
                .path("/playlists/")
                .pathSegment(spotifyPlaylistId)
                .path("/tracks")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParam("fields", "items(track(id,uri,external_urls,name,artists,album(name,images),duration_ms))")
                .build()
                .encode()
                .toUri();
        Map<String, Object> response = get(uri, accessToken);
        List<Object> items = list(response.get("items"));
        List<SpotifyTrack> result = new ArrayList<>(items.size());
        for (Object value : items) {
            Map<String, Object> item = map(value);
            Map<String, Object> track = map(item.get("track"));
            String trackId = string(track.get("id"));
            String trackUri = string(track.get("uri"));
            if (!StringUtils.hasText(trackId) || !StringUtils.hasText(trackUri)) {
                continue;
            }
            Map<String, Object> album = map(track.get("album"));
            List<Object> artists = list(track.get("artists"));
            Map<String, Object> firstArtist = artists.isEmpty()
                    ? Map.of()
                    : map(artists.getFirst());
            List<Object> images = list(album.get("images"));
            result.add(new SpotifyTrack(
                    trackId,
                    trackUri,
                    string(map(track.get("external_urls")).get("spotify")),
                    string(track.get("name")),
                    string(firstArtist.get("id")),
                    string(firstArtist.get("name")),
                    string(album.get("name")),
                    images.isEmpty() ? null : string(map(images.getFirst()).get("url")),
                    integer(track.get("duration_ms"))));
        }
        return result;
    }

    private Map<String, Object> postToken(String body) {
        validateClientCredentials();
        try {
            Map<String, Object> response = restClientBuilder.build()
                    .post()
                    .uri(properties.getAuthorizationBaseUrl() + "/api/token")
                    .header(HttpHeaders.AUTHORIZATION, basicAuthorization())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            return requiredResponse(response);
        } catch (RestClientException exception) {
            logSpotifyApiFailure("POST /api/token", exception);
            throw spotifyApiException(exception);
        }
    }

    private Map<String, Object> get(URI uri, String accessToken) {
        try {
            Map<String, Object> response = restClientBuilder.build()
                    .get()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(MAP_TYPE);
            return requiredResponse(response);
        } catch (RestClientException exception) {
            logSpotifyApiFailure("GET " + safeRequestTarget(uri), exception);
            throw spotifyApiException(exception);
        }
    }

    private Map<String, Object> postJson(
            URI uri,
            String accessToken,
            Object request) {
        try {
            Map<String, Object> response = restClientBuilder.build()
                    .post()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MAP_TYPE);
            return response == null ? Map.of() : response;
        } catch (RestClientException exception) {
            logSpotifyApiFailure("POST " + safeRequestTarget(uri), exception);
            throw spotifyApiException(exception);
        }
    }

    private void logSpotifyApiFailure(
            String operation,
            RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            log.error(
                    "Spotify API request failed. operation={}, status={}, "
                            + "responseBody={}",
                    operation,
                    responseException.getStatusCode(),
                    sanitizeResponseBody(
                            responseException.getResponseBodyAsString()),
                    responseException);
            return;
        }
        log.error(
                "Spotify API transport failed. operation={}, exceptionType={}, "
                        + "message={}",
                operation,
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception);
    }

    private String safeRequestTarget(URI uri) {
        if (uri == null) {
            return "unknown";
        }
        String path = uri.getPath();
        return StringUtils.hasText(path) ? path : "unknown";
    }

    private String sanitizeResponseBody(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "<empty>";
        }
        String sanitized = responseBody
                .replaceAll(
                        "(?i)(\"?(?:access_token|refresh_token|client_secret)"
                                + "\"?\\s*[:=]\\s*\")([^\"]+)(\")",
                        "$1<redacted>$3")
                .replaceAll(
                        "(?i)((?:access_token|refresh_token|client_secret)"
                                + "=)([^&\\s]+)",
                        "$1<redacted>")
                .replaceAll(
                        "(?i)(authorization\\s*[:=]\\s*)"
                                + "(?:bearer|basic)\\s+[^,}\\s]+",
                        "$1<redacted>")
                .replaceAll(
                        "(?i)(bearer|basic)\\s+[A-Za-z0-9._~+/=-]+",
                        "$1 <redacted>");
        if (sanitized.length() <= MAX_LOGGED_RESPONSE_BODY_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_LOGGED_RESPONSE_BODY_LENGTH)
                + "...<truncated>";
    }

    private CustomException spotifyApiException(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return switch (responseException.getStatusCode().value()) {
                case 400, 401 -> new CustomException(
                        ErrorCode.SPOTIFY_AUTHORIZATION_FAILED);
                case 403 -> new CustomException(ErrorCode.SPOTIFY_FORBIDDEN);
                case 429 -> new CustomException(ErrorCode.SPOTIFY_RATE_LIMITED);
                default -> new CustomException(ErrorCode.SPOTIFY_API_FAILED);
            };
        }
        return new CustomException(ErrorCode.SPOTIFY_API_FAILED);
    }

    private SpotifyToken toToken(Map<String, Object> response) {
        return new SpotifyToken(
                requiredString(response, "access_token"),
                string(response.get("refresh_token")),
                integer(response.get("expires_in")));
    }

    private void validateClientCredentials() {
        if (!StringUtils.hasText(properties.getClientId())
                || !StringUtils.hasText(properties.getClientSecret())) {
            throw new CustomException(ErrorCode.SPOTIFY_CONFIGURATION_ERROR);
        }
    }

    private void validateOAuthConfiguration() {
        validateClientCredentials();
        if (!StringUtils.hasText(properties.getRedirectUri())) {
            throw new CustomException(ErrorCode.SPOTIFY_CONFIGURATION_ERROR);
        }
    }

    private String basicAuthorization() {
        String credentials = properties.getClientId() + ":" + properties.getClientSecret();
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(String value) {
        return UriComponentsBuilder.newInstance()
                .queryParam("value", value)
                .build()
                .encode()
                .getQueryParams()
                .getFirst("value");
    }

    private String encodePath(String value) {
        return UriComponentsBuilder.newInstance()
                .pathSegment(value)
                .build()
                .encode()
                .getPath()
                .substring(1);
    }

    private Map<String, Object> requiredResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            throw new CustomException(ErrorCode.SPOTIFY_API_FAILED);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return value instanceof List<?> ? (List<Object>) value : List.of();
    }

    private String requiredString(Map<String, Object> values, String key) {
        String value = string(values.get(key));
        if (!StringUtils.hasText(value)) {
            throw new CustomException(ErrorCode.SPOTIFY_API_FAILED);
        }
        return value;
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Boolean bool(Object value) {
        return value instanceof Boolean bool ? bool : null;
    }
}
