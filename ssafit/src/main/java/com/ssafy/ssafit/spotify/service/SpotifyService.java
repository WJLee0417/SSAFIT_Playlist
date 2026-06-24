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
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyService {

    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int MAX_SEARCH_LIMIT = 10;
    private static final int MAX_SEARCH_OFFSET = 1_000;

    private final SpotifyApiClient spotifyApiClient;
    private final MusicService musicService;

    @PostConstruct
    void logInjectedSpotifyClient() {
        log.info(
                "Injected SpotifyApiClient implementation={}",
                spotifyApiClient.getClass().getName());
    }

    public List<SpotifyTrackMetadata> searchTracks(String query, Integer limit) {
        return searchTracks(query, 0, limit);
    }

    public List<SpotifyTrackMetadata> searchTracks(
            String query,
            Integer offset,
            Integer limit) {
        if (!StringUtils.hasText(query)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        int normalizedOffset = normalizeOffset(offset);
        int normalizedLimit = normalizeLimit(limit);
        List<SpotifyTrack> tracks;
        String clientAccessToken;
        try {
            clientAccessToken = spotifyApiClient.issueClientAccessToken();
            if (!StringUtils.hasText(clientAccessToken)) {
                throw new CustomException(ErrorCode.SPOTIFY_SEARCH_FAILED);
            }
            tracks = normalizedOffset == 0
                    ? spotifyApiClient.searchTracks(
                            clientAccessToken,
                            query.trim(),
                            normalizedLimit)
                    : spotifyApiClient.searchTracks(
                            clientAccessToken,
                            query.trim(),
                            normalizedOffset,
                            normalizedLimit);
        } catch (CustomException exception) {
            log.error(
                    "Spotify track search failed with a classified error. "
                            + "errorCode={}, queryLength={}, offset={}, limit={}",
                    exception.getErrorCode(),
                    query.trim().length(),
                    normalizedOffset,
                    normalizedLimit);
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error during Spotify track search. "
                            + "queryLength={}, offset={}, limit={}, "
                            + "exceptionType={}",
                    query.trim().length(),
                    normalizedOffset,
                    normalizedLimit,
                    exception.getClass().getSimpleName(),
                    exception);
            throw new CustomException(ErrorCode.SPOTIFY_SEARCH_FAILED);
        }
        if (tracks == null || tracks.isEmpty()) {
            return List.of();
        }
        return tracks.stream()
                .filter(this::hasSpotifyIdentity)
                .map(track -> toMetadata(clientAccessToken, track))
                .toList();
    }

    public Music saveSelectedTrack(SpotifyTrackMetadata metadata) {
        return musicService.saveSelectedSpotifyTrack(metadata);
    }

    public Music saveSpotifyTrack(String accessToken, SpotifyTrack track) {
        return musicService.saveSelectedSpotifyTrack(toMetadata(accessToken, track));
    }

    private SpotifyTrackMetadata toMetadata(
            String accessToken,
            SpotifyTrack track) {
        if (!hasSpotifyIdentity(track)) {
            return null;
        }
        Integer tempoBpm = resolveTempoBpm(accessToken, track);
        String genre = resolveGenre(accessToken, track);
        return new SpotifyTrackMetadata(
                track.getId().trim(),
                track.getUri().trim(),
                trimToNull(track.getExternalUrl()),
                trimToNull(track.getTitle()),
                trimToNull(track.getArtist()),
                trimToNull(track.getAlbumName()),
                trimToNull(track.getImageUrl()),
                track.getDurationMs(),
                tempoBpm,
                null,
                genre);
    }

    private Integer resolveTempoBpm(String accessToken, SpotifyTrack track) {
        try {
            return spotifyApiClient.getTrackTempoBpm(accessToken, track.getId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Spotify tempo metadata is unavailable. trackId={}, exceptionType={}",
                    track.getId(),
                    exception.getClass().getSimpleName());
            return null;
        }
    }

    private String resolveGenre(String accessToken, SpotifyTrack track) {
        if (!StringUtils.hasText(track.getArtistId())) {
            return null;
        }
        try {
            List<String> genres = spotifyApiClient.getArtistGenres(
                    accessToken,
                    track.getArtistId());
            return genres == null || genres.isEmpty()
                    ? null
                    : trimToNull(genres.getFirst());
        } catch (RuntimeException exception) {
            log.warn(
                    "Spotify genre metadata is unavailable. artistId={}, exceptionType={}",
                    track.getArtistId(),
                    exception.getClass().getSimpleName());
            return null;
        }
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

    private int normalizeOffset(Integer offset) {
        int normalized = offset == null ? 0 : offset;
        if (normalized < 0 || normalized > MAX_SEARCH_OFFSET) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }
}
