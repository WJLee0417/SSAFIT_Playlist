package com.ssafy.ssafit.recommendation.domain;

import com.ssafy.ssafit.music.domain.Music;

public record RecommendedMusic(
        Music music,
        Double cadenceScore,
        Double intensityScore,
        Double preferenceScore,
        Double finalScore,
        TempoMatchMode tempoMatchMode,
        String recommendationReason
) {
}
