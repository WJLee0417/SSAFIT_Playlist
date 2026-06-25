package com.ssafy.ssafit.runningrecord.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ssafy.ssafit.runningrecord.domain.RunningRecord;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class RunningRecordApiDtos {

    private RunningRecordApiDtos() {
    }

    public record CreateRequest(
            @NotNull LocalDate recordDate,
            @NotNull
            @DecimalMin(value = "0.0", inclusive = false)
            @Digits(integer = 8, fraction = 2)
            BigDecimal distanceKm,
            @NotNull @Positive Integer durationSeconds,
            @Min(60) @Max(240) Integer averageCadence,
            @Min(30) @Max(240) Integer averageHeartRateBpm,
            @Size(max = 1000) String memo) {

        public RunningRecord toDomain() {
            return RunningRecordApiDtos.toDomain(
                    null,
                    recordDate,
                    distanceKm,
                    durationSeconds,
                    averageCadence,
                    averageHeartRateBpm,
                    memo);
        }
    }

    public record UpdateRequest(
            @NotNull LocalDate recordDate,
            @NotNull
            @DecimalMin(value = "0.0", inclusive = false)
            @Digits(integer = 8, fraction = 2)
            BigDecimal distanceKm,
            @NotNull @Positive Integer durationSeconds,
            @Min(60) @Max(240) Integer averageCadence,
            @Min(30) @Max(240) Integer averageHeartRateBpm,
            @Size(max = 1000) String memo) {

        public RunningRecord toDomain(Long runningRecordId) {
            return RunningRecordApiDtos.toDomain(
                    runningRecordId,
                    recordDate,
                    distanceKm,
                    durationSeconds,
                    averageCadence,
                    averageHeartRateBpm,
                    memo);
        }
    }

    public record Response(
            Long id,
            LocalDate recordDate,
            BigDecimal distanceKm,
            Integer durationSeconds,
            Integer paceSecondsPerKm,
            Integer averageCadence,
            Integer averageHeartRateBpm,
            String memo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static Response from(RunningRecord runningRecord) {
            return new Response(
                    runningRecord.getId(),
                    runningRecord.getRecordDate(),
                    runningRecord.getDistanceKm(),
                    runningRecord.getDurationSeconds(),
                    runningRecord.getPaceSecondsPerKm(),
                    runningRecord.getAverageCadence(),
                    runningRecord.getAverageHeartRateBpm(),
                    runningRecord.getMemo(),
                    runningRecord.getCreatedAt(),
                    runningRecord.getUpdatedAt());
        }
    }

    private static RunningRecord toDomain(
            Long id,
            LocalDate recordDate,
            BigDecimal distanceKm,
            Integer durationSeconds,
            Integer averageCadence,
            Integer averageHeartRateBpm,
            String memo) {
        RunningRecord runningRecord = new RunningRecord();
        runningRecord.setId(id);
        runningRecord.setRecordDate(recordDate);
        runningRecord.setDistanceKm(distanceKm);
        runningRecord.setDurationSeconds(durationSeconds);
        runningRecord.setAverageCadence(averageCadence);
        runningRecord.setAverageHeartRateBpm(averageHeartRateBpm);
        runningRecord.setMemo(memo);
        return runningRecord;
    }
}
