package com.ssafy.ssafit.spotify.controller.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.ExportResponse;
import com.ssafy.ssafit.spotify.domain.SpotifyExportResult;
import com.ssafy.ssafit.spotify.service.PlaylistExportService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlists/{playlistId}/export")
@RequiredArgsConstructor
public class PlaylistExportApiController {

    private final PlaylistExportService playlistExportService;
    private final SessionMemberResolver sessionMemberResolver;

    @PostMapping("/spotify")
    public ApiResponse<ExportResponse> export(
            @PathVariable Long playlistId,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        SpotifyExportResult result =
                playlistExportService.exportWithResult(memberId, playlistId);
        return ApiResponse.success(
                "Spotify 플레이리스트로 내보냈습니다.",
                ExportResponse.from(result));
    }
}
