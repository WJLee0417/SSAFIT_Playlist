const PLAYLIST_ERROR_MESSAGES = {
  AUTH_REQUIRED: "로그인이 필요한 기능입니다.",
  ACCESS_DENIED: "이 플레이리스트를 변경할 권한이 없습니다.",
  PRIVATE_PLAYLIST: "비공개 플레이리스트에 접근하거나 저장할 수 없습니다.",
  PLAYLIST_NOT_FOUND: "플레이리스트가 없거나 삭제되었습니다.",
  PLAYLIST_ITEM_NOT_FOUND: "대상 수록곡이 없거나 이미 삭제되었습니다.",
  MUSIC_NOT_FOUND: "추가할 음악을 찾을 수 없습니다.",
  INVALID_PLAYLIST_TITLE: "플레이리스트 제목을 입력해 주세요.",
  INVALID_PLAYLIST_ORDER: "수록곡 순서는 1 이상의 정수여야 합니다.",
  DUPLICATE_PLAYLIST_ITEM: "이미 이 플레이리스트에 포함된 음악입니다.",
  DUPLICATE_PLAYLIST_ORDER: "이미 사용 중인 순서입니다. 비어 있는 순서 번호를 선택해 주세요.",
  DUPLICATE_SAVE: "이미 내 저장 목록에 있는 플레이리스트입니다.",
  SPOTIFY_URI_NOT_FOUND: "Spotify 기반 음악의 URI 정보가 없어 추가할 수 없습니다.",
  INVALID_REQUEST: "입력값을 다시 확인해 주세요.",
};

export function playlistErrorMessage(error) {
  return PLAYLIST_ERROR_MESSAGES[error?.errorCode]
    || (error?.status >= 500
      ? "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
      : error?.message || "플레이리스트 요청을 처리하지 못했습니다.");
}
