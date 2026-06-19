package com.ssafy.ssafit.playlist.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.playlist.domain.PlaylistItem;

@Mapper
public interface PlaylistItemMapper {

    int insert(PlaylistItem playlistItem);

    int delete(@Param("id") Long id);

    int updateOrder(
            @Param("id") Long id,
            @Param("orderNo") Integer orderNo);

    boolean existsByPlaylistAndMusic(
            @Param("playlistId") Long playlistId,
            @Param("musicId") Long musicId);

    boolean existsByPlaylistAndOrder(
            @Param("playlistId") Long playlistId,
            @Param("orderNo") Integer orderNo,
            @Param("excludeItemId") Long excludeItemId);

    Optional<PlaylistItem> selectById(@Param("id") Long id);

    List<PlaylistItem> selectByPlaylistId(@Param("playlistId") Long playlistId);

    int bulkInsertFromRecommendation(
            @Param("items") List<PlaylistItem> items);

    List<String> selectSpotifyUrisByPlaylistId(
            @Param("playlistId") Long playlistId);
}
