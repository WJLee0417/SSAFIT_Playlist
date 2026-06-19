package com.ssafy.ssafit.spotify.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.ssafit.spotify.domain.SpotifyConnection;

@Mapper
public interface SpotifyConnectionMapper {

    int upsert(SpotifyConnection connection);

    Optional<SpotifyConnection> selectByMemberId(@Param("memberId") Long memberId);

    int updateToken(SpotifyConnection connection);
}
