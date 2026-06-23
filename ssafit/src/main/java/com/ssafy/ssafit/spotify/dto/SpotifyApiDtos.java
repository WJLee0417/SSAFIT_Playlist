package com.ssafy.ssafit.spotify.dto;

import java.time.LocalDateTime;

import com.ssafy.ssafit.music.domain.SpotifyTrackMetadata;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;

public final class SpotifyApiDtos {

    private SpotifyApiDtos() {
    }

    public record TrackResponse(
            String spotifyTrackId,
            String spotifyUri,
            String spotifyExternalUrl,
            String title,
            String artist,
            String albumName,
            String imageUrl,
            Integer durationMs,
            Integer musicBpm,
            String genre) {

        public static TrackResponse from(SpotifyTrackMetadata track) {
            return new TrackResponse(
                    track.getSpotifyTrackId(),
                    track.getSpotifyUri(),
                    track.getSpotifyExternalUrl(),
                    track.getTitle(),
                    track.getArtist(),
                    track.getAlbumName(),
                    track.getImageUrl(),
                    track.getDurationMs(),
                    track.getMusicBpm(),
                    track.getGenre());
        }
    }

    public enum ConnectionState {
        CONNECTED,
        DISCONNECTED,
        RECONNECT_REQUIRED
    }

    public record ConnectionResponse(
            boolean connected,
            ConnectionState state,
            String spotifyUserId,
            LocalDateTime tokenExpiresAt) {

        public static ConnectionResponse connected(SpotifyConnection connection) {
            return new ConnectionResponse(
                    true,
                    ConnectionState.CONNECTED,
                    connection.getSpotifyUserId(),
                    connection.getTokenExpiresAt());
        }

        public static ConnectionResponse disconnected() {
            return new ConnectionResponse(
                    false,
                    ConnectionState.DISCONNECTED,
                    null,
                    null);
        }

        public static ConnectionResponse reconnectRequired(
                SpotifyConnection connection) {
            return new ConnectionResponse(
                    false,
                    ConnectionState.RECONNECT_REQUIRED,
                    connection == null ? null : connection.getSpotifyUserId(),
                    connection == null ? null : connection.getTokenExpiresAt());
        }
    }
}
