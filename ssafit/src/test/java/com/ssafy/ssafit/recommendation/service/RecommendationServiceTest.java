package com.ssafy.ssafit.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.mapper.PlaylistItemMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.domain.RecommendationResult;
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RunningProfileMapper runningProfileMapper;

    @Mock
    private MusicMapper musicMapper;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @Mock
    private PlaylistMapper playlistMapper;

    @Mock
    private PlaylistItemMapper playlistItemMapper;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                runningProfileMapper,
                musicMapper,
                recommendationLogMapper,
                playlistMapper,
                playlistItemMapper);
    }

    @Test
    void recommendationRequiresProfile() {
        when(runningProfileMapper.selectByMemberId(10L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.PROFILE_NOT_FOUND,
                () -> recommendationService.recommend(
                        10L,
                        RunningIntensity.LOW,
                        null,
                        10));
    }

    @Test
    void recommendationRejectsMissingIntensity() {
        assertError(
                ErrorCode.INVALID_INTENSITY,
                () -> recommendationService.recommend(10L, null, null, 10));

        verify(runningProfileMapper, never()).selectByMemberId(10L);
    }

    @Test
    void recommendationRejectsInsufficientData() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(null, null, "POP")));
        when(musicMapper.selectRecommendedMusic(any())).thenReturn(List.of());

        assertError(
                ErrorCode.RECOMMENDATION_DATA_NOT_ENOUGH,
                () -> recommendationService.recommend(
                        10L,
                        RunningIntensity.MODERATE,
                        null,
                        10));

        verify(recommendationLogMapper, never()).insert(any());
    }

    @Test
    void recommendationUsesProfileRangeAndSavesLog() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(135, 155, "ROCK")));
        List<Music> musicList = List.of(music(1L), music(2L));
        when(musicMapper.selectRecommendedMusic(any())).thenReturn(musicList);
        when(recommendationLogMapper.insert(any())).thenAnswer(invocation -> {
            RecommendationLog log = invocation.getArgument(0);
            log.setId(100L);
            return 1;
        });

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.HIGH,
                null,
                5);

        assertThat(result.getMusicList()).containsExactlyElementsOf(musicList);
        assertThat(result.getRecommendationLog().getId()).isEqualTo(100L);
        assertThat(result.getRecommendationLog().getRequestedBpmMin()).isEqualTo(135);
        assertThat(result.getRecommendationLog().getRequestedBpmMax()).isEqualTo(155);
        assertThat(result.getRecommendationLog().getRequestedGenre()).isEqualTo("ROCK");
        assertThat(result.getRecommendationLog().getResultSummary())
                .contains("\"musicIds\":[1,2]");

        ArgumentCaptor<MusicSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(MusicSearchCondition.class);
        verify(musicMapper).selectRecommendedMusic(conditionCaptor.capture());
        assertThat(conditionCaptor.getValue().getMinBpm()).isEqualTo(135);
        assertThat(conditionCaptor.getValue().getMaxBpm()).isEqualTo(155);
    }

    @Test
    void recommendationUsesExistingIntensityDefaultsWhenProfileRangeIsAbsent() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(null, null, null)));
        when(musicMapper.selectRecommendedMusic(any()))
                .thenReturn(List.of(music(1L)));
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.INTERVAL,
                "EDM",
                null);

        assertThat(result.getRecommendationLog().getRequestedBpmMin()).isEqualTo(140);
        assertThat(result.getRecommendationLog().getRequestedBpmMax()).isEqualTo(200);
        assertThat(result.getRecommendationLog().getRequestedGenre()).isEqualTo("EDM");
    }

    @Test
    void recommendationQueryDoesNotCreatePlaylist() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(null, null, null)));
        when(musicMapper.selectRecommendedMusic(any()))
                .thenReturn(List.of(music(1L)));
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        recommendationService.recommend(
                10L,
                RunningIntensity.LOW,
                null,
                10);

        verify(playlistMapper, never()).insertCurationCopy(any());
        verify(playlistItemMapper, never()).bulkInsertFromRecommendation(anyList());
    }

    @Test
    void savingRecommendationCreatesCurationCopyAndItemsAndConnectsLog() {
        RecommendationLog log = log(100L, 10L, "{\"count\":2,\"musicIds\":[1,2]}");
        when(recommendationLogMapper.selectById(100L)).thenReturn(Optional.of(log));
        when(playlistMapper.insertCurationCopy(any())).thenAnswer(invocation -> {
            Playlist playlist = invocation.getArgument(0);
            playlist.setId(200L);
            return 1;
        });
        when(playlistItemMapper.bulkInsertFromRecommendation(anyList())).thenReturn(2);
        when(recommendationLogMapper.updateSavedPlaylistId(100L, 200L)).thenReturn(1);

        Playlist playlist = recommendationService.saveRecommendation(
                10L,
                100L,
                "오늘의 추천",
                PlaylistVisibility.PRIVATE);

        assertThat(playlist.getId()).isEqualTo(200L);
        assertThat(playlist.getOwnerId()).isEqualTo(10L);
        assertThat(playlist.getPlaylistType()).isEqualTo(PlaylistType.USER);
        assertThat(playlist.getSourceType()).isEqualTo(PlaylistSourceType.CURATION_COPY);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PlaylistItem>> itemCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(playlistItemMapper).bulkInsertFromRecommendation(itemCaptor.capture());
        assertThat(itemCaptor.getValue())
                .extracting(PlaylistItem::getMusicId)
                .containsExactly(1L, 2L);
        assertThat(itemCaptor.getValue())
                .extracting(PlaylistItem::getOrderNo)
                .containsExactly(1, 2);
        verify(recommendationLogMapper).updateSavedPlaylistId(100L, 200L);
    }

    @Test
    void partialBulkInsertPreventsRecommendationLogConnection() {
        RecommendationLog log = log(100L, 10L, "{\"count\":2,\"musicIds\":[1,2]}");
        when(recommendationLogMapper.selectById(100L)).thenReturn(Optional.of(log));
        when(playlistMapper.insertCurationCopy(any())).thenAnswer(invocation -> {
            Playlist playlist = invocation.getArgument(0);
            playlist.setId(200L);
            return 1;
        });
        when(playlistItemMapper.bulkInsertFromRecommendation(anyList())).thenReturn(1);

        assertError(
                ErrorCode.INTERNAL_SERVER_ERROR,
                () -> recommendationService.saveRecommendation(
                        10L,
                        100L,
                        "partial copy",
                        PlaylistVisibility.PRIVATE));

        verify(recommendationLogMapper, never())
                .updateSavedPlaylistId(any(), any());
    }

    @Test
    void otherMemberCannotSaveRecommendationLog() {
        RecommendationLog log = log(100L, 20L, "{\"count\":1,\"musicIds\":[1]}");
        when(recommendationLogMapper.selectById(100L)).thenReturn(Optional.of(log));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> recommendationService.saveRecommendation(
                        10L,
                        100L,
                        "추천",
                        PlaylistVisibility.PRIVATE));

        verify(playlistMapper, never()).insertCurationCopy(any());
    }

    @Test
    void alreadySavedRecommendationCannotBeSavedAgain() {
        RecommendationLog log = log(100L, 10L, "{\"count\":1,\"musicIds\":[1]}");
        log.setSavedPlaylistId(200L);
        when(recommendationLogMapper.selectById(100L)).thenReturn(Optional.of(log));

        assertError(
                ErrorCode.RECOMMENDATION_ALREADY_SAVED,
                () -> recommendationService.saveRecommendation(
                        10L,
                        100L,
                        "추천",
                        PlaylistVisibility.PRIVATE));
    }

    @Test
    void bulkInsertFailureStopsLogConnectionForTransactionRollback() {
        RecommendationLog log = log(100L, 10L, "{\"count\":2,\"musicIds\":[1,2]}");
        when(recommendationLogMapper.selectById(100L)).thenReturn(Optional.of(log));
        when(playlistMapper.insertCurationCopy(any())).thenAnswer(invocation -> {
            Playlist playlist = invocation.getArgument(0);
            playlist.setId(200L);
            return 1;
        });
        doThrow(new IllegalStateException("bulk insert failed"))
                .when(playlistItemMapper)
                .bulkInsertFromRecommendation(anyList());

        assertThatThrownBy(() -> recommendationService.saveRecommendation(
                10L,
                100L,
                "추천",
                PlaylistVisibility.PRIVATE))
                .isInstanceOf(IllegalStateException.class);

        verify(recommendationLogMapper, never())
                .updateSavedPlaylistId(any(), any());
    }

    @Test
    void missingRecommendationLogIsRejected() {
        when(recommendationLogMapper.selectById(100L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.RECOMMENDATION_LOG_NOT_FOUND,
                () -> recommendationService.saveRecommendation(
                        10L,
                        100L,
                        "추천",
                        PlaylistVisibility.PRIVATE));
    }

    @Test
    void memberCanReadOwnRecommendationLogs() {
        RecommendationLog log = log(100L, 10L, "{\"count\":1,\"musicIds\":[1]}");
        when(recommendationLogMapper.selectByMemberId(10L)).thenReturn(List.of(log));

        assertThat(recommendationService.getMyRecommendationLogs(10L))
                .containsExactly(log);
    }

    private RunningProfile profile(Integer minBpm, Integer maxBpm, String genre) {
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(10L);
        profile.setRunningIntensity(RunningIntensity.MODERATE);
        profile.setTargetBpmMin(minBpm);
        profile.setTargetBpmMax(maxBpm);
        profile.setPreferredGenre(genre);
        return profile;
    }

    private Music music(Long id) {
        Music music = new Music();
        music.setId(id);
        music.setTitle("Music " + id);
        return music;
    }

    private RecommendationLog log(Long id, Long memberId, String summary) {
        RecommendationLog log = new RecommendationLog();
        log.setId(id);
        log.setMemberId(memberId);
        log.setRequestedIntensity(RunningIntensity.MODERATE);
        log.setRequestedBpmMin(120);
        log.setRequestedBpmMax(160);
        log.setResultSummary(summary);
        log.setRecommendationReason("추천 사유");
        return log;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
