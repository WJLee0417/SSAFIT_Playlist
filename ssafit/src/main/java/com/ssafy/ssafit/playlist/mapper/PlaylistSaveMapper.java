package com.ssafy.ssafit.playlist.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistSave;

@Mapper
public interface PlaylistSaveMapper {

    int insert(PlaylistSave playlistSave);

    int delete(
            @Param("memberId") Long memberId,
            @Param("playlistId") Long playlistId);

    boolean existsByMemberAndPlaylist(
            @Param("memberId") Long memberId,
            @Param("playlistId") Long playlistId);

    List<Playlist> selectSavedList(@Param("memberId") Long memberId);
}
