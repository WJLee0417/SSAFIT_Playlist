package com.ssafy.ssafit.mapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.musicreview.domain.MusicReview;
import com.ssafy.ssafit.musicreview.mapper.MusicReviewMapper;

class DatabaseConstraintIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private MusicReviewMapper musicReviewMapper;

    @Test
    void databaseRejectsUniqueAndForeignKeyViolations() {
        Member member = insertMember("constraint_member");
        Music music = insertMusic("Constraint Song", "POP", 150, MusicSourceType.SEED, null);
        insertReview(member.getId(), music.getId(), 4, 4);

        assertThatThrownBy(() -> insertReview(member.getId(), music.getId(), 5, 5))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO music_reviews (
                    member_id, music_id, preference_score, running_fit_score
                ) VALUES (?, ?, 4, 4)
                """,
                Long.MAX_VALUE,
                music.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO running_records (
                    member_id,
                    record_date,
                    distance_km,
                    duration_seconds,
                    pace_seconds_per_km
                ) VALUES (?, CURRENT_DATE, 0, 1800, 360)
                """,
                member.getId()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO running_sessions (
                    host_member_id,
                    title,
                    meeting_place,
                    scheduled_at,
                    capacity,
                    status
                ) VALUES (?, NULL, 'Seoul', CURRENT_TIMESTAMP, 1, 'OPEN')
                """,
                member.getId()))
                .isInstanceOf(DataAccessException.class);
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
}
