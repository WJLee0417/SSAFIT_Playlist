package com.ssafy.ssafit.runningsession.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.service.AuthService;
import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;
import com.ssafy.ssafit.runningsession.mapper.RunningSessionMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningSessionService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_MEETING_PLACE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    private final RunningSessionMapper runningSessionMapper;
    private final AuthService authService;

    @Transactional
    public RunningSession createSession(
            HttpSession session,
            RunningSession runningSession) {
        Long loginMemberId = getLoginMemberId(session);
        validateSessionValues(runningSession);

        runningSession.setId(null);
        runningSession.setHostMemberId(loginMemberId);
        runningSession.setStatus(RunningSessionStatus.OPEN);
        runningSession.setCancelledAt(null);
        runningSession.setCreatedAt(null);
        runningSession.setUpdatedAt(null);
        if (runningSessionMapper.insert(runningSession) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return runningSession;
    }

    public List<RunningSession> getSessionList(HttpSession session) {
        getLoginMemberId(session);
        return runningSessionMapper.selectList();
    }

    public RunningSession getSessionDetail(HttpSession session, Long sessionId) {
        getLoginMemberId(session);
        return getSession(sessionId);
    }

    @Transactional
    public RunningSession updateMySession(
            HttpSession session,
            RunningSession changes) {
        Long loginMemberId = getLoginMemberId(session);
        if (changes == null || changes.getId() == null) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }

        RunningSession existing = getSession(changes.getId());
        validateHost(loginMemberId, existing);
        validateNotCancelled(existing);
        validateSessionValues(changes);

        changes.setHostMemberId(existing.getHostMemberId());
        changes.setStatus(existing.getStatus());
        changes.setCancelledAt(existing.getCancelledAt());
        changes.setCreatedAt(existing.getCreatedAt());
        int updated = runningSessionMapper.update(changes);
        if (updated != 1) {
            throw new CustomException(ErrorCode.SESSION_CLOSED);
        }
        return changes;
    }

    @Transactional
    public RunningSession changeMySessionStatus(
            HttpSession session,
            Long sessionId,
            RunningSessionStatus status) {
        Long loginMemberId = getLoginMemberId(session);
        RunningSession runningSession = getSession(sessionId);
        validateHost(loginMemberId, runningSession);
        validateNotCancelled(runningSession);
        validateRecruitmentStatus(status);

        int updated = runningSessionMapper.updateStatus(sessionId, status);
        if (updated != 1) {
            throw new CustomException(ErrorCode.SESSION_CLOSED);
        }
        runningSession.setStatus(status);
        return runningSession;
    }

    @Transactional
    public RunningSession cancelMySession(HttpSession session, Long sessionId) {
        Long loginMemberId = getLoginMemberId(session);
        RunningSession runningSession = getSession(sessionId);
        validateHost(loginMemberId, runningSession);
        validateNotCancelled(runningSession);

        int updated = runningSessionMapper.cancel(sessionId);
        if (updated != 1) {
            throw new CustomException(ErrorCode.SESSION_CLOSED);
        }
        runningSession.setStatus(RunningSessionStatus.CANCELLED);
        return runningSession;
    }

    private Long getLoginMemberId(HttpSession session) {
        Member loginMember = authService.getLoginMember(session);
        if (loginMember == null || loginMember.getId() == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
        return loginMember.getId();
    }

    private RunningSession getSession(Long sessionId) {
        if (sessionId == null) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }
        return runningSessionMapper.selectDetail(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void validateHost(Long loginMemberId, RunningSession runningSession) {
        if (!loginMemberId.equals(runningSession.getHostMemberId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateNotCancelled(RunningSession runningSession) {
        if (runningSession.getStatus() == RunningSessionStatus.CANCELLED) {
            throw new CustomException(ErrorCode.SESSION_CLOSED);
        }
    }

    private void validateRecruitmentStatus(RunningSessionStatus status) {
        if (status == null || status == RunningSessionStatus.CANCELLED) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateSessionValues(RunningSession runningSession) {
        if (runningSession == null
                || isBlank(runningSession.getTitle())
                || runningSession.getTitle().trim().length() > MAX_TITLE_LENGTH
                || isBlank(runningSession.getMeetingPlace())
                || runningSession.getMeetingPlace().trim().length()
                        > MAX_MEETING_PLACE_LENGTH
                || runningSession.getScheduledAt() == null
                || runningSession.getCapacity() == null
                || runningSession.getCapacity() <= 0
                || (runningSession.getDescription() != null
                        && runningSession.getDescription().length()
                                > MAX_DESCRIPTION_LENGTH)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        runningSession.setTitle(runningSession.getTitle().trim());
        runningSession.setMeetingPlace(runningSession.getMeetingPlace().trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
