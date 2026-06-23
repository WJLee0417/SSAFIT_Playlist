package com.ssafy.ssafit.recommendation.policy;

import org.springframework.stereotype.Component;

import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;

@Component
public class CadenceMatchingPolicy {

    private static final double SCORE_DEDUCTION_PER_SPM = 5.0;

    public CadenceMatchResult calculate(
            Integer targetCadenceSpm,
            Integer musicBpm) {
        if (targetCadenceSpm == null || musicBpm == null) {
            return new CadenceMatchResult(null, TempoMatchMode.NONE, null);
        }

        int directDifference = Math.abs(targetCadenceSpm - musicBpm);
        int halfTimeDifference = Math.abs(targetCadenceSpm - musicBpm * 2);
        TempoMatchMode mode = directDifference <= halfTimeDifference
                ? TempoMatchMode.DIRECT
                : TempoMatchMode.HALF_TIME;
        int difference = Math.min(directDifference, halfTimeDifference);
        double score = Math.max(
                0.0,
                100.0 - difference * SCORE_DEDUCTION_PER_SPM);
        return new CadenceMatchResult(score, mode, difference);
    }

    public record CadenceMatchResult(
            Double score,
            TempoMatchMode mode,
            Integer difference
    ) {
    }
}
