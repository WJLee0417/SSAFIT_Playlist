package com.ssafy.ssafit.runningrecord.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.common.exception.GlobalExceptionHandler;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.service.RunningRecordService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class RunningRecordApiControllerTest {

    @Mock
    private RunningRecordService runningRecordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RunningRecordApiController(runningRecordService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createUsesSessionAndReturnsSafeResponseDto() throws Exception {
        RunningRecord created = record(7L);
        when(runningRecordService.createRecord(
                any(HttpSession.class),
                any(RunningRecord.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/running-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": "2026-06-22",
                                  "distanceKm": 5.25,
                                  "durationSeconds": 1800,
                                  "memo": "저녁 러닝",
                                  "memberId": 999,
                                  "paceSecondsPerKm": 1,
                                  "deletedAt": "2026-06-22T00:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.paceSecondsPerKm").value(343))
                .andExpect(jsonPath("$.data.memberId").doesNotExist())
                .andExpect(jsonPath("$.data.deletedAt").doesNotExist());

        ArgumentCaptor<RunningRecord> captor =
                ArgumentCaptor.forClass(RunningRecord.class);
        verify(runningRecordService).createRecord(
                any(HttpSession.class),
                captor.capture());
        RunningRecord requestDomain = captor.getValue();
        assertThat(requestDomain.getMemberId()).isNull();
        assertThat(requestDomain.getPaceSecondsPerKm()).isNull();
        assertThat(requestDomain.getDeletedAt()).isNull();
    }

    @Test
    void listReturnsOnlyResponseDtoFields() throws Exception {
        when(runningRecordService.getMyRecordList(any(HttpSession.class)))
                .thenReturn(List.of(record(7L)));

        mockMvc.perform(get("/api/running-records/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].recordDate").value("2026-06-22"))
                .andExpect(jsonPath("$.data[0].memberId").doesNotExist())
                .andExpect(jsonPath("$.data[0].deletedAt").doesNotExist());
    }

    @Test
    void detailDelegatesOwnershipCheckToService() throws Exception {
        when(runningRecordService.getMyRecordDetail(
                any(HttpSession.class),
                eq(7L)))
                .thenReturn(record(7L));

        mockMvc.perform(get("/api/running-records/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));

        verify(runningRecordService).getMyRecordDetail(
                any(HttpSession.class),
                eq(7L));
    }

    @Test
    void updateUsesPathIdInsteadOfClientOwnedFields() throws Exception {
        when(runningRecordService.updateMyRecord(
                any(HttpSession.class),
                any(RunningRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(put("/api/running-records/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 999,
                                  "recordDate": "2026-06-23",
                                  "distanceKm": 10.00,
                                  "durationSeconds": 3600,
                                  "memo": "수정 기록",
                                  "memberId": 999
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.recordDate").value("2026-06-23"))
                .andExpect(jsonPath("$.data.memberId").doesNotExist());

        ArgumentCaptor<RunningRecord> captor =
                ArgumentCaptor.forClass(RunningRecord.class);
        verify(runningRecordService).updateMyRecord(
                any(HttpSession.class),
                captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7L);
        assertThat(captor.getValue().getMemberId()).isNull();
    }

    @Test
    void deleteDelegatesSoftDeleteAndReturnsSuccessEnvelope() throws Exception {
        mockMvc.perform(delete("/api/running-records/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(runningRecordService).deleteMyRecord(
                any(HttpSession.class),
                eq(7L));
    }

    @Test
    void invalidRequestIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/running-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": null,
                                  "distanceKm": 0,
                                  "durationSeconds": -1,
                                  "memo": "invalid"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(runningRecordService, never()).createRecord(
                any(HttpSession.class),
                any(RunningRecord.class));
    }

    @Test
    void serviceExceptionUsesGlobalErrorContract() throws Exception {
        when(runningRecordService.getMyRecordDetail(
                any(HttpSession.class),
                eq(7L)))
                .thenThrow(new CustomException(ErrorCode.ACCESS_DENIED));

        mockMvc.perform(get("/api/running-records/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private RunningRecord record(Long id) {
        RunningRecord runningRecord = new RunningRecord();
        runningRecord.setId(id);
        runningRecord.setMemberId(10L);
        runningRecord.setRecordDate(LocalDate.of(2026, 6, 22));
        runningRecord.setDistanceKm(new BigDecimal("5.25"));
        runningRecord.setDurationSeconds(1800);
        runningRecord.setPaceSecondsPerKm(343);
        runningRecord.setMemo("저녁 러닝");
        runningRecord.setCreatedAt(LocalDateTime.of(2026, 6, 22, 20, 0));
        runningRecord.setUpdatedAt(LocalDateTime.of(2026, 6, 22, 20, 0));
        runningRecord.setDeletedAt(LocalDateTime.of(2026, 6, 23, 20, 0));
        return runningRecord;
    }
}
