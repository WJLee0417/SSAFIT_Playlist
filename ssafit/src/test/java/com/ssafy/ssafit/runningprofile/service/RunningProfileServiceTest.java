package com.ssafy.ssafit.runningprofile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.ssafy.ssafit.member.mapper.MemberMapper;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

@ExtendWith(MockitoExtension.class)
class RunningProfileServiceTest {

    @Mock
    private RunningProfileMapper runningProfileMapper;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private RunningRecordMapper runningRecordMapper;

    private RunningProfileService runningProfileService;

    @BeforeEach
    void setUp() {
        runningProfileService = new RunningProfileService(
                runningProfileMapper,
                memberMapper,
                runningRecordMapper);
    }

    @Test
    void getProfileRejectsOtherMemberAccess() {
        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> runningProfileService.getProfile(1L, 2L));
    }

    @Test
    void getProfileThrowsWhenProfileDoesNotExist() {
        when(memberMapper.selectById(1L)).thenReturn(Optional.of(new Member()));
        when(runningProfileMapper.selectByMemberId(1L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.PROFILE_NOT_FOUND,
                () -> runningProfileService.getProfile(1L, 1L));
    }

    @Test
    void saveOrUpdateInsertsWhenProfileDoesNotExist() {
        RunningProfile profile = validProfile(null);
        profile.setAge(32);
        when(memberMapper.selectById(1L)).thenReturn(Optional.of(new Member()));
        when(runningProfileMapper.selectByMemberId(1L)).thenReturn(Optional.empty());
        when(runningProfileMapper.insert(profile)).thenAnswer(invocation -> {
            profile.setId(10L);
            return 1;
        });

        RunningProfile saved = runningProfileService.saveOrUpdate(1L, profile);

        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getEstimatedMaxHeartRateBpm()).isEqualTo(188);
        verify(runningProfileMapper).insert(profile);
        verify(runningProfileMapper, never()).update(profile);
    }

    @Test
    void saveOrUpdateRejectsInvalidAge() {
        RunningProfile profile = validProfile(1L);
        profile.setAge(101);
        when(memberMapper.selectById(1L)).thenReturn(Optional.of(new Member()));

        assertError(
                ErrorCode.INVALID_PROFILE_VALUE,
                () -> runningProfileService.saveOrUpdate(1L, profile));
    }

    @Test
    void saveOrUpdateUpdatesWhenProfileAlreadyExistsAndPreservesAutoTargets() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 19, 12, 0);
        RunningProfile existing = validProfile(1L);
        existing.setId(10L);
        existing.setCreatedAt(createdAt);
        existing.setTargetBpmMin(120);
        existing.setTargetBpmMax(160);
        existing.setTargetCadenceMin(168);
        existing.setTargetCadenceMax(180);
        RunningProfile changed = validProfile(1L);
        changed.setPreferredGenre("ROCK");

        when(memberMapper.selectById(1L)).thenReturn(Optional.of(new Member()));
        when(runningProfileMapper.selectByMemberId(1L)).thenReturn(Optional.of(existing));
        when(runningProfileMapper.update(changed)).thenReturn(1);

        RunningProfile saved = runningProfileService.saveOrUpdate(1L, changed);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getPreferredGenre()).isEqualTo("ROCK");
        assertThat(saved.getTargetBpmMin()).isEqualTo(120);
        assertThat(saved.getTargetBpmMax()).isEqualTo(160);
        assertThat(saved.getTargetCadenceMin()).isEqualTo(168);
        assertThat(saved.getTargetCadenceMax()).isEqualTo(180);
        verify(runningProfileMapper).update(changed);
        verify(runningProfileMapper, never()).insert(changed);
    }

    @Test
    void refreshAutoTargetsUsesRecentTenRecords() {
        when(runningRecordMapper.selectRecentByMemberId(1L, 10))
                .thenReturn(List.of(
                        record(151, 170),
                        record(162, 178),
                        record(null, 172),
                        record(145, null)));

        runningProfileService.refreshAutoTargets(1L);

        verify(runningProfileMapper).updateAutoTargets(
                1L,
                145,
                162,
                170,
                178);
    }

    @Test
    void refreshAutoTargetsClearsRangesWhenNoVitalsExist() {
        when(runningRecordMapper.selectRecentByMemberId(1L, 10))
                .thenReturn(List.of(record(null, null)));

        runningProfileService.refreshAutoTargets(1L);

        verify(runningProfileMapper).updateAutoTargets(
                1L,
                null,
                null,
                null,
                null);
    }

    private RunningProfile validProfile(Long memberId) {
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(memberId);
        profile.setPreferredGenre("POP");
        return profile;
    }

    private RunningRecord record(Integer heartRate, Integer cadence) {
        RunningRecord record = new RunningRecord();
        record.setRecordDate(LocalDate.of(2026, 6, 20));
        record.setDistanceKm(new BigDecimal("5.00"));
        record.setDurationSeconds(1800);
        record.setAverageHeartRateBpm(heartRate);
        record.setAverageCadence(cadence);
        return record;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
