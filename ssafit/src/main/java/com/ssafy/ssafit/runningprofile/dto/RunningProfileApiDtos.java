package com.ssafy.ssafit.runningprofile.dto;

import java.time.LocalDateTime;

import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RunningProfileApiDtos {

    private RunningProfileApiDtos() {
    }

    public record SaveRequest(
            @NotNull RunningIntensity runningIntensity,
            @NotNull @Min(60) @Max(220) Integer targetBpmMin,
            @NotNull @Min(60) @Max(220) Integer targetBpmMax,
            @Size(max = 100) String preferredGenre) {

        public RunningProfile toDomain(Long memberId) {
            RunningProfile profile = new RunningProfile();
            profile.setMemberId(memberId);
            profile.setRunningIntensity(runningIntensity);
            profile.setTargetBpmMin(targetBpmMin);
            profile.setTargetBpmMax(targetBpmMax);
            profile.setPreferredGenre(preferredGenre);
            return profile;
        }
    }

    public record Response(
            Long id,
            Long memberId,
            RunningIntensity runningIntensity,
            Integer targetBpmMin,
            Integer targetBpmMax,
            String preferredGenre,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static Response from(RunningProfile profile) {
            return new Response(
                    profile.getId(),
                    profile.getMemberId(),
                    profile.getRunningIntensity(),
                    profile.getTargetBpmMin(),
                    profile.getTargetBpmMax(),
                    profile.getPreferredGenre(),
                    profile.getCreatedAt(),
                    profile.getUpdatedAt());
        }
    }
}
