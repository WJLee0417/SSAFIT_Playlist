package com.ssafy.ssafit.musicreview.dto;

import java.time.LocalDateTime;

import com.ssafy.ssafit.musicreview.domain.MusicReview;
import com.ssafy.ssafit.musicreview.domain.MusicReviewAverage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class MusicReviewApiDtos {

    private MusicReviewApiDtos() {
    }

    public record CreateRequest(
            @NotNull @Min(1) @Max(5) Integer preferenceScore,
            @NotNull @Min(1) @Max(5) Integer runningFitScore,
            @Size(max = 2000) String reviewContent) {

        public MusicReview toDomain(Long musicId) {
            MusicReview review = new MusicReview();
            review.setMusicId(musicId);
            review.setPreferenceScore(preferenceScore);
            review.setRunningFitScore(runningFitScore);
            review.setReviewContent(reviewContent);
            return review;
        }
    }

    public record UpdateRequest(
            @NotNull @Min(1) @Max(5) Integer preferenceScore,
            @NotNull @Min(1) @Max(5) Integer runningFitScore,
            @Size(max = 2000) String reviewContent) {

        public MusicReview toDomain(Long reviewId) {
            MusicReview review = new MusicReview();
            review.setId(reviewId);
            review.setPreferenceScore(preferenceScore);
            review.setRunningFitScore(runningFitScore);
            review.setReviewContent(reviewContent);
            return review;
        }
    }

    public record ReviewResponse(
            Long id,
            Long memberId,
            String authorLoginId,
            Long musicId,
            Integer preferenceScore,
            Integer runningFitScore,
            String reviewContent,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static ReviewResponse from(MusicReview review) {
            return new ReviewResponse(
                    review.getId(),
                    review.getMemberId(),
                    review.getAuthorLoginId(),
                    review.getMusicId(),
                    review.getPreferenceScore(),
                    review.getRunningFitScore(),
                    review.getReviewContent(),
                    review.getCreatedAt(),
                    review.getUpdatedAt());
        }
    }

    public record AverageResponse(
            Long musicId,
            Double averagePreferenceScore,
            Double averageRunningFitScore,
            Long reviewCount) {

        public static AverageResponse from(MusicReviewAverage average) {
            return new AverageResponse(
                    average.getMusicId(),
                    average.getAveragePreferenceScore(),
                    average.getAverageRunningFitScore(),
                    average.getReviewCount());
        }
    }
}
