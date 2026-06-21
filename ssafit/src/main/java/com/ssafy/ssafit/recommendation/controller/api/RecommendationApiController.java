package com.ssafy.ssafit.recommendation.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.dto.PlaylistApiDtos.PlaylistResponse;
import com.ssafy.ssafit.recommendation.domain.RecommendationResult;
import com.ssafy.ssafit.recommendation.dto.RecommendationApiDtos.LogResponse;
import com.ssafy.ssafit.recommendation.dto.RecommendationApiDtos.RecommendRequest;
import com.ssafy.ssafit.recommendation.dto.RecommendationApiDtos.ResultResponse;
import com.ssafy.ssafit.recommendation.dto.RecommendationApiDtos.SaveRequest;
import com.ssafy.ssafit.recommendation.dto.RecommendationApiDtos.SaveResponse;
import com.ssafy.ssafit.recommendation.service.RecommendationService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping
    public ApiResponse<ResultResponse> recommend(
            @Valid @ModelAttribute RecommendRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        RecommendationResult result = recommendationService.recommend(
                memberId,
                request.intensity(),
                request.genre(),
                request.size());
        return ApiResponse.success(ResultResponse.from(result));
    }

    @GetMapping("/logs/me")
    public ApiResponse<List<LogResponse>> getMyLogs(HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(
                recommendationService.getMyRecommendationLogs(memberId)
                        .stream()
                        .map(LogResponse::from)
                        .toList());
    }

    @PostMapping("/{logId}/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SaveResponse> save(
            @PathVariable Long logId,
            @Valid @RequestBody SaveRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        Playlist playlist = recommendationService.saveRecommendation(
                memberId,
                logId,
                request.title(),
                request.visibility());
        return ApiResponse.success(
                "자동 큐레이션을 개인 플레이리스트로 저장했습니다.",
                new SaveResponse(PlaylistResponse.from(playlist)));
    }
}
