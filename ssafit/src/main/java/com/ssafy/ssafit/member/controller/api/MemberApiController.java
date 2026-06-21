package com.ssafy.ssafit.member.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.dto.MemberApiDtos.JoinRequest;
import com.ssafy.ssafit.member.dto.MemberApiDtos.MemberResponse;
import com.ssafy.ssafit.member.service.MemberService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;
    private final SessionMemberResolver sessionMemberResolver;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> join(
            @Valid @RequestBody JoinRequest request) {
        Member member = memberService.register(request.toMember(), request.password());
        return ApiResponse.success("회원가입이 완료되었습니다.", MemberResponse.from(member));
    }

    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(MemberResponse.from(memberService.getMyInfo(memberId)));
    }
}
