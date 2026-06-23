package com.ssafy.ssafit.runningprofile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.mapper.MemberMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningProfileService {

    private static final int MIN_BPM = 60;
    private static final int MAX_BPM = 220;
    private static final int MIN_CADENCE_SPM = 60;
    private static final int MAX_CADENCE_SPM = 240;

    private final RunningProfileMapper runningProfileMapper;
    private final MemberMapper memberMapper;

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
        validateProfileValues(runningProfile);

        return runningProfileMapper.selectByMemberId(targetMemberId)
                .map(existing -> updateExisting(existing, runningProfile))
                .orElseGet(() -> insertNew(runningProfile));
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
        Integer minBpm = runningProfile.getTargetBpmMin();
        Integer maxBpm = runningProfile.getTargetBpmMax();

        if (runningProfile.getRunningIntensity() == null
                || minBpm == null
                || maxBpm == null
                || minBpm < MIN_BPM
                || maxBpm > MAX_BPM
                || minBpm > maxBpm) {
            throw new CustomException(ErrorCode.INVALID_PROFILE_VALUE);
        }

        validateCadenceRange(
                runningProfile.getTargetCadenceMin(),
                runningProfile.getTargetCadenceMax());
    }

    private void validateCadenceRange(
            Integer targetCadenceMin,
            Integer targetCadenceMax) {
        if (targetCadenceMin == null && targetCadenceMax == null) {
            return;
        }
        if (targetCadenceMin == null
                || targetCadenceMax == null) {
            throw new CustomException(ErrorCode.INVALID_CADENCE_RANGE);
        }
        if (targetCadenceMin < MIN_CADENCE_SPM
                || targetCadenceMin > MAX_CADENCE_SPM
                || targetCadenceMax < MIN_CADENCE_SPM
                || targetCadenceMax > MAX_CADENCE_SPM) {
            throw new CustomException(ErrorCode.INVALID_CADENCE_VALUE);
        }
        if (targetCadenceMin > targetCadenceMax) {
            throw new CustomException(ErrorCode.INVALID_CADENCE_RANGE);
        }
    }
}
