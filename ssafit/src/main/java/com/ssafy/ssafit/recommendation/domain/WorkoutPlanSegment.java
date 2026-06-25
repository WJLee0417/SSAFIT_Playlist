package com.ssafy.ssafit.recommendation.domain;

public record WorkoutPlanSegment(
        String name,
        Integer durationMinutes,
        Integer bpmMin,
        Integer bpmMax,
        String purpose) {
}
