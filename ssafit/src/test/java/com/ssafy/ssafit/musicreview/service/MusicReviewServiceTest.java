package com.ssafy.ssafit.musicreview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.musicreview.domain.MusicReview;
import com.ssafy.ssafit.musicreview.domain.MusicReviewAverage;
import com.ssafy.ssafit.musicreview.mapper.MusicReviewMapper;

@ExtendWith(MockitoExtension.class)
class MusicReviewServiceTest {

    @Mock
    private MusicReviewMapper musicReviewMapper;

    @Mock
    private MusicMapper musicMapper;

    private MusicReviewService musicReviewService;

    @BeforeEach
    void setUp() {
        musicReviewService = new MusicReviewService(musicReviewMapper, musicMapper);
    }

    @Test
    void createReviewSucceeds() {
        MusicReview review = review(null, 1L, 5, 4);
        when(musicMapper.selectMusicDetail(1L)).thenReturn(Optional.of(new Music()));
        when(musicReviewMapper.existsByMemberAndMusic(10L, 1L)).thenReturn(false);
        when(musicReviewMapper.insert(review)).thenAnswer(invocation -> {
            review.setId(100L);
            return 1;
        });

        MusicReview created = musicReviewService.create(10L, review);

        assertThat(created.getId()).isEqualTo(100L);
        assertThat(created.getMemberId()).isEqualTo(10L);
        verify(musicReviewMapper).insert(review);
    }

    @Test
    void createReviewRejectsDuplicateReview() {
        MusicReview review = review(null, 1L, 5, 4);
        when(musicMapper.selectMusicDetail(1L)).thenReturn(Optional.of(new Music()));
        when(musicReviewMapper.existsByMemberAndMusic(10L, 1L)).thenReturn(true);

        assertError(
                ErrorCode.DUPLICATE_MUSIC_REVIEW,
                () -> musicReviewService.create(10L, review));

        verify(musicReviewMapper, never()).insert(review);
    }

    @Test
    void createReviewRejectsInvalidScore() {
        MusicReview review = review(null, 1L, 6, 4);
        when(musicMapper.selectMusicDetail(1L)).thenReturn(Optional.of(new Music()));

        assertError(
                ErrorCode.INVALID_SCORE,
                () -> musicReviewService.create(10L, review));

        verify(musicReviewMapper, never()).insert(review);
    }

    @Test
    void createReviewRejectsUnauthenticatedAccess() {
        assertError(
                ErrorCode.AUTH_REQUIRED,
                () -> musicReviewService.create(null, review(null, 1L, 5, 4)));
    }

    @Test
    void createReviewRejectsMissingMusic() {
        MusicReview review = review(null, 1L, 5, 4);
        when(musicMapper.selectMusicDetail(1L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.MUSIC_NOT_FOUND,
                () -> musicReviewService.create(10L, review));
    }

    @Test
    void updateReviewSucceedsForAuthor() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 19, 12, 0);
        MusicReview existing = review(100L, 1L, 3, 3);
        existing.setMemberId(10L);
        existing.setCreatedAt(createdAt);
        MusicReview changed = review(100L, 999L, 5, 4);
        when(musicReviewMapper.selectById(100L)).thenReturn(Optional.of(existing));

        MusicReview updated = musicReviewService.update(10L, changed);

        assertThat(updated.getMemberId()).isEqualTo(10L);
        assertThat(updated.getMusicId()).isEqualTo(1L);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getPreferenceScore()).isEqualTo(5);
        verify(musicReviewMapper).update(changed);
    }

    @Test
    void updateReviewRejectsOtherMember() {
        MusicReview existing = review(100L, 1L, 3, 3);
        existing.setMemberId(10L);
        when(musicReviewMapper.selectById(100L)).thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> musicReviewService.update(20L, review(100L, 1L, 5, 4)));

        verify(musicReviewMapper, never())
                .update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteReviewSucceedsForAuthor() {
        MusicReview existing = review(100L, 1L, 3, 3);
        existing.setMemberId(10L);
        when(musicReviewMapper.selectById(100L)).thenReturn(Optional.of(existing));

        musicReviewService.delete(10L, 100L);

        verify(musicReviewMapper).delete(100L);
    }

    @Test
    void deleteReviewRejectsOtherMember() {
        MusicReview existing = review(100L, 1L, 3, 3);
        existing.setMemberId(10L);
        when(musicReviewMapper.selectById(100L)).thenReturn(Optional.of(existing));

        assertError(
                ErrorCode.ACCESS_DENIED,
                () -> musicReviewService.delete(20L, 100L));

        verify(musicReviewMapper, never()).delete(100L);
    }

    @Test
    void getReviewsByMusicIdReturnsReviews() {
        MusicReview review = review(100L, 1L, 5, 4);
        when(musicMapper.selectMusicDetail(1L)).thenReturn(Optional.of(new Music()));
        when(musicReviewMapper.selectByMusicId(1L)).thenReturn(List.of(review));

        List<MusicReview> result = musicReviewService.getReviewsByMusicId(1L);

        assertThat(result).containsExactly(review);
    }

    @Test
    void getAverageScoresReturnsAggregatedScores() {
        MusicReviewAverage average = new MusicReviewAverage(1L, 4.5, 4.0, 2L);
        when(musicMapper.selectMusicDetail(1L)).thenReturn(Optional.of(new Music()));
        when(musicReviewMapper.selectAverageScores(1L)).thenReturn(average);

        MusicReviewAverage result = musicReviewService.getAverageScores(1L);

        assertThat(result.getAveragePreferenceScore()).isEqualTo(4.5);
        assertThat(result.getAverageRunningFitScore()).isEqualTo(4.0);
        assertThat(result.getReviewCount()).isEqualTo(2L);
    }

    @Test
    void missingReviewThrowsReviewNotFound() {
        when(musicReviewMapper.selectById(100L)).thenReturn(Optional.empty());

        assertError(
                ErrorCode.MUSIC_REVIEW_NOT_FOUND,
                () -> musicReviewService.delete(10L, 100L));
    }

    private MusicReview review(
            Long id,
            Long musicId,
            Integer preferenceScore,
            Integer runningFitScore) {
        MusicReview review = new MusicReview();
        review.setId(id);
        review.setMusicId(musicId);
        review.setPreferenceScore(preferenceScore);
        review.setRunningFitScore(runningFitScore);
        review.setReviewContent("러닝에 잘 어울립니다.");
        return review;
    }

    private void assertError(ErrorCode expected, Runnable executable) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
