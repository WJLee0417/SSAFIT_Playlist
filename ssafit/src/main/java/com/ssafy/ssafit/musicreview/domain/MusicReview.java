package com.ssafy.ssafit.musicreview.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MusicReview {

    private Long id;
    private Long memberId;
    private Long musicId;
    private Integer preferenceScore;
    private Integer runningFitScore;
    private String reviewContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
