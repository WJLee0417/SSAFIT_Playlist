package com.ssafy.ssafit.music.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.domain.MusicSortType;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.domain.SpotifyTrackMetadata;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.music.tempo.MusicTempoEnrichmentProperties;
import com.ssafy.ssafit.music.tempo.MusicTempoProvider;
import com.ssafy.ssafit.music.tempo.NoOpMusicTempoProvider;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;

@Service
@Transactional(readOnly = true)
public class MusicService {

    private static final int MIN_BPM = 60;
    private static final int MAX_BPM = 220;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final MusicMapper musicMapper;
    private final MusicTempoProvider musicTempoProvider;
    private final MusicTempoEnrichmentProperties tempoEnrichmentProperties;

    @Autowired
    public MusicService(
            MusicMapper musicMapper,
            MusicTempoProvider musicTempoProvider,
            MusicTempoEnrichmentProperties tempoEnrichmentProperties) {
        this.musicMapper = musicMapper;
        this.musicTempoProvider = musicTempoProvider;
        this.tempoEnrichmentProperties = tempoEnrichmentProperties;
    }

    public MusicService(MusicMapper musicMapper) {
        this(
                musicMapper,
                new NoOpMusicTempoProvider(),
                new MusicTempoEnrichmentProperties());
    }

    public List<Music> getMusicList(MusicSearchCondition condition) {
        return musicMapper.selectMusicList(normalizeCondition(condition));
    }

    public long countMusicList(MusicSearchCondition condition) {
        return musicMapper.countMusicList(normalizeCondition(condition));
    }

    public List<Music> getRecommendedMusic(MusicSearchCondition condition) {
        return musicMapper.selectRecommendedMusic(normalizeCondition(condition));
    }

    public Music getMusicDetail(Long musicId) {
        if (musicId == null) {
            throw new CustomException(ErrorCode.MUSIC_NOT_FOUND);
        }

        return musicMapper.selectMusicDetail(musicId)
                .orElseThrow(() -> new CustomException(ErrorCode.MUSIC_NOT_FOUND));
    }

