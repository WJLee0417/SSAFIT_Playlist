package com.ssafy.ssafit.recommendation.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.recommendation.domain.RecommendedMusic;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlan;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlanSegment;
import com.ssafy.ssafit.recommendation.policy.CadenceMatchingPolicy;
import com.ssafy.ssafit.recommendation.policy.CadenceMatchingPolicy.CadenceMatchResult;
import com.ssafy.ssafit.recommendation.policy.IntensityMatchingPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationScoringService {

    private static final double CADENCE_WEIGHT = 0.60;
    private static final double INTENSITY_WEIGHT = 0.30;
    private static final double PREFERENCE_WEIGHT = 0.10;
    private static final int DEFAULT_TRACK_DURATION_MS = 210_000;

    private final CadenceMatchingPolicy cadenceMatchingPolicy;
    private final IntensityMatchingPolicy intensityMatchingPolicy;

    List<RecommendedMusic> assembleWorkoutPlaylist(
            List<Music> candidates,
            Integer targetCadenceSpm,
            RunningIntensityGoal intensityGoal,
            String genre,
            Map<Long, Double> preferenceScores,
            WorkoutPlan workoutPlan,
            int maxTracks) {
        List<RecommendedMusic> scored = candidates.stream()
                .filter(music -> music.getId() != null)
                .map(music -> scoreMusic(
                        music,
                        targetCadenceSpm,
                        intensityGoal,
                        genre,
                        preferenceScores.getOrDefault(music.getId(), 3.0)))
                .toList();
        List<RecommendedMusic> selected = new ArrayList<>();
        Set<Long> usedMusicIds = new HashSet<>();
        int totalTargetMs = workoutPlan.totalDurationMinutes() * 60_000;
        int selectedDurationMs = 0;
        for (WorkoutPlanSegment segment : workoutPlan.segments()) {
            int segmentTargetMs = segment.durationMinutes() * 60_000;
            int segmentDurationMs = 0;
            while (selected.size() < maxTracks
                    && selectedDurationMs < totalTargetMs * 1.08
                    && segmentDurationMs < segmentTargetMs * 0.92) {
                RecommendedMusic next = bestSegmentTrack(
                        scored,
                        usedMusicIds,
                        segment,
                        genre);
                if (next == null) {
                    break;
                }
                usedMusicIds.add(next.music().getId());
                RecommendedMusic withSegmentReason = appendSegmentReason(next, segment);
                selected.add(withSegmentReason);
                int durationMs = trackDurationMs(next.music());
                selectedDurationMs += durationMs;
                segmentDurationMs += durationMs;
            }
        }
        if (selected.isEmpty()) {
            selected = scored.stream()
                    .sorted(recommendationComparator(genre))
                    .limit(maxTracks)
                    .toList();
        }
        return List.copyOf(selected);
    }

    RecommendedMusic scoreMusic(
            Music music,
            Integer targetCadenceSpm,
            RunningIntensityGoal intensityGoal,
            String preferredGenre,
            Double rawPreferenceScore) {
        CadenceMatchResult cadenceMatch = cadenceMatchingPolicy.calculate(
                targetCadenceSpm,
                music.getMusicBpm());
        Integer rawIntensityScore =
                intensityMatchingPolicy.calculateIntensityScore(
                        intensityGoal,
                        music.getMusicEnergyLevel());
        Double intensityScore = rawIntensityScore == null
                ? null
                : roundScore(rawIntensityScore / 3.0 * 100.0);
        Double preferenceScore = roundScore(
                Math.max(0.0, Math.min(5.0, rawPreferenceScore))
                        / 5.0
                        * 100.0);
        Double cadenceScore = cadenceMatch.score() == null
                ? null
                : roundScore(cadenceMatch.score());
        Double finalScore = weightedScore(
                cadenceScore,
                intensityScore,
                preferenceScore);
        return new RecommendedMusic(
                music,
                cadenceScore,
                intensityScore,
                preferenceScore,
                finalScore,
                cadenceMatch.mode(),
                recommendationReason(
                        music,
                        targetCadenceSpm,
                        intensityGoal,
                        preferredGenre,
                        cadenceMatch));
    }

    Comparator<RecommendedMusic> recommendationComparator(String preferredGenre) {
        Comparator<Double> descendingNullable =
                Comparator.nullsLast(Comparator.reverseOrder());
        return Comparator
                .comparing(RecommendedMusic::finalScore, descendingNullable)
                .thenComparing(result -> genreMatchRank(
                        result.music(),
                        preferredGenre))
                .thenComparing(
                        RecommendedMusic::cadenceScore,
                        descendingNullable)
                .thenComparing(
                        RecommendedMusic::intensityScore,
                        descendingNullable)
                .thenComparing(
                        result -> result.music().getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(result -> result.music().getId());
    }

    private RecommendedMusic bestSegmentTrack(
            List<RecommendedMusic> scored,
            Set<Long> usedMusicIds,
            WorkoutPlanSegment segment,
            String genre) {
        return scored.stream()
                .filter(candidate -> !usedMusicIds.contains(candidate.music().getId()))
                .min((left, right) -> compareForSegment(left, right, segment, genre))
                .orElse(null);
    }

    private int compareForSegment(
            RecommendedMusic left,
            RecommendedMusic right,
            WorkoutPlanSegment segment,
            String genre) {
        int bpmCompare = Integer.compare(
                segmentBpmDistance(left.music(), segment),
                segmentBpmDistance(right.music(), segment));
        if (bpmCompare != 0) {
            return bpmCompare;
        }
        return recommendationComparator(genre).compare(left, right);
    }

    private int segmentBpmDistance(Music music, WorkoutPlanSegment segment) {
        Integer bpm = music.getMusicBpm();
        if (bpm == null) {
            return Integer.MAX_VALUE / 2;
        }
        if (bpm >= segment.bpmMin() && bpm <= segment.bpmMax()) {
            return 0;
        }
        return Math.min(Math.abs(bpm - segment.bpmMin()), Math.abs(bpm - segment.bpmMax()));
    }

    private RecommendedMusic appendSegmentReason(
            RecommendedMusic music,
            WorkoutPlanSegment segment) {
        return new RecommendedMusic(
                music.music(),
                music.cadenceScore(),
                music.intensityScore(),
                music.preferenceScore(),
                music.finalScore(),
                music.tempoMatchMode(),
                music.recommendationReason()
                        + " / "
                        + segment.name()
                        + " "
                        + segment.bpmMin()
                        + "~"
                        + segment.bpmMax()
                        + " BPM 구간");
    }

    private Double weightedScore(
            Double cadenceScore,
            Double intensityScore,
            Double preferenceScore) {
        double weightedTotal = 0.0;
        double availableWeight = 0.0;
        if (cadenceScore != null) {
            weightedTotal += cadenceScore * CADENCE_WEIGHT;
            availableWeight += CADENCE_WEIGHT;
        }
        if (intensityScore != null) {
            weightedTotal += intensityScore * INTENSITY_WEIGHT;
            availableWeight += INTENSITY_WEIGHT;
        }
        if (preferenceScore != null) {
            weightedTotal += preferenceScore * PREFERENCE_WEIGHT;
            availableWeight += PREFERENCE_WEIGHT;
        }
        return availableWeight == 0.0
                ? 0.0
                : roundScore(weightedTotal / availableWeight);
    }

    private String recommendationReason(
            Music music,
            Integer targetCadenceSpm,
            RunningIntensityGoal intensityGoal,
            String preferredGenre,
            CadenceMatchResult cadenceMatch) {
        List<String> reasons = new ArrayList<>();
        if (genreMatchRank(music, preferredGenre) == 0) {
            reasons.add("선호 장르 " + preferredGenre + " 일치");
        }
        if (cadenceMatch.score() != null) {
            if (cadenceMatch.mode() == TempoMatchMode.HALF_TIME) {
                reasons.add("목표 케이던스 "
                        + targetCadenceSpm
                        + " SPM과 음악 템포 "
                        + music.getMusicBpm()
                        + " BPM의 하프타임 매칭");
            } else {
                reasons.add("목표 케이던스 "
                        + targetCadenceSpm
                        + " SPM과 음악 템포 "
                        + music.getMusicBpm()
                        + " BPM의 직접 매칭");
            }
        }
        if (intensityGoal != null && music.getMusicEnergyLevel() != null) {
            reasons.add("운동 강도 "
                    + intensityGoal
                    + "와 음악 에너지 "
                    + music.getMusicEnergyLevel()
                    + " 매칭");
        }
        if (reasons.isEmpty()) {
            return "템포 또는 에너지 정보가 없어 현재 선호 점수를 중심으로 추천했습니다.";
        }
        return String.join(", ", reasons);
    }

    private int genreMatchRank(Music music, String preferredGenre) {
        if (music == null
                || preferredGenre == null
                || music.getGenre() == null) {
            return 1;
        }
        return preferredGenre.trim().equalsIgnoreCase(music.getGenre().trim())
                ? 0
                : 1;
    }

    private double roundScore(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int trackDurationMs(Music music) {
        return music.getDurationMs() == null || music.getDurationMs() <= 0
                ? DEFAULT_TRACK_DURATION_MS
                : music.getDurationMs();
    }
}
