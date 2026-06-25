package com.ssafy.ssafit.runningprofile.domain;

public record RunningFitnessMetric(
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

    public static RunningFitnessMetric empty() {
        return new RunningFitnessMetric(
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
