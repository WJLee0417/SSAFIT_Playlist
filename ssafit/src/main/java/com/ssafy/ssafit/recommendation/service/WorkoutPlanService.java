package com.ssafy.ssafit.recommendation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.ssafy.ssafit.airunningreport.provider.AiRunningReportLlmProvider;
import com.ssafy.ssafit.airunningreport.provider.AiRunningReportProviderException;
import com.ssafy.ssafit.airunningreport.provider.AiRunningReportProviderResponse;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlan;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlanSegment;
import com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric;
import com.ssafy.ssafit.runningprofile.domain.RunningGender;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkoutPlanService {

    private static final int MIN_BPM = 60;
    private static final int MAX_BPM = 220;
    private static final String WORKOUT_PLAN_SYSTEM_PROMPT = """
            당신은 러닝 코치이자 러닝 음악 큐레이터입니다.
            사용자의 프로필과 서버가 계산한 Running Fitness Score를 바탕으로 오늘 한 번의 운동 계획을 제안하세요.
            심박수는 운동 강도 참고값으로만 다루고 건강, 질병, 치료, 진단 조언을 하지 마세요.
            응답은 한국어 JSON 객체만 반환하고 JSON 외 텍스트는 반환하지 마세요.
            JSON 필드는 totalDurationMinutes, targetPace, targetPaceSecondsPerKm,
            cadenceMinSpm, cadenceMaxSpm, heartRateMinBpm, heartRateMaxBpm,
            musicBpmMin, musicBpmMax, coachingSummary, musicStrategy, segments만 사용하세요.
            segments는 name, durationMinutes, bpmMin, bpmMax, purpose 필드를 가진 배열입니다.
            전체 segments.durationMinutes 합계는 totalDurationMinutes와 같게 하세요.
            """;

    private final List<AiRunningReportLlmProvider> llmProviders;
    private final ObjectMapper objectMapper;

    WorkoutPlan generate(
            RunningProfile profile,
            RunningFitnessMetric metric,
            List<RunningRecord> recentRecords,
            RunningIntensity intensity,
            Double targetDistanceKm) {
        WorkoutPlan fallback = fallbackWorkoutPlan(
                profile,
                metric,
                recentRecords,
                intensity,
                targetDistanceKm);
        if (llmProviders == null || llmProviders.isEmpty()) {
            return fallback;
        }

        AiRunningReportLlmProvider provider = llmProviders.getFirst();
        try {
            AiRunningReportProviderResponse response = provider.generate(
                    null,
                    WORKOUT_PLAN_SYSTEM_PROMPT,
                    workoutPlanUserPrompt(
                            profile,
                            metric,
                            recentRecords,
                            intensity,
                            targetDistanceKm));
            AiWorkoutPlanContent content =
                    objectMapper.readValue(response.contentJson(), AiWorkoutPlanContent.class);
            return normalizeWorkoutPlan(
                    content,
                    fallback,
                    response.providerName(),
                    response.modelName(),
                    false);
        } catch (AiRunningReportProviderException | JacksonException exception) {
            log.warn("AI workout plan generation failed. fallback=true");
            return fallback;
        }
    }

    int[] resolveBpmRange(
            RunningProfile profile,
            RunningIntensity intensity) {
        if (profile != null
                && isValidBpmRange(
                        profile.getTargetBpmMin(),
                        profile.getTargetBpmMax())) {
            return new int[] {profile.getTargetBpmMin(), profile.getTargetBpmMax()};
        }

        return switch (intensity) {
            case LOW -> new int[] {90, 130};
            case MODERATE -> new int[] {120, 160};
            case HIGH -> new int[] {150, 190};
            case INTERVAL -> new int[] {140, 200};
        };
    }

    private String workoutPlanUserPrompt(
            RunningProfile profile,
            RunningFitnessMetric metric,
            List<RunningRecord> recentRecords,
            RunningIntensity intensity,
            Double targetDistanceKm) {
        Integer minHeartRate = minRecentHeartRate(recentRecords);
        Integer maxHeartRate = maxRecentHeartRate(recentRecords);
        Integer minCadence = minRecentCadence(recentRecords);
        Integer maxCadence = maxRecentCadence(recentRecords);
        return "너는 러닝 코치로서 사용자의 다음 운동 목표 및 적절한 음악 bpm 범위를 제안해야 한다. "
                + "사용자는 "
                + nullSafe(profile == null ? null : profile.getAge(), "미입력")
                + " 세의 "
                + genderLabel(profile == null ? null : profile.getGender())
                + "이며 최근 러닝 거리는 "
                + nullSafe(metric.totalDistanceKm(), "기록 없음")
                + " km, 평균 페이스는 "
                + formatPace(metric.averagePaceSecondsPerKm())
                + "이다. 해당 기간 평균 심박수 범위는 "
                + nullSafe(minHeartRate, "기록 없음")
                + "부터 "
                + nullSafe(maxHeartRate, "기록 없음")
                + " bpm 수준이며 케이던스는 "
                + nullSafe(minCadence, "기록 없음")
                + " 부터 "
                + nullSafe(maxCadence, "기록 없음")
                + " spm 수준이다. 사용자의 러닝 수준은 평균 거리, 심박 효율, 케이던스 안정성, 기록 변동성 등을 고려했을 때 100점 만점 중 "
                + nullSafe(metric.score(), "미계산")
                + "점이다. 사용자는 오늘 "
                + targetDistanceKm
                + " km를 달리고 싶어 하고 목표 운동 강도는 "
                + intensity
                + "이다. 적절한 전체 러닝 시간과 도출되는 케이던스, 심박수 목표를 응답하라. "
                + "해당 운동을 하는 동안 듣기 적절한 음악의 bpm 범위를 제안하라.";
    }

    private WorkoutPlan fallbackWorkoutPlan(
            RunningProfile profile,
            RunningFitnessMetric metric,
            List<RunningRecord> recentRecords,
            RunningIntensity intensity,
            Double targetDistanceKm) {
        int basePaceSeconds = metric.averagePaceSecondsPerKm() == null
                ? defaultPaceSeconds(intensity)
                : metric.averagePaceSecondsPerKm();
        double scoreFactor = metric.score() == null
                ? 1.0
                : 1.08 - (Math.max(0, Math.min(100, metric.score())) / 100.0 * 0.13);
        int targetPaceSeconds = clampInt(
                (int) Math.round(basePaceSeconds * intensityPaceFactor(intensity) * scoreFactor),
                240,
                720);
        int totalDurationMinutes = clampInt(
                (int) Math.round(targetDistanceKm * targetPaceSeconds / 60.0),
                8,
                240);
        int cadenceCenter = firstNonNull(
                midpoint(
                        profile == null ? null : profile.getTargetCadenceMin(),
                        profile == null ? null : profile.getTargetCadenceMax()),
                midpoint(minRecentCadence(recentRecords), maxRecentCadence(recentRecords)),
                defaultCadenceCenter(intensity));
        int cadenceMin = clampInt(cadenceCenter - 4, 60, 240);
        int cadenceMax = clampInt(cadenceCenter + 4, 60, 240);
        int[] heartRateRange = fallbackHeartRateRange(profile, recentRecords, intensity);
        int[] musicBpmRange = resolveBpmRange(profile, intensity);
        List<WorkoutPlanSegment> segments = defaultSegments(
                totalDurationMinutes,
                musicBpmRange[0],
                musicBpmRange[1],
                intensity);
        return new WorkoutPlan(
                totalDurationMinutes,
                formatPace(targetPaceSeconds),
                targetPaceSeconds,
                cadenceMin,
                cadenceMax,
                heartRateRange[0],
                heartRateRange[1],
                musicBpmRange[0],
                musicBpmRange[1],
                "서버 계산 기반으로 오늘 목표 거리와 현재 러닝 점수에 맞춘 보수적인 운동 목표입니다.",
                "초반은 낮은 BPM으로 진입하고, 주 구간에서 목표 BPM을 유지한 뒤 마지막에 완만히 낮춥니다.",
                "fallback",
                null,
                true,
                segments);
    }

    private WorkoutPlan normalizeWorkoutPlan(
            AiWorkoutPlanContent content,
            WorkoutPlan fallback,
            String provider,
            String modelName,
            boolean fallbackSource) {
        if (content == null) {
            return fallback;
        }
        int duration = clampInt(
                firstNonNull(content.totalDurationMinutes(), fallback.totalDurationMinutes()),
                8,
                240);
        int paceSeconds = clampInt(
                firstNonNull(content.targetPaceSecondsPerKm(), fallback.targetPaceSecondsPerKm()),
                240,
                720);
        int cadenceMin = clampInt(
                firstNonNull(content.cadenceMinSpm(), fallback.cadenceMinSpm()),
                60,
                240);
        int cadenceMax = clampInt(
                firstNonNull(content.cadenceMaxSpm(), fallback.cadenceMaxSpm()),
                cadenceMin,
                240);
        int heartRateMin = clampInt(
                firstNonNull(content.heartRateMinBpm(), fallback.heartRateMinBpm()),
                30,
                240);
        int heartRateMax = clampInt(
                firstNonNull(content.heartRateMaxBpm(), fallback.heartRateMaxBpm()),
                heartRateMin,
                240);
        int musicBpmMin = clampInt(
                firstNonNull(content.musicBpmMin(), fallback.musicBpmMin()),
                MIN_BPM,
                MAX_BPM);
        int musicBpmMax = clampInt(
                firstNonNull(content.musicBpmMax(), fallback.musicBpmMax()),
                musicBpmMin,
                MAX_BPM);
        List<WorkoutPlanSegment> segments = normalizeSegments(
                content.segments(),
                fallback.segments(),
                duration,
                musicBpmMin,
                musicBpmMax);
        return new WorkoutPlan(
                duration,
                firstText(content.targetPace(), formatPace(paceSeconds)),
                paceSeconds,
                cadenceMin,
                cadenceMax,
                heartRateMin,
                heartRateMax,
                musicBpmMin,
                musicBpmMax,
                firstText(content.coachingSummary(), fallback.coachingSummary()),
                firstText(content.musicStrategy(), fallback.musicStrategy()),
                provider,
                modelName,
                fallbackSource,
                segments);
    }

    private List<WorkoutPlanSegment> normalizeSegments(
            List<AiWorkoutPlanSegmentContent> rawSegments,
            List<WorkoutPlanSegment> fallbackSegments,
            int duration,
            int bpmMin,
            int bpmMax) {
        if (rawSegments == null || rawSegments.isEmpty()) {
            return fallbackSegments;
        }
        List<WorkoutPlanSegment> segments = new ArrayList<>();
        int usedMinutes = 0;
        for (int index = 0; index < rawSegments.size(); index++) {
            AiWorkoutPlanSegmentContent raw = rawSegments.get(index);
            int segmentDuration = clampInt(
                    firstNonNull(raw.durationMinutes(), duration / rawSegments.size()),
                    1,
                    duration);
            if (index == rawSegments.size() - 1) {
                segmentDuration = Math.max(1, duration - usedMinutes);
            }
            usedMinutes += segmentDuration;
            int segmentBpmMin = clampInt(
                    firstNonNull(raw.bpmMin(), bpmMin),
                    MIN_BPM,
                    MAX_BPM);
            int segmentBpmMax = clampInt(
                    firstNonNull(raw.bpmMax(), bpmMax),
                    segmentBpmMin,
                    MAX_BPM);
            segments.add(new WorkoutPlanSegment(
                    firstText(raw.name(), "구간 " + (index + 1)),
                    segmentDuration,
                    segmentBpmMin,
                    segmentBpmMax,
                    firstText(raw.purpose(), "목표 BPM에 맞춰 리듬을 유지합니다.")));
        }
        return segments;
    }

    private List<WorkoutPlanSegment> defaultSegments(
            int durationMinutes,
            int bpmMin,
            int bpmMax,
            RunningIntensity intensity) {
        if (intensity == RunningIntensity.INTERVAL && durationMinutes >= 20) {
            int warmup = Math.max(5, (int) Math.round(durationMinutes * 0.18));
            int cooldown = Math.max(4, (int) Math.round(durationMinutes * 0.14));
            int work = Math.max(1, durationMinutes - warmup - cooldown);
            int lowerMax = Math.max(bpmMin, bpmMin + (bpmMax - bpmMin) / 3);
            return List.of(
                    new WorkoutPlanSegment("워밍업", warmup, bpmMin, lowerMax, "호흡을 천천히 올립니다."),
                    new WorkoutPlanSegment("인터벌", work, lowerMax, bpmMax, "빠른 구간과 회복 구간의 리듬을 번갈아 유지합니다."),
                    new WorkoutPlanSegment("쿨다운", cooldown, bpmMin, lowerMax, "페이스와 심박을 서서히 낮춥니다."));
        }
        int warmup = Math.max(3, (int) Math.round(durationMinutes * 0.15));
        int cooldown = Math.max(3, (int) Math.round(durationMinutes * 0.15));
        int main = Math.max(1, durationMinutes - warmup - cooldown);
        int lowerMax = Math.max(bpmMin, bpmMin + (bpmMax - bpmMin) / 4);
        return List.of(
                new WorkoutPlanSegment("워밍업", warmup, bpmMin, lowerMax, "가볍게 몸을 엽니다."),
                new WorkoutPlanSegment("메인 러닝", main, bpmMin, bpmMax, "목표 페이스와 케이던스를 안정적으로 유지합니다."),
                new WorkoutPlanSegment("쿨다운", cooldown, bpmMin, lowerMax, "호흡을 정리하며 마무리합니다."));
    }

    private boolean isValidBpmRange(Integer minBpm, Integer maxBpm) {
        return minBpm != null
                && maxBpm != null
                && minBpm >= MIN_BPM
                && maxBpm <= MAX_BPM
                && minBpm <= maxBpm;
    }

    private int defaultPaceSeconds(RunningIntensity intensity) {
        return switch (intensity) {
            case LOW -> 420;
            case MODERATE -> 360;
            case HIGH -> 320;
            case INTERVAL -> 300;
        };
    }

    private double intensityPaceFactor(RunningIntensity intensity) {
        return switch (intensity) {
            case LOW -> 1.12;
            case MODERATE -> 1.0;
            case HIGH -> 0.94;
            case INTERVAL -> 0.9;
        };
    }

    private int defaultCadenceCenter(RunningIntensity intensity) {
        return switch (intensity) {
            case LOW -> 164;
            case MODERATE -> 170;
            case HIGH -> 176;
            case INTERVAL -> 180;
        };
    }

    private int[] fallbackHeartRateRange(
            RunningProfile profile,
            List<RunningRecord> recentRecords,
            RunningIntensity intensity) {
        if (profile != null && isValidBpmRange(
                profile.getTargetBpmMin(),
                profile.getTargetBpmMax())) {
            return new int[] {profile.getTargetBpmMin(), profile.getTargetBpmMax()};
        }
        Integer recentMin = minRecentHeartRate(recentRecords);
        Integer recentMax = maxRecentHeartRate(recentRecords);
        if (recentMin != null && recentMax != null) {
            return new int[] {
                    clampInt(recentMin, 30, 240),
                    clampInt(recentMax, recentMin, 240)};
        }
        int maxHeartRate = profile == null || profile.getEstimatedMaxHeartRateBpm() == null
                ? 190
                : profile.getEstimatedMaxHeartRateBpm();
        double[] ratio = switch (intensity) {
            case LOW -> new double[] {0.60, 0.70};
            case MODERATE -> new double[] {0.70, 0.80};
            case HIGH -> new double[] {0.80, 0.88};
            case INTERVAL -> new double[] {0.84, 0.92};
        };
        return new int[] {
                clampInt((int) Math.round(maxHeartRate * ratio[0]), 30, 240),
                clampInt((int) Math.round(maxHeartRate * ratio[1]), 30, 240)};
    }

    private Integer minRecentHeartRate(List<RunningRecord> records) {
        return records == null ? null : records.stream()
                .map(RunningRecord::getAverageHeartRateBpm)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private Integer maxRecentHeartRate(List<RunningRecord> records) {
        return records == null ? null : records.stream()
                .map(RunningRecord::getAverageHeartRateBpm)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private Integer minRecentCadence(List<RunningRecord> records) {
        return records == null ? null : records.stream()
                .map(RunningRecord::getAverageCadence)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private Integer maxRecentCadence(List<RunningRecord> records) {
        return records == null ? null : records.stream()
                .map(RunningRecord::getAverageCadence)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private Integer midpoint(Integer min, Integer max) {
        if (min == null || max == null) {
            return null;
        }
        return (int) Math.round((min + max) / 2.0);
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first.trim();
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatPace(Integer secondsPerKm) {
        if (secondsPerKm == null || secondsPerKm <= 0) {
            return "기록 없음";
        }
        int minutes = secondsPerKm / 60;
        int seconds = secondsPerKm % 60;
        return "%d'%02d\"/km".formatted(minutes, seconds);
    }

    private String genderLabel(RunningGender gender) {
        if (gender == null) {
            return "성별 미입력 사용자";
        }
        return switch (gender) {
            case FEMALE -> "여성";
            case MALE -> "남성";
            case OTHER -> "기타 성별";
            case UNSPECIFIED -> "성별 미지정 사용자";
        };
    }

    private String nullSafe(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    public record AiWorkoutPlanContent(
            Integer totalDurationMinutes,
            String targetPace,
            Integer targetPaceSecondsPerKm,
            Integer cadenceMinSpm,
            Integer cadenceMaxSpm,
            Integer heartRateMinBpm,
            Integer heartRateMaxBpm,
            Integer musicBpmMin,
            Integer musicBpmMax,
            String coachingSummary,
            String musicStrategy,
            List<AiWorkoutPlanSegmentContent> segments) {
    }

    public record AiWorkoutPlanSegmentContent(
            String name,
            Integer durationMinutes,
            Integer bpmMin,
            Integer bpmMax,
            String purpose) {
    }
}
