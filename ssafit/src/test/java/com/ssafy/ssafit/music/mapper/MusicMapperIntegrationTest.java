package com.ssafy.ssafit.music.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ssafy.ssafit.mapper.DatabaseIntegrationTestSupport;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicEnergyLevel;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.domain.MusicSortType;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.musicreview.domain.MusicReview;
import com.ssafy.ssafit.musicreview.domain.MusicReviewAverage;
import com.ssafy.ssafit.musicreview.mapper.MusicReviewMapper;

class MusicMapperIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private MusicReviewMapper musicReviewMapper;

    @Test
    void musicMapperAppliesSearchSortAndPaging() {
        insertMusic("MarathonPulse Alpha", "POP", 130, MusicSourceType.SEED, null);
        insertMusic("Velocity Beta", "POP", 145, MusicSourceType.ADMIN, null);
        insertMusic("Velocity Gamma", "POP", 160, MusicSourceType.EXTERNAL, null);
        insertMusic("Rock Outside", "ROCK", 150, MusicSourceType.SEED, null);

        MusicSearchCondition firstPage = condition("POP", 120, 170, 1, 2);
        firstPage.setSortType(MusicSortType.BPM_ASC);
        MusicSearchCondition secondPage = condition("POP", 120, 170, 2, 2);
        secondPage.setSortType(MusicSortType.BPM_ASC);

        List<Music> first = musicMapper.selectMusicList(firstPage);
        List<Music> second = musicMapper.selectMusicList(secondPage);
        MusicSearchCondition keyword = condition(null, 120, 170, 1, 10);
        keyword.setKeyword("MarathonPulse");
        keyword.setSortType(MusicSortType.TITLE_ASC);

        assertThat(first).extracting(Music::getTitle)
                .containsExactly("MarathonPulse Alpha", "Velocity Beta");
        assertThat(second).extracting(Music::getTitle)
                .containsExactly("Velocity Gamma");
        assertThat(musicMapper.countMusicList(firstPage)).isEqualTo(3);
        assertThat(musicMapper.selectMusicList(keyword))
                .extracting(Music::getTitle)
                .containsExactly("MarathonPulse Alpha");
    }

    @Test
    void musicMapperPersistsNullableEnergyAndSpotifyMetadataIndependently() {
        Music spotifyMusic = new Music();
        spotifyMusic.setTitle("Energy Metadata Song");
        spotifyMusic.setArtist("Mapper Test Artist");
        spotifyMusic.setMusicBpm(86);
        spotifyMusic.setMusicEnergyLevel(MusicEnergyLevel.PEAK);
        spotifyMusic.setSourceType(MusicSourceType.SPOTIFY);
        spotifyMusic.setSpotifyTrackId("energy-metadata-track");
        spotifyMusic.setSpotifyUri("spotify:track:energy-metadata-track");
        spotifyMusic.setSpotifyExternalUrl(
                "https://open.spotify.com/track/energy-metadata-track");

        assertThat(musicMapper.insertFromSpotify(spotifyMusic)).isEqualTo(1);
        Music selected = musicMapper.selectMusicDetail(spotifyMusic.getId())
                .orElseThrow();
        assertThat(selected.getMusicBpm()).isEqualTo(86);
        assertThat(selected.getMusicEnergyLevel()).isEqualTo(MusicEnergyLevel.PEAK);
        assertThat(selected.getSpotifyTrackId()).isEqualTo("energy-metadata-track");
        assertThat(selected.getSpotifyUri())
                .isEqualTo("spotify:track:energy-metadata-track");

        selected.setMusicBpm(172);
        selected.setMusicEnergyLevel(null);
        assertThat(musicMapper.updateSpotifyMetadata(selected)).isEqualTo(1);
        Music updated = musicMapper.selectMusicDetail(selected.getId())
                .orElseThrow();
        assertThat(updated.getMusicBpm()).isEqualTo(172);
        assertThat(updated.getMusicEnergyLevel()).isNull();
    }

    @Test
    void reviewMapperCalculatesAverageScores() {
        Member first = insertMember("reviewer_first");
        Member second = insertMember("reviewer_second");
        Music music = insertMusic("Average Song", "POP", 150, MusicSourceType.SEED, null);
        insertReview(first.getId(), music.getId(), 5, 4);
        insertReview(second.getId(), music.getId(), 3, 2);

        MusicReviewAverage average = musicReviewMapper.selectAverageScores(music.getId());

        assertThat(musicReviewMapper.selectByMusicId(music.getId())).hasSize(2);
        assertThat(average.getAveragePreferenceScore()).isEqualTo(4.0);
        assertThat(average.getAverageRunningFitScore()).isEqualTo(3.0);
        assertThat(average.getReviewCount()).isEqualTo(2);
    }

    private Music insertMusic(
            String title,
            String genre,
            int bpm,
            MusicSourceType sourceType,
            String spotifyUri) {
        Music music = new Music();
        music.setTitle(title);
        music.setArtist("Mapper Test Artist");
        music.setGenre(genre);
        music.setBpm(bpm);
        music.setSourceType(sourceType);
        if (sourceType == MusicSourceType.SPOTIFY) {
            music.setSpotifyTrackId(title.toLowerCase().replace(' ', '-'));
            music.setSpotifyUri(spotifyUri);
            musicMapper.insertFromSpotify(music);
        } else {
            jdbcTemplate.update(
                    """
                    INSERT INTO music (title, artist, genre, bpm, source_type)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    music.getTitle(),
                    music.getArtist(),
                    music.getGenre(),
                    music.getBpm(),
                    music.getSourceType().name());
            music.setId(jdbcTemplate.queryForObject(
                    "SELECT id FROM music WHERE title = ?",
                    Long.class,
                    title));
        }
        return music;
    }

    private void insertReview(
            Long memberId,
            Long musicId,
            int preference,
            int runningFit) {
        MusicReview review = new MusicReview();
        review.setMemberId(memberId);
        review.setMusicId(musicId);
        review.setPreferenceScore(preference);
        review.setRunningFitScore(runningFit);
        review.setReviewContent("Mapper integration review");
        musicReviewMapper.insert(review);
    }

    private MusicSearchCondition condition(
            String genre,
            int minBpm,
            int maxBpm,
            int page,
            int size) {
        MusicSearchCondition condition = new MusicSearchCondition();
        condition.setGenre(genre);
        condition.setMinBpm(minBpm);
        condition.setMaxBpm(maxBpm);
        condition.setPage(page);
        condition.setSize(size);
        condition.setOffset((page - 1) * size);
        return condition;
    }
}
