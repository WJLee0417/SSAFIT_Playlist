package com.ssafy.ssafit.runningrecord.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.runningrecord.domain.RunningRecord;

@Mapper
public interface RunningRecordMapper {

    int insert(RunningRecord runningRecord);

    List<RunningRecord> selectListByMemberId(@Param("memberId") Long memberId);

    Optional<RunningRecord> selectById(@Param("id") Long id);

    int update(RunningRecord runningRecord);

    int delete(@Param("id") Long id);
}
