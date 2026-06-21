package com.ssafy.ssafit.member.controller.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.dto.MemberApiDtos.LoginRequest;
import com.ssafy.ssafit.member.dto.MemberApiDtos.LoginResponse;
import com.ssafy.ssafit.member.service.AuthService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;
    private final SessionMemberResolver sessionMemberResolver;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {
        Member member = authService.login(
                session,
                request.loginId(),
                request.password());
        return ApiResponse.success("로그인했습니다.", LoginResponse.from(member));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        sessionMemberResolver.requireMemberId(session);
        authService.logout(session);
        return ApiResponse.success("로그아웃했습니다.");
    }
}
