package com.ssafy.ssafit.recommendation.domain;

import java.util.List;

public record WorkoutPlan(
        Integer totalDurationMinutes,
        String targetPace,
        Integer targetPaceSecondsPerKm,
        Integer cadenceMinSpm,
        Integer cadenceMaxSpm,
        Integer heartRateMinBpm,
        Integer heartRateMaxBpm,
        Integer musicBpmMin,
        Integer musicBpmMax,
        String coachingSummary,
        String musicStrategy,
        String provider,
        String modelName,
        boolean fallback,
        List<WorkoutPlanSegment> segments) {

    public WorkoutPlan {
        segments = segments == null ? List.of() : List.copyOf(segments);
    }
}
