package com.ssafy.ssafit.common.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalViewModelAdvice {

    private final String frontendOrigin;
    private final SessionMemberResolver sessionMemberResolver;

    public GlobalViewModelAdvice(
            @Value("${ssafit.frontend.origin}") String frontendOrigin,
            SessionMemberResolver sessionMemberResolver) {
        this.frontendOrigin = frontendOrigin;
        this.sessionMemberResolver = sessionMemberResolver;
    }

    @ModelAttribute("frontendOrigin")
    public String frontendOrigin() {
        return frontendOrigin;
    }

    @ModelAttribute("loggedIn")
    public boolean loggedIn(HttpSession session) {
        return sessionMemberResolver.optionalMemberId(session) != null;
    }
}
