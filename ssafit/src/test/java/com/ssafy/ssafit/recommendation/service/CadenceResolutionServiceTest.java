package com.ssafy.ssafit.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.recommendation.domain.CadenceResolutionResult;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

@ExtendWith(MockitoExtension.class)
class CadenceResolutionServiceTest {

    @Mock
    private RunningRecordMapper runningRecordMapper;

    @Test
    void requestedCadenceHasPriority() {
        CadenceResolutionService service =
                new CadenceResolutionService(runningRecordMapper);

        CadenceResolutionResult result = service.resolve(1L, 172, null);

        assertThat(result.targetCadenceSpm()).isEqualTo(172);
        assertThat(result.source()).isEqualTo("USER_SELECTED");
        verify(runningRecordMapper, never())
                .selectRecentAverageCadences(1L, 5);
    }

    @Test
    void profileRangeUsesFloorMidpointConsistently() {
        RunningProfile profile = new RunningProfile();
        profile.setTargetCadenceMin(165);
        profile.setTargetCadenceMax(180);
        CadenceResolutionService service =
                new CadenceResolutionService(runningRecordMapper);

        CadenceResolutionResult result =
                service.resolve(1L, null, profile);

        assertThat(result.targetCadenceSpm()).isEqualTo(172);
        assertThat(result.source()).isEqualTo("PROFILE_MIDPOINT");
    }

    @Test
    void recentHistoryUsesMedianAndIgnoresNulls() {
        when(runningRecordMapper.selectRecentAverageCadences(1L, 5))
                .thenReturn(java.util.Arrays.asList(180, null, 168, 172));
        CadenceResolutionService service =
                new CadenceResolutionService(runningRecordMapper);

        CadenceResolutionResult result =
                service.resolve(1L, null, null);

        assertThat(result.targetCadenceSpm()).isEqualTo(172);
        assertThat(result.source()).isEqualTo("RECENT_HISTORY_MEDIAN");
    }

    @Test
    void noCadenceSourceLeavesMatchingDisabled() {
        when(runningRecordMapper.selectRecentAverageCadences(1L, 5))
                .thenReturn(List.of());
        CadenceResolutionService service =
                new CadenceResolutionService(runningRecordMapper);

        CadenceResolutionResult result =
                service.resolve(1L, null, null);

        assertThat(result.targetCadenceSpm()).isNull();
        assertThat(result.source()).isEqualTo("UNKNOWN");
    }
}
