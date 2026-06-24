package com.ssafy.ssafit.airunningreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.airunningreport.dto.AiRunningReportAggregationDtos.Aggregation;
import com.ssafy.ssafit.airunningreport.dto.AiRunningReportAggregationDtos.DataStatus;
import com.ssafy.ssafit.airunningreport.dto.AiRunningReportAggregationDtos.RecommendationBasis;
import com.ssafy.ssafit.airunningreport.dto.AiRunningReportAggregationDtos.TrendDirection;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

@ExtendWith(MockitoExtension.class)
class AiRunningReportAggregationServiceTest {

    @Mock
    private RunningRecordMapper runningRecordMapper;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @Mock
    private RunningProfileMapper runningProfileMapper;

    @Mock
    private PlaylistMapper playlistMapper;

    private AiRunningReportAggregationService aggregationService;

    @BeforeEach
    void setUp() {
        aggregationService = new AiRunningReportAggregationService(
                runningRecordMapper,
                recommendationLogMapper,
                runningProfileMapper,
                playlistMapper);
    }

    @Test
    void aggregateBuildsNormalizedDataForMemberWithEnoughRunningAndRecommendationData() {
        Long memberId = 10L;
        when(runningRecordMapper.selectRecentByMemberId(memberId, 8))
                .thenReturn(List.of(
                        record("2026-06-24", "5.00", 1800, 360, 170, 150),
                        record("2026-06-23", "5.00", 1800, 360, 171, 151),
                        record("2026-06-22", "5.00", 1800, 360, 172, 152),
                        record("2026-06-21", "5.00", 1800, 360, 173, 153),
                        record("2026-06-20", "5.00", 1800, 360, 174, 154),
                        record("2026-06-19", "5.00", 1800, 360, 175, 155),
                        record("2026-06-18", "5.00", 1800, 360, 176, 156),
                        record("2026-06-17", "5.00", 1800, 360, 177, 157)));
        when(recommendationLogMapper.selectRecentByMemberId(memberId, 10))
                .thenReturn(List.of(
                        log(1L, RunningIntensity.MODERATE, 120, 160, "POP", 200L),
                        log(2L, RunningIntensity.HIGH, 130, 170, "ROCK", 201L),
                        log(3L, RunningIntensity.MODERATE, 125, 165, "POP", null)));
        when(runningProfileMapper.selectByMemberId(memberId))
                .thenReturn(Optional.of(profile(RunningIntensity.MODERATE)));
        when(playlistMapper.selectByIds(List.of(200L, 201L)))
                .thenReturn(List.of(
                        playlist(200L, "Morning Run", SpotifyExportStatus.SUCCESS),
                        playlist(201L, "Hill Repeat", SpotifyExportStatus.NOT_EXPORTED)));

        Aggregation result = aggregationService.aggregate(memberId);

        assertThat(result.analysisStartDate()).isEqualTo(LocalDate.parse("2026-06-17"));
        assertThat(result.analysisEndDate()).isEqualTo(LocalDate.parse("2026-06-24"));
        assertThat(result.runningRecordCount()).isEqualTo(8);
        assertThat(result.running().totalDistanceKm()).isEqualByComparingTo("40.00");
        assertThat(result.running().averageDistanceKm()).isEqualByComparingTo("5.00");
        assertThat(result.running().totalDurationSeconds()).isEqualTo(14400);
        assertThat(result.running().averageDurationSeconds()).isEqualTo(1800);
        assertThat(result.running().averagePaceSecondsPerKm()).isEqualTo(360);
        assertThat(result.cadence().average()).isEqualTo(173.5);
        assertThat(result.cadence().min()).isEqualTo(170);
        assertThat(result.cadence().max()).isEqualTo(177);
        assertThat(result.heartRate().average()).isEqualTo(153.5);
        assertThat(result.intensity().currentProfileIntensity())
                .isEqualTo(RunningIntensity.MODERATE);
        assertThat(result.intensity().recentRecommendationDistribution())
                .containsEntry(RunningIntensity.MODERATE, 2)
                .containsEntry(RunningIntensity.HIGH, 1);
        assertThat(result.trend().paceDirection()).isEqualTo(TrendDirection.STABLE);
        assertThat(result.recommendations().recommendationCount()).isEqualTo(3);
        assertThat(result.recommendations().musicBpm().min()).isEqualTo(120);
        assertThat(result.recommendations().musicBpm().max()).isEqualTo(170);
        assertThat(result.recommendations().genreDistribution())
                .containsEntry("POP", 2)
                .containsEntry("ROCK", 1);
        assertThat(result.recommendations().savedPlaylists())
                .extracting("playlistId")
                .containsExactlyInAnyOrder(200L, 201L);
        assertThat(result.dataQuality().overallStatus()).isEqualTo(DataStatus.OK);
        assertThat(result.dataQuality().missingItems()).isEmpty();
        assertThat(result.recentRecords().getFirst().recordDate())
                .isEqualTo(LocalDate.parse("2026-06-24"));
    }

