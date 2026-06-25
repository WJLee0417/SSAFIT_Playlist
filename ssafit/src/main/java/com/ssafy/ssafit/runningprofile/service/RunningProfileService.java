package com.ssafy.ssafit.runningprofile.service;

import java.util.IntSummaryStatistics;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.mapper.MemberMapper;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningProfileService {

    private static final int MIN_AGE = 10;
    private static final int MAX_AGE = 100;
    private static final int RECENT_RECORD_LIMIT = 10;

    private final RunningProfileMapper runningProfileMapper;
    private final MemberMapper memberMapper;
    private final RunningRecordMapper runningRecordMapper;

    public RunningProfile getProfile(Long loginMemberId, Long targetMemberId) {
        validateSelf(loginMemberId, targetMemberId);
        validateMemberExists(targetMemberId);

        return runningProfileMapper.selectByMemberId(targetMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));
    }

    @Transactional
    public RunningProfile saveOrUpdate(Long loginMemberId, RunningProfile runningProfile) {
        if (runningProfile == null) {
            throw new CustomException(ErrorCode.INVALID_PROFILE_VALUE);
        }

        Long targetMemberId = runningProfile.getMemberId();
        if (targetMemberId == null) {
            targetMemberId = loginMemberId;
            runningProfile.setMemberId(targetMemberId);
        }

        validateSelf(loginMemberId, targetMemberId);
        validateMemberExists(targetMemberId);
        applyDerivedValues(runningProfile);
        validateProfileValues(runningProfile);

        return runningProfileMapper.selectByMemberId(targetMemberId)
                .map(existing -> updateExisting(existing, runningProfile))
                .orElseGet(() -> insertNew(runningProfile));
    }

    @Transactional
    public void refreshAutoTargets(Long memberId) {
        if (memberId == null) {
            return;
        }

        List<RunningRecord> records = runningRecordMapper.selectRecentByMemberId(
                memberId,
                RECENT_RECORD_LIMIT);
        IntSummaryStatistics heartRateSummary = records.stream()
                .map(RunningRecord::getAverageHeartRateBpm)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .summaryStatistics();
        IntSummaryStatistics cadenceSummary = records.stream()
                .map(RunningRecord::getAverageCadence)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .summaryStatistics();

        runningProfileMapper.updateAutoTargets(
                memberId,
                minOrNull(heartRateSummary),
                maxOrNull(heartRateSummary),
                minOrNull(cadenceSummary),
                maxOrNull(cadenceSummary));
    }

    private RunningProfile insertNew(RunningProfile runningProfile) {
        if (runningProfileMapper.insert(runningProfile) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return runningProfile;
    }

    private RunningProfile updateExisting(
            RunningProfile existing,
            RunningProfile runningProfile) {
        runningProfile.setId(existing.getId());
        runningProfile.setCreatedAt(existing.getCreatedAt());
        preserveAutoTargets(existing, runningProfile);
        if (runningProfileMapper.update(runningProfile) != 1) {
            throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
        }
        return runningProfile;
    }

    private void validateMemberExists(Long memberId) {
        if (memberMapper.selectById(memberId).isEmpty()) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private void validateSelf(Long loginMemberId, Long targetMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
        if (targetMemberId == null || !loginMemberId.equals(targetMemberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateProfileValues(RunningProfile runningProfile) {
        Integer age = runningProfile.getAge();
        if (age != null && (age < MIN_AGE || age > MAX_AGE)) {
            throw new CustomException(ErrorCode.INVALID_PROFILE_VALUE);
        }
    }

    private void applyDerivedValues(RunningProfile runningProfile) {
        Integer age = runningProfile.getAge();
        runningProfile.setEstimatedMaxHeartRateBpm(
                age == null ? null : 220 - age);
    }

    private void preserveAutoTargets(
            RunningProfile existing,
            RunningProfile runningProfile) {
        runningProfile.setTargetBpmMin(existing.getTargetBpmMin());
        runningProfile.setTargetBpmMax(existing.getTargetBpmMax());
        runningProfile.setTargetCadenceMin(existing.getTargetCadenceMin());
        runningProfile.setTargetCadenceMax(existing.getTargetCadenceMax());
    }

    private Integer minOrNull(IntSummaryStatistics statistics) {
        return statistics.getCount() == 0 ? null : statistics.getMin();
    }

    private Integer maxOrNull(IntSummaryStatistics statistics) {
        return statistics.getCount() == 0 ? null : statistics.getMax();
    }
}
