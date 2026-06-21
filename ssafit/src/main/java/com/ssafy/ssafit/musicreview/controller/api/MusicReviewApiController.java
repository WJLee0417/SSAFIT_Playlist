package com.ssafy.ssafit.musicreview.controller.api;

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

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.musicreview.dto.MusicReviewApiDtos.CreateRequest;
import com.ssafy.ssafit.musicreview.dto.MusicReviewApiDtos.ReviewResponse;
import com.ssafy.ssafit.musicreview.dto.MusicReviewApiDtos.UpdateRequest;
import com.ssafy.ssafit.musicreview.service.MusicReviewService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MusicReviewApiController {

    private final MusicReviewService musicReviewService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping("/api/music/{musicId}/reviews")
    public ApiResponse<List<ReviewResponse>> getReviews(
            @PathVariable Long musicId) {
        return ApiResponse.success(musicReviewService.getReviewsByMusicId(musicId)
                .stream()
                .map(ReviewResponse::from)
                .toList());
    }

    @PostMapping("/api/music/{musicId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(
            @PathVariable Long musicId,
            @Valid @RequestBody CreateRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(
                "음악 평가를 작성했습니다.",
                ReviewResponse.from(musicReviewService.create(
                        memberId,
                        request.toDomain(musicId))));
    }

    @PutMapping("/api/music-reviews/{reviewId}")
    public ApiResponse<ReviewResponse> update(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(
                "음악 평가를 수정했습니다.",
                ReviewResponse.from(musicReviewService.update(
                        memberId,
                        request.toDomain(reviewId))));
    }

    @DeleteMapping("/api/music-reviews/{reviewId}")
    public ApiResponse<Void> delete(
            @PathVariable Long reviewId,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        musicReviewService.delete(memberId, reviewId);
        return ApiResponse.success("음악 평가를 삭제했습니다.");
    }
}
