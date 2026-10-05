package com.ssafy.ssafit.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.airunningreport.provider.AiRunningReportLlmProvider;
import com.ssafy.ssafit.airunningreport.provider.AiRunningReportProviderException;
import com.ssafy.ssafit.airunningreport.provider.AiRunningReportProviderFailureReason;
import com.ssafy.ssafit.airunningreport.provider.AiRunningReportProviderResponse;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlan;
import com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WorkoutPlanServiceTest {

    @Mock
    private AiRunningReportLlmProvider provider;

    @Test
    void returnsFallbackPlanWhenNoLlmProviderIsConfigured() {
        WorkoutPlanService service = new WorkoutPlanService(
                List.of(),
                new ObjectMapper());

        WorkoutPlan result = service.generate(
                profile(),
                metric(),
                List.of(),
                RunningIntensity.MODERATE,
                5.0);

        assertThat(result.fallback()).isTrue();
        assertThat(result.provider()).isEqualTo("fallback");
        assertThat(result.totalDurationMinutes()).isBetween(8, 240);
        assertThat(result.cadenceMinSpm()).isLessThanOrEqualTo(result.cadenceMaxSpm());
        assertThat(result.musicBpmMin()).isLessThanOrEqualTo(result.musicBpmMax());
        assertThat(result.segments()).isNotEmpty();
    }

    @Test
    void usesNormalizedLlmResponseWhenProviderReturnsValidJson() {
        when(provider.generate(any(), anyString(), anyString()))
                .thenReturn(new AiRunningReportProviderResponse(
                        """
                        {
                          "totalDurationMinutes": 40,
                          "targetPace": "5'50\"/km",
                          "targetPaceSecondsPerKm": 350,
                          "cadenceMinSpm": 168,
                          "cadenceMaxSpm": 176,
                          "heartRateMinBpm": 135,
                          "heartRateMaxBpm": 165,
                          "musicBpmMin": 160,
                          "musicBpmMax": 178,
                          "coachingSummary": "40분 지속주",
                          "musicStrategy": "중반부 템포 유지",
                          "segments": [
                            {
                              "name": "메인",
                              "durationMinutes": 40,
                              "bpmMin": 160,
                              "bpmMax": 178,
                              "purpose": "목표 페이스 유지"
                            }
                          ]
                        }
                        """,
                        "test-provider",
                        "test-model"));

        WorkoutPlanService service = new WorkoutPlanService(
                List.of(provider),
                new ObjectMapper());

        WorkoutPlan result = service.generate(
                profile(),
                metric(),
                List.of(),
                RunningIntensity.MODERATE,
                5.0);

        assertThat(result.fallback()).isFalse();
        assertThat(result.provider()).isEqualTo("test-provider");
        assertThat(result.modelName()).isEqualTo("test-model");
        assertThat(result.totalDurationMinutes()).isEqualTo(40);
        assertThat(result.targetPaceSecondsPerKm()).isEqualTo(350);
        assertThat(result.segments()).hasSize(1);
        assertThat(result.segments().getFirst().name()).isEqualTo("메인");
    }

    @Test
    void fallsBackWhenLlmProviderFails() {
        when(provider.generate(any(), anyString(), anyString()))
                .thenThrow(new AiRunningReportProviderException(
                        AiRunningReportProviderFailureReason.UPSTREAM_ERROR,
                        "provider failed"));

        WorkoutPlanService service = new WorkoutPlanService(
                List.of(provider),
                new ObjectMapper());

        WorkoutPlan result = service.generate(
                profile(),
                metric(),
                List.of(),
                RunningIntensity.HIGH,
                10.0);

        assertThat(result.fallback()).isTrue();
        assertThat(result.provider()).isEqualTo("fallback");
        assertThat(result.totalDurationMinutes()).isBetween(8, 240);
    }

    @Test
    void keepsProfileBpmRangeWhenItIsValid() {
        WorkoutPlanService service = new WorkoutPlanService(
                List.of(),
                new ObjectMapper());
        RunningProfile profile = profile();
        profile.setTargetBpmMin(130);
        profile.setTargetBpmMax(155);

        assertThat(service.resolveBpmRange(profile, RunningIntensity.HIGH))
                .containsExactly(130, 155);
    }

    private RunningProfile profile() {
        RunningProfile profile = new RunningProfile();
        profile.setAge(29);
        profile.setTargetCadenceMin(166);
        profile.setTargetCadenceMax(174);
        profile.setEstimatedMaxHeartRateBpm(190);
        return profile;
    }

    private RunningFitnessMetric metric() {
        return new RunningFitnessMetric(
                70,
                5,
                25.0,
                360,
                5.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
