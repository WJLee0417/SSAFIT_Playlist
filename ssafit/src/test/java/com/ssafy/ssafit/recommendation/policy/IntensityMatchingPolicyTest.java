package com.ssafy.ssafit.recommendation.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ssafy.ssafit.music.domain.MusicEnergyLevel;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;

class IntensityMatchingPolicyTest {

    private final IntensityMatchingPolicy policy = new IntensityMatchingPolicy();

    @Test
    void easyModerateAndHighUseIndependentEnergyMatrix() {
        assertThat(policy.calculateIntensityScore(
                RunningIntensityGoal.EASY,
                MusicEnergyLevel.CALM)).isEqualTo(3);
        assertThat(policy.calculateIntensityScore(
                RunningIntensityGoal.MODERATE,
                MusicEnergyLevel.STEADY)).isEqualTo(3);
        assertThat(policy.calculateIntensityScore(
                RunningIntensityGoal.HIGH,
                MusicEnergyLevel.PEAK)).isEqualTo(3);
        assertThat(policy.calculateIntensityScore(
                RunningIntensityGoal.EASY,
                MusicEnergyLevel.PEAK)).isEqualTo(1);
    }

    @Test
    void missingEnergyDoesNotProduceIntensityScore() {
        assertThat(policy.calculateIntensityScore(
                RunningIntensityGoal.HIGH,
                null)).isNull();
    }
}
