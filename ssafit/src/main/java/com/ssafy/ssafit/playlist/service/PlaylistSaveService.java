package com.ssafy.ssafit.playlist.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistSave;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.mapper.PlaylistMapper;
import com.ssafy.ssafit.playlist.mapper.PlaylistSaveMapper;

import lombok.RequiredArgsConstructor;

/**
 * 기존 플레이리스트를 회원 저장 목록에 연결하는 참조 저장 서비스다.
 * 자동 큐레이션 저장은 이 테이블을 사용하지 않으며, 이후 별도 로직에서
 * USER + CURATION_COPY 플레이리스트를 생성하고 추천 곡을 복사해야 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistSaveService {

    private final PlaylistSaveMapper playlistSaveMapper;
    private final PlaylistMapper playlistMapper;

    @Transactional
    public PlaylistSave save(Long loginMemberId, Long playlistId) {
        validateAuthenticated(loginMemberId);
        Playlist playlist = getPlaylist(playlistId);
        validateSaveAccess(loginMemberId, playlist);

        if (playlistSaveMapper.existsByMemberAndPlaylist(loginMemberId, playlistId)) {
            throw new CustomException(ErrorCode.DUPLICATE_SAVE);
        }

        PlaylistSave playlistSave = new PlaylistSave();
        playlistSave.setMemberId(loginMemberId);
        playlistSave.setPlaylistId(playlistId);
        try {
            playlistSaveMapper.insert(playlistSave);
        } catch (DuplicateKeyException exception) {
            throw new CustomException(ErrorCode.DUPLICATE_SAVE);
        }
        return playlistSave;
    }

    @Transactional
    public void cancel(Long loginMemberId, Long playlistId) {
        validateAuthenticated(loginMemberId);
        getPlaylist(playlistId);

        if (!playlistSaveMapper.existsByMemberAndPlaylist(loginMemberId, playlistId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        playlistSaveMapper.delete(loginMemberId, playlistId);
    }

    public List<Playlist> getMySavedList(Long loginMemberId) {
        validateAuthenticated(loginMemberId);
        return playlistSaveMapper.selectSavedList(loginMemberId);
    }

    private Playlist getPlaylist(Long playlistId) {
        if (playlistId == null) {
            throw new CustomException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        return playlistMapper.selectDetail(playlistId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYLIST_NOT_FOUND));
    }

    private void validateSaveAccess(Long loginMemberId, Playlist playlist) {
        if (playlist.getPlaylistType() == PlaylistType.OFFICIAL
                || playlist.getVisibility() == PlaylistVisibility.PUBLIC) {
            return;
        }
        if (playlist.getOwnerId() == null
                || !loginMemberId.equals(playlist.getOwnerId())) {
            throw new CustomException(ErrorCode.PRIVATE_PLAYLIST);
        }
    }

    private void validateAuthenticated(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
    }
}
