package com.ssafy.ssafit.music.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicPreferenceScore;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;

@Mapper
public interface MusicMapper {

    List<Music> selectMusicList(MusicSearchCondition condition);

    Optional<Music> selectMusicDetail(@Param("id") Long id);

    List<Music> selectByIds(@Param("ids") List<Long> ids);

    long countMusicList(MusicSearchCondition condition);

    List<Music> selectRecommendedMusic(MusicSearchCondition condition);

    List<Music> selectRecommendationCandidates(
            @Param("genre") String genre,
            @Param("limit") int limit);

    List<MusicPreferenceScore> selectPreferenceScores(
            @Param("memberId") Long memberId);

    Optional<Music> selectBySpotifyTrackId(
            @Param("spotifyTrackId") String spotifyTrackId);

    Optional<Music> selectBySpotifyUri(@Param("spotifyUri") String spotifyUri);

    int insertFromSpotify(Music music);

    int updateSpotifyMetadata(Music music);

    int insertInternal(Music music);

    int updateInternal(Music music);
}
