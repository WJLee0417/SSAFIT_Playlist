const SPOTIFY_ERROR_MESSAGES = {
  AUTH_REQUIRED: "SSAFIT 로그인이 필요합니다.",
  ACCESS_DENIED: "이 플레이리스트를 Spotify로 내보낼 권한이 없습니다.",
  SPOTIFY_AUTH_REQUIRED: "Spotify 계정을 연결한 뒤 다시 시도해 주세요.",
  SPOTIFY_AUTH_DENIED: "Spotify 인증이 취소되었습니다. 원할 때 다시 연결할 수 있습니다.",
  SPOTIFY_OAUTH_STATE_MISMATCH: "Spotify 인증 요청을 확인하지 못했습니다. 연결을 처음부터 다시 시도해 주세요.",
  SPOTIFY_OAUTH_CALLBACK_ERROR: "Spotify 인증 완료 처리에 실패했습니다. 다시 연결해 주세요.",
  SPOTIFY_CONFIGURATION_ERROR: "Spotify 연결 설정이 준비되지 않았습니다. 관리자에게 환경 설정을 확인해 달라고 요청해 주세요.",
  SPOTIFY_AUTH_URL_ERROR: "Spotify 인증 주소를 만들지 못했습니다. 잠시 후 다시 시도해 주세요.",
  SPOTIFY_TOKEN_EXPIRED: "Spotify 연결이 만료되었습니다. 다시 인증해 주세요.",
  SPOTIFY_SEARCH_FAILED: "Spotify 검색이 일시적으로 실패했습니다. 잠시 후 다시 시도해 주세요.",
  SPOTIFY_TRACK_NOT_FOUND: "선택한 곡을 Spotify에서 찾을 수 없습니다.",
  SPOTIFY_URI_NOT_FOUND: "이 곡에는 Spotify URI가 없어 수록하거나 내보낼 수 없습니다.",
  SPOTIFY_PLAYLIST_CREATE_FAILED: "Spotify 플레이리스트를 만들지 못했습니다. 다시 시도하기 전에 현재 상태를 확인해 주세요.",
  SPOTIFY_PLAYLIST_ITEM_ADD_FAILED: "Spotify 플레이리스트는 생성됐지만 곡 추가에 실패했습니다. 자동 재시도하지 말고 저장된 상태와 URL을 확인해 주세요.",
  SPOTIFY_ALREADY_EXPORTED: "이미 Spotify로 내보낸 플레이리스트입니다. 기존 Spotify 링크를 이용해 주세요.",
  SPOTIFY_EXPORT_FAILED: "이전 내보내기 정보가 남아 있거나 내보내기 상태가 불완전해 다시 실행할 수 없습니다.",
  DUPLICATE_PLAYLIST_ITEM: "이미 이 플레이리스트에 포함된 곡입니다.",
  DUPLICATE_PLAYLIST_ORDER: "이미 사용 중인 수록곡 순서입니다. 상세 화면에서 순서를 확인해 주세요.",
  PLAYLIST_NOT_FOUND: "플레이리스트를 찾을 수 없습니다.",
  INVALID_REQUEST: "검색어 또는 요청 값을 다시 확인해 주세요.",
};

export function spotifyErrorMessage(error) {
  return SPOTIFY_ERROR_MESSAGES[error?.errorCode]
    || (error?.status >= 500
      ? "Spotify 연동 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
      : error?.message || "Spotify 연동 요청을 처리하지 못했습니다.");
}

export function spotifyErrorState(error) {
  if (error?.loginRequired) return "loginRequired";
  if (error?.status === 403) return "unauthorized";
  return "error";
}

export function requiresSpotifyReconnect(error) {
  return [
    "SPOTIFY_AUTH_REQUIRED",
    "SPOTIFY_AUTH_DENIED",
    "SPOTIFY_TOKEN_EXPIRED",
    "SPOTIFY_OAUTH_STATE_MISMATCH",
    "SPOTIFY_OAUTH_CALLBACK_ERROR",
  ].includes(error?.errorCode);
}
