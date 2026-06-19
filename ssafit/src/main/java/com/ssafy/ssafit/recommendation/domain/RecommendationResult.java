package com.ssafy.ssafit.recommendation.domain;

import java.util.List;

import com.ssafy.ssafit.music.domain.Music;

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
    private List<Music> musicList;
}
