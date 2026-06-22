package com.ssafy.ssafit.runningrecord.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;
import com.ssafy.ssafit.runningrecord.dto.RunningRecordApiDtos.CreateRequest;
import com.ssafy.ssafit.runningrecord.dto.RunningRecordApiDtos.Response;
import com.ssafy.ssafit.runningrecord.dto.RunningRecordApiDtos.UpdateRequest;
import com.ssafy.ssafit.runningrecord.service.RunningRecordService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/running-records")
@RequiredArgsConstructor
public class RunningRecordApiController {

    private final RunningRecordService runningRecordService;

    @GetMapping("/me")
    public ApiResponse<List<Response>> getMyRecords(HttpSession session) {
        return ApiResponse.success(runningRecordService.getMyRecordList(session)
                .stream()
                .map(Response::from)
                .toList());
    }

    @GetMapping("/{runningRecordId}")
    public ApiResponse<Response> getMyRecord(
            @PathVariable Long runningRecordId,
            HttpSession session) {
        return ApiResponse.success(Response.from(
                runningRecordService.getMyRecordDetail(
                        session,
                        runningRecordId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Response> create(
            @Valid @RequestBody CreateRequest request,
            HttpSession session) {
        RunningRecord runningRecord =
                runningRecordService.createRecord(session, request.toDomain());
        return ApiResponse.success(
                "러닝 기록을 등록했습니다.",
                Response.from(runningRecord));
    }

    @PutMapping("/{runningRecordId}")
    public ApiResponse<Response> update(
            @PathVariable Long runningRecordId,
            @Valid @RequestBody UpdateRequest request,
            HttpSession session) {
        RunningRecord runningRecord = runningRecordService.updateMyRecord(
                session,
                request.toDomain(runningRecordId));
        return ApiResponse.success(
                "러닝 기록을 수정했습니다.",
                Response.from(runningRecord));
    }

    @DeleteMapping("/{runningRecordId}")
    public ApiResponse<Void> delete(
            @PathVariable Long runningRecordId,
            HttpSession session) {
        runningRecordService.deleteMyRecord(session, runningRecordId);
        return ApiResponse.success("러닝 기록을 삭제했습니다.");
    }
}
