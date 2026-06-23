package com.ssafy.ssafit.spotify.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;

public record SpotifyExportResult(
        Long playlistId,
        int requestedCount,
        int exportedCount,
        int skippedCount,
        List<Long> skippedMusicIds,
        List<Integer> failedChunkIndexes,
        String spotifyPlaylistId,
        String spotifyPlaylistUrl,
        SpotifyExportStatus spotifyExportStatus,
        String spotifyExportErrorCode,
        LocalDateTime exportedAt
) {
    public SpotifyExportResult {
        skippedMusicIds = skippedMusicIds == null
                ? List.of()
                : List.copyOf(skippedMusicIds);
        failedChunkIndexes = failedChunkIndexes == null
                ? List.of()
                : List.copyOf(failedChunkIndexes);
    }
}
