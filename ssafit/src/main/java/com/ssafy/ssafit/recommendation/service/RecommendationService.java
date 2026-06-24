package com.ssafy.ssafit.recommendation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.airunningreport.provider.AiRunningReportLlmProvider;
import com.ssafy.ssafit.airunningreport.provider.AiRunningReportProviderException;
import com.ssafy.ssafit.airunningreport.provider.AiRunningReportProviderResponse;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicPreferenceScore;
import com.ssafy.ssafit.music.domain.SpotifyTrackMetadata;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.mapper.PlaylistItemMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.domain.RecommendedMusic;
import com.ssafy.ssafit.recommendation.domain.RecommendationResult;
import com.ssafy.ssafit.recommendation.domain.CadenceResolutionResult;
import com.ssafy.ssafit.recommendation.domain.IntensityResolutionResult;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlan;
import com.ssafy.ssafit.recommendation.domain.WorkoutPlanSegment;
import com.ssafy.ssafit.recommendation.policy.CadenceMatchingPolicy;
import com.ssafy.ssafit.recommendation.policy.CadenceMatchingPolicy.CadenceMatchResult;
import com.ssafy.ssafit.recommendation.policy.IntensityMatchingPolicy;
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric;
import com.ssafy.ssafit.runningprofile.domain.RunningGender;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;
import com.ssafy.ssafit.runningprofile.service.RunningFitnessMetricService;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;
import com.ssafy.ssafit.spotify.service.SpotifyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RecommendationService {

    private static final int MIN_BPM = 60;
    private static final int MAX_BPM = 220;
    private static final int DEFAULT_RECOMMENDATION_SIZE = 10;
    private static final int MAX_RECOMMENDATION_SIZE = 50;
    private static final int MAX_CANDIDATE_SIZE = 500;
    private static final double CADENCE_WEIGHT = 0.60;
    private static final double INTENSITY_WEIGHT = 0.30;
    private static final double PREFERENCE_WEIGHT = 0.10;
    private static final int MAX_PLAYLIST_TITLE_LENGTH = 200;
    private static final int RECENT_RECORD_LIMIT = 10;
    private static final int DEFAULT_TRACK_DURATION_MS = 210_000;
    private static final int MAX_WORKOUT_PLAYLIST_SIZE = 50;
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
    private static final Pattern MUSIC_IDS_PATTERN =
            Pattern.compile("\"musicIds\"\\s*:\\s*\\[([0-9,\\s]*)]");

    private final RunningProfileMapper runningProfileMapper;
    private final MusicMapper musicMapper;
    private final RecommendationLogMapper recommendationLogMapper;
    private final PlaylistMapper playlistMapper;
    private final PlaylistItemMapper playlistItemMapper;
    private final RunningIntensityResolutionService intensityResolutionService;
    private final CadenceResolutionService cadenceResolutionService;
    private final CadenceMatchingPolicy cadenceMatchingPolicy;
    private final IntensityMatchingPolicy intensityMatchingPolicy;
    private final SpotifyService spotifyService;
    private final RunningFitnessMetricService runningFitnessMetricService;
    private final RunningRecordMapper runningRecordMapper;
    private final List<AiRunningReportLlmProvider> llmProviders;
    private final ObjectMapper objectMapper;

    /**
     * 추천 조회는 음악과 로그만 생성한다.
     * playlists와 playlist_items는 saveRecommendation 호출 전까지 생성하지 않는다.
     */
    @Transactional
    public RecommendationResult recommend(
            Long loginMemberId,
            RunningIntensity requestedIntensity,
            String requestedGenre,
            Integer size) {
        return recommend(
                loginMemberId,
                requestedIntensity,
                null,
                null,
                null,
                requestedGenre,
                size,
                null,
                null);
    }

    @Transactional
    public RecommendationResult recommend(
            Long loginMemberId,
            RunningIntensity requestedIntensity,
            Integer requestedTargetCadenceSpm,
            RunningIntensityGoal requestedIntensityGoal,
            String requestedGenre,
            Integer size,
            Integer limit,
            Integer page) {
        return recommend(
                loginMemberId,
                requestedIntensity,
                null,
                requestedTargetCadenceSpm,
                requestedIntensityGoal,
                requestedGenre,
                size,
                limit,
                page);
    }

    @Transactional
    public RecommendationResult recommend(
            Long loginMemberId,
            RunningIntensity requestedIntensity,
            RunningIntensityGoal requestedIntensityGoal,
            String requestedGenre,
            Integer size) {
        return recommend(
                loginMemberId,
                requestedIntensity,
                null,
                null,
                requestedIntensityGoal,
                requestedGenre,
                size,
                null,
                null);
    }

    @Transactional
    public RecommendationResult recommend(
            Long loginMemberId,
            RunningIntensity requestedIntensity,
            Integer requestedTargetCadenceSpm,
            RunningIntensityGoal requestedIntensityGoal,
            String requestedGenre,
            Integer size,
            Integer limit) {
        return recommend(
                loginMemberId,
                requestedIntensity,
                null,
                requestedTargetCadenceSpm,
                requestedIntensityGoal,
                requestedGenre,
                size,
                limit,
                null);
    }

    @Transactional
    public RecommendationResult recommend(
            Long loginMemberId,
            RunningIntensity requestedIntensity,
            Double targetDistanceKm,
            Integer requestedTargetCadenceSpm,
            RunningIntensityGoal requestedIntensityGoal,
            String requestedGenre,
            Integer size,
            Integer limit,
            Integer page) {
        validateAuthenticated(loginMemberId);
        if (requestedIntensity == null) {
            throw new CustomException(ErrorCode.INVALID_INTENSITY);
        }

        RunningProfile profile = runningProfileMapper.selectByMemberId(loginMemberId)
                .orElse(null);
        boolean workoutPlanMode = targetDistanceKm != null;
        Double normalizedTargetDistanceKm = workoutPlanMode
                ? normalizeTargetDistance(targetDistanceKm)
                : null;
        RunningFitnessMetric fitnessMetric =
                runningFitnessMetricService.calculate(loginMemberId);
        if (fitnessMetric == null) {
            fitnessMetric = RunningFitnessMetric.empty();
        }
        List<RunningRecord> recentRecords = runningRecordMapper.selectRecentByMemberId(
                loginMemberId,
                RECENT_RECORD_LIMIT);
        if (recentRecords == null) {
            recentRecords = List.of();
        }
        WorkoutPlan workoutPlan = workoutPlanMode
                ? generateWorkoutPlan(
                        profile,
                        fitnessMetric,
                        recentRecords,
                        requestedIntensity,
                        normalizedTargetDistanceKm)
                : null;
        int[] bpmRange = workoutPlan == null
                ? resolveBpmRange(profile, requestedIntensity)
                : new int[] {workoutPlan.musicBpmMin(), workoutPlan.musicBpmMax()};
        String genre = firstNonBlank(
                requestedGenre,
                profile == null ? null : profile.getPreferredGenre());
        int recommendationSize = workoutPlanMode && size == null && limit == null
                ? MAX_WORKOUT_PLAYLIST_SIZE
                : normalizeSize(size);
        if (limit != null) {
            recommendationSize = normalizeSize(limit);
        }
        int resultPage = normalizePage(page);
        int pageSize = workoutPlanMode
                ? Math.min(recommendationSize, MAX_WORKOUT_PLAYLIST_SIZE)
                : Math.min(recommendationSize, 10);
        int offset = (resultPage - 1) * pageSize;
        CadenceResolutionResult cadenceResolution = cadenceResolutionService.resolve(
                loginMemberId,
                requestedTargetCadenceSpm,
                profile);
        Integer workoutCadenceSpm = workoutPlan == null
                ? null
                : midpoint(workoutPlan.cadenceMinSpm(), workoutPlan.cadenceMaxSpm());
        Integer targetCadenceSpm = firstNonNull(
                requestedTargetCadenceSpm,
                workoutCadenceSpm,
                cadenceResolution.targetCadenceSpm());
        RunningIntensityGoal effectiveIntensityGoal = requestedIntensityGoal == null
                ? intensityGoalFromRunningIntensity(requestedIntensity)
                : requestedIntensityGoal;
        IntensityResolutionResult intensityResolution =
                intensityResolutionService.resolve(
                        loginMemberId,
                        effectiveIntensityGoal);

        List<Music> candidates = musicMapper.selectRecommendationCandidates(
                genre,
                MAX_CANDIDATE_SIZE);
        boolean spotifyGenreDiscovery = false;
        boolean spotifyPaged = false;
        int spotifySourceCount = 0;
        if (!workoutPlanMode && genre != null) {
            try {
                CandidatePage discovered = importSpotifyCandidates(
                        genre,
                        requestedIntensity,
                        offset,
                        pageSize);
                if (!discovered.music().isEmpty()) {
                    candidates = discovered.music();
                    spotifyGenreDiscovery = true;
                    spotifyPaged = true;
                    spotifySourceCount = discovered.sourceCount();
                }
            } catch (CustomException exception) {
                if (candidates == null || candidates.isEmpty()) {
                    throw exception;
                }
                log.warn(
                        "Spotify candidate discovery failed; using local candidates. errorCode={}",
                        exception.getErrorCode());
            }
        } else if (!workoutPlanMode && (candidates == null || candidates.isEmpty())) {
            CandidatePage discovered = importSpotifyCandidates(
                    null,
                    requestedIntensity,
                    offset,
                    pageSize);
            candidates = discovered.music();
            spotifyPaged = true;
            spotifySourceCount = discovered.sourceCount();
        }
        if (candidates == null || candidates.isEmpty()) {
            throw new CustomException(ErrorCode.RECOMMENDATION_DATA_NOT_ENOUGH);
        }
        Map<Long, Double> preferenceScores = preferenceScores(loginMemberId);
        List<Music> candidateSnapshot = List.copyOf(candidates);
        long resultOffset = spotifyPaged ? 0L : offset;
        List<RecommendedMusic> musicList = workoutPlanMode
                ? assembleWorkoutPlaylist(
                        candidateSnapshot,
                        targetCadenceSpm,
                        intensityResolution.resolvedGoal(),
                        genre,
                        preferenceScores,
                        workoutPlan,
                        pageSize)
                : candidateSnapshot.stream()
                        .map(music -> scoreMusic(
                                music,
                                targetCadenceSpm,
                                intensityResolution.resolvedGoal(),
                                genre,
                                preferenceScores.getOrDefault(music.getId(), 3.0)))
                        .sorted(recommendationComparator(genre))
                        .skip(resultOffset)
                        .limit(pageSize)
                        .toList();
        boolean hasNext = workoutPlanMode
                ? false
                : spotifyPaged
                        ? spotifySourceCount == pageSize
                        : candidateSnapshot.size() > offset + pageSize;
        boolean cadenceApplied = musicList.stream()
                .anyMatch(value -> value.cadenceScore() != null);
        boolean intensityApplied = musicList.stream()
                .anyMatch(value -> value.intensityScore() != null);

        RecommendationLog log = createLog(
                loginMemberId,
                requestedIntensity,
                genre,
                bpmRange,
                cadenceResolution,
                intensityResolution,
                spotifyGenreDiscovery,
                resultPage,
                musicList,
                normalizedTargetDistanceKm,
                workoutPlan);
        if (recommendationLogMapper.insert(log) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        log.setDisplayNumber(recommendationLogMapper.countByMemberId(loginMemberId));
        return new RecommendationResult(
                log,
                List.copyOf(musicList),
                cadenceResolution.targetCadenceSpm(),
                cadenceResolution.explanation(),
                intensityResolution.resolvedGoal(),
                intensityResolution.source(),
                intensityResolution.averageHeartRateBpm(),
                intensityResolution.explanation(),
                resultPage,
                pageSize,
                resultPage > 1,
                hasNext,
                cadenceApplied,
                intensityApplied,
                normalizedTargetDistanceKm,
                workoutPlan);
    }

    /**
     * 플레이리스트 생성, 곡 복사, 추천 로그 연결을 하나의 트랜잭션으로 처리한다.
     * 이 메서드의 어느 단계에서든 실패하면 Spring 트랜잭션에 의해 전체가 롤백된다.
     */
    @Transactional
    public Playlist saveRecommendation(
            Long loginMemberId,
            Long recommendationLogId,
            String title,
            PlaylistVisibility visibility) {
        validateAuthenticated(loginMemberId);
        RecommendationLog log = getOwnedLog(loginMemberId, recommendationLogId);
        if (log.getSavedPlaylistId() != null) {
            throw new CustomException(ErrorCode.RECOMMENDATION_ALREADY_SAVED);
        }

        List<Long> musicIds = parseMusicIds(log.getResultSummary());
        if (musicIds.isEmpty()) {
            throw new CustomException(ErrorCode.RECOMMENDATION_DATA_NOT_ENOUGH);
        }

        Playlist playlist = buildCurationPlaylist(
                loginMemberId,
                title,
                visibility,
                log);
        if (playlistMapper.insertCurationCopy(playlist) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        List<PlaylistItem> items = buildPlaylistItems(playlist.getId(), musicIds);
        int insertedItems = playlistItemMapper.bulkInsertFromRecommendation(items);
        if (insertedItems != items.size()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        int updated = recommendationLogMapper.updateSavedPlaylistId(
                log.getId(),
                playlist.getId());
        if (updated != 1) {
            throw new CustomException(ErrorCode.RECOMMENDATION_ALREADY_SAVED);
        }
        log.setSavedPlaylistId(playlist.getId());
        return playlist;
    }

    public List<RecommendationLog> getMyRecommendationLogs(Long loginMemberId) {
        validateAuthenticated(loginMemberId);
        List<RecommendationLog> logs = recommendationLogMapper.selectByMemberId(loginMemberId);
        assignDisplayNumbers(logs);
        return logs;
    }

    public RecommendationResult getMyRecommendationResult(
            Long loginMemberId,
            Long recommendationLogId) {
        validateAuthenticated(loginMemberId);
        RecommendationLog log = getOwnedLog(loginMemberId, recommendationLogId);
        assignDisplayNumber(loginMemberId, log);

        List<Long> musicIds = parseMusicIds(log.getResultSummary());
        if (musicIds.isEmpty()) {
            throw new CustomException(ErrorCode.RECOMMENDATION_DATA_NOT_ENOUGH);
        }
        Map<Long, Music> musicById = musicMapper.selectByIds(musicIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        Music::getId,
                        music -> music));
        Map<Long, JsonNode> musicSnapshots = parseMusicSnapshots(log.getResultSummary());
        List<RecommendedMusic> musicList = musicIds.stream()
                .map(musicById::get)
                .filter(Objects::nonNull)
                .map(music -> restoreRecommendedMusic(
                        music,
                        musicSnapshots.get(music.getId()),
                        log))
                .toList();
        if (musicList.isEmpty()) {
            throw new CustomException(ErrorCode.RECOMMENDATION_DATA_NOT_ENOUGH);
        }

        JsonNode conditions = parseJson(log.getRequestConditions());
        WorkoutPlan workoutPlan = parseWorkoutPlan(log.getResultSummary());
        Integer targetCadenceSpm = intValue(conditions == null ? null : conditions.get("targetCadenceSpm"));
        RunningIntensityGoal intensityGoal =
                runningIntensityGoalValue(conditions == null ? null : conditions.get("runningIntensityGoal"));
        Double targetDistanceKm =
                doubleValue(conditions == null ? null : conditions.get("targetDistanceKm"));
        Integer page = intValue(conditions == null ? null : conditions.get("page"));
        return new RecommendationResult(
                log,
                musicList,
                targetCadenceSpm,
                targetCadenceSpm == null ? null : "이전 AI 추천 로그에서 복원한 케이던스 목표입니다.",
                intensityGoal,
                null,
                null,
                intensityGoal == null ? null : "이전 AI 추천 로그에서 복원한 운동 강도 목표입니다.",
                page == null ? 1 : page,
                musicList.size(),
                false,
                false,
                musicList.stream().anyMatch(value -> value.cadenceScore() != null),
                musicList.stream().anyMatch(value -> value.intensityScore() != null),
                targetDistanceKm,
                workoutPlan);
    }

    private RecommendationLog createLog(
            Long memberId,
            RunningIntensity intensity,
            String genre,
            int[] bpmRange,
            CadenceResolutionResult cadenceResolution,
            IntensityResolutionResult intensityResolution,
            boolean spotifyGenreDiscovery,
            int page,
            List<RecommendedMusic> musicList,
            Double targetDistanceKm,
            WorkoutPlan workoutPlan) {
        RecommendationLog log = new RecommendationLog();
        log.setMemberId(memberId);
        log.setRequestedIntensity(intensity);
        log.setRequestedBpmMin(bpmRange[0]);
        log.setRequestedBpmMax(bpmRange[1]);
        log.setRequestedGenre(genre);
        log.setRequestConditions(buildRequestConditions(
                intensity,
                genre,
                bpmRange,
                cadenceResolution.targetCadenceSpm(),
                intensityResolution.resolvedGoal(),
                page,
                targetDistanceKm,
                workoutPlan));
        log.setResultSummary(buildResultSummary(musicList, workoutPlan));
        log.setRecommendationReason(buildRecommendationReason(
                intensity,
                genre,
                bpmRange,
                cadenceResolution.targetCadenceSpm(),
                intensityResolution.resolvedGoal(),
                spotifyGenreDiscovery,
                targetDistanceKm,
                workoutPlan));
        return log;
    }

    private RecommendationLog getOwnedLog(Long memberId, Long logId) {
        if (logId == null) {
            throw new CustomException(ErrorCode.RECOMMENDATION_LOG_NOT_FOUND);
        }
        RecommendationLog log = recommendationLogMapper.selectById(logId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_LOG_NOT_FOUND));
        if (!memberId.equals(log.getMemberId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        return log;
    }

    private Playlist buildCurationPlaylist(
            Long memberId,
            String title,
            PlaylistVisibility visibility,
            RecommendationLog log) {
        String normalizedTitle = title == null || title.isBlank()
                ? "자동 큐레이션 - " + log.getRequestedIntensity()
                : title.trim();
        if (normalizedTitle.length() > MAX_PLAYLIST_TITLE_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_PLAYLIST_TITLE);
        }

        Playlist playlist = new Playlist();
        playlist.setOwnerId(memberId);
        playlist.setPlaylistType(PlaylistType.USER);
        playlist.setSourceType(PlaylistSourceType.CURATION_COPY);
        playlist.setVisibility(
                visibility == null ? PlaylistVisibility.PRIVATE : visibility);
        playlist.setTitle(normalizedTitle);
        playlist.setDescription(log.getRecommendationReason());
        playlist.setSpotifyExportStatus(SpotifyExportStatus.NOT_EXPORTED);
        return playlist;
    }

    private List<PlaylistItem> buildPlaylistItems(
            Long playlistId,
            List<Long> musicIds) {
        List<PlaylistItem> items = new ArrayList<>(musicIds.size());
        for (int index = 0; index < musicIds.size(); index++) {
            PlaylistItem item = new PlaylistItem();
            item.setPlaylistId(playlistId);
            item.setMusicId(musicIds.get(index));
            item.setOrderNo(index + 1);
            items.add(item);
        }
        return items;
    }

    private int[] resolveBpmRange(
            RunningProfile profile,
            RunningIntensity intensity) {
        if (profile != null
                && isValidBpmRange(
                        profile.getTargetBpmMin(),
                        profile.getTargetBpmMax())) {
            return new int[] {profile.getTargetBpmMin(), profile.getTargetBpmMax()};
        }

        // 기존 MusicService의 강도별 정책과 동일한 기본값을 사용한다.
        return switch (intensity) {
            case LOW -> new int[] {90, 130};
            case MODERATE -> new int[] {120, 160};
            case HIGH -> new int[] {150, 190};
            case INTERVAL -> new int[] {140, 200};
        };
    }

    private boolean isValidBpmRange(Integer minBpm, Integer maxBpm) {
        return minBpm != null
                && maxBpm != null
                && minBpm >= MIN_BPM
                && maxBpm <= MAX_BPM
                && minBpm <= maxBpm;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_RECOMMENDATION_SIZE;
        }
        if (size < 1 || size > MAX_RECOMMENDATION_SIZE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return size;
    }

    private int normalizePage(Integer page) {
        int normalized = page == null ? 1 : page;
        if (normalized < 1 || normalized > 5) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private String buildRequestConditions(
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

    private String buildResultSummary(
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

    private Map<Long, Double> preferenceScores(Long memberId) {
        List<MusicPreferenceScore> rows =
                musicMapper.selectPreferenceScores(memberId);
        Map<Long, Double> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (MusicPreferenceScore row : rows) {
            if (row != null
                    && row.getMusicId() != null
                    && row.getPreferenceScore() != null) {
                result.put(row.getMusicId(), row.getPreferenceScore());
            }
        }
        return result;
    }

    private CandidatePage importSpotifyCandidates(
            String preferredGenre,
            RunningIntensity intensity,
            int offset,
            int pageSize) {
        String query = preferredGenre == null
                ? spotifySearchQuery(intensity)
                : spotifyGenreSearchQuery(preferredGenre);
        List<SpotifyTrackMetadata> tracks = spotifyService.searchTracks(
                query,
                offset,
                pageSize);
        if ((tracks == null || tracks.isEmpty())
                && preferredGenre != null
                && offset == 0) {
            tracks = spotifyService.searchTracks(
                    spotifySearchQuery(intensity),
                    offset,
                    pageSize);
        }
        if (tracks == null) {
            return new CandidatePage(List.of(), 0);
        }
        List<Music> music = tracks.stream()
                .filter(java.util.Objects::nonNull)
                .map(spotifyService::saveSelectedTrack)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new CandidatePage(music, tracks.size());
    }

    private String spotifyGenreSearchQuery(String preferredGenre) {
        String normalized = preferredGenre
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-")
                .replaceAll("[^\\p{L}\\p{N}-]", "");
        return normalized.isBlank() ? "running" : "genre:" + normalized;
    }

    private String spotifySearchQuery(RunningIntensity intensity) {
        return switch (intensity) {
            case LOW -> "easy running";
            case MODERATE -> "running workout";
            case HIGH -> "tempo running";
            case INTERVAL -> "interval running";
        };
    }

    private record CandidatePage(List<Music> music, int sourceCount) {
    }

    private WorkoutPlan generateWorkoutPlan(
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

    private List<RecommendedMusic> assembleWorkoutPlaylist(
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

    private RecommendedMusic scoreMusic(
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

    private Comparator<RecommendedMusic> recommendationComparator(
            String preferredGenre) {
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
            if (cadenceMatch.mode()
                    == com.ssafy.ssafit.recommendation.domain.TempoMatchMode.HALF_TIME) {
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

    private String buildRecommendationReason(
            RunningIntensity intensity,
            String genre,
            int[] bpmRange,
            Integer targetCadenceSpm,
            RunningIntensityGoal intensityGoal,
            boolean spotifyGenreDiscovery,
            Double targetDistanceKm,
            WorkoutPlan workoutPlan) {
        if (workoutPlan != null) {
            String targetDistanceText = targetDistanceKm == null
                    ? "목표 거리 미지정"
                    : targetDistanceKm + "km";
            return "오늘 "
                    + targetDistanceText
                    + " "
                    + intensity
                    + " 러닝을 위해 AI가 "
                    + workoutPlan.totalDurationMinutes()
                    + "분, "
                    + workoutPlan.targetPace()
                    + ", 케이던스 "
                    + workoutPlan.cadenceMinSpm()
                    + "~"
                    + workoutPlan.cadenceMaxSpm()
                    + " SPM, 심박 "
                    + workoutPlan.heartRateMinBpm()
                    + "~"
                    + workoutPlan.heartRateMaxBpm()
                    + " BPM, 음악 "
                    + workoutPlan.musicBpmMin()
                    + "~"
                    + workoutPlan.musicBpmMax()
                    + " BPM 범위를 제안했습니다. "
                    + workoutPlan.coachingSummary();
        }
        String genreReason;
        if (genre == null) {
            genreReason = "장르 제한 없이";
        } else if (spotifyGenreDiscovery) {
            genreReason = "Spotify 장르 검색 필터로 "
                    + genre
                    + " 후보를 찾되 곡 장르를 추정 저장하지 않고";
        } else {
            genreReason = genre + " 장르 메타데이터가 있을 때 우선해";
        }
        String cadenceReason = targetCadenceSpm == null
                ? "케이던스 매칭 없이"
                : "목표 케이던스 " + targetCadenceSpm + " SPM 템포 매칭과";
        String intensityReason = intensityGoal == null
                ? "운동 강도 매칭 없이"
                : "운동 강도 " + intensityGoal + " 에너지 매칭을";
        return cadenceReason
                + " "
                + intensityReason
                + " 사용 가능한 점수 가중치를 재정규화하고 "
                + genreReason
                + " 추천했습니다. 기존 "
                + intensity
                + " 프로필 BPM 범위 "
                + bpmRange[0]
                + "~"
                + bpmRange[1]
                + "은 호환 로그 정보로 유지됩니다.";
    }

    private Double normalizeTargetDistance(Double targetDistanceKm) {
        if (targetDistanceKm == null || !Double.isFinite(targetDistanceKm)
                || targetDistanceKm < 0.5 || targetDistanceKm > 100.0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return BigDecimal.valueOf(targetDistanceKm)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private RunningIntensityGoal intensityGoalFromRunningIntensity(
            RunningIntensity intensity) {
        return switch (intensity) {
            case LOW -> RunningIntensityGoal.EASY;
            case MODERATE -> RunningIntensityGoal.MODERATE;
            case HIGH, INTERVAL -> RunningIntensityGoal.HIGH;
        };
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

    private int trackDurationMs(Music music) {
        return music.getDurationMs() == null || music.getDurationMs() <= 0
                ? DEFAULT_TRACK_DURATION_MS
                : music.getDurationMs();
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

    private List<Long> parseMusicIds(String resultSummary) {
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

    private void assignDisplayNumber(Long memberId, RecommendationLog target) {
        List<RecommendationLog> logs = recommendationLogMapper.selectByMemberId(memberId);
        assignDisplayNumbers(logs);
        logs.stream()
                .filter(log -> Objects.equals(log.getId(), target.getId()))
                .findFirst()
                .map(RecommendationLog::getDisplayNumber)
                .ifPresent(target::setDisplayNumber);
    }

    private void assignDisplayNumbers(List<RecommendationLog> logs) {
        int displayNumber = logs.size();
        for (RecommendationLog log : logs) {
            log.setDisplayNumber(displayNumber--);
        }
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

    private WorkoutPlan parseWorkoutPlan(String resultSummary) {
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

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private void validateAuthenticated(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
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
