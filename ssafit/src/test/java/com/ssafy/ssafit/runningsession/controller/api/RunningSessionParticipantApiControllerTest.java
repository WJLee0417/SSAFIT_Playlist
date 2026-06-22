package com.ssafy.ssafit.runningsession.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.common.exception.GlobalExceptionHandler;
import com.ssafy.ssafit.runningsession.domain.RunningSessionParticipant;
import com.ssafy.ssafit.runningsession.service.RunningSessionParticipantService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class RunningSessionParticipantApiControllerTest {

    @Mock
    private RunningSessionParticipantService participantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new RunningSessionParticipantApiController(
                                participantService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void joinUsesSessionIdentityAndReturnsSafeDto() throws Exception {
        when(participantService.joinSession(
                any(HttpSession.class),
                eq(100L)))
                .thenReturn(participant(1000L, 100L, 10L));

        mockMvc.perform(post("/api/running-sessions/100/participants/me"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1000))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.memberId").value(10))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist());

        verify(participantService).joinSession(
                any(HttpSession.class),
                eq(100L));
    }

    @Test
    void participantListExposesNoMemberPrivateFields() throws Exception {
        when(participantService.getSessionParticipants(
                any(HttpSession.class),
                eq(100L)))
                .thenReturn(List.of(participant(1000L, 100L, 10L)));

        mockMvc.perform(get("/api/running-sessions/100/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(10))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data[0].nickname").doesNotExist());
    }

    @Test
    void duplicateJoinUsesConflictErrorContract() throws Exception {
        mockJoinFailure(ErrorCode.DUPLICATE_SESSION_JOIN);

        mockMvc.perform(post("/api/running-sessions/100/participants/me"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("DUPLICATE_SESSION_JOIN"));
    }

    @Test
    void fullSessionUsesConflictErrorContract() throws Exception {
        mockJoinFailure(ErrorCode.SESSION_FULL);

        mockMvc.perform(post("/api/running-sessions/100/participants/me"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_FULL"));
    }

    @Test
    void closedOrCancelledSessionUsesConflictErrorContract() throws Exception {
        mockJoinFailure(ErrorCode.SESSION_CLOSED);

        mockMvc.perform(post("/api/running-sessions/100/participants/me"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOSED"));
    }

    @Test
    void missingSessionUsesNotFoundErrorContract() throws Exception {
        mockJoinFailure(ErrorCode.SESSION_NOT_FOUND);

        mockMvc.perform(post("/api/running-sessions/100/participants/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void unauthenticatedJoinUsesAuthenticationErrorContract() throws Exception {
        mockJoinFailure(ErrorCode.AUTH_REQUIRED);

        mockMvc.perform(post("/api/running-sessions/100/participants/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void cancelParticipationOnlyTargetsCurrentMember() throws Exception {
        mockMvc.perform(delete("/api/running-sessions/100/participants/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(participantService).cancelMyParticipation(
                any(HttpSession.class),
                eq(100L));
    }

    @Test
    void cancellingMissingParticipationUsesAccessDeniedContract()
            throws Exception {
        org.mockito.Mockito.doThrow(
                        new CustomException(ErrorCode.ACCESS_DENIED))
                .when(participantService)
                .cancelMyParticipation(any(HttpSession.class), eq(100L));

        mockMvc.perform(delete("/api/running-sessions/100/participants/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private void mockJoinFailure(ErrorCode errorCode) {
        when(participantService.joinSession(
                any(HttpSession.class),
                eq(100L)))
                .thenThrow(new CustomException(errorCode));
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
