package com.ssafy.ssafit.runningrecord.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.service.AuthService;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class RunningRecordServiceTest {

    @Mock
    private RunningRecordMapper runningRecordMapper;

    @Mock
    private AuthService authService;

    @Mock
    private HttpSession session;

    private RunningRecordService runningRecordService;

    @BeforeEach
    void setUp() {
        runningRecordService = new RunningRecordService(runningRecordMapper, authService);
    }

    @Test
    void createRecordUsesLoginMemberAndCalculatesPace() {
        mockLoginMember(10L);
        RunningRecord input = validRecord(null, 999L);
        input.setPaceSecondsPerKm(1);
        when(runningRecordMapper.insert(input)).thenAnswer(invocation -> {
            input.setId(100L);
            return 1;
        });

        RunningRecord created = runningRecordService.createRecord(session, input);

        assertThat(created.getId()).isEqualTo(100L);
        assertThat(created.getMemberId()).isEqualTo(10L);
        assertThat(created.getPaceSecondsPerKm()).isEqualTo(360);
        verify(runningRecordMapper).insert(input);
    }

    @Test
    void unauthenticatedMemberCannotCreateRecord() {
        when(authService.getLoginMember(session))
                .thenThrow(new CustomException(ErrorCode.AUTH_REQUIRED));

        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> runningRecordService.createRecord(
                        session,
                        validRecord(null, null)));

        verify(runningRecordMapper, never()).insert(any());
    }

    @Test
    void unauthenticatedMemberCannotReadRecordList() {
        when(authService.getLoginMember(session))
                .thenThrow(new CustomException(ErrorCode.AUTH_REQUIRED));

        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> runningRecordService.getMyRecordList(session));

        verify(runningRecordMapper, never()).selectListByMemberId(any());
    }

    @Test
    void getMyRecordListUsesLoginMemberId() {
        mockLoginMember(10L);
        RunningRecord record = validRecord(100L, 10L);
        when(runningRecordMapper.selectListByMemberId(10L))
                .thenReturn(List.of(record));

        List<RunningRecord> result = runningRecordService.getMyRecordList(session);

        assertThat(result).containsExactly(record);
        verify(runningRecordMapper).selectListByMemberId(10L);
    }

    @Test
    void getMyRecordDetailReturnsOwnRecord() {
        mockLoginMember(10L);
        RunningRecord record = validRecord(100L, 10L);
        when(runningRecordMapper.selectById(100L)).thenReturn(Optional.of(record));

        RunningRecord result =
                runningRecordService.getMyRecordDetail(session, 100L);

        assertThat(result).isSameAs(record);
    }

    @Test
    void getMyRecordDetailRejectsMissingRecord() {
        mockLoginMember(10L);
        when(runningRecordMapper.selectById(100L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.RUNNING_RECORD_NOT_FOUND,
                () -> runningRecordService.getMyRecordDetail(session, 100L));
    }

    @Test
    void getMyRecordDetailRejectsOtherMemberRecord() {
        mockLoginMember(10L);
        RunningRecord record = validRecord(100L, 20L);
        when(runningRecordMapper.selectById(100L)).thenReturn(Optional.of(record));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> runningRecordService.getMyRecordDetail(session, 100L));
    }

    @Test
    void updateMyRecordSucceedsForOwner() {
        mockLoginMember(10L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 12, 0);
        RunningRecord existing = validRecord(100L, 10L);
        existing.setCreatedAt(createdAt);
        RunningRecord changes = validRecord(100L, 999L);
        changes.setDistanceKm(new BigDecimal("10.00"));
        changes.setDurationSeconds(3300);
        when(runningRecordMapper.selectById(100L))
                .thenReturn(Optional.of(existing));
        when(runningRecordMapper.update(changes)).thenReturn(1);

        RunningRecord updated =
                runningRecordService.updateMyRecord(session, changes);

        assertThat(updated.getMemberId()).isEqualTo(10L);
        assertThat(updated.getPaceSecondsPerKm()).isEqualTo(330);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        verify(runningRecordMapper).update(changes);
    }

    @Test
    void updateMyRecordRejectsOtherMemberRecord() {
        mockLoginMember(10L);
        RunningRecord existing = validRecord(100L, 20L);
        when(runningRecordMapper.selectById(100L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> runningRecordService.updateMyRecord(
                        session,
                        validRecord(100L, 10L)));

        verify(runningRecordMapper, never()).update(any());
    }

    @Test
    void updateMyRecordRejectsMissingRecord() {
        mockLoginMember(10L);
        when(runningRecordMapper.selectById(100L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.RUNNING_RECORD_NOT_FOUND,
                () -> runningRecordService.updateMyRecord(
                        session,
                        validRecord(100L, 10L)));

        verify(runningRecordMapper, never()).update(any());
    }

    @Test
    void updateMyRecordRejectsConcurrentSoftDelete() {
        mockLoginMember(10L);
        RunningRecord existing = validRecord(100L, 10L);
        RunningRecord changes = validRecord(100L, 10L);
        when(runningRecordMapper.selectById(100L))
                .thenReturn(Optional.of(existing));
        when(runningRecordMapper.update(changes)).thenReturn(0);

        assertError(
                ErrorCode.RUNNING_RECORD_NOT_FOUND,
                () -> runningRecordService.updateMyRecord(session, changes));
    }

    @Test
    void deleteMyRecordSucceedsForOwner() {
        mockLoginMember(10L);
        RunningRecord existing = validRecord(100L, 10L);
        when(runningRecordMapper.selectById(100L))
                .thenReturn(Optional.of(existing));
        when(runningRecordMapper.delete(100L)).thenReturn(1);

        runningRecordService.deleteMyRecord(session, 100L);

        verify(runningRecordMapper).delete(100L);
    }

    @Test
    void deleteMyRecordRejectsConcurrentSoftDelete() {
        mockLoginMember(10L);
        RunningRecord existing = validRecord(100L, 10L);
        when(runningRecordMapper.selectById(100L))
                .thenReturn(Optional.of(existing));
        when(runningRecordMapper.delete(100L)).thenReturn(0);

        assertError(
                ErrorCode.RUNNING_RECORD_NOT_FOUND,
                () -> runningRecordService.deleteMyRecord(session, 100L));
    }

    @Test
    void deleteMyRecordRejectsOtherMemberRecord() {
        mockLoginMember(10L);
        RunningRecord existing = validRecord(100L, 20L);
        when(runningRecordMapper.selectById(100L))
                .thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> runningRecordService.deleteMyRecord(session, 100L));

        verify(runningRecordMapper, never()).delete(100L);
    }

    @Test
    void deleteMyRecordRejectsMissingRecord() {
        mockLoginMember(10L);
        when(runningRecordMapper.selectById(100L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.RUNNING_RECORD_NOT_FOUND,
                () -> runningRecordService.deleteMyRecord(session, 100L));

        verify(runningRecordMapper, never()).delete(100L);
    }

    @Test
    void createRecordRejectsZeroDistance() {
        mockLoginMember(10L);
        RunningRecord input = validRecord(null, null);
        input.setDistanceKm(BigDecimal.ZERO);

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningRecordService.createRecord(session, input));

        verify(runningRecordMapper, never()).insert(any());
    }

    @Test
    void createRecordRejectsNegativeDistance() {
        mockLoginMember(10L);
        RunningRecord input = validRecord(null, null);
        input.setDistanceKm(new BigDecimal("-1.00"));

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningRecordService.createRecord(session, input));
    }

    @Test
    void createRecordRejectsZeroDuration() {
        mockLoginMember(10L);
        RunningRecord input = validRecord(null, null);
        input.setDurationSeconds(0);

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningRecordService.createRecord(session, input));

        verify(runningRecordMapper, never()).insert(any());
    }

    @Test
    void createRecordRejectsNegativeDuration() {
        mockLoginMember(10L);
        RunningRecord input = validRecord(null, null);
        input.setDurationSeconds(-1);

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningRecordService.createRecord(session, input));
    }

    @Test
    void createRecordRejectsMissingRecordDate() {
        mockLoginMember(10L);
        RunningRecord input = validRecord(null, null);
        input.setRecordDate(null);

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningRecordService.createRecord(session, input));
    }

    @Test
    void createRecordRejectsMemoOverLimit() {
        mockLoginMember(10L);
        RunningRecord input = validRecord(null, null);
        input.setMemo("a".repeat(1001));

        assertError(
                ErrorCode.INVALID_REQUEST,
                () -> runningRecordService.createRecord(session, input));
    }

    @Test
    void createRecordAcceptsLegacyRequestWithoutCadenceAndHeartRate() {
        mockLoginMember(10L);
        RunningRecord input = validRecord(null, null);
        when(runningRecordMapper.insert(input)).thenReturn(1);

        RunningRecord created = runningRecordService.createRecord(session, input);

        assertThat(created.getAverageCadence()).isNull();
        assertThat(created.getAverageHeartRateBpm()).isNull();
    }

    @Test
    void createRecordAcceptsCadenceAndHeartRateBoundaryValues() {
        mockLoginMember(10L);
        when(runningRecordMapper.insert(any())).thenReturn(1);
        RunningRecord minimum = validRecord(null, null);
        minimum.setAverageCadence(60);
        minimum.setAverageHeartRateBpm(30);

        RunningRecord createdMinimum =
                runningRecordService.createRecord(session, minimum);
        assertThat(createdMinimum.getAverageCadence()).isEqualTo(60);
        assertThat(createdMinimum.getAverageHeartRateBpm()).isEqualTo(30);

        RunningRecord maximum = validRecord(null, null);
        maximum.setAverageCadence(240);
        maximum.setAverageHeartRateBpm(240);
        RunningRecord createdMaximum =
                runningRecordService.createRecord(session, maximum);
        assertThat(createdMaximum.getAverageCadence()).isEqualTo(240);
        assertThat(createdMaximum.getAverageHeartRateBpm()).isEqualTo(240);
    }

    @Test
    void createRecordRejectsCadenceOutsideBoundaries() {
        mockLoginMember(10L);
        for (int invalid : new int[] {59, 241}) {
            RunningRecord input = validRecord(null, null);
            input.setAverageCadence(invalid);
            assertError(
                    ErrorCode.INVALID_CADENCE_VALUE,
                    () -> runningRecordService.createRecord(session, input));
        }
    }

    @Test
    void createRecordRejectsHeartRateOutsideBoundaries() {
        mockLoginMember(10L);
        for (int invalid : new int[] {29, 241}) {
            RunningRecord input = validRecord(null, null);
            input.setAverageHeartRateBpm(invalid);
            assertError(
                    ErrorCode.INVALID_HEART_RATE_VALUE,
                    () -> runningRecordService.createRecord(session, input));
        }
    }

    @Test
    void updateMyRecordPersistsCadenceAndHeartRateChanges() {
        mockLoginMember(10L);
        RunningRecord existing = validRecord(100L, 10L);
        RunningRecord changes = validRecord(100L, 10L);
        changes.setAverageCadence(172);
        changes.setAverageHeartRateBpm(154);
        when(runningRecordMapper.selectById(100L))
                .thenReturn(Optional.of(existing));
        when(runningRecordMapper.update(changes)).thenReturn(1);

        RunningRecord updated =
                runningRecordService.updateMyRecord(session, changes);

        assertThat(updated.getAverageCadence()).isEqualTo(172);
        assertThat(updated.getAverageHeartRateBpm()).isEqualTo(154);
        verify(runningRecordMapper).update(changes);
    }

    private void mockLoginMember(Long memberId) {
        Member member = new Member();
        member.setId(memberId);
        when(authService.getLoginMember(session)).thenReturn(member);
    }

    private RunningRecord validRecord(Long id, Long memberId) {
        RunningRecord runningRecord = new RunningRecord();
        runningRecord.setId(id);
        runningRecord.setMemberId(memberId);
        runningRecord.setRecordDate(LocalDate.of(2026, 6, 20));
        runningRecord.setDistanceKm(new BigDecimal("5.00"));
        runningRecord.setDurationSeconds(1800);
        runningRecord.setMemo("저녁 러닝");
        return runningRecord;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
