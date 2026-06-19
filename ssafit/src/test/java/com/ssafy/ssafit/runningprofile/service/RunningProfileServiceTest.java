package com.ssafy.ssafit.runningprofile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;

@ExtendWith(MockitoExtension.class)
class RunningProfileServiceTest {

    @Mock
    private RunningProfileMapper runningProfileMapper;

    @Mock
    private MemberMapper memberMapper;

    private RunningProfileService runningProfileService;

    @BeforeEach
    void setUp() {
        runningProfileService = new RunningProfileService(runningProfileMapper, memberMapper);
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
        when(memberMapper.selectById(1L)).thenReturn(Optional.of(new Member()));
        when(runningProfileMapper.selectByMemberId(1L)).thenReturn(Optional.empty());
        when(runningProfileMapper.insert(profile)).thenAnswer(invocation -> {
            profile.setId(10L);
            return 1;
        });

        RunningProfile saved = runningProfileService.saveOrUpdate(1L, profile);

        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getId()).isEqualTo(10L);
        verify(runningProfileMapper).insert(profile);
        verify(runningProfileMapper, never()).update(profile);
    }

    @Test
    void saveOrUpdateUpdatesWhenProfileAlreadyExists() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 19, 12, 0);
        RunningProfile existing = validProfile(1L);
        existing.setId(10L);
        existing.setCreatedAt(createdAt);
        RunningProfile changed = validProfile(1L);
        changed.setRunningIntensity(RunningIntensity.HIGH);

        when(memberMapper.selectById(1L)).thenReturn(Optional.of(new Member()));
        when(runningProfileMapper.selectByMemberId(1L)).thenReturn(Optional.of(existing));

        RunningProfile saved = runningProfileService.saveOrUpdate(1L, changed);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getRunningIntensity()).isEqualTo(RunningIntensity.HIGH);
        verify(runningProfileMapper).update(changed);
        verify(runningProfileMapper, never()).insert(changed);
    }

    @Test
    void saveOrUpdateRejectsInvalidBpmRange() {
        RunningProfile profile = validProfile(1L);
        profile.setTargetBpmMin(180);
        profile.setTargetBpmMax(120);
        when(memberMapper.selectById(1L)).thenReturn(Optional.of(new Member()));

        assertError(
                ErrorCode.INVALID_PROFILE_VALUE,
                () -> runningProfileService.saveOrUpdate(1L, profile));
    }

    private RunningProfile validProfile(Long memberId) {
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(memberId);
        profile.setRunningIntensity(RunningIntensity.MODERATE);
        profile.setTargetBpmMin(120);
        profile.setTargetBpmMax(160);
        profile.setPreferredGenre("POP");
        return profile;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
