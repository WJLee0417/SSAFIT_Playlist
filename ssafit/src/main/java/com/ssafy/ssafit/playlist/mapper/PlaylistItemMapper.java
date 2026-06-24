package com.ssafy.ssafit.playlist.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistExportTrack;

@Mapper
public interface PlaylistItemMapper {

    int insert(PlaylistItem playlistItem);

    int delete(@Param("id") Long id);

    int deleteByPlaylistAndMusic(
            @Param("playlistId") Long playlistId,
            @Param("musicId") Long musicId);

    int updateOrder(
            @Param("id") Long id,
            @Param("orderNo") Integer orderNo);

    int shiftOrders(
            @Param("playlistId") Long playlistId,
            @Param("offset") Integer offset);

    int bulkUpdateOrders(
            @Param("items") List<PlaylistItem> items);

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

    int copyItems(
            @Param("sourcePlaylistId") Long sourcePlaylistId,
            @Param("targetPlaylistId") Long targetPlaylistId);

    List<String> selectSpotifyUrisByPlaylistId(
            @Param("playlistId") Long playlistId);

    List<PlaylistExportTrack> selectExportTracks(
            @Param("playlistId") Long playlistId);

    int countByPlaylistId(@Param("playlistId") Long playlistId);

    int countSpotifyUrisByPlaylistId(@Param("playlistId") Long playlistId);

    int selectNextOrderNo(@Param("playlistId") Long playlistId);
}
