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
    INVALID_CADENCE_VALUE(
            HttpStatus.BAD_REQUEST,
            "케이던스는 60~240 SPM 범위여야 합니다."),
    INVALID_CADENCE_RANGE(
            HttpStatus.BAD_REQUEST,
            "목표 케이던스 최소값과 최대값을 함께 입력하고 올바른 SPM 범위로 설정해야 합니다."),
    INVALID_HEART_RATE_VALUE(
            HttpStatus.BAD_REQUEST,
            "평균 심박수는 30~240 BPM 범위여야 하며 음악 템포 BPM과 다른 값입니다."),

    RUNNING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "러닝 기록을 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "러닝 모임을 찾을 수 없습니다."),
    SESSION_CLOSED(HttpStatus.CONFLICT, "참여할 수 없는 상태의 러닝 모임입니다."),
    SESSION_FULL(HttpStatus.CONFLICT, "러닝 모임 정원이 가득 찼습니다."),
    DUPLICATE_SESSION_JOIN(HttpStatus.CONFLICT, "이미 참여 중인 러닝 모임입니다."),

    MUSIC_NOT_FOUND(HttpStatus.NOT_FOUND, "음악을 찾을 수 없습니다."),
    INVALID_BPM_VALUE(HttpStatus.BAD_REQUEST, "BPM 값은 60에서 220 사이여야 합니다."),
    INVALID_BPM_RANGE(HttpStatus.BAD_REQUEST, "BPM 범위가 올바르지 않습니다."),
    INVALID_MUSIC_BPM_VALUE(
            HttpStatus.BAD_REQUEST,
            "음악 템포 BPM은 60~220 범위여야 하며 평균 심박수 BPM과 다른 값입니다."),
    SPOTIFY_TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "Spotify 트랙 정보를 찾을 수 없습니다."),
    SPOTIFY_SEARCH_FAILED(HttpStatus.BAD_GATEWAY, "Spotify 곡 검색에 실패했습니다."),
    SPOTIFY_TRACK_IDENTITY_CONFLICT(
            HttpStatus.CONFLICT,
            "Spotify track ID와 URI가 서로 다른 내부 음악을 가리킵니다."),
    SPOTIFY_AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "Spotify 계정 연결이 필요합니다."),
    SPOTIFY_AUTH_DENIED(HttpStatus.FORBIDDEN, "Spotify 인증 권한이 거부되었습니다."),
    SPOTIFY_OAUTH_STATE_MISMATCH(HttpStatus.UNAUTHORIZED, "Spotify OAuth 요청 검증에 실패했습니다."),
    SPOTIFY_OAUTH_CALLBACK_ERROR(HttpStatus.BAD_REQUEST, "Spotify OAuth callback 처리에 실패했습니다."),
    SPOTIFY_CONFIGURATION_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Spotify OAuth 환경 설정이 올바르지 않습니다."),
    SPOTIFY_AUTH_URL_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Spotify 인증 URL을 생성할 수 없습니다."),
    SPOTIFY_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Spotify 토큰을 갱신할 수 없습니다."),
    SPOTIFY_API_ERROR(HttpStatus.BAD_GATEWAY, "Spotify API 호출에 실패했습니다."),
    SPOTIFY_PLAYLIST_CREATE_FAILED(
            HttpStatus.BAD_GATEWAY,
            "Spotify 플레이리스트 생성에 실패했습니다."),
    SPOTIFY_PLAYLIST_ITEM_ADD_FAILED(
            HttpStatus.BAD_GATEWAY,
            "Spotify 플레이리스트 수록곡 추가에 실패했습니다."),
    SPOTIFY_EXPORT_FAILED(HttpStatus.BAD_GATEWAY, "Spotify 플레이리스트 내보내기에 실패했습니다."),
    SPOTIFY_NOT_CONNECTED(
            HttpStatus.UNAUTHORIZED,
            "Spotify 계정이 연결되어 있지 않습니다."),
    SPOTIFY_AUTHORIZATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "Spotify 인증 또는 토큰 교환에 실패했습니다. 다시 연결해 주세요."),
    SPOTIFY_AUTHORIZATION_CANCELLED(
            HttpStatus.BAD_REQUEST,
            "Spotify 인증이 사용자에 의해 취소되었습니다."),
    SPOTIFY_STATE_MISMATCH(
            HttpStatus.UNAUTHORIZED,
            "Spotify OAuth state 검증에 실패했습니다. 연결을 처음부터 다시 시도해 주세요."),
    SPOTIFY_TOKEN_REFRESH_FAILED(
            HttpStatus.UNAUTHORIZED,
            "Spotify 토큰 갱신에 실패했습니다. Spotify 계정을 다시 연결해 주세요."),
    SPOTIFY_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "Spotify 권한이 부족하거나 개발 모드 허용 계정이 아닙니다."),
    SPOTIFY_RATE_LIMITED(
            HttpStatus.TOO_MANY_REQUESTS,
            "Spotify API 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),
    SPOTIFY_API_FAILED(
            HttpStatus.BAD_GATEWAY,
            "Spotify API 호출에 실패했습니다."),

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
    SPOTIFY_ALREADY_EXPORTED(HttpStatus.CONFLICT, "이미 Spotify로 내보낸 플레이리스트입니다."),
    DUPLICATE_SAVE(HttpStatus.CONFLICT, "이미 저장한 플레이리스트입니다."),

    INVALID_INTENSITY(HttpStatus.BAD_REQUEST, "러닝 강도가 올바르지 않습니다."),
    RECOMMENDATION_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 로그를 찾을 수 없습니다."),
    RECOMMENDATION_ALREADY_SAVED(HttpStatus.CONFLICT, "이미 플레이리스트로 저장된 추천입니다."),
    RECOMMENDATION_DATA_NOT_ENOUGH(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "추천에 필요한 음악 데이터가 충분하지 않습니다."),

    AI_RUNNING_REPORT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AI 러닝 리포트를 찾을 수 없습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
