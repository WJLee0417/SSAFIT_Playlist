package com.ssafy.ssafit.runningsession.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningSessionParticipantService {

    private final RunningSessionParticipantMapper participantMapper;
    private final RunningSessionMapper runningSessionMapper;
    private final AuthService authService;

    @Transactional
    public RunningSessionParticipant joinSession(
            HttpSession session,
            Long sessionId) {
        Long loginMemberId = getLoginMemberId(session);
        RunningSession runningSession = getSessionForUpdate(sessionId);
        validateOpen(runningSession);

        if (participantMapper.existsParticipant(sessionId, loginMemberId)) {
            throw new CustomException(ErrorCode.DUPLICATE_SESSION_JOIN);
        }

        int participantCount = participantMapper.countParticipants(sessionId);
        if (participantCount >= runningSession.getCapacity()) {
            throw new CustomException(ErrorCode.SESSION_FULL);
        }

        RunningSessionParticipant participant = new RunningSessionParticipant();
        participant.setSessionId(sessionId);
        participant.setMemberId(loginMemberId);
        try {
            participantMapper.insert(participant);
        } catch (DuplicateKeyException exception) {
            throw new CustomException(ErrorCode.DUPLICATE_SESSION_JOIN);
        }
        return participant;
    }

    @Transactional
    public void cancelMyParticipation(HttpSession session, Long sessionId) {
        Long loginMemberId = getLoginMemberId(session);
        validateSessionExists(sessionId);

        if (!participantMapper.existsParticipant(sessionId, loginMemberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        int deleted = participantMapper.delete(sessionId, loginMemberId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    public List<RunningSessionParticipant> getSessionParticipants(
            HttpSession session,
            Long sessionId) {
        getLoginMemberId(session);
        validateSessionExists(sessionId);
        return participantMapper.selectParticipants(sessionId);
    }

    public int getParticipantCount(HttpSession session, Long sessionId) {
        getLoginMemberId(session);
        validateSessionExists(sessionId);
        return participantMapper.countParticipants(sessionId);
    }

    private Long getLoginMemberId(HttpSession session) {
        Member loginMember = authService.getLoginMember(session);
        if (loginMember == null || loginMember.getId() == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
        return loginMember.getId();
    }

    private RunningSession getSessionForUpdate(Long sessionId) {
        if (sessionId == null) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }
        return runningSessionMapper.selectByIdForUpdate(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void validateSessionExists(Long sessionId) {
        if (sessionId == null
                || runningSessionMapper.selectDetail(sessionId).isEmpty()) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }
    }

    private void validateOpen(RunningSession runningSession) {
        if (runningSession.getStatus() != RunningSessionStatus.OPEN) {
            throw new CustomException(ErrorCode.SESSION_CLOSED);
        }
    }
}
