package com.ssafy.ssafit.music.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.common.response.PageResponse;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;
import com.ssafy.ssafit.music.dto.MusicApiDtos.DetailResponse;
import com.ssafy.ssafit.music.dto.MusicApiDtos.InternalSaveRequest;
import com.ssafy.ssafit.music.dto.MusicApiDtos.MusicResponse;
import com.ssafy.ssafit.music.dto.MusicApiDtos.SearchRequest;
import com.ssafy.ssafit.music.dto.MusicApiDtos.SpotifyImportRequest;
import com.ssafy.ssafit.music.service.MusicService;
import com.ssafy.ssafit.musicreview.dto.MusicReviewApiDtos.AverageResponse;
import com.ssafy.ssafit.musicreview.dto.MusicReviewApiDtos.ReviewResponse;
import com.ssafy.ssafit.musicreview.service.MusicReviewService;
import com.ssafy.ssafit.spotify.service.SpotifyService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
public class MusicApiController {

    private final MusicService musicService;
    private final MusicReviewService musicReviewService;
    private final SpotifyService spotifyService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping
    public ApiResponse<PageResponse<MusicResponse>> getMusicList(
            @ModelAttribute SearchRequest request) {
        MusicSearchCondition condition = request.toCondition();
        List<MusicResponse> items = musicService.getMusicList(condition).stream()
                .map(MusicResponse::from)
                .toList();
        long totalCount = musicService.countMusicList(condition);
        return ApiResponse.success(PageResponse.of(
                items,
                condition.getPage(),
                condition.getSize(),
                totalCount));
    }

    @GetMapping("/{musicId}")
    public ApiResponse<DetailResponse> getMusicDetail(
            @PathVariable Long musicId) {
        Music music = musicService.getMusicDetail(musicId);
        List<ReviewResponse> reviews = musicReviewService
                .getReviewsByMusicId(musicId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
        AverageResponse average = AverageResponse.from(
                musicReviewService.getAverageScores(musicId));
        return ApiResponse.success(new DetailResponse(
                MusicResponse.from(music),
                average,
                reviews));
    }

    @PostMapping("/from-spotify")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MusicResponse> importSpotifyTrack(
            @Valid @RequestBody SpotifyImportRequest request,
            HttpSession session) {
        sessionMemberResolver.requireMemberId(session);
        Music music = spotifyService.saveSelectedTrack(request.toMetadata());
        return ApiResponse.success(
                "Spotify 음악을 저장했습니다.",
                MusicResponse.from(music));
    }

    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MusicResponse> createInternalMusic(
            @Valid @RequestBody InternalSaveRequest request,
            HttpSession session) {
        sessionMemberResolver.requireMemberId(session);
        Music music = musicService.createInternalMusic(request.toMusic());
        return ApiResponse.success(MusicResponse.from(music));
    }

    @PutMapping("/internal/{musicId}")
    public ApiResponse<MusicResponse> updateInternalMusic(
            @PathVariable Long musicId,
            @Valid @RequestBody InternalSaveRequest request,
            HttpSession session) {
        sessionMemberResolver.requireMemberId(session);
        Music music = musicService.updateInternalMusic(
                musicId,
                request.toMusic());
        return ApiResponse.success(MusicResponse.from(music));
    }
}
