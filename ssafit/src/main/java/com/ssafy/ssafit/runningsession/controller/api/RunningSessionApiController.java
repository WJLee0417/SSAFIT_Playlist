package com.ssafy.ssafit.runningsession.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.dto.RunningSessionApiDtos.CreateRequest;
import com.ssafy.ssafit.runningsession.dto.RunningSessionApiDtos.DetailResponse;
import com.ssafy.ssafit.runningsession.dto.RunningSessionApiDtos.ParticipantResponse;
import com.ssafy.ssafit.runningsession.dto.RunningSessionApiDtos.SessionResponse;
import com.ssafy.ssafit.runningsession.dto.RunningSessionApiDtos.StatusUpdateRequest;
import com.ssafy.ssafit.runningsession.dto.RunningSessionApiDtos.UpdateRequest;
import com.ssafy.ssafit.runningsession.service.RunningSessionParticipantService;
import com.ssafy.ssafit.runningsession.service.RunningSessionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningSessionApiController {

    private final RunningSessionService runningSessionService;
    private final RunningSessionParticipantService participantService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> getSessions(HttpSession session) {
        return ApiResponse.success(runningSessionService.getSessionList(session)
                .stream()
                .map(SessionResponse::from)
                .toList());
    }

    @GetMapping("/{runningSessionId}")
    public ApiResponse<DetailResponse> getSession(
            @PathVariable Long runningSessionId,
            HttpSession session) {
        RunningSession runningSession =
                runningSessionService.getSessionDetail(session, runningSessionId);
        List<ParticipantResponse> participants = participantService
                .getSessionParticipants(session, runningSessionId)
                .stream()
                .map(ParticipantResponse::from)
                .toList();
        return ApiResponse.success(new DetailResponse(
                SessionResponse.from(runningSession),
                participants.size(),
                participants));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionResponse> create(
            @Valid @RequestBody CreateRequest request,
            HttpSession session) {
        return ApiResponse.success(
                "러닝 모임을 생성했습니다.",
                SessionResponse.from(runningSessionService.createSession(
                        session,
                        request.toDomain())));
    }

    @PutMapping("/{runningSessionId}")
    public ApiResponse<SessionResponse> update(
            @PathVariable Long runningSessionId,
            @Valid @RequestBody UpdateRequest request,
            HttpSession session) {
        return ApiResponse.success(
                "러닝 모임을 수정했습니다.",
                SessionResponse.from(runningSessionService.updateMySession(
                        session,
                        request.toDomain(runningSessionId))));
    }

    @PatchMapping("/{runningSessionId}/status")
    public ApiResponse<SessionResponse> changeStatus(
            @PathVariable Long runningSessionId,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpSession session) {
        return ApiResponse.success(
                "러닝 모임 모집 상태를 변경했습니다.",
                SessionResponse.from(
                        runningSessionService.changeMySessionStatus(
                                session,
                                runningSessionId,
                                request.status())));
    }

    @PostMapping("/{runningSessionId}/cancel")
    public ApiResponse<SessionResponse> cancel(
            @PathVariable Long runningSessionId,
            HttpSession session) {
        return ApiResponse.success(
                "러닝 모임을 취소했습니다.",
                SessionResponse.from(runningSessionService.cancelMySession(
                        session,
                        runningSessionId)));
    }
}
