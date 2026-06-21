package com.ssafy.ssafit.recommendation.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.ssafit.music.dto.MusicApiDtos.MusicResponse;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.PlaylistResponse;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.domain.RecommendationResult;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RecommendationApiDtos {

    private RecommendationApiDtos() {
    }

    public record RecommendRequest(
            @NotNull RunningIntensity intensity,
            @Size(max = 100) String genre,
            @Min(1) @Max(50) Integer size) {
    }

    public record SaveRequest(
            @Size(max = 200) String title,
            PlaylistVisibility visibility) {
    }

    public record LogResponse(
            Long id,
            Long memberId,
            RunningIntensity requestedIntensity,
            Integer requestedBpmMin,
            Integer requestedBpmMax,
            String requestedGenre,
            String requestConditions,
            String resultSummary,
            String recommendationReason,
            Long savedPlaylistId,
            LocalDateTime createdAt) {

        public static LogResponse from(RecommendationLog log) {
            return new LogResponse(
                    log.getId(),
                    log.getMemberId(),
                    log.getRequestedIntensity(),
                    log.getRequestedBpmMin(),
                    log.getRequestedBpmMax(),
                    log.getRequestedGenre(),
                    log.getRequestConditions(),
                    log.getResultSummary(),
                    log.getRecommendationReason(),
                    log.getSavedPlaylistId(),
                    log.getCreatedAt());
        }
    }

    public record ResultResponse(
            LogResponse log,
            List<MusicResponse> music) {

        public ResultResponse {
            music = music == null ? List.of() : List.copyOf(music);
        }

        public static ResultResponse from(RecommendationResult result) {
            return new ResultResponse(
                    LogResponse.from(result.getRecommendationLog()),
                    result.getMusicList().stream()
                            .map(MusicResponse::from)
                            .toList());
        }
    }

    public record SaveResponse(
            PlaylistResponse playlist) {
    }
}
