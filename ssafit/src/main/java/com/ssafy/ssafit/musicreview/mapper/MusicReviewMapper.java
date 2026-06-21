package com.ssafy.ssafit.musicreview.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.musicreview.domain.MusicReview;
import com.ssafy.ssafit.musicreview.domain.MusicReviewAverage;

@Mapper
public interface MusicReviewMapper {

    int insert(MusicReview musicReview);

    int update(MusicReview musicReview);

    int delete(@Param("id") Long id);

    boolean existsByMemberAndMusic(
            @Param("memberId") Long memberId,
            @Param("musicId") Long musicId);

    Optional<MusicReview> selectById(@Param("id") Long id);

    List<MusicReview> selectByMusicId(@Param("musicId") Long musicId);

    MusicReviewAverage selectAverageScores(@Param("musicId") Long musicId);
}
