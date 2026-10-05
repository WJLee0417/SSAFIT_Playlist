package com.ssafy.ssafit.mapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.mapper.MemberMapper;

@SpringBootTest
@ActiveProfiles("mapper-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class DatabaseIntegrationTestSupport {

    private static final TestDatabaseGuard.TestDatabaseConfig TEST_DATABASE =
            TestDatabaseGuard.requireFromEnvironment();

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TEST_DATABASE::jdbcUrl);
        registry.add("spring.datasource.username", TEST_DATABASE::username);
        registry.add("spring.datasource.password", TEST_DATABASE::password);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected MemberMapper memberMapper;

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

    protected Member insertMember(String loginId) {
        Member member = new Member();
        member.setLoginId(loginId);
        member.setPasswordHash("test-hash");
        member.setEmail(loginId + "@example.test");
        member.setNickname(loginId);
        member.setStatus(MemberStatus.ACTIVE);
        memberMapper.insert(member);
        return member;
    }

    protected int countRows(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Integer.class);
    }

    private void assertDedicatedDatabasePrivileges() {
        String expectedDatabase = TEST_DATABASE.databaseName().toLowerCase();
        java.util.List<String> grants = jdbcTemplate.queryForList(
                "SHOW GRANTS FOR CURRENT_USER()",
                String.class);

        for (String grant : grants) {
            String normalized = grant.toLowerCase()
                    .replace("\`", "")
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
