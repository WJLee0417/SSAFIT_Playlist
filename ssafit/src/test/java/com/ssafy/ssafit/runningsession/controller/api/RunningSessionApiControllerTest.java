package com.ssafy.ssafit.runningsession.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.common.exception.GlobalExceptionHandler;
import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionParticipant;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;
import com.ssafy.ssafit.runningsession.service.RunningSessionParticipantService;
import com.ssafy.ssafit.runningsession.service.RunningSessionService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class RunningSessionApiControllerTest {

    @Mock
    private RunningSessionService runningSessionService;

    @Mock
    private RunningSessionParticipantService participantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new RunningSessionApiController(
                                runningSessionService,
                                participantService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createUsesSessionAndIgnoresClientOwnedFields() throws Exception {
        when(runningSessionService.createSession(
                any(HttpSession.class),
                any(RunningSession.class)))
                .thenReturn(runningSession(100L, 10L, RunningSessionStatus.OPEN));

        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "주말 러닝",
                                  "description": "함께 달립니다.",
                                  "meetingPlace": "여의나루역",
                                  "scheduledAt": "2026-07-01T09:00:00",
                                  "capacity": 10,
                                  "targetDistanceKm": 10.0,
                                  "targetPaceSecondsPerKm": 330,
                                  "hostMemberId": 999,
                                  "status": "CANCELLED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.hostMemberId").value(10))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        ArgumentCaptor<RunningSession> captor =
                ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionService).createSession(
                any(HttpSession.class),
                captor.capture());
        assertThat(captor.getValue().getHostMemberId()).isNull();
        assertThat(captor.getValue().getStatus()).isNull();
        assertThat(captor.getValue().getCancelledAt()).isNull();
    }

    @Test
    void listReturnsSessionDtosWithoutParticipantNPlusOne() throws Exception {
        when(runningSessionService.getSessionList(any(HttpSession.class)))
                .thenReturn(List.of(
                        runningSession(100L, 10L, RunningSessionStatus.OPEN)));

        mockMvc.perform(get("/api/running-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].hostMemberId").value(10))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());

        verify(participantService, never())
                .getSessionParticipants(any(HttpSession.class), any());
    }

    @Test
    void detailCombinesSessionAndSafeParticipantDtos() throws Exception {
        when(runningSessionService.getSessionDetail(
                any(HttpSession.class),
                eq(100L)))
                .thenReturn(runningSession(
                        100L,
                        10L,
                        RunningSessionStatus.OPEN));
        when(participantService.getSessionParticipants(
                any(HttpSession.class),
                eq(100L)))
                .thenReturn(List.of(participant(1000L, 100L, 20L)));

        mockMvc.perform(get("/api/running-sessions/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session.id").value(100))
                .andExpect(jsonPath("$.data.participantCount").value(1))
                .andExpect(jsonPath("$.data.participants[0].memberId").value(20))
                .andExpect(jsonPath("$.data.participants[0].passwordHash")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.participants[0].email")
                        .doesNotExist());
    }

    @Test
    void updateUsesPathIdAndDelegatesHostAuthorization() throws Exception {
        when(runningSessionService.updateMySession(
                any(HttpSession.class),
                any(RunningSession.class)))
                .thenAnswer(invocation -> {
                    RunningSession changes = invocation.getArgument(1);
                    changes.setHostMemberId(10L);
                    changes.setStatus(RunningSessionStatus.OPEN);
                    return changes;
                });

        mockMvc.perform(put("/api/running-sessions/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 999,
                                  "title": "수정 모임",
                                  "description": "수정 설명",
                                  "meetingPlace": "반포한강공원",
                                  "scheduledAt": "2026-07-02T19:00:00",
                                  "capacity": 8,
                                  "targetDistanceKm": 8.5,
                                  "targetPaceSecondsPerKm": 360,
                                  "hostMemberId": 999
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.hostMemberId").value(10));

        ArgumentCaptor<RunningSession> captor =
                ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionService).updateMySession(
                any(HttpSession.class),
                captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(100L);
    }

    @Test
    void nonHostUpdateUsesGlobalAccessDeniedContract() throws Exception {
        when(runningSessionService.updateMySession(
                any(HttpSession.class),
                any(RunningSession.class)))
                .thenThrow(new CustomException(ErrorCode.ACCESS_DENIED));

        mockMvc.perform(put("/api/running-sessions/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void statusChangeDelegatesAllowedStatusToService() throws Exception {
        when(runningSessionService.changeMySessionStatus(
                any(HttpSession.class),
                eq(100L),
                eq(RunningSessionStatus.CLOSED)))
                .thenReturn(runningSession(
                        100L,
                        10L,
                        RunningSessionStatus.CLOSED));

        mockMvc.perform(patch("/api/running-sessions/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "CLOSED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void cancelledStatusTargetUsesServiceErrorContract() throws Exception {
        when(runningSessionService.changeMySessionStatus(
                any(HttpSession.class),
                eq(100L),
                eq(RunningSessionStatus.CANCELLED)))
                .thenThrow(new CustomException(ErrorCode.INVALID_REQUEST));

        mockMvc.perform(patch("/api/running-sessions/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "CANCELLED" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void cancelUsesServiceInsteadOfInventingDelete() throws Exception {
        when(runningSessionService.cancelMySession(
                any(HttpSession.class),
                eq(100L)))
                .thenReturn(runningSession(
                        100L,
                        10L,
                        RunningSessionStatus.CANCELLED));

        mockMvc.perform(post("/api/running-sessions/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void invalidCreateRequestIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "meetingPlace": "",
                                  "scheduledAt": null,
                                  "capacity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(runningSessionService, never()).createSession(
                any(HttpSession.class),
                any(RunningSession.class));
    }

    private String validRequestBody() {
        return """
                {
                  "title": "주말 러닝",
                  "description": "함께 달립니다.",
                  "meetingPlace": "여의나루역",
                  "scheduledAt": "2026-07-01T09:00:00",
                  "capacity": 10,
                  "targetDistanceKm": 10.0,
                  "targetPaceSecondsPerKm": 330
                }
                """;
    }

    private RunningSession runningSession(
            Long id,
            Long hostMemberId,
            RunningSessionStatus status) {
        RunningSession runningSession = new RunningSession();
        runningSession.setId(id);
        runningSession.setHostMemberId(hostMemberId);
        runningSession.setTitle("주말 러닝");
        runningSession.setDescription("함께 달립니다.");
        runningSession.setMeetingPlace("여의나루역");
        runningSession.setScheduledAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        runningSession.setCapacity(10);
        runningSession.setTargetDistanceKm(BigDecimal.valueOf(10.0));
        runningSession.setTargetPaceSecondsPerKm(330);
        runningSession.setStatus(status);
        runningSession.setCreatedAt(LocalDateTime.of(2026, 6, 22, 10, 0));
        runningSession.setUpdatedAt(LocalDateTime.of(2026, 6, 22, 10, 0));
        return runningSession;
    }

    private RunningSessionParticipant participant(
            Long id,
            Long sessionId,
            Long memberId) {
        RunningSessionParticipant participant =
                new RunningSessionParticipant();
        participant.setId(id);
        participant.setSessionId(sessionId);
        participant.setMemberId(memberId);
        participant.setJoinedAt(LocalDateTime.of(2026, 6, 22, 11, 0));
        return participant;
    }
}
