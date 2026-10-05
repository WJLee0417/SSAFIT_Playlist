package com.ssafy.ssafit.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.ssafy.ssafit.mapper.DatabaseIntegrationTestSupport;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.mapper.PlaylistItemMapper;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.domain.RecommendationResult;
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;

class RecommendationPersistenceIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private RunningProfileMapper runningProfileMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private RecommendationLogMapper recommendationLogMapper;

    @Autowired
    private PlaylistItemMapper playlistItemMapper;

    @Autowired
    private RecommendationService recommendationService;

    @Test
    void recommendationQueryCreatesOnlyLogAndSaveCopiesItems() {
        Member member = insertMember("recommend_member");
        insertProfile(member.getId(), 135, 165, "POP");
        insertMusic("Recommended First", "POP", 150, MusicSourceType.SEED, null);
        insertMusic("Recommended Second", "POP", 145, MusicSourceType.ADMIN, null);

        int playlistsBefore = countRows("playlists");
        RecommendationResult result = recommendationService.recommend(
                member.getId(),
                RunningIntensity.MODERATE,
                "POP",
                2);

        assertThat(result.getMusicList()).hasSize(2);
        assertThat(countRows("recommendation_logs")).isEqualTo(1);
        assertThat(countRows("playlists")).isEqualTo(playlistsBefore);

        Playlist saved = recommendationService.saveRecommendation(
                member.getId(),
                result.getRecommendationLog().getId(),
                "Mapper Recommendation Copy",
                PlaylistVisibility.PRIVATE);

        assertThat(saved.getSourceType()).isEqualTo(PlaylistSourceType.CURATION_COPY);
        assertThat(playlistItemMapper.selectByPlaylistId(saved.getId())).hasSize(2);
        assertThat(recommendationLogMapper.selectById(result.getRecommendationLog().getId())
                .orElseThrow()
                .getSavedPlaylistId()).isEqualTo(saved.getId());
    }

    @Test
    void recommendationSaveRollsBackPlaylistWhenBulkInsertFails() {
        Member member = insertMember("rollback_member");
        Music music = insertMusic("Rollback Song", "POP", 150, MusicSourceType.SEED, null);
        RecommendationLog log = new RecommendationLog();
        log.setMemberId(member.getId());
        log.setRequestedIntensity(RunningIntensity.MODERATE);
        log.setRequestedBpmMin(130);
        log.setRequestedBpmMax(160);
        log.setRequestedGenre("POP");
        log.setRequestConditions(
                "{\"intensity\":\"MODERATE\",\"bpmMin\":130,\"bpmMax\":160}");
        log.setResultSummary(
                "{\"count\":2,\"musicIds\":["
                        + music.getId()
                        + ","
                        + music.getId()
                        + "]}");
        log.setRecommendationReason("중복 곡으로 bulk insert 실패 유도");
        recommendationLogMapper.insert(log);
        int playlistsBefore = countRows("playlists");

        assertThatThrownBy(() -> recommendationService.saveRecommendation(
                member.getId(),
                log.getId(),
                "Rollback Copy",
                PlaylistVisibility.PRIVATE))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(countRows("playlists")).isEqualTo(playlistsBefore);
        assertThat(recommendationLogMapper.selectById(log.getId())
                .orElseThrow()
                .getSavedPlaylistId()).isNull();
    }

    private void insertProfile(
            Long memberId,
            int minBpm,
            int maxBpm,
            String genre) {
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(memberId);
        profile.setTargetBpmMin(minBpm);
        profile.setTargetBpmMax(maxBpm);
        profile.setPreferredGenre(genre);
        runningProfileMapper.insert(profile);
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
}
