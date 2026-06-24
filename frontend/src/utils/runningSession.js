const RUNNING_SESSION_ERROR_MESSAGES = {
  AUTH_REQUIRED: "로그인이 필요한 기능입니다.",
  ACCESS_DENIED: "이 모임을 변경하거나 참여를 취소할 권한이 없습니다.",
  SESSION_NOT_FOUND: "러닝 세션이 없거나 접근할 수 없습니다.",
  SESSION_CLOSED: "모집이 마감되었거나 취소된 러닝 세션입니다.",
  SESSION_FULL: "러닝 세션 정원이 모두 찼습니다.",
  DUPLICATE_SESSION_JOIN: "이미 참여 중인 러닝 세션입니다.",
  INVALID_REQUEST: "모임 제목, 장소, 일정, 정원을 다시 확인해 주세요.",
};

export function runningSessionErrorMessage(error) {
  return RUNNING_SESSION_ERROR_MESSAGES[error?.errorCode]
    || (error?.status >= 500
      ? "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
      : error?.message || "러닝 세션 요청을 처리하지 못했습니다.");
}

export function validateRunningSessionForm(form) {
  const title = form.title.trim();
  if (!title) return "모임 제목을 입력해 주세요.";
  if (title.length > 200) return "모임 제목은 200자 이내로 입력해 주세요.";

  const meetingPlace = form.meetingPlace.trim();
  if (!meetingPlace) return "집결 장소를 입력해 주세요.";
  if (meetingPlace.length > 255) {
    return "집결 장소는 255자 이내로 입력해 주세요.";
  }
  if (!form.scheduledAt) return "모임 일시를 선택해 주세요.";

  const capacity = Number(form.capacity);
  if (!Number.isSafeInteger(capacity) || capacity <= 0) {
    return "정원은 1명 이상의 정수로 입력해 주세요.";
  }
  if ((form.description || "").length > 2000) {
    return "설명은 2,000자 이내로 입력해 주세요.";
  }
  return "";
}

export function runningSessionPayload(form) {
  return {
    title: form.title.trim(),
    description: form.description.trim() || null,
    meetingPlace: form.meetingPlace.trim(),
    scheduledAt: form.scheduledAt,
    capacity: Number(form.capacity),
  };
}

export function formatSessionDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("ko-KR", {
      dateStyle: "full",
      timeStyle: "short",
    }).format(date);
}

export function formatSessionListDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const year = String(date.getFullYear()).slice(2);
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const weekday = new Intl.DateTimeFormat("ko-KR", { weekday: "short" }).format(date);
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${year}${month}${day} (${weekday}) ${hours}:${minutes}`;
}

export function toDateTimeLocalValue(value) {
  return typeof value === "string" ? value.slice(0, 16) : "";
}

export function statusLabel(status) {
  return {
    OPEN: "모집 중",
    CLOSED: "모집 마감",
    CANCELLED: "취소됨",
  }[status] || status || "상태 미정";
}
