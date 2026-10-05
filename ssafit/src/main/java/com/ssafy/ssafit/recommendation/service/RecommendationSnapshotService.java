package com.ssafy.ssafit.recommendation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.domain.RecommendedMusic;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlan;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlanSegment;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RecommendationSnapshotService {

    private static final Pattern MUSIC_IDS_PATTERN =
            Pattern.compile("\"musicIds\"\\s*:\\s*\\[([0-9,\\s]*)]");

    private final ObjectMapper objectMapper;

    String buildRequestConditions(
            RunningIntensity intensity,
            String genre,
            int[] bpmRange,
            Integer targetCadenceSpm,
            RunningIntensityGoal intensityGoal,
            int page,
            Double targetDistanceKm,
            WorkoutPlan workoutPlan) {
        return "{\"intensity\":\""
                + intensity
                + "\",\"targetDistanceKm\":"
                + (targetDistanceKm == null ? "null" : targetDistanceKm)
                + ",\"bpmMin\":"
                + bpmRange[0]
                + ",\"bpmMax\":"
                + bpmRange[1]
                + ",\"genre\":"
                + jsonString(genre)
                + ",\"targetCadenceSpm\":"
                + (targetCadenceSpm == null ? "null" : targetCadenceSpm)
                + ",\"runningIntensityGoal\":"
                + jsonString(intensityGoal == null ? null : intensityGoal.name())
                + ",\"workoutDurationMinutes\":"
                + (workoutPlan == null ? "null" : workoutPlan.totalDurationMinutes())
                + ",\"page\":"
                + page
                + "}";
    }

    String buildResultSummary(
            List<RecommendedMusic> musicList,
            WorkoutPlan workoutPlan) {
        List<Long> musicIds = musicList.stream()
                .map(RecommendedMusic::music)
                .map(Music::getId)
                .toList();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("count", musicList.size());
        summary.put("musicIds", musicIds);
        summary.put("music", musicList.stream()
                .map(this::musicSnapshot)
                .toList());
        summary.put("workoutPlan", workoutPlan);
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JacksonException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    List<Long> parseMusicIds(String resultSummary) {
        if (resultSummary == null) {
            return List.of();
        }
        JsonNode summary = parseJson(resultSummary);
        JsonNode idsNode = summary == null ? null : summary.get("musicIds");
        if (idsNode != null && idsNode.isArray()) {
            List<Long> musicIds = new ArrayList<>();
            for (JsonNode idNode : idsNode) {
                Long id = longValue(idNode);
                if (id != null) {
                    musicIds.add(id);
                }
            }
            return musicIds;
        }

        Matcher matcher = MUSIC_IDS_PATTERN.matcher(resultSummary);
        if (!matcher.find() || matcher.group(1).isBlank()) {
            return List.of();
        }
        List<Long> musicIds = new ArrayList<>();
        for (String value : matcher.group(1).split(",")) {
            try {
                musicIds.add(Long.valueOf(value.trim()));
            } catch (NumberFormatException exception) {
                throw new CustomException(ErrorCode.RECOMMENDATION_DATA_NOT_ENOUGH);
            }
        }
        return musicIds;
    }

    List<RecommendedMusic> restoreRecommendedMusic(
            List<Long> musicIds,
            Map<Long, Music> musicById,
            RecommendationLog log) {
        Map<Long, JsonNode> musicSnapshots = parseMusicSnapshots(log.getResultSummary());
        return musicIds.stream()
                .map(musicById::get)
                .filter(music -> music != null)
                .map(music -> restoreRecommendedMusic(
                        music,
                        musicSnapshots.get(music.getId()),
                        log))
                .toList();
    }

    WorkoutPlan parseWorkoutPlan(String resultSummary) {
        JsonNode summary = parseJson(resultSummary);
        JsonNode plan = summary == null ? null : summary.get("workoutPlan");
        if (plan == null || plan.isNull() || !plan.isObject()) {
            return null;
        }
        List<WorkoutPlanSegment> segments = new ArrayList<>();
        JsonNode segmentsNode = plan.get("segments");
        if (segmentsNode != null && segmentsNode.isArray()) {
            for (JsonNode segment : segmentsNode) {
                segments.add(new WorkoutPlanSegment(
                        textValue(segment.get("name")),
                        intValue(segment.get("durationMinutes")),
                        intValue(segment.get("bpmMin")),
                        intValue(segment.get("bpmMax")),
                        textValue(segment.get("purpose"))));
            }
        }
        return new WorkoutPlan(
                intValue(plan.get("totalDurationMinutes")),
                textValue(plan.get("targetPace")),
                intValue(plan.get("targetPaceSecondsPerKm")),
                intValue(plan.get("cadenceMinSpm")),
                intValue(plan.get("cadenceMaxSpm")),
                intValue(plan.get("heartRateMinBpm")),
                intValue(plan.get("heartRateMaxBpm")),
                intValue(plan.get("musicBpmMin")),
                intValue(plan.get("musicBpmMax")),
                textValue(plan.get("coachingSummary")),
                textValue(plan.get("musicStrategy")),
                textValue(plan.get("provider")),
                textValue(plan.get("modelName")),
                booleanValue(plan.get("fallback")),
                segments);
    }

    RestoredConditions parseConditions(String requestConditions) {
        JsonNode conditions = parseJson(requestConditions);
        Integer targetCadenceSpm =
                intValue(conditions == null ? null : conditions.get("targetCadenceSpm"));
        RunningIntensityGoal intensityGoal =
                runningIntensityGoalValue(
                        conditions == null ? null : conditions.get("runningIntensityGoal"));
        Double targetDistanceKm =
                doubleValue(conditions == null ? null : conditions.get("targetDistanceKm"));
        Integer page = intValue(conditions == null ? null : conditions.get("page"));
        return new RestoredConditions(
                targetCadenceSpm,
                intensityGoal,
                targetDistanceKm,
                page == null ? 1 : page);
    }

    private Map<String, Object> musicSnapshot(RecommendedMusic recommendedMusic) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", recommendedMusic.music().getId());
        snapshot.put("cadenceScore", recommendedMusic.cadenceScore());
        snapshot.put("intensityScore", recommendedMusic.intensityScore());
        snapshot.put("preferenceScore", recommendedMusic.preferenceScore());
        snapshot.put("finalScore", recommendedMusic.finalScore());
        snapshot.put(
                "tempoMatchMode",
                recommendedMusic.tempoMatchMode() == null
                        ? null
                        : recommendedMusic.tempoMatchMode().name());
        snapshot.put("recommendationReason", recommendedMusic.recommendationReason());
        return snapshot;
    }

    private RecommendedMusic restoreRecommendedMusic(
            Music music,
            JsonNode snapshot,
            RecommendationLog log) {
        return new RecommendedMusic(
                music,
                doubleValue(snapshot == null ? null : snapshot.get("cadenceScore")),
                doubleValue(snapshot == null ? null : snapshot.get("intensityScore")),
                doubleValue(snapshot == null ? null : snapshot.get("preferenceScore")),
                doubleValue(snapshot == null ? null : snapshot.get("finalScore")),
                tempoMatchModeValue(snapshot == null ? null : snapshot.get("tempoMatchMode")),
                firstText(
                        textValue(snapshot == null ? null : snapshot.get("recommendationReason")),
                        log.getRecommendationReason()));
    }

    private Map<Long, JsonNode> parseMusicSnapshots(String resultSummary) {
        JsonNode summary = parseJson(resultSummary);
        JsonNode musicNode = summary == null ? null : summary.get("music");
        if (musicNode == null || !musicNode.isArray()) {
            return Map.of();
        }
        Map<Long, JsonNode> snapshots = new HashMap<>();
        for (JsonNode node : musicNode) {
            Long musicId = longValue(node.get("id"));
            if (musicId != null) {
                snapshots.put(musicId, node);
            }
        }
        return snapshots;
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            return null;
        }
    }

    private String textValue(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private Integer intValue(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private Long longValue(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }

    private Double doubleValue(JsonNode node) {
        return node == null || node.isNull() ? null : node.asDouble();
    }

    private boolean booleanValue(JsonNode node) {
        return node != null && !node.isNull() && node.asBoolean();
    }

    private RunningIntensityGoal runningIntensityGoalValue(JsonNode node) {
        String value = textValue(node);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RunningIntensityGoal.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private TempoMatchMode tempoMatchModeValue(JsonNode node) {
        String value = textValue(node);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TempoMatchMode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\""
                + value.replace("\\", "\\\\").replace("\"", "\\\"")
                + "\"";
    }

    private String firstText(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first.trim();
    }

    record RestoredConditions(
            Integer targetCadenceSpm,
            RunningIntensityGoal intensityGoal,
            Double targetDistanceKm,
            int page) {
    }
}
