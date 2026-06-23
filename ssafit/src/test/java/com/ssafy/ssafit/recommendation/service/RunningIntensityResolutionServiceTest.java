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

import com.ssafy.ssafit.recommendation.domain.IntensityResolutionResult;
import com.ssafy.ssafit.recommendation.domain.IntensitySource;
import com.ssafy.ssafit.recommendation.domain.RunningIntensityGoal;
import com.ssafy.ssafit.runningrecord.mapper.RunningRecordMapper;

@ExtendWith(MockitoExtension.class)
class RunningIntensityResolutionServiceTest {

    @Mock
    private RunningRecordMapper runningRecordMapper;

    @Test
    void userSelectionHasPriorityOverHeartRateHistory() {
        RunningIntensityResolutionService service =
                new RunningIntensityResolutionService(runningRecordMapper);

        IntensityResolutionResult result =
                service.resolve(1L, RunningIntensityGoal.HIGH);

        assertThat(result.resolvedGoal()).isEqualTo(RunningIntensityGoal.HIGH);
        assertThat(result.source()).isEqualTo(IntensitySource.USER_SELECTED);
        verify(runningRecordMapper, never())
                .selectRecentAverageHeartRates(1L, 5);
    }

    @Test
    void fewerThanFiveHeartRateRecordsRemainUnknown() {
        when(runningRecordMapper.selectRecentAverageHeartRates(1L, 5))
                .thenReturn(List.of(130, 132, 135, 138));
        RunningIntensityResolutionService service =
                new RunningIntensityResolutionService(runningRecordMapper);

        IntensityResolutionResult result = service.resolve(1L, null);

        assertThat(result.resolvedGoal()).isNull();
        assertThat(result.source()).isEqualTo(IntensitySource.UNKNOWN);
        assertThat(result.sufficientHistory()).isFalse();
    }

    @Test
    void sufficientHistoryIsReferenceOnlyAndNotMedicalDiagnosis() {
        when(runningRecordMapper.selectRecentAverageHeartRates(1L, 5))
                .thenReturn(List.of(150, 151, 152, 153, 154));
        RunningIntensityResolutionService service =
                new RunningIntensityResolutionService(runningRecordMapper);

        IntensityResolutionResult result = service.resolve(1L, null);

        assertThat(result.resolvedGoal())
                .isEqualTo(RunningIntensityGoal.MODERATE);
        assertThat(result.source())
                .isEqualTo(IntensitySource.HISTORY_REFERENCE);
        assertThat(result.sufficientHistory()).isTrue();
        assertThat(result.explanation())
                .contains("의료적 진단이 아니")
                .contains("음악 템포 BPM과도 별개");
    }
}
