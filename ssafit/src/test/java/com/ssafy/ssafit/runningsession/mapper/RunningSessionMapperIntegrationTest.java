package com.ssafy.ssafit.runningsession.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import com.ssafy.ssafit.common.auth.SessionConst;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.mapper.DatabaseIntegrationTestSupport;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionParticipant;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;
import com.ssafy.ssafit.runningsession.service.RunningSessionParticipantService;

class RunningSessionMapperIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private RunningSessionMapper runningSessionMapper;

    @Autowired
    private RunningSessionParticipantMapper runningSessionParticipantMapper;

    @Autowired
    private RunningSessionParticipantService runningSessionParticipantService;

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
        runningSession.setTargetDistanceKm(new java.math.BigDecimal("10.00"));
        runningSession.setTargetPaceSecondsPerKm(330);
        runningSession.setStatus(RunningSessionStatus.OPEN);

        runningSessionMapper.insert(runningSession);

        assertThat(runningSession.getId()).isNotNull();
        assertThat(runningSessionMapper.selectHostId(runningSession.getId()))
                .isEqualTo(host.getId());
        assertThat(runningSessionMapper.selectList(host.getId()))
                .extracting(RunningSession::getId)
                .contains(runningSession.getId());
        assertThat(runningSessionMapper.selectList(host.getId()))
                .filteredOn(session -> runningSession.getId().equals(session.getId()))
                .first()
                .satisfies(session -> {
                    assertThat(session.getParticipantCount()).isZero();
                    assertThat(session.getCurrentMemberHosting()).isTrue();
                });
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
}
