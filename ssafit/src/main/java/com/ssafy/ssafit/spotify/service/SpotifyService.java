package com.ssafy.ssafit.spotify.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.SpotifyTrackMetadata;
import com.ssafy.ssafit.music.service.MusicService;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.domain.SpotifyTrack;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int MAX_SEARCH_LIMIT = 10;

    private final SpotifyApiClient spotifyApiClient;
    private final MusicService musicService;

    public List<SpotifyTrackMetadata> searchTracks(String query, Integer limit) {
        if (!StringUtils.hasText(query)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        int normalizedLimit = normalizeLimit(limit);
        List<SpotifyTrack> tracks;
        try {
            String clientAccessToken = spotifyApiClient.issueClientAccessToken();
            if (!StringUtils.hasText(clientAccessToken)) {
                throw new CustomException(ErrorCode.SPOTIFY_SEARCH_FAILED);
            }
            tracks = spotifyApiClient.searchTracks(
                    clientAccessToken,
                    query.trim(),
                    normalizedLimit);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.SPOTIFY_SEARCH_FAILED);
        }
        if (tracks == null || tracks.isEmpty()) {
            return List.of();
        }
        return tracks.stream()
                .filter(this::hasSpotifyIdentity)
                .map(this::toMetadata)
                .toList();
    }

    public Music saveSelectedTrack(SpotifyTrackMetadata metadata) {
        return musicService.saveSelectedSpotifyTrack(metadata);
    }

    private SpotifyTrackMetadata toMetadata(SpotifyTrack track) {
        if (!hasSpotifyIdentity(track)) {
            return null;
        }
        return new SpotifyTrackMetadata(
                track.getId().trim(),
                track.getUri().trim(),
                trimToNull(track.getExternalUrl()),
                trimToNull(track.getTitle()),
                trimToNull(track.getArtist()),
                trimToNull(track.getAlbumName()),
                trimToNull(track.getImageUrl()),
                null,
                null);
    }

    private boolean hasSpotifyIdentity(SpotifyTrack track) {
        return track != null
                && StringUtils.hasText(track.getId())
                && StringUtils.hasText(track.getUri());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SEARCH_LIMIT;
        }
        if (limit < 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return Math.min(limit, MAX_SEARCH_LIMIT);
    }
}
