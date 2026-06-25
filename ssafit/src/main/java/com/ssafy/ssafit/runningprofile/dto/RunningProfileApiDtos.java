package com.ssafy.ssafit.runningprofile.dto;

import java.time.LocalDateTime;

import com.ssafy.ssafit.runningprofile.domain.RunningFitnessMetric;
import com.ssafy.ssafit.runningprofile.domain.RunningGender;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class RunningProfileApiDtos {

    private RunningProfileApiDtos() {
    }

    public record SaveRequest(
            RunningGender gender,
            @Min(10) @Max(100) Integer age,
            @Size(max = 100) String preferredGenre) {

        public RunningProfile toDomain(Long memberId) {
            RunningProfile profile = new RunningProfile();
            profile.setMemberId(memberId);
            profile.setGender(gender);
            profile.setAge(age);
            profile.setPreferredGenre(preferredGenre);
            return profile;
        }
    }

    public record Response(
            Long id,
            Long memberId,
            RunningGender gender,
            Integer age,
            Integer estimatedMaxHeartRateBpm,
            Integer targetBpmMin,
            Integer targetBpmMax,
            Integer targetCadenceMin,
            Integer targetCadenceMax,
            String preferredGenre,
            RunningFitnessMetricResponse runningFitnessMetric,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static Response from(RunningProfile profile, RunningFitnessMetric metric) {
            return new Response(
                    profile.getId(),
                    profile.getMemberId(),
                    profile.getGender(),
                    profile.getAge(),
                    profile.getEstimatedMaxHeartRateBpm(),
                    profile.getTargetBpmMin(),
                    profile.getTargetBpmMax(),
                    profile.getTargetCadenceMin(),
                    profile.getTargetCadenceMax(),
                    profile.getPreferredGenre(),
                    RunningFitnessMetricResponse.from(metric),
                    profile.getCreatedAt(),
                    profile.getUpdatedAt());
        }
    }

    public record RunningFitnessMetricResponse(
            Integer score,
            int recordCount,
            Double totalDistanceKm,
            Integer averagePaceSecondsPerKm,
            Double averageRecentDistanceKm,
            Double heartRateCost,
            Double cadenceStandardDeviation,
            Double paceCoefficientOfVariation,
            Double distanceCoefficientOfVariation,
            Double averageDistanceScore,
            Double paceEfficiencyScore,
            Double cadenceStabilityScore,
            Double variationScore) {

        public static RunningFitnessMetricResponse from(RunningFitnessMetric metric) {
            RunningFitnessMetric resolved = metric == null
                    ? RunningFitnessMetric.empty()
                    : metric;
            return new RunningFitnessMetricResponse(
                    resolved.score(),
                    resolved.recordCount(),
                    resolved.totalDistanceKm(),
                    resolved.averagePaceSecondsPerKm(),
                    resolved.averageRecentDistanceKm(),
                    resolved.heartRateCost(),
                    resolved.cadenceStandardDeviation(),
                    resolved.paceCoefficientOfVariation(),
                    resolved.distanceCoefficientOfVariation(),
                    resolved.averageDistanceScore(),
                    resolved.paceEfficiencyScore(),
                    resolved.cadenceStabilityScore(),
                    resolved.variationScore());
        }
    }
}
