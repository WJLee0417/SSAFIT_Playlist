package com.ssafy.ssafit.music.domain;

import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MusicSearchCondition {

    private String keyword;
    private String genre;
    private Integer minBpm;
    private Integer maxBpm;
    private RunningIntensity runningIntensity;
    private MusicSortType sortType;
    private Integer page;
    private Integer size;
    private Integer offset;
}
