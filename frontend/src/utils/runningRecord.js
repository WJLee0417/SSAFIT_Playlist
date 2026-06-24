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

  const durationMinutes = Number(form.durationMinutes || 0);
  const durationSeconds = Number(form.durationSeconds || 0);
  if (!Number.isSafeInteger(durationMinutes) || durationMinutes < 0) {
    return "운동 시간의 분은 0 이상의 정수로 입력해 주세요.";
  }
  if (
    !Number.isSafeInteger(durationSeconds)
    || durationSeconds < 0
    || durationSeconds > 59
  ) {
    return "운동 시간의 초는 0부터 59까지의 정수로 입력해 주세요.";
  }
  if (runningRecordDurationSeconds(form) <= 0) {
    return "운동 시간은 1초 이상으로 입력해 주세요.";
  }

  const averageCadence = optionalInteger(form.averageCadence);
  if (
    averageCadence !== null
    && (!Number.isSafeInteger(averageCadence)
      || averageCadence < 60
      || averageCadence > 240)
  ) {
    return "케이던스는 60부터 240까지의 정수로 입력해 주세요.";
  }

  const averageHeartRateBpm = optionalInteger(form.averageHeartRateBpm);
  if (
    averageHeartRateBpm !== null
    && (!Number.isSafeInteger(averageHeartRateBpm)
      || averageHeartRateBpm < 30
      || averageHeartRateBpm > 240)
  ) {
    return "평균심박수는 30부터 240까지의 정수로 입력해 주세요.";
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
    durationSeconds: runningRecordDurationSeconds(form),
    averageCadence: optionalInteger(form.averageCadence),
    averageHeartRateBpm: optionalInteger(form.averageHeartRateBpm),
    memo: form.memo.trim() || null,
  };
}

export function runningRecordDurationSeconds(form) {
  return Number(form.durationMinutes || 0) * 60 + Number(form.durationSeconds || 0);
}

export function calculatePaceSecondsPerKm(distanceKm, durationSeconds) {
  const distance = Number(distanceKm);
  const duration = Number(durationSeconds);
  if (!Number.isFinite(distance) || distance <= 0 || duration <= 0) return null;
  return Math.round(duration / distance);
}

function optionalInteger(value) {
  if (value === null || value === undefined || String(value).trim() === "") return null;
  const number = Number(value);
  return Number.isSafeInteger(number) ? number : Number.NaN;
}

export function formatDuration(totalSeconds) {
  const value = Number(totalSeconds);
  if (!Number.isFinite(value) || value < 0) return "-";
  const hours = Math.floor(value / 3600);
  const minutes = Math.floor((value % 3600) / 60);
  const seconds = Math.floor(value % 60);
  if (hours > 0) {
    return [
      hours,
      minutes,
      seconds,
    ].map((part) => String(part).padStart(2, "0")).join(":");
  }
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

export function formatPace(paceSecondsPerKm) {
  const value = Number(paceSecondsPerKm);
  if (!Number.isFinite(value) || value <= 0) return "-";
  const minutes = Math.floor(value / 60);
  const seconds = Math.floor(value % 60);
  return `${minutes}'${String(seconds).padStart(2, "0")}" / km`;
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
