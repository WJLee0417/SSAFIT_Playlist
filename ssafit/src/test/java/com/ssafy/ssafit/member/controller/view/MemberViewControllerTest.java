package com.ssafy.ssafit.member.controller.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.service.AuthService;
import com.ssafy.ssafit.member.service.MemberService;
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.service.RunningProfileService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class MemberViewControllerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private AuthService authService;

    @Mock
    private RunningProfileService runningProfileService;

    @Mock
    private SessionMemberResolver sessionMemberResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MemberViewController controller = new MemberViewController(
                memberService,
                authService,
                runningProfileService,
                sessionMemberResolver);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver(
                "/WEB-INF/views/",
                ".jsp");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void joinPageIsRenderedForGuest() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(null);

        mockMvc.perform(get("/join"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/join"))
                .andExpect(model().attributeExists("joinForm"));
    }

    @Test
    void loggedInMemberIsRedirectedAwayFromLoginPage() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(10L);

        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/me"));
    }

    @Test
    void joinValidationErrorRendersForm() throws Exception {
        mockMvc.perform(post("/join")
                        .param("loginId", "")
                        .param("password", "short")
                        .param("email", "wrong")
                        .param("nickname", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("member/join"))
                .andExpect(model().attributeHasFieldErrors(
                        "joinForm",
                        "loginId",
                        "password",
                        "email",
                        "nickname"));
    }

    @Test
    void duplicateLoginIdIsShownAsFieldError() throws Exception {
        doThrow(new CustomException(ErrorCode.DUPLICATE_LOGIN_ID))
                .when(memberService)
                .register(any(Member.class), eq("password12"));

        mockMvc.perform(post("/join")
                        .param("loginId", "runner")
                        .param("password", "password12")
                        .param("email", "runner@example.com")
                        .param("nickname", "러너"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/join"))
                .andExpect(model().attributeHasFieldErrors(
                        "joinForm",
                        "loginId"));
    }

    @Test
    void successfulJoinUsesPrgToLogin() throws Exception {
        when(memberService.register(any(Member.class), eq("password12")))
                .thenReturn(member());

        mockMvc.perform(post("/join")
                        .param("loginId", "runner")
                        .param("password", "password12")
                        .param("email", "runner@example.com")
                        .param("nickname", "러너"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void loginFailureRendersErrorWithoutRedirect() throws Exception {
        doThrow(new CustomException(ErrorCode.INVALID_LOGIN))
                .when(authService)
                .login(any(HttpSession.class), eq("runner"), eq("wrongpass"));

        mockMvc.perform(post("/login")
                        .param("loginId", "runner")
                        .param("password", "wrongpass"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/login"))
                .andExpect(model().attributeHasErrors("loginForm"));
    }

    @Test
    void successfulLoginRedirectsToMyPage() throws Exception {
        when(authService.login(
                any(HttpSession.class),
                eq("runner"),
                eq("password12")))
                .thenReturn(member());

        mockMvc.perform(post("/login")
                        .param("loginId", "runner")
                        .param("password", "password12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/me"));
    }

    @Test
    void successfulLoginReturnsToSafeVueUrl() throws Exception {
        when(authService.login(
                any(HttpSession.class),
                eq("runner"),
                eq("password12")))
                .thenReturn(member());

        mockMvc.perform(post("/login")
                        .param("loginId", "runner")
                        .param("password", "password12")
                        .param(
                                "returnTo",
                                "http://127.0.0.1:5173/playlists/10?tab=items"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "http://127.0.0.1:5173/playlists/10?tab=items"));
    }

    @Test
    void successfulLoginRejectsExternalReturnUrl() throws Exception {
        when(authService.login(
                any(HttpSession.class),
                eq("runner"),
                eq("password12")))
                .thenReturn(member());

        mockMvc.perform(post("/login")
                        .param("loginId", "runner")
                        .param("password", "password12")
                        .param("returnTo", "https://example.com/steal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/me"));
    }

    @Test
    void guestCannotOpenMyPage() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(null);

        mockMvc.perform(get("/members/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void myPageShowsMemberAndOptionalProfile() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(10L);
        when(memberService.getMyInfo(10L)).thenReturn(member());
        when(runningProfileService.getProfile(10L, 10L))
                .thenReturn(profile());

        mockMvc.perform(get("/members/me"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/my-page"))
                .andExpect(model().attributeExists("member", "runningProfile"));
    }

    @Test
    void logoutInvalidatesSessionAndRedirects() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(authService).logout(any(HttpSession.class));
    }

    private Member member() {
        Member member = new Member();
        member.setId(10L);
        member.setLoginId("runner");
        member.setEmail("runner@example.com");
        member.setNickname("러너");
        member.setStatus(MemberStatus.ACTIVE);
        return member;
    }

    private RunningProfile profile() {
        RunningProfile profile = new RunningProfile();
        profile.setId(20L);
        profile.setMemberId(10L);
        profile.setRunningIntensity(RunningIntensity.MODERATE);
        profile.setTargetBpmMin(120);
        profile.setTargetBpmMax(160);
        return profile;
    }
}
