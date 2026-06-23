package com.ssafy.ssafit.recommendation.domain;

public record IntensityResolutionResult(
        RunningIntensityGoal resolvedGoal,
        IntensitySource source,
        boolean sufficientHistory,
        Double averageHeartRateBpm,
        String explanation) {
}
