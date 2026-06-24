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
import com.ssafy.ssafit.runningprofile.service.RunningProfileService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningRecordService {

    private static final int MAX_MEMO_LENGTH = 1000;
    private static final int MIN_CADENCE_SPM = 60;
    private static final int MAX_CADENCE_SPM = 240;
    private static final int MIN_HEART_RATE_BPM = 30;
    private static final int MAX_HEART_RATE_BPM = 240;

    private final RunningRecordMapper runningRecordMapper;
    private final AuthService authService;
    private final RunningProfileService runningProfileService;

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
        if (runningRecordMapper.insert(runningRecord) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        runningProfileService.refreshAutoTargets(loginMemberId);
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
        if (runningRecordMapper.update(runningRecord) != 1) {
            throw new CustomException(ErrorCode.RUNNING_RECORD_NOT_FOUND);
        }
        runningProfileService.refreshAutoTargets(loginMemberId);
        return runningRecord;
    }

    @Transactional
    public void deleteMyRecord(HttpSession session, Long runningRecordId) {
        Long loginMemberId = getLoginMemberId(session);
        RunningRecord runningRecord = getRecord(runningRecordId);
        validateOwner(loginMemberId, runningRecord);
        if (runningRecordMapper.delete(runningRecordId) != 1) {
            throw new CustomException(ErrorCode.RUNNING_RECORD_NOT_FOUND);
        }
        runningProfileService.refreshAutoTargets(loginMemberId);
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
        validateAverageCadence(runningRecord.getAverageCadence());
        validateAverageHeartRateBpm(runningRecord.getAverageHeartRateBpm());
    }

    private void validateAverageCadence(Integer averageCadence) {
        if (averageCadence != null
                && (averageCadence < MIN_CADENCE_SPM
                        || averageCadence > MAX_CADENCE_SPM)) {
            throw new CustomException(ErrorCode.INVALID_CADENCE_VALUE);
        }
    }

    private void validateAverageHeartRateBpm(Integer averageHeartRateBpm) {
        if (averageHeartRateBpm != null
                && (averageHeartRateBpm < MIN_HEART_RATE_BPM
                        || averageHeartRateBpm > MAX_HEART_RATE_BPM)) {
            throw new CustomException(ErrorCode.INVALID_HEART_RATE_VALUE);
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
