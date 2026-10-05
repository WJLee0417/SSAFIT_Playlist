package com.ssafy.ssafit.runningsession.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import com.ssafy.ssafit.common.auth.SessionConst;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.mapper.DatabaseIntegrationTestSupport;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;
import com.ssafy.ssafit.runningsession.mapper.RunningSessionMapper;
import com.ssafy.ssafit.runningsession.mapper.RunningSessionParticipantMapper;

class RunningSessionConcurrencyIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private RunningSessionMapper runningSessionMapper;

    @Autowired
    private RunningSessionParticipantMapper runningSessionParticipantMapper;

    @Autowired
    private RunningSessionParticipantService runningSessionParticipantService;

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

    private RunningSession insertRunningSession(Long hostMemberId, int capacity) {
        RunningSession runningSession = new RunningSession();
        runningSession.setHostMemberId(hostMemberId);
        runningSession.setTitle("Participant Test Session");
        runningSession.setDescription("Participant mapper integration");
        runningSession.setMeetingPlace("Seoul");
        runningSession.setScheduledAt(
                java.time.LocalDateTime.of(2026, 7, 1, 9, 0));
        runningSession.setCapacity(capacity);
        runningSession.setTargetDistanceKm(new java.math.BigDecimal("10.00"));
        runningSession.setTargetPaceSecondsPerKm(330);
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
}
