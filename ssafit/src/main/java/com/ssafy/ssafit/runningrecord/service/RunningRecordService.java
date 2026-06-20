package com.ssafy.ssafit.runningrecord.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.service.AuthService;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningRecordService {

    private static final int MAX_MEMO_LENGTH = 1000;

    private final RunningRecordMapper runningRecordMapper;
    private final AuthService authService;

    @Transactional
    public RunningRecord createRecord(HttpSession session, RunningRecord runningRecord) {
        Long loginMemberId = getLoginMemberId(session);
        validateRecordValues(runningRecord);

        runningRecord.setId(null);
        runningRecord.setMemberId(loginMemberId);
        runningRecord.setPaceSecondsPerKm(calculatePaceSecondsPerKm(runningRecord));
        runningRecord.setCreatedAt(null);
        runningRecord.setUpdatedAt(null);
        runningRecord.setDeletedAt(null);
        runningRecordMapper.insert(runningRecord);
        return runningRecord;
    }

    public List<RunningRecord> getMyRecordList(HttpSession session) {
        Long loginMemberId = getLoginMemberId(session);
        return runningRecordMapper.selectListByMemberId(loginMemberId);
    }

    public RunningRecord getMyRecordDetail(HttpSession session, Long runningRecordId) {
        Long loginMemberId = getLoginMemberId(session);
        RunningRecord runningRecord = getRecord(runningRecordId);
        validateOwner(loginMemberId, runningRecord);
        return runningRecord;
    }

    @Transactional
    public RunningRecord updateMyRecord(
            HttpSession session,
            RunningRecord runningRecord) {
        Long loginMemberId = getLoginMemberId(session);
        if (runningRecord == null || runningRecord.getId() == null) {
            throw new CustomException(ErrorCode.RUNNING_RECORD_NOT_FOUND);
        }

        RunningRecord existing = getRecord(runningRecord.getId());
        validateOwner(loginMemberId, existing);
        validateRecordValues(runningRecord);

        runningRecord.setMemberId(existing.getMemberId());
        runningRecord.setPaceSecondsPerKm(calculatePaceSecondsPerKm(runningRecord));
        runningRecord.setCreatedAt(existing.getCreatedAt());
        runningRecord.setDeletedAt(existing.getDeletedAt());
        runningRecordMapper.update(runningRecord);
        return runningRecord;
    }

    @Transactional
    public void deleteMyRecord(HttpSession session, Long runningRecordId) {
        Long loginMemberId = getLoginMemberId(session);
        RunningRecord runningRecord = getRecord(runningRecordId);
        validateOwner(loginMemberId, runningRecord);
        runningRecordMapper.delete(runningRecordId);
    }

    private Long getLoginMemberId(HttpSession session) {
        Member loginMember = authService.getLoginMember(session);
        if (loginMember == null || loginMember.getId() == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
        return loginMember.getId();
    }

    private RunningRecord getRecord(Long runningRecordId) {
        if (runningRecordId == null) {
            throw new CustomException(ErrorCode.RUNNING_RECORD_NOT_FOUND);
        }
        return runningRecordMapper.selectById(runningRecordId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.RUNNING_RECORD_NOT_FOUND));
    }

    private void validateOwner(Long loginMemberId, RunningRecord runningRecord) {
        if (!loginMemberId.equals(runningRecord.getMemberId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateRecordValues(RunningRecord runningRecord) {
        if (runningRecord == null
                || runningRecord.getRecordDate() == null
                || runningRecord.getDistanceKm() == null
                || runningRecord.getDistanceKm().compareTo(BigDecimal.ZERO) <= 0
                || runningRecord.getDurationSeconds() == null
                || runningRecord.getDurationSeconds() <= 0
                || (runningRecord.getMemo() != null
                        && runningRecord.getMemo().length() > MAX_MEMO_LENGTH)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private int calculatePaceSecondsPerKm(RunningRecord runningRecord) {
        try {
            int pace = BigDecimal.valueOf(runningRecord.getDurationSeconds())
                    .divide(runningRecord.getDistanceKm(), 0, RoundingMode.HALF_UP)
                    .intValueExact();
            if (pace <= 0) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            return pace;
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
