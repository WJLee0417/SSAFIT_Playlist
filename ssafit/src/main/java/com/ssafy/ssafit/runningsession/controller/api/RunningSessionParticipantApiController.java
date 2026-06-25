package com.ssafy.ssafit.runningsession.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.runningsession.dto.RunningSessionApiDtos.ParticipantResponse;
import com.ssafy.ssafit.runningsession.service.RunningSessionParticipantService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/running-sessions/{runningSessionId}/participants")
@RequiredArgsConstructor
public class RunningSessionParticipantApiController {

    private final RunningSessionParticipantService participantService;

    @GetMapping
    public ApiResponse<List<ParticipantResponse>> getParticipants(
            @PathVariable Long runningSessionId,
            HttpSession session) {
        return ApiResponse.success(participantService
                .getSessionParticipants(session, runningSessionId)
                .stream()
                .map(ParticipantResponse::from)
                .toList());
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParticipantResponse> join(
            @PathVariable Long runningSessionId,
            HttpSession session) {
        return ApiResponse.success(
                "러닝 모임에 참여했습니다.",
                ParticipantResponse.from(participantService.joinSession(
                        session,
                        runningSessionId)));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> cancelMyParticipation(
            @PathVariable Long runningSessionId,
            HttpSession session) {
        participantService.cancelMyParticipation(session, runningSessionId);
        return ApiResponse.success("러닝 모임 참여를 취소했습니다.");
    }
}
