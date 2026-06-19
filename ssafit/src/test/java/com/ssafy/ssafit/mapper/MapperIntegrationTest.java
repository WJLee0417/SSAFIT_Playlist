package com.ssafy.ssafit.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.mapper.MemberMapper;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.domain.MusicSortType;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.musicreview.domain.MusicReview;
import com.ssafy.ssafit.musicreview.domain.MusicReviewAverage;
import com.ssafy.ssafit.musicreview.mapper.MusicReviewMapper;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistSave;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;
import com.ssafy.ssafit.playlist.mapper.PlaylistItemMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistSaveMapper;
import com.ssafy.ssafit.recommendation.domain.RecommendationLog;
import com.ssafy.ssafit.recommendation.domain.RecommendationResult;
import com.ssafy.ssafit.recommendation.mapper.RecommendationLogMapper;
import com.ssafy.ssafit.recommendation.service.RecommendationService;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
import com.ssafy.ssafit.spotify.mapper.SpotifyConnectionMapper;

@SpringBootTest
@ActiveProfiles("mapper-test")
@EnabledIfEnvironmentVariable(named = "SSAFIT_TEST_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapperIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private RunningProfileMapper runningProfileMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private MusicReviewMapper musicReviewMapper;

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private PlaylistItemMapper playlistItemMapper;

    @Autowired
    private PlaylistSaveMapper playlistSaveMapper;

    @Autowired
    private RecommendationLogMapper recommendationLogMapper;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private SpotifyConnectionMapper spotifyConnectionMapper;

    @BeforeAll
    void cleanBeforeTests() {
        deleteAllData();
    }

    @BeforeEach
    void cleanBeforeEachTest() {
        deleteAllData();
    }

    @AfterEach
    void cleanAfterEachTest() {
        deleteAllData();
    }

    @AfterAll
    void cleanAfterTests() {
        deleteAllData();
    }

    @Test
    void memberAndProfileMapperMapsGeneratedKeysAndEnums() {
        Member member = insertMember("mapper_member");
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(member.getId());
        profile.setRunningIntensity(RunningIntensity.MODERATE);
        profile.setTargetBpmMin(130);
        profile.setTargetBpmMax(160);
        profile.setPreferredGenre("POP");

        runningProfileMapper.insert(profile);

        RunningProfile selected = runningProfileMapper
                .selectByMemberId(member.getId())
                .orElseThrow();
        assertThat(memberMapper.existsByLoginId("mapper_member")).isTrue();
        assertThat(memberMapper.selectById(member.getId()).orElseThrow().getStatus())
                .isEqualTo(MemberStatus.ACTIVE);
        assertThat(selected.getId()).isNotNull();
        assertThat(selected.getRunningIntensity()).isEqualTo(RunningIntensity.MODERATE);
        assertThat(selected.getTargetBpmMin()).isEqualTo(130);
    }

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

    @Test
    void playlistMappersJoinVisibilityItemsSpotifyUrisAndSavedList() {
        Member owner = insertMember("playlist_owner");
        Member viewer = insertMember("playlist_viewer");
        Music spotifyMusic = insertMusic(
                "Spotify Join Song",
                "POP",
                150,
                MusicSourceType.SPOTIFY,
                "spotify:track:mapper-join");
        Music seedMusic = insertMusic(
                "Seed Join Song",
                "POP",
                140,
                MusicSourceType.SEED,
                null);
        Playlist publicPlaylist = insertPlaylist(
                owner.getId(),
                PlaylistType.USER,
                PlaylistSourceType.MANUAL,
                PlaylistVisibility.PUBLIC,
                "Public Join Playlist");
        insertPlaylist(
                owner.getId(),
                PlaylistType.USER,
                PlaylistSourceType.MANUAL,
                PlaylistVisibility.PRIVATE,
                "Private Join Playlist");
        insertItem(publicPlaylist.getId(), seedMusic.getId(), 2);
        insertItem(publicPlaylist.getId(), spotifyMusic.getId(), 1);

        PlaylistSave save = new PlaylistSave();
        save.setMemberId(viewer.getId());
        save.setPlaylistId(publicPlaylist.getId());
        playlistSaveMapper.insert(save);

        assertThat(playlistMapper.selectList(viewer.getId(), null))
                .extracting(Playlist::getTitle)
                .contains("Public Join Playlist")
                .doesNotContain("Private Join Playlist");
        assertThat(playlistItemMapper.selectByPlaylistId(publicPlaylist.getId()))
                .extracting(PlaylistItem::getMusicId)
                .containsExactly(spotifyMusic.getId(), seedMusic.getId());
        assertThat(playlistItemMapper.selectSpotifyUrisByPlaylistId(publicPlaylist.getId()))
                .containsExactly("spotify:track:mapper-join");
        assertThat(playlistSaveMapper.selectSavedList(viewer.getId()))
                .extracting(Playlist::getTitle)
                .containsExactly("Public Join Playlist");
    }

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
    }

    @Test
    void recommendationQueryCreatesOnlyLogAndSaveCopiesItems() {
        Member member = insertMember("recommend_member");
        insertProfile(member.getId(), RunningIntensity.MODERATE, 135, 165, "POP");
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

    @Test
    void spotifyConnectionMapperUpsertsAndRefreshesToken() {
        Member member = insertMember("spotify_connection_member");
        SpotifyConnection connection = new SpotifyConnection();
        connection.setMemberId(member.getId());
        connection.setSpotifyUserId("spotify-user");
        connection.setAccessToken("first-access");
        connection.setRefreshToken("first-refresh");
        connection.setTokenExpiresAt(java.time.LocalDateTime.now().plusHours(1));

        spotifyConnectionMapper.upsert(connection);
        SpotifyConnection selected = spotifyConnectionMapper
                .selectByMemberId(member.getId())
                .orElseThrow();
        assertThat(selected.getSpotifyUserId()).isEqualTo("spotify-user");
        assertThat(selected.getAccessToken()).isEqualTo("first-access");

        selected.setAccessToken("refreshed-access");
        selected.setRefreshToken("refreshed-refresh");
        selected.setTokenExpiresAt(java.time.LocalDateTime.now().plusHours(2));
        spotifyConnectionMapper.updateToken(selected);

        assertThat(spotifyConnectionMapper.selectByMemberId(member.getId())
                .orElseThrow()
                .getAccessToken()).isEqualTo("refreshed-access");
    }

    private Member insertMember(String loginId) {
        Member member = new Member();
        member.setLoginId(loginId);
        member.setPasswordHash("test-hash");
        member.setEmail(loginId + "@example.test");
        member.setNickname(loginId);
        member.setStatus(MemberStatus.ACTIVE);
        memberMapper.insert(member);
        return member;
    }

    private void insertProfile(
            Long memberId,
            RunningIntensity intensity,
            int minBpm,
            int maxBpm,
            String genre) {
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(memberId);
        profile.setRunningIntensity(intensity);
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

    private Playlist insertPlaylist(
            Long ownerId,
            PlaylistType type,
            PlaylistSourceType sourceType,
            PlaylistVisibility visibility,
            String title) {
        Playlist playlist = new Playlist();
        playlist.setOwnerId(ownerId);
        playlist.setPlaylistType(type);
        playlist.setSourceType(sourceType);
        playlist.setVisibility(visibility);
        playlist.setTitle(title);
        playlist.setSpotifyExportStatus(SpotifyExportStatus.NOT_EXPORTED);
        playlistMapper.insert(playlist);
        return playlist;
    }

    private void insertItem(Long playlistId, Long musicId, int orderNo) {
        PlaylistItem item = new PlaylistItem();
        item.setPlaylistId(playlistId);
        item.setMusicId(musicId);
        item.setOrderNo(orderNo);
        playlistItemMapper.insert(item);
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

    private int countRows(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void deleteAllData() {
        jdbcTemplate.update("DELETE FROM spotify_connections");
        jdbcTemplate.update("DELETE FROM recommendation_logs");
        jdbcTemplate.update("DELETE FROM playlist_saves");
        jdbcTemplate.update("DELETE FROM playlist_items");
        jdbcTemplate.update("DELETE FROM playlists");
        jdbcTemplate.update("DELETE FROM music_reviews");
        jdbcTemplate.update("DELETE FROM music");
        jdbcTemplate.update("DELETE FROM running_profiles");
        jdbcTemplate.update("DELETE FROM members");
    }
}
