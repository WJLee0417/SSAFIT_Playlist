package com.ssafy.ssafit.member.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.web.FrontendReturnUrl;
import com.ssafy.ssafit.spotify.service.SpotifyOAuthService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SpotifyLoginViewController {

    private final SpotifyOAuthService spotifyOAuthService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping("/login/spotify")
    public RedirectView loginWithSpotify(
            @RequestParam(required = false) String returnTo,
            HttpSession session) {
        if (sessionMemberResolver.optionalMemberId(session) != null) {
            return new RedirectView(
                    FrontendReturnUrl.safeRedirectTarget(
                            returnTo,
                            "/members/me"));
        }
        return new RedirectView(
                spotifyOAuthService.buildLoginAuthorizationUrl(session, returnTo));
    }
}
