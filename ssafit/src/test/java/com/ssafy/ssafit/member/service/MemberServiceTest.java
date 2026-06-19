package com.ssafy.ssafit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.auth.PasswordHashService;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.member.mapper.MemberMapper;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PasswordHashService passwordHashService;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberMapper, passwordHashService);
    }

    @Test
    void registerRejectsDuplicateLoginId() {
        Member member = registrationMember();
        when(memberMapper.existsByLoginId(member.getLoginId())).thenReturn(true);

        assertError(
                ErrorCode.DUPLICATE_LOGIN_ID,
                () -> memberService.register(member, "password"));

        verify(memberMapper, never()).insert(member);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        Member member = registrationMember();
        when(memberMapper.existsByLoginId(member.getLoginId())).thenReturn(false);
        when(memberMapper.existsByEmail(member.getEmail())).thenReturn(true);

        assertError(
                ErrorCode.DUPLICATE_EMAIL,
                () -> memberService.register(member, "password"));

        verify(memberMapper, never()).insert(member);
    }

    @Test
    void registerHashesPasswordAndCreatesActiveMember() {
        Member member = registrationMember();
        when(passwordHashService.encode("password")).thenReturn("encoded-password");
        when(memberMapper.insert(member)).thenAnswer(invocation -> {
            member.setId(1L);
            return 1;
        });

        Member registered = memberService.register(member, "password");

        assertThat(registered.getId()).isEqualTo(1L);
        assertThat(registered.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(registered.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberMapper).insert(member);
    }

    @Test
    void getMyInfoRejectsUnauthenticatedAccess() {
        assertError(ErrorCode.AUTH_REQUIRED, () -> memberService.getMyInfo(null));
    }

    @Test
    void getMyInfoThrowsWhenMemberDoesNotExist() {
        when(memberMapper.selectById(1L)).thenReturn(Optional.empty());

        assertError(ErrorCode.MEMBER_NOT_FOUND, () -> memberService.getMyInfo(1L));
    }

    private Member registrationMember() {
        Member member = new Member();
        member.setLoginId("runner");
        member.setEmail("runner@example.com");
        member.setNickname("러너");
        return member;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
