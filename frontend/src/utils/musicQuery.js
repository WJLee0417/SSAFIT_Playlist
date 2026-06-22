export const DEFAULT_MUSIC_QUERY = Object.freeze({
  keyword: "",
  genre: "",
  minBpm: "",
  maxBpm: "",
  runningIntensity: "",
  sortType: "CREATED_DESC",
  page: 1,
  size: 20,
});

const INTENSITIES = new Set(["LOW", "MODERATE", "HIGH", "INTERVAL"]);
const SORT_TYPES = new Set([
  "CREATED_DESC",
  "TITLE_ASC",
  "BPM_ASC",
  "BPM_DESC",
  "RUNNING_FIT_DESC",
]);

function first(value) {
  return Array.isArray(value) ? value[0] : value;
}

function positiveInteger(value, fallback) {
  const parsed = Number.parseInt(first(value), 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function optionalBpm(value) {
  const raw = first(value);
  if (raw === undefined || raw === null || raw === "") return "";
  const parsed = Number(raw);
  return Number.isInteger(parsed) ? parsed : "";
}

export function musicQueryFromRoute(query = {}) {
  const intensity = first(query.runningIntensity);
  const sortType = first(query.sortType);
  return {
    keyword: String(first(query.keyword) || "").trim(),
    genre: String(first(query.genre) || "").trim(),
    minBpm: optionalBpm(query.minBpm),
    maxBpm: optionalBpm(query.maxBpm),
    runningIntensity: INTENSITIES.has(intensity) ? intensity : "",
    sortType: SORT_TYPES.has(sortType) ? sortType : DEFAULT_MUSIC_QUERY.sortType,
    page: positiveInteger(query.page, DEFAULT_MUSIC_QUERY.page),
    size: Math.min(100, positiveInteger(query.size, DEFAULT_MUSIC_QUERY.size)),
  };
}

export function validateMusicQuery(query) {
  for (const [key, label] of [["minBpm", "최소 BPM"], ["maxBpm", "최대 BPM"]]) {
    const value = query[key];
    if (value !== "" && (!Number.isInteger(value) || value < 60 || value > 220)) {
      return `${label}은 60부터 220 사이의 정수로 입력해 주세요.`;
    }
  }
  if (
    query.minBpm !== "" &&
    query.maxBpm !== "" &&
    query.minBpm > query.maxBpm
  ) {
    return "최소 BPM은 최대 BPM보다 클 수 없습니다.";
  }
  return "";
}

export function musicQueryToApi(query) {
  return {
    keyword: query.keyword || undefined,
    genre: query.genre || undefined,
    minBpm: query.minBpm === "" ? undefined : query.minBpm,
    maxBpm: query.maxBpm === "" ? undefined : query.maxBpm,
    runningIntensity: query.runningIntensity || undefined,
    sortType: query.sortType || DEFAULT_MUSIC_QUERY.sortType,
    page: query.page,
    size: query.size,
  };
}

export function musicQueryToRoute(query) {
  const normalized = musicQueryToApi(query);
  return Object.fromEntries(
    Object.entries(normalized).filter(([, value]) => value !== undefined),
  );
}

export function normalizeMusicPage(response, fallbackQuery) {
  const items = Array.isArray(response?.items) ? response.items : [];
  const page = positiveInteger(response?.page, fallbackQuery.page);
  const size = positiveInteger(response?.size, fallbackQuery.size);
  const totalCount = Number.isFinite(Number(response?.totalCount))
    ? Math.max(0, Number(response.totalCount))
    : items.length;
  const reportedTotalPages = Number(response?.totalPages);
  const totalPages = Number.isInteger(reportedTotalPages) && reportedTotalPages >= 0
    ? reportedTotalPages
    : (totalCount === 0 ? 0 : Math.ceil(totalCount / size));
  return { items, page, size, totalCount, totalPages };
}
