package com.ssafy.ssafit.playlist.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.music.dto.MusicApiDtos.MusicResponse;
import com.ssafy.ssafit.music.service.MusicService;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.CreateRequest;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.DetailResponse;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.ItemResponse;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.PlaylistResponse;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.UpdateRequest;
import com.ssafy.ssafit.playlist.service.PlaylistItemService;
import com.ssafy.ssafit.playlist.service.PlaylistService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistApiController {

    private final PlaylistService playlistService;
    private final PlaylistItemService playlistItemService;
    private final MusicService musicService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping
    public ApiResponse<List<PlaylistResponse>> getList(
            @RequestParam(required = false) Long ownerId,
            HttpSession session) {
        Long viewerMemberId = sessionMemberResolver.optionalMemberId(session);
        return ApiResponse.success(playlistService.getList(viewerMemberId, ownerId)
                .stream()
                .map(PlaylistResponse::from)
                .toList());
    }

    @GetMapping("/{playlistId}")
    public ApiResponse<DetailResponse> getDetail(
            @PathVariable Long playlistId,
            HttpSession session) {
        Long viewerMemberId = sessionMemberResolver.optionalMemberId(session);
        Playlist playlist = playlistService.getDetail(viewerMemberId, playlistId);
        List<ItemResponse> items = playlistItemService
                .getItems(viewerMemberId, playlistId)
                .stream()
                .map(item -> ItemResponse.from(
                        item,
                        MusicResponse.from(musicService.getMusicDetail(item.getMusicId()))))
                .toList();
        return ApiResponse.success(new DetailResponse(
                PlaylistResponse.from(playlist),
                items));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlaylistResponse> create(
            @Valid @RequestBody CreateRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        Playlist playlist = playlistService.create(memberId, request.toDomain());
        return ApiResponse.success(
                "플레이리스트를 생성했습니다.",
                PlaylistResponse.from(playlist));
    }

    @PutMapping("/{playlistId}")
    public ApiResponse<PlaylistResponse> update(
            @PathVariable Long playlistId,
            @Valid @RequestBody UpdateRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        Playlist playlist = playlistService.update(
                memberId,
                request.toDomain(playlistId));
        return ApiResponse.success(
                "플레이리스트를 수정했습니다.",
                PlaylistResponse.from(playlist));
    }

    @DeleteMapping("/{playlistId}")
    public ApiResponse<Void> delete(
            @PathVariable Long playlistId,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        playlistService.delete(memberId, playlistId);
        return ApiResponse.success("플레이리스트를 삭제했습니다.");
    }
}
