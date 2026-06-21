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
            Integer bpm,
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
                    track.getBpm(),
                    track.getGenre());
        }
    }

    public record AuthorizationResponse(String authorizationUrl) {
    }

    public record ConnectionResponse(
            boolean connected,
            String spotifyUserId,
            LocalDateTime tokenExpiresAt) {

        public static ConnectionResponse connected(SpotifyConnection connection) {
            return new ConnectionResponse(
                    true,
                    connection.getSpotifyUserId(),
                    connection.getTokenExpiresAt());
        }

        public static ConnectionResponse disconnected() {
            return new ConnectionResponse(false, null, null);
        }
    }
}
