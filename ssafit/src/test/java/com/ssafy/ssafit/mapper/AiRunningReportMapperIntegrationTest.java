package com.ssafy.ssafit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.ssafy.ssafit.airunningreport.domain.AiRunningReport;
import com.ssafy.ssafit.airunningreport.domain.AiRunningReportSource;
import com.ssafy.ssafit.airunningreport.domain.AiRunningReportStatus;
import com.ssafy.ssafit.airunningreport.domain.AiRunningReportType;
import com.ssafy.ssafit.airunningreport.mapper.AiRunningReportMapper;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.mapper.MemberMapper;

@SpringBootTest
@ActiveProfiles("mapper-test")
class AiRunningReportMapperIntegrationTest {

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
    private AiRunningReportMapper aiRunningReportMapper;

    @BeforeEach
    void cleanBeforeEachTest() {
        deleteData();
    }

    @AfterEach
    void cleanAfterEachTest() {
        deleteData();
    }

    @Test
    void insertAndSelectLatestMapsJsonEnumsAndDates() {
        Member member = insertMember("ai_report_member");
        AiRunningReport report = completedReport(member.getId(), hash("a"));
        report.setAnalysisStartDate(LocalDate.of(2026, 6, 1));
        report.setAnalysisEndDate(LocalDate.of(2026, 6, 24));

        assertThat(aiRunningReportMapper.insert(report)).isEqualTo(1);

        AiRunningReport selected = aiRunningReportMapper
                .selectLatestByMemberIdAndType(
                        member.getId(),
                        AiRunningReportType.RUNNING_SUMMARY)
                .orElseThrow();
        assertThat(selected.getId()).isEqualTo(report.getId());
        assertThat(selected.getReportType()).isEqualTo(AiRunningReportType.RUNNING_SUMMARY);
        assertThat(selected.getStatus()).isEqualTo(AiRunningReportStatus.COMPLETED);
        assertThat(selected.getGenerationSource()).isEqualTo(AiRunningReportSource.LLM);
        assertThat(selected.getAnalysisStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(selected.getAnalysisEndDate()).isEqualTo(LocalDate.of(2026, 6, 24));
        assertThat(selected.getInputSnapshotJson()).contains("\"recordCount\": 8");
        assertThat(selected.getReportContentJson()).contains("\"summary\": \"ok\"");
        assertThat(selected.getCreatedAt()).isNotNull();
        assertThat(selected.getUpdatedAt()).isNotNull();
    }

    @Test
    void latestReportIsIsolatedByMemberAndUsesNewestRow() {
        Member first = insertMember("ai_report_first");
        Member second = insertMember("ai_report_second");
        AiRunningReport older = completedReport(first.getId(), hash("a"));
        AiRunningReport latest = completedReport(first.getId(), hash("b"));
        AiRunningReport otherMember = completedReport(second.getId(), hash("c"));
        aiRunningReportMapper.insert(older);
        aiRunningReportMapper.insert(otherMember);
        aiRunningReportMapper.insert(latest);

        assertThat(aiRunningReportMapper.selectLatestByMemberIdAndType(
                first.getId(),
                AiRunningReportType.RUNNING_SUMMARY)
                .orElseThrow()
                .getInputSnapshotHash()).isEqualTo(hash("b"));
        assertThat(aiRunningReportMapper.selectLatestByMemberIdAndType(
                second.getId(),
                AiRunningReportType.RUNNING_SUMMARY)
                .orElseThrow()
                .getInputSnapshotHash()).isEqualTo(hash("c"));
    }

    @Test
    void reusableReportIgnoresFailedRowsAndReturnsLatestCompletedSnapshot() {
        Member member = insertMember("ai_report_reuse");
        AiRunningReport completed = completedReport(member.getId(), hash("a"));
        AiRunningReport failed = failedReport(member.getId(), hash("a"));
        aiRunningReportMapper.insert(completed);
        aiRunningReportMapper.insert(failed);

        AiRunningReport reusable = aiRunningReportMapper
                .selectLatestReusableBySnapshot(
                        member.getId(),
                        AiRunningReportType.RUNNING_SUMMARY,
                        hash("a"))
                .orElseThrow();

        assertThat(reusable.getStatus()).isEqualTo(AiRunningReportStatus.COMPLETED);
        assertThat(reusable.getId()).isEqualTo(completed.getId());
    }

    @Test
    void statusQuerySeparatesCompletedAndFailedReports() {
        Member member = insertMember("ai_report_status");
        AiRunningReport completed = completedReport(member.getId(), hash("a"));
        AiRunningReport failed = failedReport(member.getId(), hash("b"));
        aiRunningReportMapper.insert(completed);
        aiRunningReportMapper.insert(failed);

        List<AiRunningReport> completedReports =
                aiRunningReportMapper.selectByMemberIdTypeAndStatus(
                        member.getId(),
                        AiRunningReportType.RUNNING_SUMMARY,
                        AiRunningReportStatus.COMPLETED);
        List<AiRunningReport> failedReports =
                aiRunningReportMapper.selectByMemberIdTypeAndStatus(
                        member.getId(),
                        AiRunningReportType.RUNNING_SUMMARY,
                        AiRunningReportStatus.FAILED);

        assertThat(completedReports).extracting(AiRunningReport::getId)
                .containsExactly(completed.getId());
        assertThat(failedReports).extracting(AiRunningReport::getId)
                .containsExactly(failed.getId());
        assertThat(failedReports.getFirst().getReportContentJson()).isNull();
        assertThat(failedReports.getFirst().getErrorCode()).isEqualTo("LLM_TIMEOUT");
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

    private AiRunningReport completedReport(Long memberId, String hash) {
        AiRunningReport report = new AiRunningReport();
        report.setMemberId(memberId);
        report.setReportType(AiRunningReportType.RUNNING_SUMMARY);
        report.setAnalysisStartDate(LocalDate.of(2026, 6, 1));
        report.setAnalysisEndDate(LocalDate.of(2026, 6, 24));
        report.setInputSnapshotHash(hash);
        report.setInputSnapshotJson("{\"recordCount\": 8}");
        report.setStatus(AiRunningReportStatus.COMPLETED);
        report.setGenerationSource(AiRunningReportSource.LLM);
        report.setProvider("openai");
        report.setModelName("test-model");
        report.setPromptVersion("v1");
        report.setReportContentJson("{\"summary\": \"ok\"}");
        return report;
    }

    private AiRunningReport failedReport(Long memberId, String hash) {
        AiRunningReport report = completedReport(memberId, hash);
        report.setStatus(AiRunningReportStatus.FAILED);
        report.setGenerationSource(AiRunningReportSource.LLM);
        report.setReportContentJson(null);
        report.setErrorCode("LLM_TIMEOUT");
        return report;
    }

    private String hash(String seed) {
        return seed.repeat(64).substring(0, 64);
    }

    private void deleteData() {
        jdbcTemplate.update("DELETE FROM ai_running_reports");
        jdbcTemplate.update("DELETE FROM members");
    }
}
