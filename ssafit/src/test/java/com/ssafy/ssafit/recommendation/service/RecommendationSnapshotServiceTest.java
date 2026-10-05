package com.ssafy.ssafit.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.domain.RecommendedMusic;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlan;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlanSegment;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;

import tools.jackson.databind.ObjectMapper;

class RecommendationSnapshotServiceTest {

    private final RecommendationSnapshotService service =
            new RecommendationSnapshotService(new ObjectMapper());

    @Test
    void requestConditionsRoundTripPreservesCoreFields() {
        WorkoutPlan plan = new WorkoutPlan(
                30,
                "6'00\"/km",
                360,
                166,
                174,
                135,
                165,
                150,
                175,
                "summary",
                "strategy",
                "fallback",
                null,
                true,
                List.of());

        String json = service.buildRequestConditions(
                RunningIntensity.MODERATE,
                "K-POP",
                new int[] {120, 160},
                170,
                RunningIntensityGoal.MODERATE,
                2,
                5.0,
                plan);

        RecommendationSnapshotService.RestoredConditions restored =
                service.parseConditions(json);

        assertThat(restored.targetCadenceSpm()).isEqualTo(170);
        assertThat(restored.intensityGoal()).isEqualTo(RunningIntensityGoal.MODERATE);
        assertThat(restored.targetDistanceKm()).isEqualTo(5.0);
        assertThat(restored.page()).isEqualTo(2);
    }

    @Test
    void resultSummaryRoundTripRestoresScoresAndWorkoutPlan() {
        Music music = music(1L);
        RecommendedMusic recommended = new RecommendedMusic(
                music,
                90.0,
                80.0,
                60.0,
                84.0,
                TempoMatchMode.DIRECT,
                "170 SPM 직접 매칭");
        WorkoutPlan plan = new WorkoutPlan(
                30,
                "6'00\"/km",
                360,
                166,
                174,
                135,
                165,
                150,
                175,
                "30분 지속주",
                "중반부 BPM 유지",
                "fallback",
                null,
                true,
                List.of(new WorkoutPlanSegment(
                        "메인",
                        30,
                        150,
                        175,
                        "목표 페이스 유지")));

        String summary = service.buildResultSummary(
                List.of(recommended),
                plan);

        assertThat(service.parseMusicIds(summary)).containsExactly(1L);

        RecommendationLog log = new RecommendationLog();
        log.setResultSummary(summary);
        log.setRecommendationReason("fallback reason");

        List<RecommendedMusic> restored = service.restoreRecommendedMusic(
                List.of(1L),
                Map.of(1L, music),
                log);

        assertThat(restored).hasSize(1);
        assertThat(restored.getFirst().cadenceScore()).isEqualTo(90.0);
        assertThat(restored.getFirst().intensityScore()).isEqualTo(80.0);
        assertThat(restored.getFirst().preferenceScore()).isEqualTo(60.0);
        assertThat(restored.getFirst().finalScore()).isEqualTo(84.0);
        assertThat(restored.getFirst().tempoMatchMode()).isEqualTo(TempoMatchMode.DIRECT);
        assertThat(restored.getFirst().recommendationReason()).isEqualTo("170 SPM 직접 매칭");

        WorkoutPlan restoredPlan = service.parseWorkoutPlan(summary);
        assertThat(restoredPlan.totalDurationMinutes()).isEqualTo(30);
        assertThat(restoredPlan.segments()).hasSize(1);
        assertThat(restoredPlan.segments().getFirst().name()).isEqualTo("메인");
    }

    @Test
    void legacyMusicIdsOnlySummaryRemainsReadable() {
        assertThat(service.parseMusicIds(
                "{\"count\":2,\"musicIds\":[10,20]}"))
                .containsExactly(10L, 20L);
    }

    @Test
    void invalidEnumValuesAreRestoredAsNullInsteadOfFailing() {
        RecommendationSnapshotService.RestoredConditions restored =
                service.parseConditions(
                        "{\"targetCadenceSpm\":172,"
                                + "\"runningIntensityGoal\":\"UNKNOWN\","
                                + "\"targetDistanceKm\":5.0,"
                                + "\"page\":1}");

        assertThat(restored.targetCadenceSpm()).isEqualTo(172);
        assertThat(restored.intensityGoal()).isNull();
        assertThat(restored.targetDistanceKm()).isEqualTo(5.0);
        assertThat(restored.page()).isEqualTo(1);
    }

    private Music music(Long id) {
        Music music = new Music();
        music.setId(id);
        music.setTitle("Music " + id);
        return music;
    }
}
