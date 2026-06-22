const RUNNING_RECORD_ERROR_MESSAGES = {
  AUTH_REQUIRED: "로그인이 필요한 기능입니다.",
  ACCESS_DENIED: "본인의 러닝 기록만 조회하거나 변경할 수 있습니다.",
  RUNNING_RECORD_NOT_FOUND: "러닝 기록이 없거나 이미 삭제되었습니다.",
  INVALID_REQUEST: "날짜, 거리, 운동 시간과 메모를 다시 확인해 주세요.",
};

export function runningRecordErrorMessage(error) {
  return RUNNING_RECORD_ERROR_MESSAGES[error?.errorCode]
    || (error?.status >= 500
      ? "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
      : error?.message || "러닝 기록 요청을 처리하지 못했습니다.");
}

export function validateRunningRecordForm(form) {
  if (!form.recordDate) return "러닝 날짜를 선택해 주세요.";

  const distance = Number(form.distanceKm);
  if (!Number.isFinite(distance) || distance <= 0) {
    return "거리는 0보다 큰 값으로 입력해 주세요.";
  }
  if (!/^\d{1,8}(\.\d{1,2})?$/.test(String(form.distanceKm).trim())) {
    return "거리는 정수 8자리, 소수 2자리 이내로 입력해 주세요.";
  }

  const duration = Number(form.durationSeconds);
  if (!Number.isSafeInteger(duration) || duration <= 0) {
    return "운동 시간은 1초 이상의 정수로 입력해 주세요.";
  }
  if ((form.memo || "").length > 1000) {
    return "메모는 1,000자 이내로 입력해 주세요.";
  }
  return "";
}

export function runningRecordPayload(form) {
  return {
    recordDate: form.recordDate,
    distanceKm: Number(form.distanceKm),
    durationSeconds: Number(form.durationSeconds),
    memo: form.memo.trim() || null,
  };
}

export function formatDuration(totalSeconds) {
  const value = Number(totalSeconds);
  if (!Number.isFinite(value) || value < 0) return "-";
  const hours = Math.floor(value / 3600);
  const minutes = Math.floor((value % 3600) / 60);
  const seconds = Math.floor(value % 60);
  if (hours > 0) return `${hours}시간 ${minutes}분 ${seconds}초`;
  if (minutes > 0) return `${minutes}분 ${seconds}초`;
  return `${seconds}초`;
}

export function formatPace(paceSecondsPerKm) {
  const value = Number(paceSecondsPerKm);
  if (!Number.isFinite(value) || value <= 0) return "-";
  const minutes = Math.floor(value / 60);
  const seconds = Math.floor(value % 60);
  return `${minutes}'${String(seconds).padStart(2, "0")}"/km`;
}

export function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("ko-KR", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(date);
}
