package com.ssafy.ssafit.member.controller.view;

import static org.mockito.ArgumentMatchers.any;
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
import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.service.RunningProfileService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class RunningProfileViewControllerTest {

    @Mock
    private RunningProfileService runningProfileService;

    @Mock
    private SessionMemberResolver sessionMemberResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RunningProfileViewController controller = new RunningProfileViewController(
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
    void missingProfileShowsRegistrationForm() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(10L);
        when(runningProfileService.getProfile(10L, 10L))
                .thenThrow(new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        mockMvc.perform(get("/members/me/running-profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/running-profile"))
                .andExpect(model().attributeExists("profileForm"));
    }

    @Test
    void existingProfileIsLoadedForEditing() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(10L);
        when(runningProfileService.getProfile(10L, 10L))
                .thenReturn(profile());

        mockMvc.perform(get("/members/me/running-profile"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                        "profileForm",
                        org.hamcrest.Matchers.hasProperty(
                                "targetBpmMin",
                                org.hamcrest.Matchers.is(120))));
    }

    @Test
    void validProfileSaveUsesPrg() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(10L);
        when(runningProfileService.saveOrUpdate(
                org.mockito.ArgumentMatchers.eq(10L),
                any(RunningProfile.class)))
                .thenReturn(profile());

        mockMvc.perform(post("/members/me/running-profile")
                        .param("runningIntensity", "MODERATE")
                        .param("targetBpmMin", "120")
                        .param("targetBpmMax", "160")
                        .param("preferredGenre", "POP"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/me/running-profile"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(runningProfileService).saveOrUpdate(
                org.mockito.ArgumentMatchers.eq(10L),
                any(RunningProfile.class));
    }

    @Test
    void validProfileSaveReturnsToSafeVueUrl() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(10L);
        when(runningProfileService.saveOrUpdate(
                org.mockito.ArgumentMatchers.eq(10L),
                any(RunningProfile.class)))
                .thenReturn(profile());

        mockMvc.perform(post("/members/me/running-profile")
                        .param("runningIntensity", "MODERATE")
                        .param("targetBpmMin", "120")
                        .param("targetBpmMax", "160")
                        .param("preferredGenre", "POP")
                        .param(
                                "returnTo",
                                "http://localhost:5173/recommendations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "http://localhost:5173/recommendations"));
    }

    @Test
    void invalidProfileValueRendersError() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(10L);
        when(runningProfileService.saveOrUpdate(
                org.mockito.ArgumentMatchers.eq(10L),
                any(RunningProfile.class)))
                .thenThrow(new CustomException(ErrorCode.INVALID_PROFILE_VALUE));

        mockMvc.perform(post("/members/me/running-profile")
                        .param("runningIntensity", "MODERATE")
                        .param("targetBpmMin", "170")
                        .param("targetBpmMax", "120"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/running-profile"))
                .andExpect(model().attributeHasErrors("profileForm"));
    }

    @Test
    void guestIsRedirectedToLogin() throws Exception {
        when(sessionMemberResolver.optionalMemberId(any(HttpSession.class)))
                .thenReturn(null);

        mockMvc.perform(get("/members/me/running-profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private RunningProfile profile() {
        RunningProfile profile = new RunningProfile();
        profile.setId(20L);
        profile.setMemberId(10L);
        profile.setRunningIntensity(RunningIntensity.MODERATE);
        profile.setTargetBpmMin(120);
        profile.setTargetBpmMax(160);
        profile.setPreferredGenre("POP");
        return profile;
    }
}
