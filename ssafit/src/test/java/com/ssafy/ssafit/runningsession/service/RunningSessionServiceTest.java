package com.ssafy.ssafit.runningsession.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.service.AuthService;
import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;
import com.ssafy.ssafit.runningsession.mapper.RunningSessionMapper;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class RunningSessionServiceTest {

    @Mock
    private RunningSessionMapper runningSessionMapper;

    @Mock
    private AuthService authService;

    @Mock
    private HttpSession session;

    private RunningSessionService runningSessionService;

    @BeforeEach
    void setUp() {
        runningSessionService =
                new RunningSessionService(runningSessionMapper, authService);
    }

    @Test
    void createSessionUsesLoginMemberAndOpenStatus() {
        mockLoginMember(10L);
        RunningSession input = validSession(null, 999L, RunningSessionStatus.CANCELLED);
        when(runningSessionMapper.insert(input)).thenAnswer(invocation -> {
            input.setId(100L);
            return 1;
        });

        RunningSession created =
                runningSessionService.createSession(session, input);

        assertThat(created.getId()).isEqualTo(100L);
        assertThat(created.getHostMemberId()).isEqualTo(10L);
        assertThat(created.getStatus()).isEqualTo(RunningSessionStatus.OPEN);
        assertThat(created.getTitle()).isEqualTo("주말 러닝");
        verify(runningSessionMapper).insert(input);
    }

    @Test
    void unauthenticatedMemberCannotCreateSession() {
        when(authService.getLoginMember(session))
                .thenThrow(new CustomException(ErrorCode.AUTH_REQUIRED));

        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> runningSessionService.createSession(
                        session,
                        validSession(null, null, null)));

        verify(runningSessionMapper, never()).insert(any());
    }

    @Test
    void unauthenticatedMemberCannotReadSessionList() {
        when(authService.getLoginMember(session))
                .thenThrow(new CustomException(ErrorCode.AUTH_REQUIRED));

        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> runningSessionService.getSessionList(session));

        verify(runningSessionMapper, never()).selectList();
    }

    @Test
    void getSessionListReturnsSessions() {
        mockLoginMember(10L);
        RunningSession runningSession =
                validSession(100L, 20L, RunningSessionStatus.OPEN);
        when(runningSessionMapper.selectList()).thenReturn(List.of(runningSession));

        List<RunningSession> result =
                runningSessionService.getSessionList(session);

        assertThat(result).containsExactly(runningSession);
    }

    @Test
    void getSessionDetailReturnsSession() {
        mockLoginMember(10L);
        RunningSession runningSession =
                validSession(100L, 20L, RunningSessionStatus.OPEN);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(runningSession));

        RunningSession result =
                runningSessionService.getSessionDetail(session, 100L);

        assertThat(result).isSameAs(runningSession);
    }

    @Test
    void getSessionDetailRejectsMissingSession() {
        mockLoginMember(10L);
        when(runningSessionMapper.selectDetail(100L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.SESSION_NOT_FOUND,
                () -> runningSessionService.getSessionDetail(session, 100L));
    }

    @Test
    void updateMySessionSucceedsForHost() {
        mockLoginMember(10L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 12, 0);
        RunningSession existing =
                validSession(100L, 10L, RunningSessionStatus.CLOSED);
        existing.setCreatedAt(createdAt);
        RunningSession changes =
                validSession(100L, 999L, RunningSessionStatus.OPEN);
        changes.setTitle("  변경 모임  ");
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));
        when(runningSessionMapper.update(changes)).thenReturn(1);

        RunningSession updated =
                runningSessionService.updateMySession(session, changes);

        assertThat(updated.getHostMemberId()).isEqualTo(10L);
        assertThat(updated.getStatus()).isEqualTo(RunningSessionStatus.CLOSED);
        assertThat(updated.getTitle()).isEqualTo("변경 모임");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        verify(runningSessionMapper).update(changes);
    }

    @Test
    void updateMySessionRejectsOtherMember() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 20L, RunningSessionStatus.OPEN);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> runningSessionService.updateMySession(
                        session,
                        validSession(100L, 10L, RunningSessionStatus.OPEN)));

        verify(runningSessionMapper, never()).update(any());
    }

    @Test
    void updateMySessionRejectsMissingSession() {
        mockLoginMember(10L);
        when(runningSessionMapper.selectDetail(100L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.SESSION_NOT_FOUND,
                () -> runningSessionService.updateMySession(
                        session,
                        validSession(100L, 10L, RunningSessionStatus.OPEN)));

        verify(runningSessionMapper, never()).update(any());
    }

    @Test
    void updateMySessionRejectsCancelledSession() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 10L, RunningSessionStatus.CANCELLED);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.SESSION_CLOSED,
                () -> runningSessionService.updateMySession(
                        session,
                        validSession(100L, 10L, RunningSessionStatus.OPEN)));
    }

    @Test
    void changeMySessionStatusSucceedsForHost() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 10L, RunningSessionStatus.OPEN);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));
        when(runningSessionMapper.updateStatus(
                100L,
                RunningSessionStatus.CLOSED)).thenReturn(1);

        RunningSession changed = runningSessionService.changeMySessionStatus(
                session,
                100L,
                RunningSessionStatus.CLOSED);

        assertThat(changed.getStatus()).isEqualTo(RunningSessionStatus.CLOSED);
        verify(runningSessionMapper)
                .updateStatus(100L, RunningSessionStatus.CLOSED);
    }

    @Test
    void changeMySessionStatusRejectsOtherMember() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 20L, RunningSessionStatus.OPEN);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> runningSessionService.changeMySessionStatus(
                        session,
                        100L,
                        RunningSessionStatus.CLOSED));

        verify(runningSessionMapper, never())
                .updateStatus(100L, RunningSessionStatus.CLOSED);
    }

    @Test
    void changeMySessionStatusRejectsCancelledTargetStatus() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 10L, RunningSessionStatus.OPEN);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningSessionService.changeMySessionStatus(
                        session,
                        100L,
                RunningSessionStatus.CANCELLED));
    }

    @Test
    void changeMySessionStatusRejectsConcurrentStateChange() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 10L, RunningSessionStatus.OPEN);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));
        when(runningSessionMapper.updateStatus(
                100L,
                RunningSessionStatus.CLOSED)).thenReturn(0);

        assertError(
                ErrorCode.SESSION_CLOSED,
                () -> runningSessionService.changeMySessionStatus(
                        session,
                        100L,
                        RunningSessionStatus.CLOSED));
    }

    @Test
    void cancelMySessionSucceedsForHost() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 10L, RunningSessionStatus.CLOSED);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));
        when(runningSessionMapper.cancel(100L)).thenReturn(1);

        RunningSession cancelled =
                runningSessionService.cancelMySession(session, 100L);

        assertThat(cancelled.getStatus())
                .isEqualTo(RunningSessionStatus.CANCELLED);
        verify(runningSessionMapper).cancel(100L);
    }

    @Test
    void cancelMySessionRejectsOtherMember() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 20L, RunningSessionStatus.OPEN);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> runningSessionService.cancelMySession(session, 100L));

        verify(runningSessionMapper, never()).cancel(100L);
    }

    @Test
    void cancelMySessionRejectsAlreadyCancelledSession() {
        mockLoginMember(10L);
        RunningSession existing =
                validSession(100L, 10L, RunningSessionStatus.CANCELLED);
        when(runningSessionMapper.selectDetail(100L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.SESSION_CLOSED,
                () -> runningSessionService.cancelMySession(session, 100L));
    }

    @Test
    void createSessionRejectsZeroCapacity() {
        mockLoginMember(10L);
        RunningSession input = validSession(null, null, null);
        input.setCapacity(0);

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningSessionService.createSession(session, input));

        verify(runningSessionMapper, never()).insert(any());
    }

    @Test
    void createSessionRejectsNegativeCapacity() {
        mockLoginMember(10L);
        RunningSession input = validSession(null, null, null);
        input.setCapacity(-1);

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningSessionService.createSession(session, input));
    }

    @Test
    void createSessionRejectsMissingRequiredFields() {
        mockLoginMember(10L);
        RunningSession missingTitle = validSession(null, null, null);
        missingTitle.setTitle(" ");

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningSessionService.createSession(session, missingTitle));

        RunningSession missingPlace = validSession(null, null, null);
        missingPlace.setMeetingPlace(null);
        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningSessionService.createSession(session, missingPlace));

        RunningSession missingSchedule = validSession(null, null, null);
        missingSchedule.setScheduledAt(null);
        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningSessionService.createSession(session, missingSchedule));
    }

    private void mockLoginMember(Long memberId) {
        Member member = new Member();
        member.setId(memberId);
        when(authService.getLoginMember(session)).thenReturn(member);
    }

    private RunningSession validSession(
            Long id,
            Long hostMemberId,
            RunningSessionStatus status) {
        RunningSession runningSession = new RunningSession();
        runningSession.setId(id);
        runningSession.setHostMemberId(hostMemberId);
        runningSession.setTitle("  주말 러닝  ");
        runningSession.setDescription("한강에서 함께 달립니다.");
        runningSession.setMeetingPlace("  여의나루역  ");
        runningSession.setScheduledAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        runningSession.setCapacity(10);
        runningSession.setStatus(status);
        return runningSession;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
