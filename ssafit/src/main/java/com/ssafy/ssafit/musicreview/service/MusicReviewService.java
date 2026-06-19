package com.ssafy.ssafit.musicreview.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.musicreview.domain.MusicReview;
import com.ssafy.ssafit.musicreview.domain.MusicReviewAverage;
import com.ssafy.ssafit.musicreview.mapper.MusicReviewMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MusicReviewService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    private final MusicReviewMapper musicReviewMapper;
    private final MusicMapper musicMapper;

    @Transactional
    public MusicReview create(Long loginMemberId, MusicReview musicReview) {
        validateAuthenticated(loginMemberId);
        if (musicReview == null || musicReview.getMusicId() == null) {
            throw new CustomException(ErrorCode.MUSIC_NOT_FOUND);
        }

        validateMusicExists(musicReview.getMusicId());
        validateScores(musicReview);

        if (musicReviewMapper.existsByMemberAndMusic(
                loginMemberId,
                musicReview.getMusicId())) {
            throw new CustomException(ErrorCode.DUPLICATE_MUSIC_REVIEW);
        }

        musicReview.setMemberId(loginMemberId);
        try {
            musicReviewMapper.insert(musicReview);
        } catch (DuplicateKeyException exception) {
            throw new CustomException(ErrorCode.DUPLICATE_MUSIC_REVIEW);
        }
        return musicReview;
    }

    @Transactional
    public MusicReview update(Long loginMemberId, MusicReview musicReview) {
        validateAuthenticated(loginMemberId);
        if (musicReview == null || musicReview.getId() == null) {
            throw new CustomException(ErrorCode.MUSIC_REVIEW_NOT_FOUND);
        }

        MusicReview existing = getReview(musicReview.getId());
        validateAuthor(loginMemberId, existing.getMemberId());
        validateScores(musicReview);

        musicReview.setMemberId(existing.getMemberId());
        musicReview.setMusicId(existing.getMusicId());
        musicReview.setCreatedAt(existing.getCreatedAt());
        musicReviewMapper.update(musicReview);
        return musicReview;
    }

    @Transactional
    public void delete(Long loginMemberId, Long musicReviewId) {
        validateAuthenticated(loginMemberId);
        if (musicReviewId == null) {
            throw new CustomException(ErrorCode.MUSIC_REVIEW_NOT_FOUND);
        }

        MusicReview existing = getReview(musicReviewId);
        validateAuthor(loginMemberId, existing.getMemberId());
        musicReviewMapper.delete(musicReviewId);
    }

    public List<MusicReview> getReviewsByMusicId(Long musicId) {
        validateMusicExists(musicId);
        return musicReviewMapper.selectByMusicId(musicId);
    }

    public MusicReviewAverage getAverageScores(Long musicId) {
        validateMusicExists(musicId);
        return musicReviewMapper.selectAverageScores(musicId);
    }

    private MusicReview getReview(Long musicReviewId) {
        return musicReviewMapper.selectById(musicReviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.MUSIC_REVIEW_NOT_FOUND));
    }

    private void validateMusicExists(Long musicId) {
        if (musicId == null || musicMapper.selectMusicDetail(musicId).isEmpty()) {
            throw new CustomException(ErrorCode.MUSIC_NOT_FOUND);
        }
    }

    private void validateAuthenticated(Long loginMemberId) {
        if (loginMemberId == null) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
    }

    private void validateAuthor(Long loginMemberId, Long authorMemberId) {
        if (!loginMemberId.equals(authorMemberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateScores(MusicReview musicReview) {
        if (!isValidScore(musicReview.getPreferenceScore())
                || !isValidScore(musicReview.getRunningFitScore())) {
            throw new CustomException(ErrorCode.INVALID_SCORE);
        }
    }

    private boolean isValidScore(Integer score) {
        return score != null && score >= MIN_SCORE && score <= MAX_SCORE;
    }
}
