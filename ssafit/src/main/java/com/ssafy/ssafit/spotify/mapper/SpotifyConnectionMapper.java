package com.ssafy.ssafit.spotify.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.spotify.domain.SpotifyConnection;

@Mapper
public interface SpotifyConnectionMapper {

    int insert(SpotifyConnection connection);

    Optional<SpotifyConnection> selectByMemberId(@Param("memberId") Long memberId);

    int updateConnection(SpotifyConnection connection);

    int updateToken(SpotifyConnection connection);

    int deleteByMemberId(@Param("memberId") Long memberId);
}
