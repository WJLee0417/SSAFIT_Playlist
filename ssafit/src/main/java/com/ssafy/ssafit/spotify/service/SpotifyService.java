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
        String clientAccessToken = spotifyApiClient.issueClientAccessToken();
        List<SpotifyTrack> tracks = spotifyApiClient.searchTracks(
                clientAccessToken,
                query.trim(),
                normalizedLimit);
        if (tracks == null || tracks.isEmpty()) {
            throw new CustomException(ErrorCode.SPOTIFY_TRACK_NOT_FOUND);
        }
        return tracks.stream()
                .map(this::toMetadata)
                .toList();
    }

    public Music saveSelectedTrack(SpotifyTrackMetadata metadata) {
        return musicService.saveSelectedSpotifyTrack(metadata);
    }

    private SpotifyTrackMetadata toMetadata(SpotifyTrack track) {
        if (track == null
                || !StringUtils.hasText(track.getId())
                || !StringUtils.hasText(track.getUri())
                || !StringUtils.hasText(track.getTitle())
                || !StringUtils.hasText(track.getArtist())) {
            throw new CustomException(ErrorCode.SPOTIFY_API_ERROR);
        }
        return new SpotifyTrackMetadata(
                track.getId(),
                track.getUri(),
                track.getExternalUrl(),
                track.getTitle(),
                track.getArtist(),
                track.getAlbumName(),
                track.getImageUrl(),
                null,
                null);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SEARCH_LIMIT;
        }
        if (limit < 1 || limit > MAX_SEARCH_LIMIT) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return limit;
    }
}
