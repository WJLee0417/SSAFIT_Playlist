package com.ssafy.ssafit.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.ssafy.ssafit.common.auth.SessionConst;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
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
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;
import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionParticipant;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;
import com.ssafy.ssafit.runningsession.mapper.RunningSessionMapper;
import com.ssafy.ssafit.runningsession.mapper.RunningSessionParticipantMapper;
import com.ssafy.ssafit.runningsession.service.RunningSessionParticipantService;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
import com.ssafy.ssafit.spotify.mapper.SpotifyConnectionMapper;

@SpringBootTest
@ActiveProfiles("mapper-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapperIntegrationTest {

    private static final TestDatabaseGuard.TestDatabaseConfig TEST_DATABASE =
            TestDatabaseGuard.requireFromEnvironment();

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TEST_DATABASE::jdbcUrl);
        registry.add("spring.datasource.username", TEST_DATABASE::username);
        registry.add("spring.datasource.password", TEST_DATABASE::password);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private RunningProfileMapper runningProfileMapper;

    @Autowired
    private RunningRecordMapper runningRecordMapper;

    @Autowired
    private RunningSessionMapper runningSessionMapper;

    @Autowired
    private RunningSessionParticipantMapper runningSessionParticipantMapper;

    @Autowired
    private RunningSessionParticipantService runningSessionParticipantService;

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
        String databaseName = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class);
        if (databaseName == null
                || !databaseName.toLowerCase().contains("test")) {
            throw new IllegalStateException(
                    "Mapper integration tests require a dedicated test database.");
        }
        if (!databaseName.equalsIgnoreCase(TEST_DATABASE.databaseName())) {
            throw new IllegalStateException(
                    "Connected database '"
                            + databaseName
                            + "' does not match the validated JDBC database '"
                            + TEST_DATABASE.databaseName()
                            + "'.");
        }
        String currentUser = jdbcTemplate.queryForObject(
                "SELECT CURRENT_USER()",
                String.class);
        if (currentUser == null
                || currentUser.toLowerCase().startsWith("root@")) {
            throw new IllegalStateException(
                    "Mapper integration tests must use a dedicated non-root "
                            + "test database account.");
        }
        assertDedicatedDatabasePrivileges();
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
    void runningRecordMapperSupportsCrudAndSoftDelete() {
        Member member = insertMember("running_record_member");
        RunningRecord record = new RunningRecord();
        record.setMemberId(member.getId());
        record.setRecordDate(java.time.LocalDate.of(2026, 6, 20));
        record.setDistanceKm(new java.math.BigDecimal("5.00"));
        record.setDurationSeconds(1800);
        record.setPaceSecondsPerKm(360);
        record.setMemo("Mapper running record");

        runningRecordMapper.insert(record);

        RunningRecord olderRecord = new RunningRecord();
        olderRecord.setMemberId(member.getId());
        olderRecord.setRecordDate(java.time.LocalDate.of(2026, 6, 19));
        olderRecord.setDistanceKm(new java.math.BigDecimal("3.00"));
        olderRecord.setDurationSeconds(1200);
        olderRecord.setPaceSecondsPerKm(400);
        olderRecord.setMemo("Older mapper running record");
        runningRecordMapper.insert(olderRecord);
        Member otherMember = insertMember("running_record_other_member");

        assertThat(record.getId()).isNotNull();
        RunningRecord selected = runningRecordMapper.selectById(record.getId())
                .orElseThrow();
        assertThat(selected.getMemberId()).isEqualTo(member.getId());
        assertThat(selected.getDistanceKm())
                .isEqualByComparingTo(new java.math.BigDecimal("5.00"));
        assertThat(selected.getCreatedAt()).isNotNull();
        assertThat(selected.getUpdatedAt()).isNotNull();
        assertThat(runningRecordMapper.selectListByMemberId(member.getId()))
                .extracting(RunningRecord::getId)
                .containsExactly(record.getId(), olderRecord.getId());
        assertThat(runningRecordMapper.selectListByMemberId(otherMember.getId()))
                .isEmpty();

        record.setDistanceKm(new java.math.BigDecimal("10.00"));
        record.setDurationSeconds(3300);
        record.setPaceSecondsPerKm(330);
        runningRecordMapper.update(record);

        assertThat(runningRecordMapper.selectById(record.getId())
                .orElseThrow()
                .getPaceSecondsPerKm()).isEqualTo(330);

        runningRecordMapper.delete(record.getId());

        assertThat(runningRecordMapper.selectById(record.getId())).isEmpty();
        assertThat(runningRecordMapper.selectListByMemberId(member.getId()))
                .extracting(RunningRecord::getId)
                .containsExactly(olderRecord.getId());
        assertThat(countRows("running_records")).isEqualTo(2);
    }

    @Test
    void runningSessionMapperSupportsCrudStatusCancelAndLockQuery() {
        Member host = insertMember("running_session_host");
        RunningSession runningSession = new RunningSession();
        runningSession.setHostMemberId(host.getId());
        runningSession.setTitle("Mapper Running Session");
        runningSession.setDescription("Mapper integration session");
        runningSession.setMeetingPlace("Seoul");
        runningSession.setScheduledAt(
                java.time.LocalDateTime.of(2026, 7, 1, 9, 0));
        runningSession.setCapacity(10);
        runningSession.setStatus(RunningSessionStatus.OPEN);

        runningSessionMapper.insert(runningSession);

        assertThat(runningSession.getId()).isNotNull();
        assertThat(runningSessionMapper.selectHostId(runningSession.getId()))
                .isEqualTo(host.getId());
        assertThat(runningSessionMapper.selectList())
                .extracting(RunningSession::getId)
                .contains(runningSession.getId());
        assertThat(runningSessionMapper
                .selectByIdForUpdate(runningSession.getId())
                .orElseThrow()
                .getStatus()).isEqualTo(RunningSessionStatus.OPEN);

        runningSession.setTitle("Updated Running Session");
        runningSession.setCapacity(20);
        runningSessionMapper.update(runningSession);
        runningSessionMapper.updateStatus(
                runningSession.getId(),
                RunningSessionStatus.CLOSED);

        RunningSession closed = runningSessionMapper
                .selectDetail(runningSession.getId())
                .orElseThrow();
        assertThat(closed.getTitle()).isEqualTo("Updated Running Session");
        assertThat(closed.getCapacity()).isEqualTo(20);
        assertThat(closed.getStatus()).isEqualTo(RunningSessionStatus.CLOSED);

        runningSessionMapper.cancel(runningSession.getId());

        RunningSession cancelled = runningSessionMapper
                .selectDetail(runningSession.getId())
                .orElseThrow();
        assertThat(cancelled.getStatus())
                .isEqualTo(RunningSessionStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(runningSessionMapper.update(runningSession)).isZero();
        assertThat(runningSessionMapper.updateStatus(
                runningSession.getId(),
                RunningSessionStatus.OPEN)).isZero();
        assertThat(runningSessionMapper.cancel(runningSession.getId())).isZero();
    }

    @Test
    void runningSessionParticipantMapperSupportsJoinCancelAndRejoin() {
        Member host = insertMember("participant_host");
        Member first = insertMember("participant_first");
        Member second = insertMember("participant_second");
        RunningSession runningSession = insertRunningSession(host.getId(), 1);

        RunningSessionParticipant firstParticipation =
                new RunningSessionParticipant();
        firstParticipation.setSessionId(runningSession.getId());
        firstParticipation.setMemberId(first.getId());
        runningSessionParticipantMapper.insert(firstParticipation);

        assertThat(firstParticipation.getId()).isNotNull();
        assertThat(runningSessionParticipantMapper.existsParticipant(
                runningSession.getId(),
                first.getId())).isTrue();
        assertThat(runningSessionParticipantMapper.countParticipants(
                runningSession.getId())).isEqualTo(1);
        assertThat(runningSessionParticipantMapper.selectParticipants(
                runningSession.getId()))
                .extracting(RunningSessionParticipant::getMemberId)
                .containsExactly(first.getId());
        assertThat(runningSessionParticipantMapper
                .selectBySessionIdAndMemberId(
                        runningSession.getId(),
                        first.getId()))
                .isPresent();

        RunningSessionParticipant duplicate = new RunningSessionParticipant();
        duplicate.setSessionId(runningSession.getId());
        duplicate.setMemberId(first.getId());
        assertThatThrownBy(() ->
                runningSessionParticipantMapper.insert(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(runningSessionParticipantMapper.countParticipants(
                runningSession.getId())).isEqualTo(1);

        assertThat(runningSessionParticipantMapper.delete(
                runningSession.getId(),
                second.getId())).isZero();
        assertThat(runningSessionParticipantMapper.countParticipants(
                runningSession.getId())).isEqualTo(1);

        assertThat(runningSessionParticipantMapper.delete(
                runningSession.getId(),
                first.getId())).isEqualTo(1);
        assertThat(runningSessionParticipantMapper.countParticipants(
                runningSession.getId())).isZero();

        RunningSessionParticipant rejoined = new RunningSessionParticipant();
        rejoined.setSessionId(runningSession.getId());
        rejoined.setMemberId(first.getId());
        runningSessionParticipantMapper.insert(rejoined);

        assertThat(runningSessionParticipantMapper.countParticipants(
                runningSession.getId())).isEqualTo(1);

        RunningSession fullSession = insertRunningSession(host.getId(), 1);
        runningSessionParticipantService.joinSession(
                loginSession(first.getId()),
                fullSession.getId());
        assertThatThrownBy(() -> runningSessionParticipantService.joinSession(
                loginSession(second.getId()),
                fullSession.getId()))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SESSION_FULL));

        RunningSession closedSession = insertRunningSession(host.getId(), 2);
        runningSessionMapper.updateStatus(
                closedSession.getId(),
                RunningSessionStatus.CLOSED);
        assertThatThrownBy(() -> runningSessionParticipantService.joinSession(
                loginSession(second.getId()),
                closedSession.getId()))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SESSION_CLOSED));

        assertThat(runningSessionParticipantMapper.selectParticipants(
                runningSession.getId()))
                .allSatisfy(participant -> {
                    assertThat(participant.getMemberId()).isNotNull();
                    assertThat(participant.getJoinedAt()).isNotNull();
                });
    }

    @Test
    void concurrentJoinRequestsDoNotExceedCapacity() throws Exception {
        assertConcurrencyEnvironment();

        for (int attempt = 1; attempt <= 10; attempt++) {
            Member host = insertMember("concurrent_host_" + attempt);
            Member first = insertMember("concurrent_first_" + attempt);
            Member second = insertMember("concurrent_second_" + attempt);
            RunningSession runningSession = insertRunningSession(host.getId(), 1);

            List<ErrorCode> results = joinConcurrently(
                    runningSession.getId(),
                    List.of(first, second));
            List<RunningSessionParticipant> participants =
                    runningSessionParticipantMapper.selectParticipants(
                            runningSession.getId());

            assertSoftly(softly -> {
                softly.assertThat(results.stream().filter(Objects::isNull).count())
                        .isEqualTo(1);
                softly.assertThat(results)
                        .filteredOn(Objects::nonNull)
                        .containsExactly(ErrorCode.SESSION_FULL);
                softly.assertThat(participants).hasSize(1);
                softly.assertThat(participants)
                        .extracting(RunningSessionParticipant::getMemberId)
                        .allMatch(memberId -> memberId.equals(first.getId())
                                || memberId.equals(second.getId()));
            });
        }
    }

    @Test
    void threeConcurrentJoinRequestsRespectCapacityTwo() throws Exception {
        assertConcurrencyEnvironment();

        Member host = insertMember("capacity_two_host");
        Member first = insertMember("capacity_two_first");
        Member second = insertMember("capacity_two_second");
        Member third = insertMember("capacity_two_third");
        RunningSession runningSession = insertRunningSession(host.getId(), 2);

        List<ErrorCode> results = joinConcurrently(
                runningSession.getId(),
                List.of(first, second, third));
        List<RunningSessionParticipant> participants =
                runningSessionParticipantMapper.selectParticipants(
                        runningSession.getId());

        assertSoftly(softly -> {
            softly.assertThat(results.stream().filter(Objects::isNull).count())
                    .isEqualTo(2);
            softly.assertThat(results)
                    .filteredOn(Objects::nonNull)
                    .containsExactly(ErrorCode.SESSION_FULL);
            softly.assertThat(participants).hasSize(2);
        });
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

        List<Playlist> viewerPlaylists =
                playlistMapper.selectList(viewer.getId(), null);
        assertThat(viewerPlaylists)
                .extracting(Playlist::getTitle)
                .contains("Public Join Playlist")
                .doesNotContain("Private Join Playlist");
        assertThat(viewerPlaylists.stream()
                .filter(playlist -> playlist.getId().equals(publicPlaylist.getId()))
                .findFirst()
                .orElseThrow()
                .isSaved()).isTrue();
        assertThat(playlistMapper.selectList(null, null).stream()
                .filter(playlist -> playlist.getId().equals(publicPlaylist.getId()))
                .findFirst()
                .orElseThrow()
                .isSaved()).isFalse();
        assertThat(playlistItemMapper.selectByPlaylistId(publicPlaylist.getId()))
                .extracting(PlaylistItem::getMusicId)
                .containsExactly(spotifyMusic.getId(), seedMusic.getId());
        assertThat(playlistItemMapper.selectSpotifyUrisByPlaylistId(publicPlaylist.getId()))
                .containsExactly("spotify:track:mapper-join");
        assertThat(playlistSaveMapper.selectSavedList(viewer.getId()))
                .allMatch(Playlist::isSaved)
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
    void spotifyConnectionMapperInsertsAndRefreshesToken() {
        Member member = insertMember("spotify_connection_member");
        SpotifyConnection connection = new SpotifyConnection();
        connection.setMemberId(member.getId());
        connection.setSpotifyUserId("spotify-user");
        connection.setAccessToken("first-access");
        connection.setRefreshToken("first-refresh");
        connection.setTokenExpiresAt(java.time.LocalDateTime.now().plusHours(1));

        spotifyConnectionMapper.insert(connection);
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

    private RunningSession insertRunningSession(Long hostMemberId, int capacity) {
        RunningSession runningSession = new RunningSession();
        runningSession.setHostMemberId(hostMemberId);
        runningSession.setTitle("Participant Test Session");
        runningSession.setDescription("Participant mapper integration");
        runningSession.setMeetingPlace("Seoul");
        runningSession.setScheduledAt(
                java.time.LocalDateTime.of(2026, 7, 1, 9, 0));
        runningSession.setCapacity(capacity);
        runningSession.setStatus(RunningSessionStatus.OPEN);
        runningSessionMapper.insert(runningSession);
        return runningSession;
    }

    private MockHttpSession loginSession(Long memberId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, memberId);
        return session;
    }

    private ErrorCode joinAfterSignal(
            MockHttpSession session,
            Long runningSessionId,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            runningSessionParticipantService.joinSession(session, runningSessionId);
            return null;
        } catch (CustomException exception) {
            return exception.getErrorCode();
        }
    }

    private List<ErrorCode> joinConcurrently(
            Long runningSessionId,
            List<Member> members) throws Exception {
        CountDownLatch ready = new CountDownLatch(members.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(members.size());
        List<Future<ErrorCode>> futures = new ArrayList<>();

        try {
            for (Member member : members) {
                futures.add(executor.submit(() -> joinAfterSignal(
                        loginSession(member.getId()),
                        runningSessionId,
                        ready,
                        start)));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<ErrorCode> results = new ArrayList<>();
            for (Future<ErrorCode> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            }
        }
    }

    private void assertConcurrencyEnvironment() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class))
                .containsIgnoringCase("test");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT @@transaction_isolation",
                String.class))
                .isEqualToIgnoringCase("REPEATABLE-READ");
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT ENGINE
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'running_sessions'
                """,
                String.class))
                .isEqualToIgnoringCase("InnoDB");
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

    private void assertDedicatedDatabasePrivileges() {
        String expectedDatabase = TEST_DATABASE.databaseName().toLowerCase();
        List<String> grants = jdbcTemplate.queryForList(
                "SHOW GRANTS FOR CURRENT_USER()",
                String.class);

        for (String grant : grants) {
            String normalized = grant.toLowerCase()
                    .replace("`", "")
                    .replace("\\", "");
            boolean usageGrant = normalized.contains("grant usage on *.*");
            boolean dedicatedDatabaseGrant =
                    normalized.contains("on " + expectedDatabase + ".*");
            if (!usageGrant && !dedicatedDatabaseGrant) {
                throw new IllegalStateException(
                        "Mapper integration test account has privileges outside "
                                + "the dedicated database '"
                                + TEST_DATABASE.databaseName()
                                + "': "
                                + grant);
            }
        }
    }

    private void deleteAllData() {
        jdbcTemplate.update("DELETE FROM running_session_participants");
        jdbcTemplate.update("DELETE FROM running_sessions");
        jdbcTemplate.update("DELETE FROM running_records");
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
