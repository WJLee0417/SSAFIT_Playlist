package com.ssafy.ssafit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.auth.PasswordHashService;
import com.ssafy.ssafit.common.auth.SessionConst;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.mapper.MemberMapper;
import com.ssafy.ssafit.playlist.service.FavoritePlaylistService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PasswordHashService passwordHashService;

    @Mock
    private FavoritePlaylistService favoritePlaylistService;

    @Mock
    private HttpSession session;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                memberMapper,
                passwordHashService,
                favoritePlaylistService);
    }

    @Test
    void authenticateRejectsUnknownLoginId() {
        when(memberMapper.selectByLoginId("unknown")).thenReturn(Optional.empty());

        assertError(
                ErrorCode.INVALID_LOGIN,
                () -> authService.authenticate("unknown", "password"));
    }

    @Test
    void authenticateRejectsWrongPassword() {
        Member member = activeMember();
        when(memberMapper.selectByLoginId("runner")).thenReturn(Optional.of(member));
        when(passwordHashService.matches("wrong", member.getPasswordHash())).thenReturn(false);

        assertError(
                ErrorCode.INVALID_LOGIN,
                () -> authService.authenticate("runner", "wrong"));
    }

    @Test
    void authenticateRejectsInactiveMember() {
        Member member = activeMember();
        member.setStatus(MemberStatus.SUSPENDED);
        when(memberMapper.selectByLoginId("runner")).thenReturn(Optional.of(member));
        when(passwordHashService.matches("password", member.getPasswordHash())).thenReturn(true);

        assertError(
                ErrorCode.INVALID_LOGIN,
                () -> authService.authenticate("runner", "password"));
    }

    @Test
    void loginStoresOnlyMemberIdInSession() {
        Member member = activeMember();
        when(memberMapper.selectByLoginId("runner")).thenReturn(Optional.of(member));
        when(passwordHashService.matches("password", member.getPasswordHash())).thenReturn(true);

        Member loggedIn = authService.login(session, "runner", "password");

        assertThat(loggedIn).isSameAs(member);
        verify(session).setAttribute(SessionConst.LOGIN_MEMBER_ID, 1L);
        verify(favoritePlaylistService).ensureForMember(1L);
    }

    @Test
    void getLoginMemberIdRejectsMissingSessionAttribute() {
        when(session.getAttribute(SessionConst.LOGIN_MEMBER_ID)).thenReturn(null);

        assertError(ErrorCode.AUTH_REQUIRED, () -> authService.getLoginMemberId(session));
    }

    @Test
    void validateSelfRejectsOtherMemberAccess() {
        assertError(ErrorCode.ACCESS_DENIED, () -> authService.validateSelf(1L, 2L));
    }

    private Member activeMember() {
        Member member = new Member();
        member.setId(1L);
        member.setLoginId("runner");
        member.setPasswordHash("encoded-password");
        member.setStatus(MemberStatus.ACTIVE);
        return member;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
