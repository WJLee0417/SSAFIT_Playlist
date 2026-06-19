package com.ssafy.ssafit.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "로그인 ID 또는 비밀번호가 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),

    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "러닝 프로필을 찾을 수 없습니다."),
    INVALID_PROFILE_VALUE(HttpStatus.BAD_REQUEST, "러닝 프로필 값이 올바르지 않습니다."),

    MUSIC_NOT_FOUND(HttpStatus.NOT_FOUND, "음악을 찾을 수 없습니다."),
    INVALID_BPM_VALUE(HttpStatus.BAD_REQUEST, "BPM 값은 60에서 220 사이여야 합니다."),
    INVALID_BPM_RANGE(HttpStatus.BAD_REQUEST, "BPM 범위가 올바르지 않습니다."),
    SPOTIFY_TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "Spotify 트랙 정보를 찾을 수 없습니다."),

    MUSIC_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "음악 평가를 찾을 수 없습니다."),
    DUPLICATE_MUSIC_REVIEW(HttpStatus.CONFLICT, "이미 해당 음악을 평가했습니다."),
    INVALID_SCORE(HttpStatus.BAD_REQUEST, "평가 점수는 1에서 5 사이여야 합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트를 찾을 수 없습니다."),
    PLAYLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트 수록곡을 찾을 수 없습니다."),
    INVALID_PLAYLIST_TITLE(HttpStatus.BAD_REQUEST, "플레이리스트 제목이 올바르지 않습니다."),
    INVALID_PLAYLIST_ORDER(HttpStatus.BAD_REQUEST, "플레이리스트 수록곡 순서가 올바르지 않습니다."),
    DUPLICATE_PLAYLIST_ITEM(HttpStatus.CONFLICT, "이미 플레이리스트에 포함된 음악입니다."),
    DUPLICATE_PLAYLIST_ORDER(HttpStatus.CONFLICT, "플레이리스트 수록곡 순서가 중복되었습니다."),
    PRIVATE_PLAYLIST(HttpStatus.FORBIDDEN, "비공개 플레이리스트에 접근할 수 없습니다."),
    SPOTIFY_URI_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "Spotify URI가 없는 음악입니다."),
    DUPLICATE_SAVE(HttpStatus.CONFLICT, "이미 저장한 플레이리스트입니다."),

    INVALID_INTENSITY(HttpStatus.BAD_REQUEST, "러닝 강도가 올바르지 않습니다."),
    RECOMMENDATION_DATA_NOT_ENOUGH(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "추천에 필요한 음악 데이터가 충분하지 않습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
