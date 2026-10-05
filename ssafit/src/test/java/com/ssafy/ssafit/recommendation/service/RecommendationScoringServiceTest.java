package com.ssafy.ssafit.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicEnergyLevel;
import com.ssafy.ssafit.recommendation.domain.RecommendedMusic;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlan;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlanSegment;
import com.ssafy.ssafit.recommendation.policy.CadenceMatchingPolicy;
import com.ssafy.ssafit.recommendation.policy.IntensityMatchingPolicy;

class RecommendationScoringServiceTest {

    private final RecommendationScoringService service =
            new RecommendationScoringService(
                    new CadenceMatchingPolicy(),
                    new IntensityMatchingPolicy());

    @Test
    void renormalizesWeightsWhenCadenceOrIntensityMetadataIsMissing() {
        Music complete = music(1L, 86, MusicEnergyLevel.PEAK);
        Music noBpm = music(2L, null, MusicEnergyLevel.PEAK);
        Music noEnergy = music(3L, 172, null);

        RecommendedMusic completeResult = service.scoreMusic(
                complete,
                172,
                RunningIntensityGoal.HIGH,
                null,
                3.0);
        RecommendedMusic noBpmResult = service.scoreMusic(
                noBpm,
                172,
                RunningIntensityGoal.HIGH,
                null,
                3.0);
        RecommendedMusic noEnergyResult = service.scoreMusic(
                noEnergy,
                172,
                RunningIntensityGoal.HIGH,
                null,
                3.0);

        assertThat(completeResult.tempoMatchMode()).isEqualTo(TempoMatchMode.HALF_TIME);
        assertThat(completeResult.cadenceScore()).isEqualTo(100.0);
        assertThat(completeResult.intensityScore()).isEqualTo(100.0);
        assertThat(completeResult.preferenceScore()).isEqualTo(60.0);
        assertThat(completeResult.finalScore()).isEqualTo(96.0);

        assertThat(noBpmResult.cadenceScore()).isNull();
        assertThat(noBpmResult.intensityScore()).isEqualTo(100.0);
        assertThat(noBpmResult.finalScore()).isEqualTo(90.0);

        assertThat(noEnergyResult.cadenceScore()).isEqualTo(100.0);
        assertThat(noEnergyResult.intensityScore()).isNull();
        assertThat(noEnergyResult.finalScore()).isEqualTo(94.29);
    }

    @Test
    void comparatorUsesMusicIdAsStableFinalTieBreaker() {
        Music laterId = music(2L, null, null);
        Music earlierId = music(1L, null, null);

        List<RecommendedMusic> sorted = List.of(
                        service.scoreMusic(laterId, null, null, null, 3.0),
                        service.scoreMusic(earlierId, null, null, null, 3.0))
                .stream()
                .sorted(service.recommendationComparator(null))
                .toList();

        assertThat(sorted)
                .extracting(result -> result.music().getId())
                .containsExactly(1L, 2L);
    }

    @Test
    void workoutPlaylistPrefersTracksInsideEachSegmentBpmRangeWithoutDuplicates() {
        Music warmupTrack = music(1L, 125, MusicEnergyLevel.STEADY);
        Music mainTrack = music(2L, 170, MusicEnergyLevel.ENERGETIC);
        Music outsideTrack = music(3L, 205, MusicEnergyLevel.PEAK);
        warmupTrack.setDurationMs(180_000);
        mainTrack.setDurationMs(180_000);
        outsideTrack.setDurationMs(180_000);

        WorkoutPlan workoutPlan = new WorkoutPlan(
                6,
                "6'00\"/km",
                360,
                168,
                176,
                130,
                165,
                120,
                180,
                "test",
                "test",
                "fallback",
                null,
                true,
                List.of(
                        new WorkoutPlanSegment("워밍업", 3, 120, 130, "warmup"),
                        new WorkoutPlanSegment("메인", 3, 165, 175, "main")));

        List<RecommendedMusic> result = service.assembleWorkoutPlaylist(
                List.of(outsideTrack, mainTrack, warmupTrack),
                172,
                RunningIntensityGoal.MODERATE,
                null,
                Map.of(),
                workoutPlan,
                3);

        assertThat(result)
                .extracting(value -> value.music().getId())
                .containsExactly(1L, 2L);
        assertThat(result)
                .extracting(RecommendedMusic::recommendationReason)
                .allMatch(reason -> reason.contains("BPM 구간"));
    }

    private Music music(
            Long id,
            Integer bpm,
            MusicEnergyLevel energyLevel) {
        Music music = new Music();
        music.setId(id);
        music.setTitle("Music " + id);
        music.setMusicBpm(bpm);
        music.setMusicEnergyLevel(energyLevel);
        return music;
    }
}
