const REVIEW_ERROR_MESSAGES = {
  AUTH_REQUIRED: "리뷰를 작성하려면 로그인이 필요합니다.",
  ACCESS_DENIED: "본인이 작성한 리뷰만 수정하거나 삭제할 수 있습니다.",
  DUPLICATE_MUSIC_REVIEW: "이미 이 음악에 리뷰를 작성했습니다. 기존 리뷰를 수정해 주세요.",
  INVALID_SCORE: "평가 점수는 1부터 5 사이의 정수여야 합니다.",
  MUSIC_NOT_FOUND: "리뷰를 작성할 음악을 찾을 수 없습니다.",
  MUSIC_REVIEW_NOT_FOUND: "대상 리뷰가 없거나 이미 삭제되었습니다.",
  INVALID_REQUEST: "리뷰 입력값을 다시 확인해 주세요.",
};

export function reviewErrorMessage(error) {
  return REVIEW_ERROR_MESSAGES[error?.errorCode]
    || (error?.status >= 500
      ? "서버에서 리뷰를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
      : error?.message || "리뷰 요청을 처리하지 못했습니다.");
}
