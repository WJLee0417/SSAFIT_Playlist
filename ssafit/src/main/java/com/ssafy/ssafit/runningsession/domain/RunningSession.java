package com.ssafy.ssafit.runningsession.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunningSession {

    private Long id;
    private Long hostMemberId;
    private String title;
    private String description;
    private String meetingPlace;
    private LocalDateTime scheduledAt;
    private Integer capacity;
    private RunningSessionStatus status;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
