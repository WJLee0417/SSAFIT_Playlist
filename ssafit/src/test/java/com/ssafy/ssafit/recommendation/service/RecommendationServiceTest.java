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
                spotifyService,
                runningFitnessMetricService,
                runningRecordMapper,
                List.of(),
                new ObjectMapper());
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