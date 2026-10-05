package com.ssafy.ssafit.recommendation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;
import com.ssafy.ssafit.runningprofile.service.RunningFitnessMetricService;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;
import com.ssafy.ssafit.spotify.service.SpotifyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RecommendationService {

    private static final int DEFAULT_RECOMMENDATION_SIZE = 10;
    private static final int MAX_RECOMMENDATION_SIZE = 50;
    private static final int MAX_CANDIDATE_SIZE = 500;
    private static final int MAX_PLAYLIST_TITLE_LENGTH = 200;
    private static final int RECENT_RECORD_LIMIT = 10;
    private static final int MAX_WORKOUT_PLAYLIST_SIZE = 50;
    private static final Pattern MUSIC_IDS_PATTERN =
            Pattern.compile("\"musicIds\"\\s*:\\s*\\[([0-9,\\s]*)]");

    private final RunningProfileMapper runningProfileMapper;
    private final MusicMapper musicMapper;
    private final RecommendationLogMapper recommendationLogMapper;
    private final PlaylistMapper playlistMapper;
    private final PlaylistItemMapper playlistItemMapper;
    private final RunningIntensityResolutionService intensityResolutionService;
    private final CadenceResolutionService cadenceResolutionService;
    private final RecommendationScoringService recommendationScoringService;
    private final WorkoutPlanService workoutPlanService;
    private final SpotifyService spotifyService;
    private final RunningFitnessMetricService runningFitnessMetricService;
    private final RunningRecordMapper runningRecordMapper;
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
                ? workoutPlanService.generate(
                        profile,
                        fitnessMetric,
                        recentRecords,
                        requestedIntensity,
                        normalizedTargetDistanceKm)
                : null;
        int[] bpmRange = workoutPlan == null
                ? workoutPlanService.resolveBpmRange(profile, requestedIntensity)
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
                ? recommendationScoringService.assembleWorkoutPlaylist(
                        candidateSnapshot,
                        targetCadenceSpm,
                        intensityResolution.resolvedGoal(),
                        genre,
                        preferenceScores,
                        workoutPlan,
                        pageSize)
                : candidateSnapshot.stream()
                        .map(music -> recommendationScoringService.scoreMusic(
                                music,
                                targetCadenceSpm,
                                intensityResolution.resolvedGoal(),
                                genre,
                                preferenceScores.getOrDefault(music.getId(), 3.0)))
                        .sorted(recommendationScoringService.recommendationComparator(genre))
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

    @Transactional
    public void deleteMyRecommendationLog(Long loginMemberId, Long recommendationLogId) {
        validateAuthenticated(loginMemberId);
        RecommendationLog log = getOwnedLog(loginMemberId, recommendationLogId);
        if (recommendationLogMapper.deleteByIdAndMemberId(
                log.getId(),
                loginMemberId) != 1) {
            throw new CustomException(ErrorCode.RECOMMENDATION_LOG_NOT_FOUND);
        }
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

}
