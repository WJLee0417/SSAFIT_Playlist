package com.ssafy.ssafit.runningprofile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

@ExtendWith(MockitoExtension.class)
class RunningFitnessMetricServiceTest {

    @Mock
    private RunningRecordMapper runningRecordMapper;

    private RunningFitnessMetricService runningFitnessMetricService;

    @BeforeEach
    void setUp() {
        runningFitnessMetricService = new RunningFitnessMetricService(runningRecordMapper);
    }

    @Test
    void calculateReturnsEmptyMetricWhenNoRecentRecordsExist() {
        when(runningRecordMapper.selectRecentByMemberId(10L, 10)).thenReturn(List.of());

        RunningFitnessMetric metric = runningFitnessMetricService.calculate(10L);

        assertThat(metric.score()).isNull();
        assertThat(metric.recordCount()).isZero();
    }

    @Test
    void calculateUsesGoldStandardComponentScores() {
        when(runningRecordMapper.selectRecentByMemberId(10L, 10))
                .thenReturn(List.of(
                        record("5.00", 390, 150, 170),
                        record("7.00", 360, 150, 174),
                        record("6.00", 420, 150, 166)));

        RunningFitnessMetric metric = runningFitnessMetricService.calculate(10L);

        assertThat(metric.score()).isEqualTo(68);
        assertThat(metric.recordCount()).isEqualTo(3);
        assertThat(metric.averageRecentDistanceKm()).isEqualTo(6.0);
        assertThat(metric.heartRateCost()).isEqualTo(2.6);
        assertThat(metric.averageDistanceScore()).isEqualTo(60.0);
        assertThat(metric.paceEfficiencyScore()).isEqualTo(63.64);
        assertThat(metric.cadenceStabilityScore()).isEqualTo(83.67);
        assertThat(metric.variationScore()).isEqualTo(79.69);
    }

    private RunningRecord record(
            String distanceKm,
            int paceSecondsPerKm,
            int averageHeartRateBpm,
            int averageCadence) {
        RunningRecord record = new RunningRecord();
        record.setDistanceKm(new BigDecimal(distanceKm));
        record.setPaceSecondsPerKm(paceSecondsPerKm);
        record.setAverageHeartRateBpm(averageHeartRateBpm);
        record.setAverageCadence(averageCadence);
        return record;
    }
}
