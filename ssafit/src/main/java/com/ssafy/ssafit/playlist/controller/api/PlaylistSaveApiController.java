package com.ssafy.ssafit.playlist.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.PlaylistResponse;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.SaveResponse;
import com.ssafy.ssafit.playlist.service.PlaylistSaveService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlist-saves")
@RequiredArgsConstructor
public class PlaylistSaveApiController {

    private final PlaylistSaveService playlistSaveService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping("/me")
    public ApiResponse<List<PlaylistResponse>> getMySavedList(
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(playlistSaveService.getMySavedList(memberId)
                .stream()
                .map(PlaylistResponse::from)
                .toList());
    }

    @PostMapping("/{playlistId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SaveResponse> save(
            @PathVariable Long playlistId,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(
                "플레이리스트를 저장했습니다.",
                SaveResponse.from(playlistSaveService.save(memberId, playlistId)));
    }

    @DeleteMapping("/{playlistId}")
    public ApiResponse<Void> cancel(
            @PathVariable Long playlistId,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        playlistSaveService.cancel(memberId, playlistId);
        return ApiResponse.success("플레이리스트 저장을 취소했습니다.");
    }
}
