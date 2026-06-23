package com.ssafy.ssafit.runningprofile.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunningProfile {

    private Long id;
    private Long memberId;
    private RunningIntensity runningIntensity;
    private Integer targetBpmMin;
    private Integer targetBpmMax;
    private Integer targetCadenceMin;
    private Integer targetCadenceMax;
    private String preferredGenre;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
