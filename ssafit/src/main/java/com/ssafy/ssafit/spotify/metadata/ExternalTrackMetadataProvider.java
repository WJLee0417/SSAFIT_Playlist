package com.ssafy.ssafit.spotify.metadata;

import java.net.URI;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.ssafy.ssafit.spotify.domain.SpotifyTrack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalTrackMetadataProvider implements TrackMetadataProvider {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final int MIN_SUPPORTED_BPM = 60;
    private static final int MAX_SUPPORTED_BPM = 220;
    private static final Set<String> NON_GENRE_TAGS = Set.of(
            "seen live",
            "favorite",
            "favorites",
            "favourite",
            "favourites",
            "spotify",
            "cover",
            "covers",
            "remix",
            "single",
            "albums i own",
            "songs i love");

    private final RestClient.Builder restClientBuilder;
    private final TrackMetadataEnrichmentProperties properties;

    @Override
    public TrackMetadata resolve(SpotifyTrack track) {
        if (!properties.isEnabled()
                || track == null
                || !StringUtils.hasText(track.getTitle())
                || !StringUtils.hasText(track.getArtist())) {
            return TrackMetadata.empty();
        }

        TrackMetadata getsong = resolveGetsong(track);
        if (StringUtils.hasText(getsong.genre())) {
            return getsong;
        }
        TrackMetadata lastfm = resolveLastfm(track);
        return new TrackMetadata(
                getsong.musicBpm(),
                firstNonBlank(lastfm.genre(), getsong.genre()));
    }

    private TrackMetadata resolveLastfm(SpotifyTrack track) {
        if (!StringUtils.hasText(properties.getLastfm().getApiKey())) {
            return TrackMetadata.empty();
        }
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(properties.getLastfm().getApiBaseUrl())
                    .queryParam("method", "track.gettoptags")
                    .queryParam("artist", track.getArtist())
                    .queryParam("track", track.getTitle())
                    .queryParam("api_key", properties.getLastfm().getApiKey())
                    .queryParam("format", "json")
                    .queryParam("autocorrect", "1")
                    .build()
                    .encode()
                    .toUri();
            Map<String, Object> response = get(uri);
            List<Object> tags = list(map(response.get("toptags")).get("tag"));
            String genre = tags.stream()
                    .map(this::tagName)
                    .map(this::cleanGenre)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
            return new TrackMetadata(null, genre);
        } catch (RuntimeException exception) {
            log.warn(
                    "Last.fm metadata is unavailable. artist={}, title={}, exceptionType={}",
                    safeLogValue(track.getArtist()),
                    safeLogValue(track.getTitle()),
                    exception.getClass().getSimpleName());
            return TrackMetadata.empty();
        }
    }

    private TrackMetadata resolveGetsong(SpotifyTrack track) {
        if (!StringUtils.hasText(properties.getGetsong().getApiKey())) {
            return TrackMetadata.empty();
        }
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(properties.getGetsong().getApiBaseUrl())
                    .path("/search/")
                    .queryParam("api_key", properties.getGetsong().getApiKey())
                    .queryParam("type", "both")
                    .queryParam("lookup", "song:" + track.getTitle()
                            + " artist:" + track.getArtist())
                    .queryParam("limit", 5)
                    .build()
                    .encode()
                    .toUri();
            Map<String, Object> response = get(uri);
            Map<String, Object> candidate = bestGetsongCandidate(
                    list(response.get("search")),
                    track);
            if (candidate.isEmpty()) {
                return TrackMetadata.empty();
            }
            return new TrackMetadata(
                    supportedBpm(candidate.get("tempo")),
                    firstGenre(candidate.get("artist")));
        } catch (RuntimeException exception) {
            log.warn(
                    "GetSongBPM metadata is unavailable. artist={}, title={}, exceptionType={}",
                    safeLogValue(track.getArtist()),
                    safeLogValue(track.getTitle()),
                    exception.getClass().getSimpleName());
            return TrackMetadata.empty();
        }
    }

    private Map<String, Object> bestGetsongCandidate(
            List<Object> candidates,
            SpotifyTrack track) {
        String targetTitle = normalized(track.getTitle());
        String targetArtist = normalized(track.getArtist());
        Map<String, Object> best = Map.of();
        int bestScore = 0;

        for (Object value : candidates) {
            Map<String, Object> candidate = map(value);
            int score = matchScore(
                    targetTitle,
                    targetArtist,
                    normalized(string(candidate.get("title"))),
                    normalized(artistName(candidate.get("artist"))));
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return bestScore >= 3 ? best : Map.of();
    }

    private int matchScore(
            String targetTitle,
            String targetArtist,
            String candidateTitle,
            String candidateArtist) {
        int titleScore = 0;
        int artistScore = 0;
        if (!targetTitle.isEmpty() && targetTitle.equals(candidateTitle)) {
            titleScore = 3;
        } else if (!targetTitle.isEmpty()
                && !candidateTitle.isEmpty()
                && (targetTitle.contains(candidateTitle)
                || candidateTitle.contains(targetTitle))) {
            titleScore = 1;
        }
        if (!targetArtist.isEmpty() && targetArtist.equals(candidateArtist)) {
            artistScore = 3;
        } else if (!targetArtist.isEmpty()
                && !candidateArtist.isEmpty()
                && (targetArtist.contains(candidateArtist)
                || candidateArtist.contains(targetArtist))) {
            artistScore = 2;
        }
        return titleScore > 0 && artistScore > 0
                ? titleScore + artistScore
                : 0;
    }

    private Map<String, Object> get(URI uri) {
        Map<String, Object> response = restClientBuilder.build()
                .get()
                .uri(uri)
                .retrieve()
                .body(MAP_TYPE);
        return response == null ? Map.of() : response;
    }

    private String tagName(Object value) {
        return string(map(value).get("name"));
    }

    private String firstGenre(Object artistValue) {
        Map<String, Object> artist = artistObject(artistValue);
        return list(artist.get("genres")).stream()
                .map(this::string)
                .map(this::cleanGenre)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String artistName(Object artistValue) {
        return string(artistObject(artistValue).get("name"));
    }

    private Map<String, Object> artistObject(Object artistValue) {
        if (artistValue instanceof List<?> values && !values.isEmpty()) {
            return map(values.getFirst());
        }
        return map(artistValue);
    }

    private String cleanGenre(String value) {
        String genre = trimToNull(value);
        if (genre == null || genre.length() > 100) {
            return null;
        }
        String normalized = normalized(genre);
        if (NON_GENRE_TAGS.contains(normalized)) {
            return null;
        }
        if (normalized.matches("\\d{2,4}s?")) {
            return null;
        }
        return genre.toLowerCase(Locale.ROOT);
    }

    private Integer supportedBpm(Object value) {
        Integer bpm = integer(value);
        if (bpm == null || bpm < MIN_SUPPORTED_BPM || bpm > MAX_SUPPORTED_BPM) {
            return null;
        }
        return bpm;
    }

    private String normalized(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String safeLogValue(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "<blank>";
        }
        return normalized.length() <= 80
                ? normalized
                : normalized.substring(0, 80) + "...";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : trimToNull(second);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return value instanceof List<?> values
                ? (List<Object>) values
                : value == null
                        ? List.of()
                        : List.of(value);
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }
}
