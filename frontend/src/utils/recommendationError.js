const RECOMMENDATION_ERROR_MESSAGES = {
  AUTH_REQUIRED: "자동 큐레이션을 이용하려면 로그인이 필요합니다.",
  ACCESS_DENIED: "다른 사용자의 추천 로그는 저장할 수 없습니다.",
  PROFILE_NOT_FOUND: "러닝 프로필이 없습니다. 프로필을 먼저 작성하면 BPM과 선호 장르를 추천에 반영할 수 있습니다.",
  INVALID_INTENSITY: "목표 강도를 다시 선택해 주세요.",
  INVALID_REQUEST: "추천 곡 수와 조회 조건을 다시 확인해 주세요.",
  RECOMMENDATION_DATA_NOT_ENOUGH: "조건에 맞는 추천 음악이 부족합니다. 장르 조건을 비우거나 다른 강도를 선택해 보세요.",
  RECOMMENDATION_LOG_NOT_FOUND: "저장할 추천 로그를 찾을 수 없습니다. 추천을 다시 조회해 주세요.",
  RECOMMENDATION_ALREADY_SAVED: "이미 개인 플레이리스트로 저장된 추천입니다.",
  INVALID_PLAYLIST_TITLE: "플레이리스트 제목은 200자 이내로 입력해 주세요.",
};

export function recommendationErrorMessage(error) {
  return RECOMMENDATION_ERROR_MESSAGES[error?.errorCode]
    || (error?.status >= 500
      ? "서버에서 자동 큐레이션 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
      : error?.message || "자동 큐레이션 요청을 처리하지 못했습니다.");
}

export function recommendationErrorState(error) {
  if (error?.loginRequired) return "loginRequired";
  if (error?.status === 403) return "unauthorized";
  return "error";
}
