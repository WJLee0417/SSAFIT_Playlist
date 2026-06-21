package com.ssafy.ssafit.recommendation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.domain.MusicSortType;
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
import com.ssafy.ssafit.recommendation.domain.RecommendationResult;
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private static final int MIN_BPM = 60;
    private static final int MAX_BPM = 220;
    private static final int DEFAULT_RECOMMENDATION_SIZE = 10;
    private static final int MAX_RECOMMENDATION_SIZE = 50;
    private static final int MAX_PLAYLIST_TITLE_LENGTH = 200;
    private static final Pattern MUSIC_IDS_PATTERN =
            Pattern.compile("\"musicIds\"\\s*:\\s*\\[([0-9,\\s]*)]");

    private final RunningProfileMapper runningProfileMapper;
    private final MusicMapper musicMapper;
    private final RecommendationLogMapper recommendationLogMapper;
    private final PlaylistMapper playlistMapper;
    private final PlaylistItemMapper playlistItemMapper;

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
        validateAuthenticated(loginMemberId);
        if (requestedIntensity == null) {
            throw new CustomException(ErrorCode.INVALID_INTENSITY);
        }

        RunningProfile profile = runningProfileMapper.selectByMemberId(loginMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));
        int[] bpmRange = resolveBpmRange(profile, requestedIntensity);
        String genre = firstNonBlank(requestedGenre, profile.getPreferredGenre());
        int recommendationSize = normalizeSize(size);

        MusicSearchCondition condition = new MusicSearchCondition();
        condition.setGenre(genre);
        condition.setMinBpm(bpmRange[0]);
        condition.setMaxBpm(bpmRange[1]);
        condition.setRunningIntensity(requestedIntensity);
        condition.setSortType(MusicSortType.RUNNING_FIT_DESC);
        condition.setPage(1);
        condition.setSize(recommendationSize);
        condition.setOffset(0);

        List<Music> musicList = musicMapper.selectRecommendedMusic(condition);
        if (musicList == null || musicList.isEmpty()) {
            throw new CustomException(ErrorCode.RECOMMENDATION_DATA_NOT_ENOUGH);
        }

        RecommendationLog log = createLog(
                loginMemberId,
                requestedIntensity,
                genre,
                bpmRange,
                musicList);
        if (recommendationLogMapper.insert(log) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new RecommendationResult(log, List.copyOf(musicList));
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
            List<Music> musicList) {
        RecommendationLog log = new RecommendationLog();
        log.setMemberId(memberId);
        log.setRequestedIntensity(intensity);
        log.setRequestedBpmMin(bpmRange[0]);
        log.setRequestedBpmMax(bpmRange[1]);
        log.setRequestedGenre(genre);
        log.setRequestConditions(buildRequestConditions(intensity, genre, bpmRange));
        log.setResultSummary(buildResultSummary(musicList));
        log.setRecommendationReason(buildRecommendationReason(intensity, genre, bpmRange));
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
        if (isValidBpmRange(profile.getTargetBpmMin(), profile.getTargetBpmMax())) {
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

    private String buildRequestConditions(
            RunningIntensity intensity,
            String genre,
            int[] bpmRange) {
        return "{\"intensity\":\""
                + intensity
                + "\",\"bpmMin\":"
                + bpmRange[0]
                + ",\"bpmMax\":"
                + bpmRange[1]
                + ",\"genre\":"
                + jsonString(genre)
                + "}";
    }

    private String buildResultSummary(List<Music> musicList) {
        String musicIds = musicList.stream()
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

    private String buildRecommendationReason(
            RunningIntensity intensity,
            String genre,
            int[] bpmRange) {
        String genreReason = genre == null ? "장르 제한 없이" : genre + " 장르를 우선해";
        return intensity
                + " 러닝 강도와 "
                + bpmRange[0]
                + "~"
                + bpmRange[1]
                + " BPM 적합도, 음악 선호도 및 러닝 적합도 평균을 조합하고 "
                + genreReason
                + " 추천했습니다.";
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
