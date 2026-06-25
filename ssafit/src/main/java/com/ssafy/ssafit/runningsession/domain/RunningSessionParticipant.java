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
public class RunningSessionParticipant {

    private Long id;
    private Long sessionId;
    private Long memberId;
    private String memberNickname;
    private LocalDateTime joinedAt;
}
