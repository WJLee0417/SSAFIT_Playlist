package com.ssafy.ssafit.common.auth;

import org.springframework.stereotype.Component;

import com.ssafy.ssafit.member.service.AuthService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionMemberResolver {

    private final AuthService authService;

    public Long requireMemberId(HttpSession session) {
        return authService.getLoginMemberId(session);
    }

    public Long optionalMemberId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        return value instanceof Long memberId ? memberId : null;
    }
}
