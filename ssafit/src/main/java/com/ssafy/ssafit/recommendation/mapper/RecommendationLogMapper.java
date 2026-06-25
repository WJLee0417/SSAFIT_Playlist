package com.ssafy.ssafit.recommendation.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.recommendation.domain.RecommendationLog;

@Mapper
public interface RecommendationLogMapper {

    int insert(RecommendationLog recommendationLog);

    Optional<RecommendationLog> selectById(@Param("id") Long id);

    List<RecommendationLog> selectByMemberId(@Param("memberId") Long memberId);

    List<RecommendationLog> selectRecentByMemberId(
            @Param("memberId") Long memberId,
            @Param("limit") int limit);

    int countByMemberId(@Param("memberId") Long memberId);

    int updateSavedPlaylistId(
            @Param("id") Long id,
            @Param("savedPlaylistId") Long savedPlaylistId);

    int deleteByIdAndMemberId(
            @Param("id") Long id,
            @Param("memberId") Long memberId);
}
