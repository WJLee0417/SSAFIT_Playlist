package com.ssafy.ssafit.runningsession.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;

@Mapper
public interface RunningSessionMapper {

    int insert(RunningSession runningSession);

    List<RunningSession> selectList(@Param("memberId") Long memberId);

    Optional<RunningSession> selectDetail(@Param("sessionId") Long sessionId);

    Optional<RunningSession> selectByIdForUpdate(
            @Param("sessionId") Long sessionId);

    int update(RunningSession runningSession);

    int updateStatus(
            @Param("sessionId") Long sessionId,
            @Param("status") RunningSessionStatus status);

    int cancel(@Param("sessionId") Long sessionId);

    Long selectHostId(@Param("sessionId") Long sessionId);
}
