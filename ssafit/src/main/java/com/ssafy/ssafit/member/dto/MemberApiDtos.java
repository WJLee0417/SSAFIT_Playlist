package com.ssafy.ssafit.member.dto;

import java.time.LocalDateTime;

import com.ssafy.ssafit.member.domain.Member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MemberApiDtos {

    private MemberApiDtos() {
    }

    public record JoinRequest(
            @NotBlank @Size(max = 50) String loginId,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 100) String nickname) {

        public Member toMember() {
            Member member = new Member();
            member.setLoginId(loginId);
            member.setEmail(email);
            member.setNickname(nickname);
            return member;
        }
    }

    public record LoginRequest(
            @NotBlank String loginId,
            @NotBlank String password) {
    }

    public record MemberResponse(
            Long id,
            String loginId,
            String email,
            String nickname,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static MemberResponse from(Member member) {
            return new MemberResponse(
                    member.getId(),
                    member.getLoginId(),
                    member.getEmail(),
                    member.getNickname(),
                    member.getStatus() == null ? null : member.getStatus().name(),
                    member.getCreatedAt(),
                    member.getUpdatedAt());
        }
    }

    public record LoginResponse(
            boolean authenticated,
            MemberResponse member) {

        public static LoginResponse from(Member member) {
            return new LoginResponse(true, MemberResponse.from(member));
        }
    }
}
