package com.ssafy.ssafit.runningprofile.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.runningprofile.domain.RunningProfile;

@Mapper
public interface RunningProfileMapper {

    Optional<RunningProfile> selectByMemberId(@Param("memberId") Long memberId);

    int insert(RunningProfile runningProfile);

    int update(RunningProfile runningProfile);

    int updateAutoTargets(
            @Param("memberId") Long memberId,
            @Param("targetBpmMin") Integer targetBpmMin,
            @Param("targetBpmMax") Integer targetBpmMax,
            @Param("targetCadenceMin") Integer targetCadenceMin,
            @Param("targetCadenceMax") Integer targetCadenceMax);
}
