package com.ssafy.ssafit.runningrecord.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ssafy.ssafit.runningrecord.domain.RunningRecord;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
            @Size(max = 1000) String memo) {

        public RunningRecord toDomain() {
            return RunningRecordApiDtos.toDomain(
                    null,
                    recordDate,
                    distanceKm,
                    durationSeconds,
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
            @Size(max = 1000) String memo) {

        public RunningRecord toDomain(Long runningRecordId) {
            return RunningRecordApiDtos.toDomain(
                    runningRecordId,
                    recordDate,
                    distanceKm,
                    durationSeconds,
                    memo);
        }
    }

    public record Response(
            Long id,
            LocalDate recordDate,
            BigDecimal distanceKm,
            Integer durationSeconds,
            Integer paceSecondsPerKm,
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
            String memo) {
        RunningRecord runningRecord = new RunningRecord();
        runningRecord.setId(id);
        runningRecord.setRecordDate(recordDate);
        runningRecord.setDistanceKm(distanceKm);
        runningRecord.setDurationSeconds(durationSeconds);
        runningRecord.setMemo(memo);
        return runningRecord;
    }
}
