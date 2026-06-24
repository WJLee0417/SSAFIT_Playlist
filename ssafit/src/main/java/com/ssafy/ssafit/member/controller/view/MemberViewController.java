package com.ssafy.ssafit.member.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.common.web.FrontendReturnUrl;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.dto.MemberViewForms.IdentityForm;
import com.ssafy.ssafit.member.dto.MemberViewForms.JoinForm;
import com.ssafy.ssafit.member.dto.MemberViewForms.LoginForm;
import com.ssafy.ssafit.member.service.AuthService;
import com.ssafy.ssafit.member.service.MemberService;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.service.RunningProfileService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberViewController {

    private final MemberService memberService;
    private final AuthService authService;
    private final RunningProfileService runningProfileService;
    private final SessionMemberResolver sessionMemberResolver;
    private static final String SPOTIFY_LOGIN_ID_PREFIX = "spotify_";

    @GetMapping("/join")
    public String joinForm(HttpSession session, Model model) {
        if (sessionMemberResolver.optionalMemberId(session) != null) {
            return "redirect:/members/me";
        }
        if (!model.containsAttribute("joinForm")) {
            model.addAttribute("joinForm", new JoinForm());
        }
        return "member/join";
    }

    @PostMapping("/join")
    public String join(
            @Valid @ModelAttribute("joinForm") JoinForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "member/join";
        }

        try {
            memberService.register(form.toMember(), form.getPassword());
        } catch (CustomException exception) {
            applyJoinError(bindingResult, exception);
            return "member/join";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "회원가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm(
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            Model model) {
        if (sessionMemberResolver.optionalMemberId(session) != null) {
            return FrontendReturnUrl.safeRedirect(returnTo, "redirect:/members/me");
        }
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        model.addAttribute("returnTo", returnTo);
        return "member/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginForm") LoginForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            Model model) {
        model.addAttribute("returnTo", returnTo);
        if (bindingResult.hasErrors()) {
            return "member/login";
        }

        try {
            authService.login(session, form.getLoginId(), form.getPassword());
        } catch (CustomException exception) {
            bindingResult.reject("login.failed", exception.getErrorCode().getMessage());
            return "member/login";
        }
        return FrontendReturnUrl.safeRedirect(returnTo, "redirect:/members/me");
    }

    @PostMapping("/logout")
    public String logout(
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        authService.logout(session);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "로그아웃했습니다.");
        return "redirect:/login";
    }

    @GetMapping("/members/me")
    public String myPage(HttpSession session, Model model) {
        Long memberId = sessionMemberResolver.optionalMemberId(session);
        if (memberId == null) {
            return "redirect:/login";
        }

        Member member = memberService.getMyInfo(memberId);
        RunningProfile runningProfile = findProfile(memberId);
        populateMyPageModel(model, member, runningProfile);
        return "member/my-page";
    }

    @PostMapping("/members/me/identity")
    public String updateIdentity(
            @Valid @ModelAttribute("identityForm") IdentityForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Long memberId = sessionMemberResolver.optionalMemberId(session);
        if (memberId == null) {
            return "redirect:/login";
        }

        Member current = memberService.getMyInfo(memberId);
        RunningProfile runningProfile = findProfile(memberId);
        if (bindingResult.hasErrors()) {
            populateMyPageModel(model, current, runningProfile);
            model.addAttribute("forceIdentityOnboarding", true);
            return "member/my-page";
        }

        try {
            memberService.updateIdentity(memberId, form.toMember(memberId));
        } catch (CustomException exception) {
            applyIdentityError(bindingResult, exception);
            populateMyPageModel(model, current, runningProfile);
            model.addAttribute("forceIdentityOnboarding", true);
            return "member/my-page";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "계정 정보를 저장했습니다. 러닝 프로필을 이어서 등록해 주세요.");
        return "redirect:/members/me/running-profile";
    }

    private RunningProfile findProfile(Long memberId) {
        try {
            return runningProfileService.getProfile(memberId, memberId);
        } catch (CustomException exception) {
            if (exception.getErrorCode() == ErrorCode.PROFILE_NOT_FOUND) {
                return null;
            }
            throw exception;
        }
    }

    private void populateMyPageModel(
            Model model,
            Member member,
            RunningProfile runningProfile) {
        boolean spotifyOnboardingRequired = isSpotifyTemporaryAccount(member);
        model.addAttribute("member", member);
        model.addAttribute("runningProfile", runningProfile);
        model.addAttribute("profileRegistrationRequired", runningProfile == null);
        model.addAttribute("spotifyOnboardingRequired", spotifyOnboardingRequired);
        if (!model.containsAttribute("identityForm")) {
            IdentityForm identityForm = IdentityForm.from(member);
            if (spotifyOnboardingRequired) {
                identityForm.setLoginId("");
            }
            model.addAttribute("identityForm", identityForm);
        }
    }

    private boolean isSpotifyTemporaryAccount(Member member) {
        return member != null
                && member.getLoginId() != null
                && member.getLoginId().startsWith(SPOTIFY_LOGIN_ID_PREFIX);
    }

    private void applyJoinError(
            BindingResult bindingResult,
            CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        if (errorCode == ErrorCode.DUPLICATE_LOGIN_ID) {
            bindingResult.rejectValue(
                    "loginId",
                    errorCode.name(),
                    errorCode.getMessage());
            return;
        }
        if (errorCode == ErrorCode.DUPLICATE_EMAIL) {
            bindingResult.rejectValue(
                    "email",
                    errorCode.name(),
                    errorCode.getMessage());
            return;
        }
        bindingResult.reject(errorCode.name(), errorCode.getMessage());
    }

    private void applyIdentityError(
            BindingResult bindingResult,
            CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        if (errorCode == ErrorCode.DUPLICATE_LOGIN_ID) {
            bindingResult.rejectValue(
                    "loginId",
                    errorCode.name(),
                    errorCode.getMessage());
            return;
        }
        bindingResult.reject(errorCode.name(), errorCode.getMessage());
    }
}
