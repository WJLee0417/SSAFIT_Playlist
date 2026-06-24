package com.ssafy.ssafit.spotify.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.common.response.ApiResponse;
import com.ssafy.ssafit.common.web.FrontendReturnUrl;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;
import com.ssafy.ssafit.spotify.dto.SpotifyApiDtos.ConnectionResponse;
import com.ssafy.ssafit.spotify.dto.SpotifyApiDtos.PlaylistImportRequest;
import com.ssafy.ssafit.spotify.dto.SpotifyApiDtos.PlaylistImportResponse;
import com.ssafy.ssafit.spotify.dto.SpotifyApiDtos.PlaylistSummaryResponse;
import com.ssafy.ssafit.spotify.dto.SpotifyApiDtos.TrackResponse;
import com.ssafy.ssafit.spotify.service.SpotifyOAuthService;
import com.ssafy.ssafit.spotify.service.SpotifyOAuthService.SpotifyLoginResult;
import com.ssafy.ssafit.spotify.service.SpotifyPlaylistImportService;
import com.ssafy.ssafit.spotify.service.SpotifyService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyApiController {

    private final SpotifyService spotifyService;
    private final SpotifyOAuthService spotifyOAuthService;
    private final SpotifyPlaylistImportService spotifyPlaylistImportService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping("/tracks/search")
    public ApiResponse<List<TrackResponse>> searchTracks(
            @RequestParam String query,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(spotifyService.searchTracks(query, offset, limit)
                .stream()
                .map(TrackResponse::from)
                .toList());
    }

    @GetMapping("/oauth/authorize")
    public RedirectView authorize(HttpSession session) {
        sessionMemberResolver.requireMemberId(session);
        return new RedirectView(
                spotifyOAuthService.buildAuthorizationUrl(session));
    }

    @GetMapping("/oauth/callback")
    public RedirectView callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state,
            HttpSession session) {
        Long memberId = sessionMemberResolver.optionalMemberId(session);
        try {
            if (memberId == null) {
                SpotifyLoginResult result = spotifyOAuthService.handleLoginCallback(
                        code,
                        error,
                        state,
                        session);
                return new RedirectView(
                        FrontendReturnUrl.safeRedirectTarget(
                                result.returnTo(),
                                "/members/me"));
            }
            spotifyOAuthService.handleCallback(
                    memberId,
                    code,
                    error,
                    state,
                    session);
            return new RedirectView(
                    FrontendReturnUrl.spotifyCompletionRedirect(null));
        } catch (CustomException exception) {
            return new RedirectView(
                    FrontendReturnUrl.spotifyCompletionRedirect(
                            exception.getErrorCode().name()));
        }
    }

    @GetMapping("/me/connection")
    public ApiResponse<ConnectionResponse> getConnection(HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        try {
            return ApiResponse.success(ConnectionResponse.connected(
                    spotifyOAuthService.getValidConnection(memberId)));
        } catch (CustomException exception) {
            if (exception.getErrorCode() == ErrorCode.SPOTIFY_NOT_CONNECTED) {
                return ApiResponse.success(ConnectionResponse.disconnected());
            }
            if (exception.getErrorCode()
                    == ErrorCode.SPOTIFY_TOKEN_REFRESH_FAILED) {
                return ApiResponse.success(
                        ConnectionResponse.reconnectRequired(
                                spotifyOAuthService.getConnection(memberId)));
            }
            throw exception;
        }
    }

    @GetMapping("/me/playlists")
    public ApiResponse<List<PlaylistSummaryResponse>> getCurrentUserPlaylists(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(spotifyOAuthService
                .getCurrentUserPlaylists(memberId, offset, limit)
                .stream()
                .map(PlaylistSummaryResponse::from)
                .toList());
    }

    @PostMapping("/playlists/import")
    public ApiResponse<PlaylistImportResponse> importPlaylist(
            @Valid @RequestBody PlaylistImportRequest request,
            HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(
                "Spotify 플레이리스트를 가져왔습니다.",
                PlaylistImportResponse.from(spotifyPlaylistImportService.importPlaylist(
                        memberId,
                        request.spotifyPlaylistId(),
                        request.title())));
    }

    @DeleteMapping("/me/connection")
    public ApiResponse<Void> disconnect(HttpSession session) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        spotifyOAuthService.disconnect(memberId);
        return ApiResponse.success("Spotify 연결을 해제했습니다.");
    }
}
