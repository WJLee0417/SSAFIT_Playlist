package com.ssafy.ssafit.recommendation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.recommendation.domain.CadenceResolutionResult;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CadenceResolutionService {

    private static final int MIN_CADENCE = 60;
    private static final int MAX_CADENCE = 240;
    private static final int HISTORY_LIMIT = 5;

    private final RunningRecordMapper runningRecordMapper;

    public CadenceResolutionResult resolve(
            Long memberId,
            Integer requestedCadenceSpm,
            RunningProfile profile) {
        if (requestedCadenceSpm != null) {
            validateCadence(requestedCadenceSpm);
            return new CadenceResolutionResult(
                    requestedCadenceSpm,
                    "USER_SELECTED",
                    "요청에서 직접 지정한 목표 케이던스 "
                            + requestedCadenceSpm
                            + " SPM을 사용했습니다.");
        }

        if (profile != null
                && profile.getTargetCadenceMin() != null
                && profile.getTargetCadenceMax() != null) {
            int midpoint = midpoint(
                    profile.getTargetCadenceMin(),
                    profile.getTargetCadenceMax());
            validateCadence(midpoint);
            return new CadenceResolutionResult(
                    midpoint,
                    "PROFILE_MIDPOINT",
                    "러닝 프로필 목표 케이던스 범위의 중앙값 "
                            + midpoint
                            + " SPM을 사용했습니다.");
        }

        List<Integer> recentCadences =
                runningRecordMapper.selectRecentAverageCadences(
                        memberId,
                        HISTORY_LIMIT);
        Integer representative = median(recentCadences);
        if (representative != null) {
            validateCadence(representative);
            return new CadenceResolutionResult(
                    representative,
                    "RECENT_HISTORY_MEDIAN",
                    "최근 유효 러닝 기록의 평균 케이던스 중앙값 "
                            + representative
                            + " SPM을 참고했습니다.");
        }

        return new CadenceResolutionResult(
                null,
                "UNKNOWN",
                "목표 케이던스 정보가 없어 케이던스 매칭을 적용하지 않았습니다.");
    }

    private int midpoint(int minimum, int maximum) {
        return minimum + (maximum - minimum) / 2;
    }

    private Integer median(List<Integer> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        List<Integer> values = new ArrayList<>(source.stream()
                .filter(value -> value != null)
                .toList());
        if (values.isEmpty()) {
            return null;
        }
        Collections.sort(values);
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle);
        }
        return midpoint(values.get(middle - 1), values.get(middle));
    }

    private void validateCadence(Integer cadenceSpm) {
        if (cadenceSpm < MIN_CADENCE || cadenceSpm > MAX_CADENCE) {
            throw new CustomException(ErrorCode.INVALID_CADENCE_VALUE);
        }
    }
}
