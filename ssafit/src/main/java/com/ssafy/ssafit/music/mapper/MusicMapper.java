package com.ssafy.ssafit.music.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSearchCondition;

@Mapper
public interface MusicMapper {

    List<Music> selectMusicList(MusicSearchCondition condition);

    Optional<Music> selectMusicDetail(@Param("id") Long id);

    long countMusicList(MusicSearchCondition condition);

    List<Music> selectRecommendedMusic(MusicSearchCondition condition);

    Optional<Music> selectBySpotifyTrackId(
            @Param("spotifyTrackId") String spotifyTrackId);

    int insertFromSpotify(Music music);

    int updateSpotifyMetadata(Music music);
}
