package com.ssafy.ssafit.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.auth.PasswordHashService;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordHashService passwordHashService;

    @Transactional
    public Member register(Member member, String rawPassword) {
        validateRegistrationInput(member, rawPassword);
        validateDuplicateLoginId(member.getLoginId());
        validateDuplicateEmail(member.getEmail());

        member.setPasswordHash(passwordHashService.encode(rawPassword));
        member.setStatus(MemberStatus.ACTIVE);
        if (memberMapper.insert(member) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return member;
    }

    @Transactional
    public Member updateIdentity(Long loginMemberId, Member changes) {
        if (loginMemberId == null || changes == null
                || isBlank(changes.getLoginId())
                || isBlank(changes.getNickname())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Member current = getMyInfo(loginMemberId);
        String loginId = changes.getLoginId().trim();
        String nickname = changes.getNickname().trim();
        memberMapper.selectByLoginId(loginId)
                .filter(member -> !loginMemberId.equals(member.getId()))
                .ifPresent(member -> {
                    throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
                });

        current.setLoginId(loginId);
        current.setNickname(nickname);
        if (memberMapper.updateIdentity(current) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return current;
    }

    public void validateDuplicateLoginId(String loginId) {
        if (memberMapper.existsByLoginId(loginId)) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    public void validateDuplicateEmail(String email) {
        if (memberMapper.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    public Member getMyInfo(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }

        return memberMapper.selectById(loginMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateRegistrationInput(Member member, String rawPassword) {
        if (member == null
                || isBlank(member.getLoginId())
                || isBlank(member.getEmail())
                || isBlank(member.getNickname())
                || isBlank(rawPassword)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
