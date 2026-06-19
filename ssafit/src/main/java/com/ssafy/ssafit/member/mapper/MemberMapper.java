package com.ssafy.ssafit.member.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.member.domain.Member;

@Mapper
public interface MemberMapper {

    int insert(Member member);

    boolean existsByLoginId(@Param("loginId") String loginId);

    boolean existsByEmail(@Param("email") String email);

    Optional<Member> selectByLoginId(@Param("loginId") String loginId);

    Optional<Member> selectById(@Param("id") Long id);
}
