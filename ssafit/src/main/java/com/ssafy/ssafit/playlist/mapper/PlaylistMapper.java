package com.ssafy.ssafit.playlist.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;

@Mapper
public interface PlaylistMapper {

    int insert(Playlist playlist);

    List<Playlist> selectList(
            @Param("viewerMemberId") Long viewerMemberId,
            @Param("ownerId") Long ownerId);

    Optional<Playlist> selectDetail(@Param("id") Long id);

    List<Playlist> selectByIds(@Param("ids") List<Long> ids);

    Optional<Playlist> selectFavoriteByOwner(@Param("ownerId") Long ownerId);

    int update(Playlist playlist);

    int softDelete(@Param("id") Long id);

    Long selectOwnerId(@Param("id") Long id);

    int insertCurationCopy(Playlist playlist);

    int updateSpotifyExportResult(Playlist playlist);

    int updateSpotifyExportPending(Playlist playlist);

    int updateSpotifyExportFailure(Playlist playlist);

    int updateSpotifyExportPartial(Playlist playlist);

    int updateSpotifyExportStatus(
            @Param("id") Long id,
            @Param("status") SpotifyExportStatus status);

    Optional<Playlist> selectExportTargetPlaylist(@Param("id") Long id);
}
