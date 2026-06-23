package com.ssafy.ssafit.spotify.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;

import jakarta.servlet.http.HttpSession;

@Component
public class SpotifyOAuthStateManager {

    static final String SESSION_ATTRIBUTE = "SPOTIFY_OAUTH_STATE";
    private static final int STATE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String createAndStore(HttpSession session) {
        byte[] bytes = new byte[STATE_BYTES];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_ATTRIBUTE, state);
        return state;
    }

    public void validateAndConsume(HttpSession session, String returnedState) {
        Object storedValue = session.getAttribute(SESSION_ATTRIBUTE);
        String storedState = storedValue instanceof String value ? value : null;

        if (!StringUtils.hasText(storedState)
                || !StringUtils.hasText(returnedState)
                || !storedState.equals(returnedState)) {
            session.removeAttribute(SESSION_ATTRIBUTE);
            throw new CustomException(ErrorCode.SPOTIFY_STATE_MISMATCH);
        }

        session.removeAttribute(SESSION_ATTRIBUTE);
    }

    public void discard(HttpSession session) {
        session.removeAttribute(SESSION_ATTRIBUTE);
    }
}
