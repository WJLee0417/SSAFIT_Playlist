package com.ssafy.ssafit.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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
import com.ssafy.ssafit.music.domain.MusicEnergyLevel;
import com.ssafy.ssafit.music.domain.SpotifyTrackMetadata;
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
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;
import com.ssafy.ssafit.recommendation.domain.CadenceResolutionResult;
import com.ssafy.ssafit.recommendation.domain.IntensityResolutionResult;
import com.ssafy.ssafit.recommendation.domain.IntensitySource;
import com.ssafy.ssafit.recommendation.policy.CadenceMatchingPolicy;
import com.ssafy.ssafit.recommendation.policy.IntensityMatchingPolicy;
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;
import com.ssafy.ssafit.runningprofile.service.RunningFitnessMetricService;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;
import com.ssafy.ssafit.spotify.service.SpotifyService;

import tools.jackson.databind.ObjectMapper;

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

    @Mock
    private RunningIntensityResolutionService intensityResolutionService;

    @Mock
    private CadenceResolutionService cadenceResolutionService;

    @Mock
    private SpotifyService spotifyService;

    @Mock
    private RunningFitnessMetricService runningFitnessMetricService;

    @Mock
    private RunningRecordMapper runningRecordMapper;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                runningProfileMapper,
                musicMapper,
                recommendationLogMapper,
                playlistMapper,
                playlistItemMapper,
                intensityResolutionService,
                cadenceResolutionService,
                new RecommendationScoringService(
                        new CadenceMatchingPolicy(),
                        new IntensityMatchingPolicy()),
                new WorkoutPlanService(
                        List.of(),
                        new ObjectMapper()),
                new RecommendationSnapshotService(
                        new ObjectMapper()),
                spotifyService,
                runningFitnessMetricService,
                runningRecordMapper);
        lenient().when(runningFitnessMetricService.calculate(any()))
                .thenReturn(com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric.empty());
        lenient().when(runningRecordMapper.selectRecentByMemberId(any(), anyInt()))
                .thenReturn(List.of());
        lenient().when(cadenceResolutionService.resolve(any(), any(), any()))
                .thenReturn(new CadenceResolutionResult(null, "UNKNOWN", "none"));
        lenient().when(intensityResolutionService.resolve(any(), any()))
                .thenReturn(new IntensityResolutionResult(
                        null,
                        IntensitySource.UNKNOWN,
                        false,
                        null,
                        "none"));
    }

    @Test
    void recommendationCanUseDefaultsWithoutProfile() {
        when(runningProfileMapper.selectByMemberId(10L)).thenReturn(Optional.empty());
        when(musicMapper.selectRecommendationCandidates(any(), any(Integer.class)))
                .thenReturn(List.of(music(1L)));
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.LOW,
                null,
                10);

        assertThat(result.getMusicList()).hasSize(1);
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
        when(musicMapper.selectRecommendationCandidates(any(), any(Integer.class)))
                .thenReturn(List.of());

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
    void recommendationImportsSpotifyTracksWhenLocalCatalogIsEmpty() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(120, 160, "POP")));
        Music imported = music(1L);
        when(musicMapper.selectRecommendationCandidates("POP", 500))
                .thenReturn(List.of());
        SpotifyTrackMetadata spotifyTrack = new SpotifyTrackMetadata();
        spotifyTrack.setSpotifyTrackId("spotify-track-1");
        spotifyTrack.setSpotifyUri("spotify:track:1");
        spotifyTrack.setTitle("Imported Track");
        spotifyTrack.setArtist("Artist");
        when(spotifyService.searchTracks("genre:pop", 0, 10))
                .thenReturn(List.of(spotifyTrack));
        when(spotifyService.saveSelectedTrack(spotifyTrack)).thenReturn(imported);
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.MODERATE,
                null,
                10);

        assertThat(result.getMusicList())
                .extracting(value -> value.music().getId())
                .containsExactly(1L);
        assertThat(result.getRecommendationLog().getRecommendationReason())
                .contains("Spotify 장르 검색 필터", "곡 장르를 추정 저장하지 않고");
        verify(spotifyService).saveSelectedTrack(spotifyTrack);
    }

    @Test
    void recommendationFallsBackWhenSpotifyGenreSearchIsEmpty() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(120, 160, "HIP HOP")));
        Music imported = music(1L);
        when(musicMapper.selectRecommendationCandidates("HIP HOP", 500))
                .thenReturn(List.of());
        SpotifyTrackMetadata spotifyTrack = new SpotifyTrackMetadata();
        spotifyTrack.setSpotifyTrackId("spotify-track-1");
        spotifyTrack.setSpotifyUri("spotify:track:1");
        spotifyTrack.setTitle("Imported Track");
        spotifyTrack.setArtist("Artist");
        when(spotifyService.searchTracks("genre:hip-hop", 0, 10))
                .thenReturn(List.of());
        when(spotifyService.searchTracks("running workout", 0, 10))
                .thenReturn(List.of(spotifyTrack));
        when(spotifyService.saveSelectedTrack(spotifyTrack)).thenReturn(imported);
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.MODERATE,
                null,
                10);

        assertThat(result.getMusicList()).hasSize(1);
        verify(spotifyService).searchTracks("genre:hip-hop", 0, 10);
        verify(spotifyService).searchTracks("running workout", 0, 10);
    }

    @Test
    void recommendationUsesSpotifyOffsetForNextPage() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(120, 160, "POP")));
        when(musicMapper.selectRecommendationCandidates("POP", 500))
                .thenReturn(List.of());
        SpotifyTrackMetadata spotifyTrack = new SpotifyTrackMetadata();
        spotifyTrack.setSpotifyTrackId("spotify-track-11");
        spotifyTrack.setSpotifyUri("spotify:track:11");
        spotifyTrack.setTitle("Page Two Track");
        spotifyTrack.setArtist("Artist");
        Music imported = music(11L);
        when(spotifyService.searchTracks("genre:pop", 10, 10))
                .thenReturn(List.of(spotifyTrack));
        when(spotifyService.saveSelectedTrack(spotifyTrack)).thenReturn(imported);
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.MODERATE,
                null,
                RunningIntensityGoal.MODERATE,
                "POP",
                null,
                10,
                2);

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.isHasPrevious()).isTrue();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getMusicList())
                .extracting(value -> value.music().getId())
                .containsExactly(11L);
        verify(spotifyService).searchTracks("genre:pop", 10, 10);
    }

    @Test
    void recommendationRejectsPageAfterFive() {
        when(runningProfileMapper.selectByMemberId(10L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> recommendationService.recommend(
                        10L,
                        RunningIntensity.MODERATE,
                        null,
                        RunningIntensityGoal.MODERATE,
                        "POP",
                        null,
                        10,
                        6));

        verify(spotifyService, never()).searchTracks(any(), any(), any());
    }

    @Test
    void recommendationUsesProfileRangeAndSavesLog() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(135, 155, "ROCK")));
        List<Music> musicList = List.of(music(1L), music(2L));
        when(musicMapper.selectRecommendationCandidates(any(), any(Integer.class)))
                .thenReturn(musicList);
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
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

        assertThat(result.getMusicList().stream()
                .map(value -> value.music().getId())
                .toList()).containsExactly(1L, 2L);
        assertThat(result.getRecommendationLog().getId()).isEqualTo(100L);
        assertThat(result.getRecommendationLog().getRequestedBpmMin()).isEqualTo(135);
        assertThat(result.getRecommendationLog().getRequestedBpmMax()).isEqualTo(155);
        assertThat(result.getRecommendationLog().getRequestedGenre()).isEqualTo("ROCK");
        assertThat(result.getRecommendationLog().getResultSummary())
                .contains("\"musicIds\":[1,2]");

        verify(musicMapper).selectRecommendationCandidates("ROCK", 500);
    }

    @Test
    void recommendationTreatsPreferredGenreAsSoftPreference() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(120, 160, "POP")));
        Music unclassified = music(1L);
        Music matching = music(2L);
        matching.setGenre("pop");
        when(musicMapper.selectRecommendationCandidates("POP", 500))
                .thenReturn(List.of(unclassified, matching));
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.MODERATE,
                null,
                10);

        assertThat(result.getMusicList())
                .extracting(value -> value.music().getId())
                .containsExactly(2L, 1L);
        assertThat(result.getMusicList().getFirst().recommendationReason())
                .contains("선호 장르 POP 일치");
    }

    @Test
    void recommendationUsesExistingIntensityDefaultsWhenProfileRangeIsAbsent() {
        when(runningProfileMapper.selectByMemberId(10L))
                .thenReturn(Optional.of(profile(null, null, null)));
        when(musicMapper.selectRecommendationCandidates(any(), any(Integer.class)))
                .thenReturn(List.of(music(1L)));
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
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
        when(musicMapper.selectRecommendationCandidates(any(), any(Integer.class)))
                .thenReturn(List.of(music(1L)));
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
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
        RecommendationLog newestLog = log(120L, 10L, "{\"count\":1,\"musicIds\":[1]}");
        RecommendationLog oldestLog = log(100L, 10L, "{\"count\":1,\"musicIds\":[2]}");
        when(recommendationLogMapper.selectByMemberId(10L))
                .thenReturn(List.of(newestLog, oldestLog));

        assertThat(recommendationService.getMyRecommendationLogs(10L))
                .containsExactly(newestLog, oldestLog)
                .extracting(RecommendationLog::getDisplayNumber)
                .containsExactly(2, 1);
    }

    @Test
    void memberCanRestoreOwnRecommendationResultFromLog() {
        RecommendationLog log = log(
                100L,
                10L,
                """
                {
                  "count": 1,
                  "musicIds": [1],
                  "music": [{
                    "id": 1,
                    "cadenceScore": 90.0,
                    "intensityScore": 80.0,
                    "preferenceScore": 3.0,
                    "finalScore": 82.0,
                    "tempoMatchMode": "DIRECT",
                    "recommendationReason": "170 SPM에 맞는 곡"
                  }],
                  "workoutPlan": {
                    "totalDurationMinutes": 30,
                    "targetPace": "6'00\\"/km",
                    "targetPaceSecondsPerKm": 360,
                    "cadenceMinSpm": 166,
                    "cadenceMaxSpm": 174,
                    "heartRateMinBpm": 140,
                    "heartRateMaxBpm": 165,
                    "musicBpmMin": 165,
                    "musicBpmMax": 175,
                    "coachingSummary": "30분 지속주",
                    "musicStrategy": "중반부 BPM 유지",
                    "provider": "fallback",
                    "modelName": null,
                    "fallback": true,
                    "segments": [{
                      "name": "메인",
                      "durationMinutes": 30,
                      "bpmMin": 165,
                      "bpmMax": 175,
                      "purpose": "목표 페이스 유지"
                    }]
                  }
                }
                """);
        log.setRequestConditions(
                "{\"targetDistanceKm\":5.0,\"targetCadenceSpm\":170,"
                        + "\"runningIntensityGoal\":\"MODERATE\",\"page\":1}");
        Music music = music(1L);
        when(recommendationLogMapper.selectById(100L)).thenReturn(Optional.of(log));
        when(recommendationLogMapper.selectByMemberId(10L)).thenReturn(List.of(log));
        when(musicMapper.selectByIds(List.of(1L))).thenReturn(List.of(music));

        RecommendationResult result =
                recommendationService.getMyRecommendationResult(10L, 100L);

        assertThat(result.getRecommendationLog().getId()).isEqualTo(100L);
        assertThat(result.getRecommendationLog().getDisplayNumber()).isEqualTo(1);
        assertThat(result.getTargetDistanceKm()).isEqualTo(5.0);
        assertThat(result.getWorkoutPlan().totalDurationMinutes()).isEqualTo(30);
        assertThat(result.getMusicList()).hasSize(1);
        assertThat(result.getMusicList().getFirst().finalScore()).isEqualTo(82.0);
        assertThat(result.getMusicList().getFirst().recommendationReason())
                .isEqualTo("170 SPM에 맞는 곡");
    }

    @Test
    void recommendationSeparatesScoresAndRenormalizesMissingMetadata() {
        when(runningProfileMapper.selectByMemberId(10L)).thenReturn(Optional.empty());
        when(cadenceResolutionService.resolve(10L, 172, null))
                .thenReturn(new CadenceResolutionResult(
                        172,
                        "USER_SELECTED",
                        "172 SPM"));
        when(intensityResolutionService.resolve(10L, RunningIntensityGoal.HIGH))
                .thenReturn(new IntensityResolutionResult(
                        RunningIntensityGoal.HIGH,
                        IntensitySource.USER_SELECTED,
                        true,
                        154.0,
                        "heart-rate reference only"));
        Music complete = music(1L);
        complete.setMusicBpm(86);
        complete.setMusicEnergyLevel(MusicEnergyLevel.PEAK);
        Music noBpm = music(2L);
        noBpm.setMusicEnergyLevel(MusicEnergyLevel.PEAK);
        Music noEnergy = music(3L);
        noEnergy.setMusicBpm(172);
        Music preferenceOnly = music(4L);
        when(musicMapper.selectRecommendationCandidates(null, 500))
                .thenReturn(List.of(complete, noBpm, noEnergy, preferenceOnly));
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.HIGH,
                172,
                RunningIntensityGoal.HIGH,
                null,
                null,
                10);

        var byId = result.getMusicList().stream()
                .collect(java.util.stream.Collectors.toMap(
                        value -> value.music().getId(),
                        value -> value));
        assertThat(byId.get(1L).tempoMatchMode())
                .isEqualTo(TempoMatchMode.HALF_TIME);
        assertThat(byId.get(1L).cadenceScore()).isEqualTo(100.0);
        assertThat(byId.get(1L).intensityScore()).isEqualTo(100.0);
        assertThat(byId.get(2L).cadenceScore()).isNull();
        assertThat(byId.get(2L).finalScore()).isEqualTo(90.0);
        assertThat(byId.get(3L).intensityScore()).isNull();
        assertThat(byId.get(3L).finalScore()).isEqualTo(94.29);
        assertThat(byId.get(4L).finalScore()).isEqualTo(60.0);
        assertThat(byId.get(1L).recommendationReason())
                .contains("172 SPM", "86 BPM");
    }

    @Test
    void heartRateReferenceValueDoesNotChangeCadenceScoreOrMusicBpm() {
        when(runningProfileMapper.selectByMemberId(10L)).thenReturn(Optional.empty());
        when(cadenceResolutionService.resolve(10L, 172, null))
                .thenReturn(new CadenceResolutionResult(
                        172,
                        "USER_SELECTED",
                        "172 SPM"));
        when(intensityResolutionService.resolve(10L, null))
                .thenReturn(
                        new IntensityResolutionResult(
                                RunningIntensityGoal.MODERATE,
                                IntensitySource.HISTORY_REFERENCE,
                                true,
                                120.0,
                                "reference"),
                        new IntensityResolutionResult(
                                RunningIntensityGoal.MODERATE,
                                IntensitySource.HISTORY_REFERENCE,
                                true,
                                180.0,
                                "reference"));
        Music candidate = music(1L);
        candidate.setMusicBpm(86);
        candidate.setMusicEnergyLevel(MusicEnergyLevel.STEADY);
        when(musicMapper.selectRecommendationCandidates(null, 500))
                .thenReturn(List.of(candidate));
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult first = recommendationService.recommend(
                10L,
                RunningIntensity.MODERATE,
                172,
                null,
                null,
                1,
                null);
        RecommendationResult second = recommendationService.recommend(
                10L,
                RunningIntensity.MODERATE,
                172,
                null,
                null,
                1,
                null);

        assertThat(first.getMusicList().getFirst().cadenceScore())
                .isEqualTo(second.getMusicList().getFirst().cadenceScore());
        assertThat(first.getMusicList().getFirst().music().getMusicBpm())
                .isEqualTo(86);
        assertThat(second.getMusicList().getFirst().music().getMusicBpm())
                .isEqualTo(86);
    }

    @Test
    void identicalScoresUseStableMusicIdOrdering() {
        when(runningProfileMapper.selectByMemberId(10L)).thenReturn(Optional.empty());
        Music laterId = music(2L);
        Music earlierId = music(1L);
        when(musicMapper.selectRecommendationCandidates(null, 500))
                .thenReturn(List.of(laterId, earlierId));
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);

        RecommendationResult result = recommendationService.recommend(
                10L,
                RunningIntensity.LOW,
                null,
                10);

        assertThat(result.getMusicList())
                .extracting(value -> value.music().getId())
                .containsExactly(1L, 2L);
    }

    @Test
    void workoutRecommendationResultSummaryIsValidJson() throws Exception {
        when(runningProfileMapper.selectByMemberId(10L)).thenReturn(Optional.empty());
        when(musicMapper.selectRecommendationCandidates(null, 500))
                .thenReturn(List.of(music(1L)));
        when(musicMapper.selectPreferenceScores(10L)).thenReturn(List.of());
        when(recommendationLogMapper.insert(any())).thenReturn(1);
        ArgumentCaptor<RecommendationLog> logCaptor =
                ArgumentCaptor.forClass(RecommendationLog.class);

        recommendationService.recommend(
                10L,
                RunningIntensity.MODERATE,
                5.0,
                null,
                null,
                null,
                null,
                null,
                1);

        verify(recommendationLogMapper).insert(logCaptor.capture());
        var summary = new ObjectMapper().readTree(
                logCaptor.getValue().getResultSummary());
        assertThat(summary.get("count").asInt()).isEqualTo(1);
        assertThat(summary.get("workoutPlan").isObject()).isTrue();
    }

    private RunningProfile profile(Integer minBpm, Integer maxBpm, String genre) {
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(10L);
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
