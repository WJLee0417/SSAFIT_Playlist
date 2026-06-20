package com.ssafy.ssafit.runningrecord.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunningRecord {

    private Long id;
    private Long memberId;
    private LocalDate recordDate;
    private BigDecimal distanceKm;
    private Integer durationSeconds;
    private Integer paceSecondsPerKm;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
