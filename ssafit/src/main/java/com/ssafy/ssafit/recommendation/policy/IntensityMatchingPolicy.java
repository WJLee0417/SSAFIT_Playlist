package com.ssafy.ssafit.recommendation.policy;

import org.springframework.stereotype.Component;

import com.ssafy.ssafit.music.domain.MusicEnergyLevel;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;

/**
 * Matches a running intensity goal to a curated music energy tag.
 *
 * <p>Scores are ordinal: 3 (high), 2 (medium), 1 (low). A null result means
 * that the score cannot be calculated because either input is unknown.</p>
 */
@Component
public class IntensityMatchingPolicy {

    public static final int HIGH_SCORE = 3;
    public static final int MEDIUM_SCORE = 2;
    public static final int LOW_SCORE = 1;

    public Integer calculateIntensityScore(
            RunningIntensityGoal intensityGoal,
            MusicEnergyLevel musicEnergyLevel) {
        if (intensityGoal == null || musicEnergyLevel == null) {
            return null;
        }
        return switch (intensityGoal) {
            case EASY -> switch (musicEnergyLevel) {
                case CALM -> HIGH_SCORE;
                case STEADY -> MEDIUM_SCORE;
                case ENERGETIC, PEAK -> LOW_SCORE;
            };
            case MODERATE -> switch (musicEnergyLevel) {
                case CALM, ENERGETIC -> MEDIUM_SCORE;
                case STEADY -> HIGH_SCORE;
                case PEAK -> LOW_SCORE;
            };
            case HIGH -> switch (musicEnergyLevel) {
                case CALM -> LOW_SCORE;
                case STEADY -> MEDIUM_SCORE;
                case ENERGETIC, PEAK -> HIGH_SCORE;
            };
        };
    }
}
