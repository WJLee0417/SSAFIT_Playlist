package com.ssafy.ssafit.runningprofile.controller.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.dto.RunningProfileApiDtos.Response;
import com.ssafy.ssafit.runningprofile.dto.RunningProfileApiDtos.SaveRequest;
import com.ssafy.ssafit.runningprofile.service.RunningFitnessMetricService;
import com.ssafy.ssafit.runningprofile.service.RunningProfileService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members/me/running-profile")
@RequiredArgsConstructor
public class RunningProfileApiController {

    private final RunningProfileService runningProfileService;
    private final RunningFitnessMetricService runningFitnessMetricService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping
    public ApiResponse<Response> getProfile(HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        RunningProfile profile = runningProfileService.getProfile(memberId, memberId);
        return ApiResponse.success(Response.from(profile, calculateMetric(memberId)));
    }

    @PutMapping
    public ApiResponse<Response> saveProfile(
            @Valid @RequestBody SaveRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        RunningProfile profile = runningProfileService.saveOrUpdate(
                memberId,
                request.toDomain(memberId));
        return ApiResponse.success(
                "러닝 프로필을 저장했습니다.",
                Response.from(profile, calculateMetric(memberId)));
    }

    private RunningFitnessMetric calculateMetric(Long memberId) {
        return runningFitnessMetricService.calculate(memberId);
    }
}
