package com.ssafy.ssafit.recommendation.domain;

import java.time.LocalDateTime;

import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationLog {

    private Long id;
    private Long memberId;
    private RunningIntensity requestedIntensity;
    private Integer requestedBpmMin;
    private Integer requestedBpmMax;
    private String requestedGenre;
    private String requestConditions;
    private String resultSummary;
    private String recommendationReason;
    private Long savedPlaylistId;
    private LocalDateTime createdAt;
}
