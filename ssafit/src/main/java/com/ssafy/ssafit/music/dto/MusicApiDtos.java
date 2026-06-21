package com.ssafy.ssafit.music.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.ssafit.common.response.PageRequest;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.domain.MusicSortType;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.domain.SpotifyTrackMetadata;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.musicreview.dto.MusicReviewApiDtos.AverageResponse;
import com.ssafy.ssafit.musicreview.dto.MusicReviewApiDtos.ReviewResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MusicApiDtos {

    private MusicApiDtos() {
    }

    public record SearchRequest(
            String keyword,
            String genre,
            Integer minBpm,
            Integer maxBpm,
            RunningIntensity runningIntensity,
            MusicSortType sortType,
            Integer page,
            Integer size) {

        public MusicSearchCondition toCondition() {
            PageRequest paging = new PageRequest(page, size);
            MusicSearchCondition condition = new MusicSearchCondition();
            condition.setKeyword(keyword);
            condition.setGenre(genre);
            condition.setMinBpm(minBpm);
            condition.setMaxBpm(maxBpm);
            condition.setRunningIntensity(runningIntensity);
            condition.setSortType(sortType);
            condition.setPage(paging.page());
            condition.setSize(paging.size());
            condition.setOffset(paging.offset());
            return condition;
        }
    }

    public record SpotifyImportRequest(
            @NotBlank String spotifyTrackId,
            @NotBlank String spotifyUri,
            String spotifyExternalUrl,
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 255) String artist,
            @Size(max = 255) String albumName,
            @Size(max = 500) String imageUrl,
            @Min(60) @Max(220) Integer bpm,
            @Size(max = 100) String genre) {

        public SpotifyTrackMetadata toMetadata() {
            return new SpotifyTrackMetadata(
                    spotifyTrackId,
                    spotifyUri,
                    spotifyExternalUrl,
                    title,
                    artist,
                    albumName,
                    imageUrl,
                    bpm,
                    genre);
        }
    }

    public record MusicResponse(
            Long id,
            String title,
            String artist,
            String albumName,
            String imageUrl,
            String genre,
            Integer bpm,
            MusicSourceType sourceType,
            String spotifyTrackId,
            String spotifyUri,
            String spotifyExternalUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static MusicResponse from(Music music) {
            return new MusicResponse(
                    music.getId(),
                    music.getTitle(),
                    music.getArtist(),
                    music.getAlbumName(),
                    music.getImageUrl(),
                    music.getGenre(),
                    music.getBpm(),
                    music.getSourceType(),
                    music.getSpotifyTrackId(),
                    music.getSpotifyUri(),
                    music.getSpotifyExternalUrl(),
                    music.getCreatedAt(),
                    music.getUpdatedAt());
        }
    }

    public record DetailResponse(
            MusicResponse music,
            AverageResponse reviewAverage,
            List<ReviewResponse> reviews) {

        public DetailResponse {
            reviews = reviews == null ? List.of() : List.copyOf(reviews);
        }
    }
}