    @Transactional
    public Music createInternalMusic(Music music) {
        normalizeInternalMusic(music);
        if (musicMapper.insertInternal(music) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return music;
    }

    @Transactional
    public Music updateInternalMusic(Long musicId, Music request) {
        Music existing = getMusicDetail(musicId);
        request.setId(existing.getId());
        request.setSourceType(existing.getSourceType());
        normalizeInternalMusic(request);
        if (musicMapper.updateInternal(request) != 1) {
            throw new CustomException(ErrorCode.MUSIC_NOT_FOUND);
        }
        return musicMapper.selectMusicDetail(musicId)
                .orElseThrow(() -> new CustomException(ErrorCode.MUSIC_NOT_FOUND));
    }

    @Transactional
    public Music saveSelectedSpotifyTrack(SpotifyTrackMetadata metadata) {
        validateSpotifyMetadata(metadata);
        validateMusicBpm(metadata.getMusicBpm());

        String spotifyTrackId = metadata.getSpotifyTrackId().trim();
        String spotifyUri = metadata.getSpotifyUri().trim();
        Optional<Music> existing = findExistingSpotifyMusic(
                spotifyTrackId,
                spotifyUri);
        if (existing.isPresent()) {
            return enrichExistingMusic(existing.get(), metadata);
        }

        Music music = toSpotifyMusic(metadata);
        try {
            if (musicMapper.insertFromSpotify(music) != 1) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            return music;
        } catch (DuplicateKeyException exception) {
            return findExistingSpotifyMusic(spotifyTrackId, spotifyUri)
                    .map(value -> enrichExistingMusic(value, metadata))
                    .orElseThrow(() -> exception);
        }
    }

    private MusicSearchCondition normalizeCondition(MusicSearchCondition source) {
        MusicSearchCondition condition = source == null
                ? new MusicSearchCondition()
                : copyCondition(source);

        condition.setKeyword(trimToNull(condition.getKeyword()));
        condition.setGenre(trimToNull(condition.getGenre()));
        applyIntensityBpmRange(condition);
        validateBpmRange(condition.getMinBpm(), condition.getMaxBpm());

        int page = condition.getPage() == null ? DEFAULT_PAGE : condition.getPage();
        int size = condition.getSize() == null ? DEFAULT_SIZE : condition.getSize();
        if (page < 1 || size < 1 || size > MAX_SIZE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        condition.setPage(page);
        condition.setSize(size);
        condition.setOffset((page - 1) * size);
        if (condition.getSortType() == null) {
            condition.setSortType(MusicSortType.CREATED_DESC);
        }
        return condition;
    }

    private MusicSearchCondition copyCondition(MusicSearchCondition source) {
        return new MusicSearchCondition(
                source.getKeyword(),
                source.getGenre(),
                source.getMinBpm(),
                source.getMaxBpm(),
                source.getRunningIntensity(),
                source.getSortType(),
                source.getPage(),
                source.getSize(),
                source.getOffset());
    }

    private void applyIntensityBpmRange(MusicSearchCondition condition) {
        if (condition.getRunningIntensity() == null
                || condition.getMinBpm() != null
                || condition.getMaxBpm() != null) {
            return;
        }

        int[] range = bpmRangeFor(condition.getRunningIntensity());
        condition.setMinBpm(range[0]);
        condition.setMaxBpm(range[1]);
    }

    private int[] bpmRangeFor(RunningIntensity intensity) {
        return switch (intensity) {
            case LOW -> new int[] {90, 130};
            case MODERATE -> new int[] {120, 160};
            case HIGH -> new int[] {150, 190};
            case INTERVAL -> new int[] {140, 200};
        };
    }

    private void validateBpm(Integer bpm) {
        if (bpm != null && (bpm < MIN_BPM || bpm > MAX_BPM)) {
            throw new CustomException(ErrorCode.INVALID_BPM_VALUE);
        }
    }

    private void validateMusicBpm(Integer musicBpm) {
        if (musicBpm != null
                && (musicBpm < MIN_BPM || musicBpm > MAX_BPM)) {
            throw new CustomException(ErrorCode.INVALID_MUSIC_BPM_VALUE);
        }
    }

    private void validateBpmRange(Integer minBpm, Integer maxBpm) {
        validateBpm(minBpm);
        validateBpm(maxBpm);
        if (minBpm != null && maxBpm != null && minBpm > maxBpm) {
            throw new CustomException(ErrorCode.INVALID_BPM_RANGE);
        }
    }

    private void validateSpotifyMetadata(SpotifyTrackMetadata metadata) {
        if (metadata == null
                || isBlank(metadata.getSpotifyTrackId())
                || isBlank(metadata.getSpotifyUri())
                || isBlank(metadata.getTitle())
                || isBlank(metadata.getArtist())) {
            throw new CustomException(ErrorCode.SPOTIFY_TRACK_NOT_FOUND);
        }
        if (metadata.getDurationMs() != null && metadata.getDurationMs() < 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void normalizeInternalMusic(Music music) {
        if (music == null
                || isBlank(music.getTitle())
                || isBlank(music.getArtist())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        validateMusicBpm(music.getMusicBpm());
        if (music.getDurationMs() != null && music.getDurationMs() < 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        music.setTitle(music.getTitle().trim());
        music.setArtist(music.getArtist().trim());
        music.setAlbumName(trimToNull(music.getAlbumName()));
        music.setImageUrl(trimToNull(music.getImageUrl()));
        music.setGenre(trimToNull(music.getGenre()));
        if (music.getSourceType() == null) {
            music.setSourceType(MusicSourceType.ADMIN);
        }
    }

    private Music toSpotifyMusic(SpotifyTrackMetadata metadata) {
        Music music = new Music();
        music.setTitle(metadata.getTitle().trim());
        music.setArtist(metadata.getArtist().trim());
        music.setAlbumName(trimToNull(metadata.getAlbumName()));
        music.setImageUrl(trimToNull(metadata.getImageUrl()));
        music.setGenre(trimToNull(metadata.getGenre()));
        music.setDurationMs(metadata.getDurationMs());
        music.setMusicEnergyLevel(null);
        music.setMusicBpm(resolveMusicBpm(
                metadata.getMusicBpm(),
                metadata.getSpotifyTrackId()));
        music.setSourceType(MusicSourceType.SPOTIFY);
        music.setSpotifyTrackId(metadata.getSpotifyTrackId().trim());
        music.setSpotifyUri(metadata.getSpotifyUri().trim());
        music.setSpotifyExternalUrl(trimToNull(metadata.getSpotifyExternalUrl()));
        return music;
    }

    private Music enrichExistingMusic(Music music, SpotifyTrackMetadata metadata) {
        validateSpotifyIdentity(music, metadata);
        boolean changed = false;
        changed |= fillIfBlank(music.getTitle(), metadata.getTitle(), music::setTitle);
        changed |= fillIfBlank(music.getArtist(), metadata.getArtist(), music::setArtist);
        changed |= fillIfBlank(
                music.getAlbumName(),
                metadata.getAlbumName(),
                music::setAlbumName);
        changed |= fillIfBlank(music.getImageUrl(), metadata.getImageUrl(), music::setImageUrl);
        changed |= fillIfBlank(music.getGenre(), metadata.getGenre(), music::setGenre);
        changed |= fillIfBlank(
                music.getSpotifyTrackId(),
                metadata.getSpotifyTrackId(),
                music::setSpotifyTrackId);
        changed |= fillIfBlank(
                music.getSpotifyUri(),
                metadata.getSpotifyUri(),
                music::setSpotifyUri);
        changed |= fillIfBlank(
                music.getSpotifyExternalUrl(),
                metadata.getSpotifyExternalUrl(),
                music::setSpotifyExternalUrl);

        if (music.getDurationMs() == null && metadata.getDurationMs() != null) {
            music.setDurationMs(metadata.getDurationMs());
            changed = true;
        }
        if (music.getMusicBpm() == null) {
            Integer resolvedMusicBpm = resolveMusicBpm(
                    metadata.getMusicBpm(),
                    metadata.getSpotifyTrackId());
            if (resolvedMusicBpm != null) {
                music.setMusicBpm(resolvedMusicBpm);
                changed = true;
            }
        }

        if (changed) {
            if (musicMapper.updateSpotifyMetadata(music) != 1) {
                throw new CustomException(ErrorCode.MUSIC_NOT_FOUND);
            }
        }
        return music;
    }

    private Optional<Music> findExistingSpotifyMusic(
            String spotifyTrackId,
            String spotifyUri) {
        Optional<Music> byTrackId =
                musicMapper.selectBySpotifyTrackId(spotifyTrackId);
        Optional<Music> byUri = musicMapper.selectBySpotifyUri(spotifyUri);
        if (byTrackId.isPresent()
                && byUri.isPresent()
                && !byTrackId.get().getId().equals(byUri.get().getId())) {
            throw new CustomException(ErrorCode.SPOTIFY_TRACK_IDENTITY_CONFLICT);
        }
        return byTrackId.isPresent() ? byTrackId : byUri;
    }

    private void validateSpotifyIdentity(
            Music music,
            SpotifyTrackMetadata metadata) {
        if ((!isBlank(music.getSpotifyTrackId())
                && !music.getSpotifyTrackId().equals(
                        metadata.getSpotifyTrackId().trim()))
                || (!isBlank(music.getSpotifyUri())
                && !music.getSpotifyUri().equals(metadata.getSpotifyUri().trim()))) {
            throw new CustomException(ErrorCode.SPOTIFY_TRACK_IDENTITY_CONFLICT);
        }
    }

    private Integer resolveMusicBpm(
            Integer operatorProvidedMusicBpm,
            String spotifyTrackId) {
        if (operatorProvidedMusicBpm != null) {
            return operatorProvidedMusicBpm;
        }
        if (!tempoEnrichmentProperties.isEnabled()) {
            return null;
        }
        try {
            Integer resolved = musicTempoProvider
                    .resolveMusicBpm(spotifyTrackId)
                    .orElse(null);
            validateMusicBpm(resolved);
            return resolved;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean fillIfBlank(
            String currentValue,
            String candidateValue,
            java.util.function.Consumer<String> setter) {
        String normalized = trimToNull(candidateValue);
        if (isBlank(currentValue) && normalized != null) {
            setter.accept(normalized);
            return true;
        }
        return false;
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
