package com.ssafy.ssafit.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.auth.PasswordHashService;
import com.ssafy.ssafit.common.auth.SessionConst;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.mapper.MemberMapper;
import com.ssafy.ssafit.playlist.service.FavoritePlaylistService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberMapper memberMapper;
    private final PasswordHashService passwordHashService;
    private final FavoritePlaylistService favoritePlaylistService;

    public Member authenticate(String loginId, String rawPassword) {
        if (loginId == null || loginId.isBlank() || rawPassword == null) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }

        Member member = memberMapper.selectByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN));

        if (!passwordHashService.matches(rawPassword, member.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }

        validateActiveMember(member);
        return member;
    }

    @Transactional
    public Member login(HttpSession session, String loginId, String rawPassword) {
        Member member = authenticate(loginId, rawPassword);
        login(session, member);
        return member;
    }

    @Transactional
    public void login(HttpSession session, Member member) {
        if (session == null || member == null || member.getId() == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
        validateActiveMember(member);
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
        favoritePlaylistService.ensureForMember(member.getId());
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public Long getLoginMemberId(HttpSession session) {
        if (session == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }

        Object value = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        if (!(value instanceof Long memberId)) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
        return memberId;
    }

    public Member getLoginMember(HttpSession session) {
        Long memberId = getLoginMemberId(session);
        Member member = memberMapper.selectById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        validateActiveMember(member);
        return member;
    }

    public void validateSelf(Long loginMemberId, Long targetMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
        if (targetMemberId == null || !loginMemberId.equals(targetMemberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateActiveMember(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE || member.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }
    }
}