    @Test
    void aggregateRepresentsMemberWithoutRunningRecordsAsInsufficientWithoutZeroFilling() {
        Long memberId = 20L;
        when(runningRecordMapper.selectRecentByMemberId(memberId, 8))
                .thenReturn(List.of());
        when(recommendationLogMapper.selectRecentByMemberId(memberId, 10))
                .thenReturn(List.of());
        when(runningProfileMapper.selectByMemberId(memberId))
                .thenReturn(Optional.empty());

        Aggregation result = aggregationService.aggregate(memberId);

        assertThat(result.analysisStartDate()).isNull();
        assertThat(result.analysisEndDate()).isNull();
        assertThat(result.runningRecordCount()).isZero();
        assertThat(result.running().averageDistanceKm()).isNull();
        assertThat(result.running().averageDurationSeconds()).isNull();
        assertThat(result.running().averagePaceSecondsPerKm()).isNull();
        assertThat(result.cadence().average()).isNull();
        assertThat(result.heartRate().average()).isNull();
        assertThat(result.dataQuality().overallStatus())
                .isEqualTo(DataStatus.INSUFFICIENT);
        assertThat(result.dataQuality().missingItems())
                .contains("RUNNING_RECORDS", "CADENCE", "HEART_RATE", "RECOMMENDATION_LOGS");
        verify(playlistMapper, never()).selectByIds(anyList());
    }

    @Test
    void aggregateMarksCadenceAndHeartRateAsUnavailableOrInsufficientWhenValuesAreMissing() {
        Long memberId = 30L;
        when(runningRecordMapper.selectRecentByMemberId(memberId, 8))
                .thenReturn(List.of(
                        record("2026-06-24", "5.00", 1800, 360, null, 150),
                        record("2026-06-23", "5.20", 1810, 348, null, null),
                        record("2026-06-22", "5.40", 1820, 337, null, null)));
        when(recommendationLogMapper.selectRecentByMemberId(memberId, 10))
                .thenReturn(List.of(log(
                        1L,
                        RunningIntensity.LOW,
                        90,
                        130,
                        null,
                        null)));
        when(runningProfileMapper.selectByMemberId(memberId))
                .thenReturn(Optional.empty());

        Aggregation result = aggregationService.aggregate(memberId);

        assertThat(result.cadence().status()).isEqualTo(DataStatus.NOT_AVAILABLE);
        assertThat(result.cadence().average()).isNull();
        assertThat(result.cadence().sampleCount()).isZero();
        assertThat(result.heartRate().status()).isEqualTo(DataStatus.INSUFFICIENT);
        assertThat(result.heartRate().average()).isEqualTo(150.0);
        assertThat(result.heartRate().sampleCount()).isEqualTo(1);
        assertThat(result.dataQuality().overallStatus()).isEqualTo(DataStatus.LIMITED);
        assertThat(result.dataQuality().missingItems())
                .contains("CADENCE", "HEART_RATE");
    }

    @Test
    void aggregateUsesRunningProfileForMusicDirectionWhenMemberHasNoRecommendationLogs() {
        Long memberId = 40L;
        when(runningRecordMapper.selectRecentByMemberId(memberId, 8))
                .thenReturn(List.of(
                        record("2026-06-24", "5.00", 1800, 360, 170, 150),
                        record("2026-06-23", "5.00", 1800, 360, 171, 151),
                        record("2026-06-22", "5.00", 1800, 360, 172, 152)));
        when(recommendationLogMapper.selectRecentByMemberId(memberId, 10))
                .thenReturn(List.of());
        when(runningProfileMapper.selectByMemberId(memberId))
                .thenReturn(Optional.of(profile(RunningIntensity.HIGH)));

        Aggregation result = aggregationService.aggregate(memberId);

        assertThat(result.recommendations().status())
                .isEqualTo(DataStatus.LIMITED);
        assertThat(result.recommendations().recommendationCount()).isZero();
        assertThat(result.recommendations().musicBpm().min()).isEqualTo(150);
        assertThat(result.recommendations().musicBpm().max()).isEqualTo(190);
        assertThat(result.recommendations().genreDistribution()).isEmpty();
        assertThat(result.recommendations().profilePreferredGenre())
                .isEqualTo("ROCK");
        assertThat(result.recommendations().basis())
                .isEqualTo(RecommendationBasis.RUNNING_PROFILE);
        assertThat(result.recommendations().savedPlaylists()).isEmpty();
        assertThat(result.intensity().currentProfileIntensity())
                .isEqualTo(RunningIntensity.HIGH);
        assertThat(result.dataQuality().missingItems())
                .contains("RECOMMENDATION_HISTORY")
                .doesNotContain("RECOMMENDATION_LOGS");
        verify(playlistMapper, never()).selectByIds(anyList());
    }

