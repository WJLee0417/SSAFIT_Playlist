package com.ssafy.ssafit.member.dto;

import com.ssafy.ssafit.member.domain.Member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MemberViewForms {

    private MemberViewForms() {
    }

    public static class JoinForm {

        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(max = 50, message = "로그인 ID는 50자 이하여야 합니다.")
        private String loginId;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
        private String password;

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        private String email;

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 100, message = "닉네임은 100자 이하여야 합니다.")
        private String nickname;

        public Member toMember() {
            Member member = new Member();
            member.setLoginId(loginId);
            member.setEmail(email);
            member.setNickname(nickname);
            return member;
        }

        public String getLoginId() {
            return loginId;
        }

        public void setLoginId(String loginId) {
            this.loginId = loginId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    public static class LoginForm {

        @NotBlank(message = "로그인 ID를 입력해 주세요.")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        private String password;

        public String getLoginId() {
            return loginId;
        }

        public void setLoginId(String loginId) {
            this.loginId = loginId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class IdentityForm {

        @NotBlank(message = "로그인 ID를 입력해 주세요.")
        @Size(max = 50, message = "로그인 ID는 50자 이하여야 합니다.")
        private String loginId;

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(max = 100, message = "닉네임은 100자 이하여야 합니다.")
        private String nickname;

        public static IdentityForm from(Member member) {
            IdentityForm form = new IdentityForm();
            form.setLoginId(member.getLoginId());
            form.setNickname(member.getNickname());
            return form;
        }

        public Member toMember(Long memberId) {
            Member member = new Member();
            member.setId(memberId);
            member.setLoginId(loginId);
            member.setNickname(nickname);
            return member;
        }

        public String getLoginId() {
            return loginId;
        }

        public void setLoginId(String loginId) {
            this.loginId = loginId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}
