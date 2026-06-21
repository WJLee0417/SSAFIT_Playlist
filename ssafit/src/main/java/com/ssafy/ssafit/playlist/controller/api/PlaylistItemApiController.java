package com.ssafy.ssafit.playlist.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.music.dto.MusicApiDtos.MusicResponse;
import com.ssafy.ssafit.music.service.MusicService;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.ItemCreateRequest;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.ItemOrderUpdateRequest;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.ItemResponse;
import com.ssafy.ssafit.playlist.service.PlaylistItemService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlaylistItemApiController {

    private final PlaylistItemService playlistItemService;
    private final MusicService musicService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping("/playlists/{playlistId}/items")
    public ApiResponse<List<ItemResponse>> getItems(
            @PathVariable Long playlistId,
            HttpSession session) {
        Long viewerMemberId = sessionMemberResolver.optionalMemberId(session);
        return ApiResponse.success(playlistItemService
                .getItems(viewerMemberId, playlistId)
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping("/playlists/{playlistId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ItemResponse> add(
            @PathVariable Long playlistId,
            @Valid @RequestBody ItemCreateRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        PlaylistItem item = playlistItemService.add(
                memberId,
                request.toDomain(playlistId));
        return ApiResponse.success("수록곡을 추가했습니다.", toResponse(item));
    }

    @PatchMapping("/playlist-items/{itemId}/order")
    public ApiResponse<ItemResponse> updateOrder(
            @PathVariable Long itemId,
            @Valid @RequestBody ItemOrderUpdateRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        PlaylistItem item = playlistItemService.updateOrder(
                memberId,
                itemId,
                request.orderNo());
        return ApiResponse.success("수록곡 순서를 변경했습니다.", toResponse(item));
    }

    @DeleteMapping("/playlist-items/{itemId}")
    public ApiResponse<Void> delete(
            @PathVariable Long itemId,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        playlistItemService.delete(memberId, itemId);
        return ApiResponse.success("수록곡을 삭제했습니다.");
    }

    private ItemResponse toResponse(PlaylistItem item) {
        return ItemResponse.from(
                item,
                MusicResponse.from(musicService.getMusicDetail(item.getMusicId())));
    }
}