    @Test
    void aggregateClampsRequestedRecordLimitToTen() {
        Long memberId = 50L;
        when(runningRecordMapper.selectRecentByMemberId(memberId, 10))
                .thenReturn(List.of(
                        record("2026-06-24", "5.00", 1800, 360, 170, 150),
                        record("2026-06-23", "5.00", 1800, 360, 170, 150),
                        record("2026-06-22", "5.00", 1800, 360, 170, 150),
                        record("2026-06-21", "5.00", 1800, 360, 170, 150),
                        record("2026-06-20", "5.00", 1800, 360, 170, 150),
                        record("2026-06-19", "5.00", 1800, 360, 170, 150),
                        record("2026-06-18", "5.00", 1800, 360, 170, 150),
                        record("2026-06-17", "5.00", 1800, 360, 170, 150),
                        record("2026-06-16", "5.00", 1800, 360, 170, 150),
                        record("2026-06-15", "5.00", 1800, 360, 170, 150)));
        when(recommendationLogMapper.selectRecentByMemberId(memberId, 10))
                .thenReturn(List.of());
        when(runningProfileMapper.selectByMemberId(memberId))
                .thenReturn(Optional.empty());

        Aggregation result = aggregationService.aggregate(memberId, 50);

        assertThat(result.requestedRecordLimit()).isEqualTo(10);
        assertThat(result.runningRecordCount()).isEqualTo(10);
        assertThat(result.recentRecords()).hasSize(10);
        verify(runningRecordMapper).selectRecentByMemberId(memberId, 10);
    }

    private RunningRecord record(
            String recordDate,
            String distanceKm,
            Integer durationSeconds,
            Integer paceSecondsPerKm,
            Integer cadence,
            Integer heartRate) {
        RunningRecord record = new RunningRecord();
        record.setRecordDate(LocalDate.parse(recordDate));
        record.setDistanceKm(new BigDecimal(distanceKm));
        record.setDurationSeconds(durationSeconds);
        record.setPaceSecondsPerKm(paceSecondsPerKm);
        record.setAverageCadence(cadence);
        record.setAverageHeartRateBpm(heartRate);
        return record;
    }

    private RecommendationLog log(
            Long id,
            RunningIntensity intensity,
            Integer bpmMin,
            Integer bpmMax,
            String genre,
            Long savedPlaylistId) {
        RecommendationLog log = new RecommendationLog();
        log.setId(id);
        log.setRequestedIntensity(intensity);
        log.setRequestedBpmMin(bpmMin);
        log.setRequestedBpmMax(bpmMax);
        log.setRequestedGenre(genre);
        log.setSavedPlaylistId(savedPlaylistId);
        log.setCreatedAt(LocalDateTime.of(2026, 6, 24, 12, 0).minusDays(id));
        return log;
    }

    private RunningProfile profile(RunningIntensity intensity) {
        RunningProfile profile = new RunningProfile();
        profile.setRunningIntensity(intensity);
        profile.setTargetBpmMin(150);
        profile.setTargetBpmMax(190);
        profile.setPreferredGenre("ROCK");
        return profile;
    }

    private Playlist playlist(
            Long id,
            String title,
            SpotifyExportStatus exportStatus) {
        Playlist playlist = new Playlist();
        playlist.setId(id);
        playlist.setTitle(title);
        playlist.setSpotifyExportStatus(exportStatus);
        playlist.setSpotifyPlaylistUrl("https://open.spotify.com/playlist/" + id);
        playlist.setCreatedAt(LocalDateTime.of(2026, 6, 24, 12, 0).minusHours(id));
        return playlist;
    }
}
