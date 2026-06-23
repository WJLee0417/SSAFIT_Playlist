package com.ssafy.ssafit.recommendation.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;

class CadenceMatchingPolicyTest {

    private final CadenceMatchingPolicy policy = new CadenceMatchingPolicy();

    @Test
    void matchesDirectTempo() {
        CadenceMatchingPolicy.CadenceMatchResult result =
                policy.calculate(172, 172);

        assertThat(result.mode()).isEqualTo(TempoMatchMode.DIRECT);
        assertThat(result.difference()).isZero();
        assertThat(result.score()).isEqualTo(100.0);
    }

    @Test
    void matchesHalfTimeTempo() {
        CadenceMatchingPolicy.CadenceMatchResult result =
                policy.calculate(172, 86);

        assertThat(result.mode()).isEqualTo(TempoMatchMode.HALF_TIME);
        assertThat(result.difference()).isZero();
        assertThat(result.score()).isEqualTo(100.0);
    }

    @Test
    void missingMusicBpmDoesNotProduceCadenceScore() {
        CadenceMatchingPolicy.CadenceMatchResult result =
                policy.calculate(172, null);

        assertThat(result.mode()).isEqualTo(TempoMatchMode.NONE);
        assertThat(result.difference()).isNull();
        assertThat(result.score()).isNull();
    }
}
