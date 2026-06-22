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
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.dto.RunningProfileViewForm;
import com.ssafy.ssafit.runningprofile.service.RunningProfileService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RunningProfileViewController {

    private final RunningProfileService runningProfileService;
    private final SessionMemberResolver sessionMemberResolver;

    @GetMapping("/members/me/running-profile")
    public String profileForm(
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            Model model) {
        Long memberId = sessionMemberResolver.optionalMemberId(session);
        if (memberId == null) {
            return "redirect:/login"
                    + (returnTo == null || returnTo.isBlank()
                            ? ""
                            : "?returnTo=" + java.net.URLEncoder.encode(
                                    returnTo,
                                    java.nio.charset.StandardCharsets.UTF_8));
        }

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", loadForm(memberId));
        }
        model.addAttribute("returnTo", returnTo);
        return "member/running-profile";
    }

    @PostMapping("/members/me/running-profile")
    public String saveProfile(
            @Valid @ModelAttribute("profileForm") RunningProfileViewForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {
        model.addAttribute("returnTo", returnTo);
        Long memberId = sessionMemberResolver.optionalMemberId(session);
        if (memberId == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            return "member/running-profile";
        }

        try {
            runningProfileService.saveOrUpdate(
                    memberId,
                    form.toDomain(memberId));
        } catch (CustomException exception) {
            bindingResult.reject(
                    exception.getErrorCode().name(),
                    exception.getErrorCode().getMessage());
            return "member/running-profile";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "러닝 프로필을 저장했습니다.");
        return FrontendReturnUrl.safeRedirect(
                returnTo,
                "redirect:/members/me/running-profile");
    }

    private RunningProfileViewForm loadForm(Long memberId) {
        try {
            RunningProfile profile = runningProfileService.getProfile(
                    memberId,
                    memberId);
            return RunningProfileViewForm.from(profile);
        } catch (CustomException exception) {
            if (exception.getErrorCode() == ErrorCode.PROFILE_NOT_FOUND) {
                return new RunningProfileViewForm();
            }
            throw exception;
        }
    }
}
