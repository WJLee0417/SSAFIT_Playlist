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
    static final String FLOW_ATTRIBUTE = "SPOTIFY_OAUTH_FLOW";
    static final String RETURN_TO_ATTRIBUTE = "SPOTIFY_OAUTH_RETURN_TO";
    private static final int STATE_BYTES = 32;
    private static final String DEFAULT_FLOW = "CONNECT";

    private final SecureRandom secureRandom = new SecureRandom();

    public String createAndStore(HttpSession session) {
        return createAndStore(session, DEFAULT_FLOW, null);
    }

    public String createAndStore(
            HttpSession session,
            String flow,
            String returnTo) {
        byte[] bytes = new byte[STATE_BYTES];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_ATTRIBUTE, state);
        session.setAttribute(
                FLOW_ATTRIBUTE,
                StringUtils.hasText(flow) ? flow : DEFAULT_FLOW);
        if (StringUtils.hasText(returnTo)) {
            session.setAttribute(RETURN_TO_ATTRIBUTE, returnTo);
        } else {
            session.removeAttribute(RETURN_TO_ATTRIBUTE);
        }
        return state;
    }

    public OAuthState validateAndConsume(HttpSession session, String returnedState) {
        Object storedValue = session.getAttribute(SESSION_ATTRIBUTE);
        String storedState = storedValue instanceof String value ? value : null;
        Object storedFlow = session.getAttribute(FLOW_ATTRIBUTE);
        String flow = storedFlow instanceof String value ? value : DEFAULT_FLOW;
        Object storedReturnTo = session.getAttribute(RETURN_TO_ATTRIBUTE);
        String returnTo = storedReturnTo instanceof String value ? value : null;

        if (!StringUtils.hasText(storedState)
                || !StringUtils.hasText(returnedState)
                || !storedState.equals(returnedState)) {
            clear(session);
            throw new CustomException(ErrorCode.SPOTIFY_STATE_MISMATCH);
        }

        clear(session);
        return new OAuthState(flow, returnTo);
    }

    public void discard(HttpSession session) {
        clear(session);
    }

    private void clear(HttpSession session) {
        session.removeAttribute(SESSION_ATTRIBUTE);
        session.removeAttribute(FLOW_ATTRIBUTE);
        session.removeAttribute(RETURN_TO_ATTRIBUTE);
    }

    public record OAuthState(String flow, String returnTo) {
    }
}
