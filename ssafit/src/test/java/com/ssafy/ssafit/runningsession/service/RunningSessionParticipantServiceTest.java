package com.ssafy.ssafit.runningsession.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.service.AuthService;
import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionParticipant;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;
import com.ssafy.ssafit.runningsession.mapper.RunningSessionMapper;
import com.ssafy.ssafit.runningsession.mapper.RunningSessionParticipantMapper;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class RunningSessionParticipantServiceTest {

    @Mock
    private RunningSessionParticipantMapper participantMapper;

    @Mock
    private RunningSessionMapper runningSessionMapper;

    @Mock
    private AuthService authService;

    @Mock
    private HttpSession session;

    private RunningSessionParticipantService participantService;

    @BeforeEach
    void setUp() {
        participantService = new RunningSessionParticipantService(
                participantMapper,
                runningSessionMapper,
                authService);
    }

    @Test
    void joinOpenSessionSucceedsInRequiredOrder() {
        mockLoginMember(10L);
        RunningSession runningSession = session(100L, RunningSessionStatus.OPEN, 2);
        when(runningSessionMapper.selectByIdForUpdate(100L))
                .thenReturn(Optional.of(runningSession));
        when(participantMapper.existsParticipant(100L, 10L)).thenReturn(false);
        when(participantMapper.countParticipants(100L)).thenReturn(1);
        when(participantMapper.insert(any())).thenAnswer(invocation -> {
            RunningSessionParticipant participant = invocation.getArgument(0);
            participant.setId(1000L);
            return 1;
        });

        RunningSessionParticipant joined =
                participantService.joinSession(session, 100L);

        assertThat(joined.getId()).isEqualTo(1000L);
        assertThat(joined.getSessionId()).isEqualTo(100L);
        assertThat(joined.getMemberId()).isEqualTo(10L);

        InOrder order = inOrder(
                authService,
                runningSessionMapper,
                participantMapper);
        order.verify(authService).getLoginMember(session);
        order.verify(runningSessionMapper).selectByIdForUpdate(100L);
        order.verify(participantMapper).existsParticipant(100L, 10L);
        order.verify(participantMapper).countParticipants(100L);
        order.verify(participantMapper).insert(joined);
    }

    @Test
    void duplicateParticipationIsRejected() {
        mockJoinSession(RunningSessionStatus.OPEN, 10);
        when(participantMapper.existsParticipant(100L, 10L)).thenReturn(true);

        assertError(
                ErrorCode.DUPLICATE_SESSION_JOIN,
                () -> participantService.joinSession(session, 100L));

        verify(participantMapper, never()).countParticipants(100L);
        verify(participantMapper, never()).insert(any());
    }

    @Test
    void uniqueConstraintRaceIsConvertedAndLeavesNoSecondParticipation() {
        mockJoinSession(RunningSessionStatus.OPEN, 10);
        when(participantMapper.existsParticipant(100L, 10L)).thenReturn(false);
        when(participantMapper.countParticipants(100L)).thenReturn(0);
        when(participantMapper.insert(any()))
                .thenThrow(new DuplicateKeyException("duplicate participation"));

        assertError(
                ErrorCode.DUPLICATE_SESSION_JOIN,
                () -> participantService.joinSession(session, 100L));

        verify(participantMapper).insert(any());
    }

    @Test
    void insertFailureDoesNotReturnParticipation() {
        mockJoinSession(RunningSessionStatus.OPEN, 10);
        when(participantMapper.existsParticipant(100L, 10L)).thenReturn(false);
        when(participantMapper.countParticipants(100L)).thenReturn(0);
        when(participantMapper.insert(any()))
                .thenThrow(new IllegalStateException("insert failed"));

        assertThatThrownBy(() -> participantService.joinSession(session, 100L))
                .isInstanceOf(IllegalStateException.class);

        verify(participantMapper).insert(any());
    }

    @Test
    void fullSessionRejectsParticipation() {
        mockJoinSession(RunningSessionStatus.OPEN, 1);
        when(participantMapper.existsParticipant(100L, 10L)).thenReturn(false);
        when(participantMapper.countParticipants(100L)).thenReturn(1);

        assertError(
                ErrorCode.SESSION_FULL,
                () -> participantService.joinSession(session, 100L));

        verify(participantMapper, never()).insert(any());
    }

    @Test
    void closedSessionRejectsParticipation() {
        mockJoinSession(RunningSessionStatus.CLOSED, 10);

        assertError(
                ErrorCode.SESSION_CLOSED,
                () -> participantService.joinSession(session, 100L));

        verify(participantMapper, never())
                .existsParticipant(100L, 10L);
    }

    @Test
    void cancelledSessionRejectsParticipation() {
        mockJoinSession(RunningSessionStatus.CANCELLED, 10);

        assertError(
                ErrorCode.SESSION_CLOSED,
                () -> participantService.joinSession(session, 100L));
    }

    @Test
    void missingSessionRejectsParticipation() {
        mockLoginMember(10L);
        when(runningSessionMapper.selectByIdForUpdate(100L))
                .thenReturn(Optional.empty());

        assertError(
                ErrorCode.SESSION_NOT_FOUND,
                () -> participantService.joinSession(session, 100L));
    }

    @Test
    void unauthenticatedMemberCannotJoin() {
        when(authService.getLoginMember(session))
                .thenThrow(new CustomException(ErrorCode.AUTH_REQUIRED));

        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> participantService.joinSession(session, 100L));

        verify(runningSessionMapper, never()).selectByIdForUpdate(100L);
    }

    @Test
    void cancelMyParticipationSucceeds() {
        mockLoginMember(10L);
        mockExistingSession();
        when(participantMapper.existsParticipant(100L, 10L)).thenReturn(true);
        when(participantMapper.delete(100L, 10L)).thenReturn(1);

        participantService.cancelMyParticipation(session, 100L);

        verify(participantMapper).delete(100L, 10L);
    }

    @Test
    void memberCannotCancelAnotherMembersParticipation() {
        mockLoginMember(10L);
        mockExistingSession();
        when(participantMapper.existsParticipant(100L, 10L)).thenReturn(false);

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> participantService.cancelMyParticipation(session, 100L));

        verify(participantMapper, never()).delete(100L, 20L);
        verify(participantMapper, never()).delete(100L, 10L);
    }

    @Test
    void cancelledParticipationCanJoinAgain() {
        mockLoginMember(10L);
        RunningSession runningSession = session(100L, RunningSessionStatus.OPEN, 2);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(runningSession));
        when(participantMapper.existsParticipant(100L, 10L))
                .thenReturn(true, false);
        when(participantMapper.delete(100L, 10L)).thenReturn(1);
        when(runningSessionMapper.selectByIdForUpdate(100L))
                .thenReturn(Optional.of(runningSession));
        when(participantMapper.countParticipants(100L)).thenReturn(0);
        when(participantMapper.insert(any())).thenReturn(1);

        participantService.cancelMyParticipation(session, 100L);
        RunningSessionParticipant rejoined =
                participantService.joinSession(session, 100L);

        assertThat(rejoined.getMemberId()).isEqualTo(10L);
        verify(participantMapper).delete(100L, 10L);
        verify(participantMapper).insert(rejoined);
    }

    @Test
    void getSessionParticipantsReturnsParticipants() {
        mockLoginMember(10L);
        mockExistingSession();
        RunningSessionParticipant participant = participant(100L, 20L);
        when(participantMapper.selectParticipants(100L))
                .thenReturn(List.of(participant));

        List<RunningSessionParticipant> result =
                participantService.getSessionParticipants(session, 100L);

        assertThat(result).containsExactly(participant);
    }

    @Test
    void getParticipantCountReturnsCount() {
        mockLoginMember(10L);
        mockExistingSession();
        when(participantMapper.countParticipants(100L)).thenReturn(3);

        assertThat(participantService.getParticipantCount(session, 100L))
                .isEqualTo(3);
    }

    private void mockJoinSession(RunningSessionStatus status, int capacity) {
        mockLoginMember(10L);
        when(runningSessionMapper.selectByIdForUpdate(100L))
                .thenReturn(Optional.of(session(100L, status, capacity)));
    }

    private void mockExistingSession() {
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(
                        session(100L, RunningSessionStatus.OPEN, 10)));
    }

    private void mockLoginMember(Long memberId) {
        Member member = new Member();
        member.setId(memberId);
        when(authService.getLoginMember(session)).thenReturn(member);
    }

    private RunningSession session(
            Long id,
            RunningSessionStatus status,
            int capacity) {
        RunningSession runningSession = new RunningSession();
        runningSession.setId(id);
        runningSession.setStatus(status);
        runningSession.setCapacity(capacity);
        return runningSession;
    }

    private RunningSessionParticipant participant(Long sessionId, Long memberId) {
        RunningSessionParticipant participant = new RunningSessionParticipant();
        participant.setSessionId(sessionId);
        participant.setMemberId(memberId);
        return participant;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
