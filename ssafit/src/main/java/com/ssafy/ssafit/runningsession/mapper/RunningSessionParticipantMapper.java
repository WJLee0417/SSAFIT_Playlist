package com.ssafy.ssafit.runningsession.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.runningsession.domain.RunningSessionParticipant;

@Mapper
public interface RunningSessionParticipantMapper {

    int insert(RunningSessionParticipant participant);

    int delete(
            @Param("sessionId") Long sessionId,
            @Param("memberId") Long memberId);

    boolean existsParticipant(
            @Param("sessionId") Long sessionId,
            @Param("memberId") Long memberId);

    int countParticipants(@Param("sessionId") Long sessionId);

    List<RunningSessionParticipant> selectParticipants(
            @Param("sessionId") Long sessionId);

    Optional<RunningSessionParticipant> selectBySessionIdAndMemberId(
            @Param("sessionId") Long sessionId,
            @Param("memberId") Long memberId);
}
