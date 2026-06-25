package com.ssafy.ssafit.music.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MusicPreferenceScore {

    private Long musicId;
    private Double preferenceScore;
}
