package com.ssafy.ssafit.recommendation.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResult {

    private RecommendationLog recommendationLog;
    private List<RecommendedMusic> musicList;
    private Integer resolvedTargetCadenceSpm;
    private String cadenceExplanation;
    private RunningIntensityGoal resolvedIntensityGoal;
    private IntensitySource intensitySource;
    private Double averageHeartRateBpm;
    private String intensityExplanation;
    private Integer page;
    private Integer pageSize;
    private boolean hasPrevious;
    private boolean hasNext;
    private boolean cadenceApplied;
    private boolean intensityApplied;
    private Double targetDistanceKm;
    private WorkoutPlan workoutPlan;

    public RecommendationResult(
            RecommendationLog recommendationLog,
            List<RecommendedMusic> musicList) {
        this.recommendationLog = recommendationLog;
        this.musicList = musicList;
    }
}
