package com.ssafy.ssafit.recommendation.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.ssafit.music.domain.MusicEnergyLevel;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.PlaylistResponse;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.domain.RecommendedMusic;
import com.ssafy.ssafit.recommendation.domain.RecommendationResult;
import com.ssafy.ssafit.recommendation.domain.IntensitySource;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.recommendation.domain.TempoMatchMode;
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
            @Min(60) @Max(240) Integer targetCadenceSpm,
            RunningIntensityGoal runningIntensityGoal,
            @Size(max = 100) String genre,
            @Min(1) @Max(50) Integer size,
            @Min(1) @Max(10) Integer limit,
            @Min(1) @Max(5) Integer page) {
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
            List<RecommendedMusicResponse> music,
            Integer resolvedTargetCadenceSpm,
            String cadenceExplanation,
            RunningIntensityGoal resolvedIntensityGoal,
            IntensitySource intensitySource,
            Double averageHeartRateBpm,
            String intensityExplanation,
            Integer page,
            Integer pageSize,
            boolean hasPrevious,
            boolean hasNext,
            boolean cadenceApplied,
            boolean intensityApplied) {

        public ResultResponse {
            music = music == null ? List.of() : List.copyOf(music);
        }

        public static ResultResponse from(RecommendationResult result) {
            return new ResultResponse(
                    LogResponse.from(result.getRecommendationLog()),
                    result.getMusicList().stream()
                            .map(RecommendedMusicResponse::from)
                            .toList(),
                    result.getResolvedTargetCadenceSpm(),
                    result.getCadenceExplanation(),
                    result.getResolvedIntensityGoal(),
                    result.getIntensitySource(),
                    result.getAverageHeartRateBpm(),
                    result.getIntensityExplanation(),
                    result.getPage(),
                    result.getPageSize(),
                    result.isHasPrevious(),
                    result.isHasNext(),
                    result.isCadenceApplied(),
                    result.isIntensityApplied());
        }
    }

    public record RecommendedMusicResponse(
            Long id,
            String title,
            String artist,
            String albumName,
            String imageUrl,
            String genre,
            Integer musicBpm,
            Integer durationMs,
            MusicEnergyLevel musicEnergyLevel,
            MusicSourceType sourceType,
            String spotifyTrackId,
            String spotifyUri,
            String spotifyExternalUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Double cadenceScore,
            Double intensityScore,
            Double preferenceScore,
            Double finalScore,
            TempoMatchMode tempoMatchMode,
            String recommendationReason) {

        public static RecommendedMusicResponse from(RecommendedMusic result) {
            return new RecommendedMusicResponse(
                    result.music().getId(),
                    result.music().getTitle(),
                    result.music().getArtist(),
                    result.music().getAlbumName(),
                    result.music().getImageUrl(),
                    result.music().getGenre(),
                    result.music().getMusicBpm(),
                    result.music().getDurationMs(),
                    result.music().getMusicEnergyLevel(),
                    result.music().getSourceType(),
                    result.music().getSpotifyTrackId(),
                    result.music().getSpotifyUri(),
                    result.music().getSpotifyExternalUrl(),
                    result.music().getCreatedAt(),
                    result.music().getUpdatedAt(),
                    result.cadenceScore(),
                    result.intensityScore(),
                    result.preferenceScore(),
                    result.finalScore(),
                    result.tempoMatchMode(),
                    result.recommendationReason());
        }
    }

    public record SaveResponse(
            PlaylistResponse playlist) {
    }
}
