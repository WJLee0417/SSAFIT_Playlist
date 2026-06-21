package com.ssafy.ssafit.spotify.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
import com.ssafy.ssafit.spotify.dto.SpotifyApiDtos.AuthorizationResponse;
import com.ssafy.ssafit.spotify.dto.SpotifyApiDtos.ConnectionResponse;
import com.ssafy.ssafit.spotify.dto.SpotifyApiDtos.TrackResponse;
import com.ssafy.ssafit.spotify.service.SpotifyOAuthService;
import com.ssafy.ssafit.spotify.service.SpotifyService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyApiController {

    private final SpotifyService spotifyService;
    private final SpotifyOAuthService spotifyOAuthService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping("/tracks/search")
    public ApiResponse<List<TrackResponse>> searchTracks(
            @RequestParam String query,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(spotifyService.searchTracks(query, limit)
                .stream()
                .map(TrackResponse::from)
                .toList());
    }

    @GetMapping("/oauth/authorize")
    public ApiResponse<AuthorizationResponse> authorize(HttpSession session) {
        sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(new AuthorizationResponse(
                spotifyOAuthService.buildAuthorizationUrl(session)));
    }

    @GetMapping("/oauth/callback")
    public ApiResponse<ConnectionResponse> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam String state,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        SpotifyConnection connection = spotifyOAuthService.handleCallback(
                memberId,
                code,
                error,
                state,
                session);
        return ApiResponse.success(
                "Spotify 계정을 연결했습니다.",
                ConnectionResponse.connected(connection));
    }

    @GetMapping("/me/connection")
    public ApiResponse<ConnectionResponse> getConnection(HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        try {
            return ApiResponse.success(ConnectionResponse.connected(
                    spotifyOAuthService.getConnection(memberId)));
        } catch (CustomException exception) {
            if (exception.getErrorCode() == ErrorCode.SPOTIFY_AUTH_REQUIRED) {
                return ApiResponse.success(ConnectionResponse.disconnected());
            }
            throw exception;
        }
    }

    @DeleteMapping("/me/connection")
    public ApiResponse<Void> disconnect(HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        spotifyOAuthService.disconnect(memberId);
        return ApiResponse.success("Spotify 연결을 해제했습니다.");
    }
}
