package com.ssafy.ssafit.runningprofile.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningFitnessMetricService {

    private static final int RECENT_RECORD_LIMIT = 10;
    private static final double BEST_HEART_RATE_COST = 1.8;
    private static final double WORST_HEART_RATE_COST = 4.0;
    private static final double AVERAGE_DISTANCE_WEIGHT = 0.35;
    private static final double PACE_EFFICIENCY_WEIGHT = 0.35;
    private static final double CADENCE_STABILITY_WEIGHT = 0.15;
    private static final double VARIATION_WEIGHT = 0.15;
    private static final double NEUTRAL_SCORE = 50.0;

    private final RunningRecordMapper runningRecordMapper;

    public RunningFitnessMetric calculate(Long memberId) {
        if (memberId == null) {
            return RunningFitnessMetric.empty();
        }

        List<RunningRecord> records = runningRecordMapper.selectRecentByMemberId(
                memberId,
                RECENT_RECORD_LIMIT);
        if (records == null || records.isEmpty()) {
            return RunningFitnessMetric.empty();
        }

        double averageDistance = records.stream()
                .map(RunningRecord::getDistanceKm)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
        double averageDistanceScore = clamp(averageDistance / 10.0 * 100.0);

        List<Double> heartRateCosts = records.stream()
                .filter(record -> record.getPaceSecondsPerKm() != null)
                .filter(record -> record.getAverageHeartRateBpm() != null)
                .filter(record -> record.getAverageHeartRateBpm() > 0)
                .map(record -> record.getPaceSecondsPerKm()
                        / (double) record.getAverageHeartRateBpm())
                .toList();
        Double heartRateCost = average(heartRateCosts);
        double paceEfficiencyScore = heartRateCost == null
                ? NEUTRAL_SCORE
                : clamp((WORST_HEART_RATE_COST - heartRateCost)
                        / (WORST_HEART_RATE_COST - BEST_HEART_RATE_COST)
                        * 100.0);

        List<Double> cadences = records.stream()
                .map(RunningRecord::getAverageCadence)
                .filter(Objects::nonNull)
                .map(Integer::doubleValue)
                .toList();
        Double cadenceStandardDeviation = cadences.size() < 2
                ? null
                : standardDeviation(cadences);
        double cadenceStabilityScore = cadenceStandardDeviation == null
                ? NEUTRAL_SCORE
                : clamp(100.0 - cadenceStandardDeviation * 5.0);

        List<Double> paces = records.stream()
                .map(RunningRecord::getPaceSecondsPerKm)
                .filter(Objects::nonNull)
                .map(Integer::doubleValue)
                .toList();
        List<Double> distances = records.stream()
                .map(RunningRecord::getDistanceKm)
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .toList();
        Double paceCoefficientOfVariation = coefficientOfVariation(paces);
        Double distanceCoefficientOfVariation = coefficientOfVariation(distances);
        double variationScore = paceCoefficientOfVariation == null
                || distanceCoefficientOfVariation == null
                ? NEUTRAL_SCORE
                : 100.0 - clamp(
                        paceCoefficientOfVariation * 150.0
                                + distanceCoefficientOfVariation * 80.0);

        int totalScore = (int) Math.round(
                averageDistanceScore * AVERAGE_DISTANCE_WEIGHT
                        + paceEfficiencyScore * PACE_EFFICIENCY_WEIGHT
                        + cadenceStabilityScore * CADENCE_STABILITY_WEIGHT
                        + variationScore * VARIATION_WEIGHT);

        return new RunningFitnessMetric(
                clampInt(totalScore),
                records.size(),
                round(averageDistance),
                roundNullable(heartRateCost),
                roundNullable(cadenceStandardDeviation),
                roundNullable(paceCoefficientOfVariation),
                roundNullable(distanceCoefficientOfVariation),
                round(averageDistanceScore),
                round(paceEfficiencyScore),
                round(cadenceStabilityScore),
                round(variationScore));
    }

    private Double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private Double standardDeviation(List<Double> values) {
        Double average = average(values);
        if (average == null) {
            return null;
        }
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - average, 2.0))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private Double coefficientOfVariation(List<Double> values) {
        if (values == null || values.size() < 2) {
            return null;
        }
        Double average = average(values);
        if (average == null || average <= 0.0) {
            return null;
        }
        return standardDeviation(values) / average;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private int clampInt(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Double roundNullable(Double value) {
        return value == null ? null : round(value);
    }
}
