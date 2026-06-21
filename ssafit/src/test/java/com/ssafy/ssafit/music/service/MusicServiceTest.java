package com.ssafy.ssafit.music.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.domain.MusicSortType;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.domain.SpotifyTrackMetadata;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;

@ExtendWith(MockitoExtension.class)
class MusicServiceTest {

    @Mock
    private MusicMapper musicMapper;

    private MusicService musicService;

    @BeforeEach
    void setUp() {
        musicService = new MusicService(musicMapper);
    }

    @Test
    void getMusicListNormalizesSearchSortAndPaging() {
        MusicSearchCondition condition = new MusicSearchCondition();
        condition.setKeyword("  run  ");
        condition.setGenre("  POP ");
        condition.setRunningIntensity(RunningIntensity.MODERATE);
        condition.setSortType(MusicSortType.BPM_ASC);
        condition.setPage(2);
        condition.setSize(10);
        when(musicMapper.selectMusicList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        musicService.getMusicList(condition);

        ArgumentCaptor<MusicSearchCondition> captor =
                ArgumentCaptor.forClass(MusicSearchCondition.class);
        verify(musicMapper).selectMusicList(captor.capture());
        MusicSearchCondition normalized = captor.getValue();
        assertThat(normalized.getKeyword()).isEqualTo("run");
        assertThat(normalized.getGenre()).isEqualTo("POP");
        assertThat(normalized.getMinBpm()).isEqualTo(120);
        assertThat(normalized.getMaxBpm()).isEqualTo(160);
        assertThat(normalized.getSortType()).isEqualTo(MusicSortType.BPM_ASC);
        assertThat(normalized.getPage()).isEqualTo(2);
        assertThat(normalized.getSize()).isEqualTo(10);
        assertThat(normalized.getOffset()).isEqualTo(10);
    }

    @Test
    void explicitBpmRangeTakesPriorityOverRunningIntensity() {
        MusicSearchCondition condition = new MusicSearchCondition();
        condition.setRunningIntensity(RunningIntensity.HIGH);
        condition.setMinBpm(100);
        condition.setMaxBpm(110);
        when(musicMapper.countMusicList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(0L);

        musicService.countMusicList(condition);

        ArgumentCaptor<MusicSearchCondition> captor =
                ArgumentCaptor.forClass(MusicSearchCondition.class);
        verify(musicMapper).countMusicList(captor.capture());
        assertThat(captor.getValue().getMinBpm()).isEqualTo(100);
        assertThat(captor.getValue().getMaxBpm()).isEqualTo(110);
    }

    @Test
    void getMusicListRejectsInvalidBpmRange() {
        MusicSearchCondition condition = new MusicSearchCondition();
        condition.setMinBpm(180);
        condition.setMaxBpm(120);

        assertError(
                ErrorCode.INVALID_BPM_RANGE,
                () -> musicService.getMusicList(condition));
    }

    @Test
    void getMusicListRejectsBpmOutsideAllowedRange() {
        MusicSearchCondition condition = new MusicSearchCondition();
        condition.setMinBpm(59);

        assertError(
                ErrorCode.INVALID_BPM_VALUE,
                () -> musicService.getMusicList(condition));
    }

    @Test
    void getMusicDetailThrowsWhenMusicDoesNotExist() {
        when(musicMapper.selectMusicDetail(1L)).thenReturn(Optional.empty());

        assertError(ErrorCode.MUSIC_NOT_FOUND, () -> musicService.getMusicDetail(1L));
    }

    @Test
    void saveSelectedSpotifyTrackInsertsNewMusic() {
        SpotifyTrackMetadata metadata = validMetadata();
        when(musicMapper.selectBySpotifyTrackId("track-1")).thenReturn(Optional.empty());
        when(musicMapper.insertFromSpotify(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Music music = invocation.getArgument(0);
                    music.setId(10L);
                    return 1;
                });

        Music saved = musicService.saveSelectedSpotifyTrack(metadata);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getSourceType()).isEqualTo(MusicSourceType.SPOTIFY);
        assertThat(saved.getSpotifyTrackId()).isEqualTo("track-1");
        assertThat(saved.getSpotifyUri()).isEqualTo("spotify:track:track-1");
        assertThat(saved.getAlbumName()).isEqualTo("Running Album");
        assertThat(saved.getImageUrl()).isEqualTo("https://image.example/cover.jpg");
        verify(musicMapper).insertFromSpotify(saved);
    }

    @Test
    void saveSelectedSpotifyTrackReturnsExistingMusicWithoutDuplicateInsert() {
        SpotifyTrackMetadata metadata = validMetadata();
        Music existing = completeExistingMusic();
        when(musicMapper.selectBySpotifyTrackId("track-1"))
                .thenReturn(Optional.of(existing));

        Music saved = musicService.saveSelectedSpotifyTrack(metadata);

        assertThat(saved).isSameAs(existing);
        verify(musicMapper, never()).insertFromSpotify(org.mockito.ArgumentMatchers.any());
        verify(musicMapper, never()).updateSpotifyMetadata(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void saveSelectedSpotifyTrackEnrichesMissingMetadata() {
        SpotifyTrackMetadata metadata = validMetadata();
        Music existing = completeExistingMusic();
        existing.setAlbumName(null);
        existing.setImageUrl(null);
        existing.setGenre(null);
        existing.setBpm(null);
        when(musicMapper.selectBySpotifyTrackId("track-1"))
                .thenReturn(Optional.of(existing));
        when(musicMapper.updateSpotifyMetadata(existing)).thenReturn(1);

        Music saved = musicService.saveSelectedSpotifyTrack(metadata);

        assertThat(saved.getAlbumName()).isEqualTo("Running Album");
        assertThat(saved.getImageUrl()).isEqualTo("https://image.example/cover.jpg");
        assertThat(saved.getGenre()).isEqualTo("POP");
        assertThat(saved.getBpm()).isEqualTo(150);
        verify(musicMapper).updateSpotifyMetadata(existing);
        verify(musicMapper, never()).insertFromSpotify(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void saveSelectedSpotifyTrackUsesNormalizedTrackIdForLookup() {
        SpotifyTrackMetadata metadata = validMetadata();
        metadata.setSpotifyTrackId("  track-1  ");
        Music existing = completeExistingMusic();
        when(musicMapper.selectBySpotifyTrackId("track-1"))
                .thenReturn(Optional.of(existing));

        assertThat(musicService.saveSelectedSpotifyTrack(metadata)).isSameAs(existing);

        verify(musicMapper).selectBySpotifyTrackId("track-1");
        verify(musicMapper, never()).insertFromSpotify(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void duplicateInsertRaceReusesAndEnrichesStoredMusic() {
        SpotifyTrackMetadata metadata = validMetadata();
        Music concurrentlyInserted = completeExistingMusic();
        concurrentlyInserted.setAlbumName(null);
        when(musicMapper.selectBySpotifyTrackId("track-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentlyInserted));
        when(musicMapper.insertFromSpotify(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DuplicateKeyException("duplicate spotify track"));
        when(musicMapper.updateSpotifyMetadata(concurrentlyInserted)).thenReturn(1);

        Music saved = musicService.saveSelectedSpotifyTrack(metadata);

        assertThat(saved).isSameAs(concurrentlyInserted);
        assertThat(saved.getAlbumName()).isEqualTo("Running Album");
        verify(musicMapper).updateSpotifyMetadata(concurrentlyInserted);
    }

    @Test
    void saveSelectedSpotifyTrackRejectsMissingTrackId() {
        SpotifyTrackMetadata metadata = validMetadata();
        metadata.setSpotifyTrackId(" ");

        assertError(
                ErrorCode.SPOTIFY_TRACK_NOT_FOUND,
                () -> musicService.saveSelectedSpotifyTrack(metadata));
    }

    @Test
    void saveSelectedSpotifyTrackRejectsInvalidBpm() {
        SpotifyTrackMetadata metadata = validMetadata();
        metadata.setBpm(221);

        assertError(
                ErrorCode.INVALID_BPM_VALUE,
                () -> musicService.saveSelectedSpotifyTrack(metadata));
    }

    private SpotifyTrackMetadata validMetadata() {
        return new SpotifyTrackMetadata(
                "track-1",
                "spotify:track:track-1",
                "https://open.spotify.com/track/track-1",
                "Run Song",
                "Runner",
                "Running Album",
                "https://image.example/cover.jpg",
                150,
                "POP");
    }

    private Music completeExistingMusic() {
        Music music = new Music();
        music.setId(1L);
        music.setTitle("Run Song");
        music.setArtist("Runner");
        music.setAlbumName("Running Album");
        music.setImageUrl("https://image.example/cover.jpg");
        music.setGenre("POP");
        music.setBpm(150);
        music.setSourceType(MusicSourceType.SPOTIFY);
        music.setSpotifyTrackId("track-1");
        music.setSpotifyUri("spotify:track:track-1");
        music.setSpotifyExternalUrl("https://open.spotify.com/track/track-1");
        return music;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
