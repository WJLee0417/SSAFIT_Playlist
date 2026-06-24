package com.ssafy.ssafit.recommendation.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import com.ssafy.ssafit.recommendation.policy.CadenceMatchingPolicy;
import com.ssafy.ssafit.recommendation.policy.CadenceMatchingPolicy.CadenceMatchResult;
import com.ssafy.ssafit.recommendation.policy.IntensityMatchingPolicy;
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;
import com.ssafy.ssafit.spotify.service.SpotifyService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
                requestedGenre,
                size,
                null,
                null);
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
        int[] bpmRange = resolveBpmRange(profile, requestedIntensity);
        String genre = firstNonBlank(
                requestedGenre,
                profile == null ? null : profile.getPreferredGenre());
        int recommendationSize = normalizeSize(size);
        if (limit != null) {
            recommendationSize = normalizeSize(limit);
        }
        int resultPage = normalizePage(page);
        int pageSize = Math.min(recommendationSize, 10);
        int offset = (resultPage - 1) * pageSize;
        CadenceResolutionResult cadenceResolution = cadenceResolutionService.resolve(
                loginMemberId,
                requestedTargetCadenceSpm,
                profile);
        IntensityResolutionResult intensityResolution =
                intensityResolutionService.resolve(
                        loginMemberId,
                        requestedIntensityGoal);

        List<Music> candidates = musicMapper.selectRecommendationCandidates(
                genre,
                MAX_CANDIDATE_SIZE);
        boolean spotifyGenreDiscovery = false;
        boolean spotifyPaged = false;
        int spotifySourceCount = 0;
        if (genre != null) {
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
        } else if (candidates == null || candidates.isEmpty()) {
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
        List<RecommendedMusic> musicList = candidateSnapshot.stream()
                .map(music -> scoreMusic(
                        music,
                        cadenceResolution.targetCadenceSpm(),
                        intensityResolution.resolvedGoal(),
                        genre,
                        preferenceScores.getOrDefault(music.getId(), 3.0)))
                .sorted(recommendationComparator(genre))
                .skip(resultOffset)
                .limit(pageSize)
                .toList();
        boolean hasNext = spotifyPaged
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
                musicList);
        if (recommendationLogMapper.insert(log) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
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
                intensityApplied);
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
        return recommendationLogMapper.selectByMemberId(loginMemberId);
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
            List<RecommendedMusic> musicList) {
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
                page));
        log.setResultSummary(buildResultSummary(musicList));
        log.setRecommendationReason(buildRecommendationReason(
                intensity,
                genre,
                bpmRange,
                cadenceResolution.targetCadenceSpm(),
                intensityResolution.resolvedGoal(),
                spotifyGenreDiscovery));
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
            int page) {
        return "{\"intensity\":\""
                + intensity
                + "\",\"bpmMin\":"
                + bpmRange[0]
                + ",\"bpmMax\":"
                + bpmRange[1]
                + ",\"genre\":"
                + jsonString(genre)
                + ",\"targetCadenceSpm\":"
                + (targetCadenceSpm == null ? "null" : targetCadenceSpm)
                + ",\"runningIntensityGoal\":"
                + jsonString(intensityGoal == null ? null : intensityGoal.name())
                + ",\"page\":"
                + page
                + "}";
    }

    private String buildResultSummary(List<RecommendedMusic> musicList) {
        String musicIds = musicList.stream()
                .map(RecommendedMusic::music)
                .map(Music::getId)
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return "{\"count\":"
                + musicList.size()
                + ",\"musicIds\":["
                + musicIds
                + "]}";
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
            boolean spotifyGenreDiscovery) {
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

    private List<Long> parseMusicIds(String resultSummary) {
        if (resultSummary == null) {
            return List.of();
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
