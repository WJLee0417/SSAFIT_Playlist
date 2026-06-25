package com.ssafy.ssafit.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.recommendation.domain.IntensityResolutionResult;
import com.ssafy.ssafit.recommendation.domain.IntensitySource;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningIntensityResolutionService {

    private static final int MINIMUM_HISTORY_COUNT = 5;
    private static final int EASY_REFERENCE_MAX_EXCLUSIVE = 120;
    private static final int MODERATE_REFERENCE_MAX_EXCLUSIVE = 160;

    private final RunningRecordMapper runningRecordMapper;

    public IntensityResolutionResult resolve(
            Long memberId,
            RunningIntensityGoal userSelectedGoal) {
        if (userSelectedGoal != null) {
            return new IntensityResolutionResult(
                    userSelectedGoal,
                    IntensitySource.USER_SELECTED,
                    true,
                    null,
                    "사용자가 추천 요청에서 선택한 운동 강도 목표를 적용했습니다.");
        }

        List<Integer> recentHeartRates =
                runningRecordMapper.selectRecentAverageHeartRates(
                        memberId,
                        MINIMUM_HISTORY_COUNT);
        if (recentHeartRates == null
                || recentHeartRates.size() < MINIMUM_HISTORY_COUNT) {
            return new IntensityResolutionResult(
                    null,
                    IntensitySource.UNKNOWN,
                    false,
                    null,
                    "평균 심박수 BPM이 있는 최근 러닝 기록이 5건 미만이므로 운동 강도 목표를 추정하지 않았습니다.");
        }

        double averageHeartRate = recentHeartRates.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElseThrow();
        RunningIntensityGoal resolvedGoal = referenceGoal(averageHeartRate);
        return new IntensityResolutionResult(
                resolvedGoal,
                IntensitySource.HISTORY_REFERENCE,
                true,
                averageHeartRate,
                "최근 5건의 평균 심박수 BPM 이력을 서비스용 참고 구간으로만 해석했습니다. "
                        + "이는 의료적 진단이 아니며 음악 템포 BPM과도 별개의 값입니다.");
    }

    private RunningIntensityGoal referenceGoal(double averageHeartRateBpm) {
        if (averageHeartRateBpm < EASY_REFERENCE_MAX_EXCLUSIVE) {
            return RunningIntensityGoal.EASY;
        }
        if (averageHeartRateBpm < MODERATE_REFERENCE_MAX_EXCLUSIVE) {
            return RunningIntensityGoal.MODERATE;
        }
        return RunningIntensityGoal.HIGH;
    }
}
