package com.ssafy.ssafit.common.web;

import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class FrontendReturnUrl {

    private static String frontendOrigin = "http://127.0.0.1:5173";
    private static URI frontendUri = URI.create(frontendOrigin);

    public FrontendReturnUrl(
            @Value("${ssafit.frontend.origin}") String configuredFrontendOrigin) {
        setFrontendOrigin(configuredFrontendOrigin);
    }

    public static String safeRedirect(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        try {
            URI uri = URI.create(candidate);
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    || !frontendUri.getHost().equalsIgnoreCase(uri.getHost())
                    || frontendUri.getPort() != uri.getPort()
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                return fallback;
            }
            return "redirect:" + normalizedPath(uri);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public static String safeRedirectTarget(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        try {
            URI uri = URI.create(candidate);
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    || !frontendUri.getHost().equalsIgnoreCase(uri.getHost())
                    || frontendUri.getPort() != uri.getPort()
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                return fallback;
            }
            return frontendOrigin + normalizedPath(uri);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public static String spotifyCompletionRedirect(String errorCode) {
        return spotifyCompletionRedirect(errorCode, null);
    }

    public static String spotifyCompletionRedirect(String errorCode, String returnTo) {
        List<String> queryParams = new ArrayList<>();
        if (errorCode != null && !errorCode.isBlank()) {
            queryParams.add("oauthError=" + encode(errorCode));
        }
        String safeReturnTo = safeFrontendPath(returnTo);
        if (safeReturnTo != null) {
            queryParams.add("returnTo=" + encode(safeReturnTo));
        }
        String suffix = queryParams.isEmpty()
                ? ""
                : "?" + String.join("&", queryParams);
        return frontendOrigin + "/oauth/spotify/complete" + suffix;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String safeFrontendPath(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(candidate);
            String path = uri.getRawPath();
            if (uri.isAbsolute()
                    || uri.getRawAuthority() != null
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null
                    || path == null
                    || path.isBlank()
                    || !path.startsWith("/")
                    || path.startsWith("//")) {
                return null;
            }
            if (!path.equals("/recommendations")
                    && !path.equals("/playlists")
                    && !path.startsWith("/playlists/")
                    && !path.equals("/oauth/spotify/complete")) {
                return null;
            }
            return normalizedPath(uri);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void setFrontendOrigin(String configuredFrontendOrigin) {
        URI uri = URI.create(configuredFrontendOrigin);
        if (!"http".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null
                || uri.getRawQuery() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isBlank())) {
            throw new IllegalArgumentException(
                    "ssafit.frontend.origin must be an http origin without path, query, or fragment.");
        }
        frontendOrigin = uri.getPort() < 0
                ? uri.getScheme() + "://" + uri.getHost()
                : uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
        frontendUri = URI.create(frontendOrigin);
    }

    private static String normalizedPath(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            path = "/";
        }
        String query = uri.getRawQuery();
        return query == null ? path : path + "?" + query;
    }
}
