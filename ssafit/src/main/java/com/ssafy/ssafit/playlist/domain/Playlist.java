package com.ssafy.ssafit.playlist.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Playlist {

    private Long id;
    private Long ownerId;
    private PlaylistType playlistType;
    private PlaylistSourceType sourceType;
    private PlaylistVisibility visibility;
    private String title;
    private String description;
    private String spotifyPlaylistId;
    private String spotifyPlaylistUrl;
    private SpotifyExportStatus spotifyExportStatus;
    private String spotifyExportErrorCode;
    private LocalDateTime spotifyExportedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
