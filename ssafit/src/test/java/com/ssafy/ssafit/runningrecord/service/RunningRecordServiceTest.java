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
    void deleteMyRecordSucceedsForOwner() {
        mockLoginMember(10L);
        RunningRecord existing = validRecord(100L, 10L);
        when(runningRecordMapper.selectById(100L))
                .thenReturn(Optional.of(existing));

        runningRecordService.deleteMyRecord(session, 100L);

        verify(runningRecordMapper).delete(100L);
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
