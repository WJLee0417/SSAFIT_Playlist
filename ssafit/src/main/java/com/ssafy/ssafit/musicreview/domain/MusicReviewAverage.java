package com.ssafy.ssafit.musicreview.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MusicReviewAverage {

    private Long musicId;
    private Double averagePreferenceScore;
    private Double averageRunningFitScore;
    private Long reviewCount;
}
