package com.ssafy.ssafit.common.web;

import java.net.URI;

public final class FrontendReturnUrl {

    private static final String FRONTEND_ORIGIN = "http://127.0.0.1:5173";

    private FrontendReturnUrl() {
    }

    public static String safeRedirect(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        try {
            URI uri = URI.create(candidate);
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    || !"127.0.0.1".equalsIgnoreCase(uri.getHost())
                    || uri.getPort() != 5173
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                return fallback;
            }
            return "redirect:" + FRONTEND_ORIGIN + normalizedPath(uri);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public static String spotifyCompletionRedirect(String errorCode) {
        String suffix = errorCode == null || errorCode.isBlank()
                ? ""
                : "?oauthError=" + errorCode;
        return FRONTEND_ORIGIN + "/oauth/spotify/complete" + suffix;
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
