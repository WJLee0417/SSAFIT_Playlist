package com.ssafy.ssafit.playlist.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.ssafit.music.dto.MusicApiDtos.MusicResponse;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistSave;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PlaylistApiDtos {

    private PlaylistApiDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            PlaylistVisibility visibility) {

        public Playlist toDomain() {
            Playlist playlist = new Playlist();
            playlist.setTitle(title);
            playlist.setDescription(description);
            playlist.setVisibility(visibility);
            return playlist;
        }
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            PlaylistVisibility visibility) {

        public Playlist toDomain(Long playlistId) {
            Playlist playlist = new Playlist();
            playlist.setId(playlistId);
            playlist.setTitle(title);
            playlist.setDescription(description);
            playlist.setVisibility(visibility);
            return playlist;
        }
    }

    public record ItemCreateRequest(
            @NotNull Long musicId,
            @NotNull @Min(1) Integer orderNo) {

        public PlaylistItem toDomain(Long playlistId) {
            PlaylistItem item = new PlaylistItem();
            item.setPlaylistId(playlistId);
            item.setMusicId(musicId);
            item.setOrderNo(orderNo);
            return item;
        }
    }

    public record ItemOrderUpdateRequest(
            @NotNull @Min(1) Integer orderNo) {
    }

    public record PlaylistResponse(
            Long id,
            Long ownerId,
            PlaylistType playlistType,
            PlaylistSourceType sourceType,
            PlaylistVisibility visibility,
            String title,
            String description,
            String spotifyPlaylistId,
            String spotifyPlaylistUrl,
            SpotifyExportStatus spotifyExportStatus,
            String spotifyExportErrorCode,
            LocalDateTime spotifyExportedAt,
            boolean saved,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static PlaylistResponse from(Playlist playlist) {
            return new PlaylistResponse(
                    playlist.getId(),
                    playlist.getOwnerId(),
                    playlist.getPlaylistType(),
                    playlist.getSourceType(),
                    playlist.getVisibility(),
                    playlist.getTitle(),
                    playlist.getDescription(),
                    playlist.getSpotifyPlaylistId(),
                    playlist.getSpotifyPlaylistUrl(),
                    playlist.getSpotifyExportStatus(),
                    playlist.getSpotifyExportErrorCode(),
                    playlist.getSpotifyExportedAt(),
                    playlist.isSaved(),
                    playlist.getCreatedAt(),
                    playlist.getUpdatedAt());
        }
    }

    public record ItemResponse(
            Long id,
            Long playlistId,
            Long musicId,
            Integer orderNo,
            MusicResponse music,
            LocalDateTime createdAt) {

        public static ItemResponse from(PlaylistItem item, MusicResponse music) {
            return new ItemResponse(
                    item.getId(),
                    item.getPlaylistId(),
                    item.getMusicId(),
                    item.getOrderNo(),
                    music,
                    item.getCreatedAt());
        }
    }

    public record DetailResponse(
            PlaylistResponse playlist,
            List<ItemResponse> items) {

        public DetailResponse {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record SaveResponse(
            Long id,
            Long memberId,
            Long playlistId,
            boolean saved,
            LocalDateTime createdAt) {

        public static SaveResponse from(PlaylistSave save) {
            return new SaveResponse(
                    save.getId(),
                    save.getMemberId(),
                    save.getPlaylistId(),
                    true,
                    save.getCreatedAt());
        }
    }

    public record ExportResponse(
            Long playlistId,
            String spotifyPlaylistId,
            String spotifyPlaylistUrl,
            SpotifyExportStatus spotifyExportStatus,
            String spotifyExportErrorCode,
            LocalDateTime spotifyExportedAt) {

        public static ExportResponse from(Playlist playlist) {
            return new ExportResponse(
                    playlist.getId(),
                    playlist.getSpotifyPlaylistId(),
                    playlist.getSpotifyPlaylistUrl(),
                    playlist.getSpotifyExportStatus(),
                    playlist.getSpotifyExportErrorCode(),
                    playlist.getSpotifyExportedAt());
        }
    }
}
