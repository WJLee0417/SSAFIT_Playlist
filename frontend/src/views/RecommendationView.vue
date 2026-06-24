<template>
  <AppLayout>
    <span class="eyebrow">AI RUNNING COACH</span>
    <h1>오늘의 러닝 목표로 플레이리스트 생성</h1>
    <p class="lead">
      Fitness Score와 최근 러닝 지표를 바탕으로 AI가 오늘의 페이스,
      케이던스, 심박 목표, 음악 BPM 범위를 제안합니다.
    </p>

    <form class="recommendation-query-form workout-query-form" @submit.prevent="requestRecommendation">
      <label>
        목표 거리
        <input
          v-model="query.targetDistanceKm"
          inputmode="decimal"
          placeholder="예: 5"
          autocomplete="off"
          required
        />
      </label>
      <label>
        목표 운동 강도
        <select v-model="query.intensity" required>
          <option disabled value="">강도 선택</option>
          <option value="LOW">LOW · 회복주</option>
          <option value="MODERATE">MODERATE · 지속주</option>
          <option value="HIGH">HIGH · 템포주</option>
          <option value="INTERVAL">INTERVAL · 인터벌</option>
        </select>
      </label>
      <label>
        선호 장르
        <input v-model="query.genre" maxlength="100" placeholder="비우면 프로필 장르 사용" />
      </label>
      <button type="submit" :disabled="querying">
        {{ querying ? "AI 계획 생성 중..." : "운동 계획 생성" }}
      </button>
    </form>

    <p v-if="validationMessage" class="field-error" role="alert">
      {{ validationMessage }}
    </p>

    <PageStatePanel
      v-if="queryStatus !== 'idle' && queryStatus !== 'ready'"
      :status="queryStatus"
      :error="queryDisplayError"
      :error-title="queryErrorTitle"
      :retryable="queryStatus === 'error' && queryError?.status >= 500"
      @retry="requestRecommendation"
    />

    <section v-if="queryError?.errorCode === 'PROFILE_NOT_FOUND'" class="profile-guidance">
      <h2>러닝 프로필을 먼저 작성해 주세요</h2>
      <p>나이, 성별, 목표 심박/케이던스가 있으면 AI 운동 목표가 더 정확해집니다.</p>
      <a class="primary-button" :href="profileUrl">러닝 프로필 작성</a>
    </section>

    <RecommendationResult
      v-if="queryStatus === 'ready' && result"
      :result="result"
      :saving="saving"
      :saved-playlist="savedPlaylist"
      :save-message="saveMessage"
      :save-message-type="saveMessageType"
      @save="createCurationCopy"
    />

    <section class="recommendation-log-section">
      <div class="page-heading">
        <div>
          <span class="eyebrow">RECENT LOGS</span>
          <h2>최근 AI 추천 로그</h2>
        </div>
        <button type="button" class="secondary-button" :disabled="logsLoading" @click="loadLogs">
          {{ logsLoading ? "불러오는 중..." : "로그 새로고침" }}
        </button>
      </div>
      <p v-if="logsError" class="save-status save-error">{{ recommendationErrorMessage(logsError) }}</p>
      <p v-else-if="!logsLoading && logs.length === 0" class="empty-inline">추천 로그가 없습니다.</p>
      <div v-else class="stack-list">
        <article
          v-for="log in logs"
          :key="log.id"
          class="recommendation-log-row"
          :class="{ selected: selectedLog?.id === log.id }"
        >
          <div>
            <strong>로그 #{{ displayLogNumber(log) }} · {{ intensityLabel(log.requestedIntensity) }}</strong>
            <small>
              {{ log.requestedBpmMin }}~{{ log.requestedBpmMax }} BPM ·
              {{ log.requestedGenre || "장르 제한 없음" }}
            </small>
            <small>{{ formatDateTime(log.createdAt) }}</small>
          </div>
          <div class="recommendation-log-actions">
            <button type="button" class="secondary-button inline-button" @click="selectLog(log)">
              {{ selectedLog?.id === log.id ? "상세 닫기" : "상세 보기" }}
            </button>
            <RouterLink
              v-if="log.savedPlaylistId"
              class="text-link"
              :to="{ name: 'playlist-detail', params: { playlistId: log.savedPlaylistId }, query: { returnTo: '/recommendations' } }"
            >
              저장된 플레이리스트 #{{ log.savedPlaylistId }}
            </RouterLink>
            <span v-else class="muted">아직 저장하지 않음</span>
          </div>
        </article>
      </div>

      <article v-if="selectedLog" class="detail-card recommendation-log-detail">
        <div class="page-heading">
          <div>
            <span class="eyebrow">LOG DETAIL</span>
            <h3>이전 AI 추천 로그 #{{ displayLogNumber(selectedLog) }}</h3>
          </div>
          <span class="source-badge">{{ formatDateTime(selectedLog.createdAt) }}</span>
        </div>
        <dl class="summary-list recommendation-summary">
          <div>
            <dt>운동 강도</dt>
            <dd>{{ intensityLabel(selectedLog.requestedIntensity) }}</dd>
          </div>
          <div>
            <dt>요청 BPM</dt>
            <dd>{{ selectedLog.requestedBpmMin }}~{{ selectedLog.requestedBpmMax }} BPM</dd>
          </div>
          <div>
            <dt>선호 장르</dt>
            <dd>{{ selectedLog.requestedGenre || "장르 제한 없음" }}</dd>
          </div>
          <div>
            <dt>추천 곡 수</dt>
            <dd>{{ selectedLogSummary.count ?? "확인 불가" }}</dd>
          </div>
          <div>
            <dt>목표 거리</dt>
            <dd>{{ selectedLogConditions.targetDistanceKm ? `${selectedLogConditions.targetDistanceKm}km` : "기록 없음" }}</dd>
          </div>
          <div>
            <dt>저장 여부</dt>
            <dd>{{ selectedLog.savedPlaylistId ? `플레이리스트 #${selectedLog.savedPlaylistId}` : "아직 저장하지 않음" }}</dd>
          </div>
        </dl>
        <section v-if="selectedWorkoutPlan" class="workout-segment-list" aria-label="이전 추천 운동 계획">
          <article
            v-for="segment in selectedWorkoutPlan.segments"
            :key="`${segment.name}-${segment.durationMinutes}`"
          >
            <strong>{{ segment.name }}</strong>
            <span>{{ segment.durationMinutes }}분 · {{ segment.bpmMin }}~{{ segment.bpmMax }} BPM</span>
            <small>{{ segment.purpose }}</small>
          </article>
        </section>
        <aside class="policy-callout">
          <strong>AI 추천 사유</strong>
          <span>{{ selectedLog.recommendationReason || "추천 사유가 기록되지 않았습니다." }}</span>
        </aside>
      </article>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { isApiError } from "../api/http";
import { recommendationApi } from "../api/recommendationApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import RecommendationResult from "../components/RecommendationResult.vue";
import {
  buildJspRunningProfileUrl,
  currentReturnPath,
} from "../utils/navigation";
import {
  recommendationErrorMessage,
  recommendationErrorState,
} from "../utils/recommendationError";

const route = useRoute();
const query = reactive({
  targetDistanceKm: "5",
  intensity: "",
  genre: "",
});
const result = ref(null);
const queryStatus = ref("idle");
const queryError = ref(null);
const validationMessage = ref("");
const querying = ref(false);
const saving = ref(false);
const savedPlaylist = ref(null);
const saveMessage = ref("");
const saveMessageType = ref("success");
const logs = ref([]);
const logsLoading = ref(false);
const logsError = ref(null);
const selectedLog = ref(null);
let querySequence = 0;

const intensityLabels = {
  LOW: "LOW",
  MODERATE: "MODERATE",
  HIGH: "HIGH",
  INTERVAL: "INTERVAL",
};
const profileUrl = computed(() => (
  buildJspRunningProfileUrl(currentReturnPath(route))
));
const queryErrorTitle = computed(() => {
  if (queryError.value?.errorCode === "RECOMMENDATION_DATA_NOT_ENOUGH") {
    return "추천할 로컬 음악 데이터가 부족합니다";
  }
  if (queryError.value?.errorCode === "INVALID_INTENSITY") {
    return "목표 운동 강도를 확인해 주세요";
  }
  if (queryError.value?.errorCode === "PROFILE_NOT_FOUND") {
    return "러닝 프로필이 필요합니다";
  }
  return "AI 추천 결과를 불러오지 못했습니다";
});
const queryDisplayError = computed(() => {
  if (!queryError.value) return null;
  return {
    status: queryError.value.status,
    errorCode: queryError.value.errorCode,
    message: recommendationErrorMessage(queryError.value),
    notFound: queryError.value.notFound,
    conflict: queryError.value.conflict,
    upstreamUnavailable: queryError.value.upstreamUnavailable,
  };
});
const selectedLogConditions = computed(() => (
  parseJsonObject(selectedLog.value?.requestConditions)
));
const selectedLogSummary = computed(() => (
  parseJsonObject(selectedLog.value?.resultSummary)
));
const selectedWorkoutPlan = computed(() => {
  const plan = selectedLogSummary.value?.workoutPlan;
  if (!plan || typeof plan !== "object") return null;
  return {
    ...plan,
    segments: Array.isArray(plan.segments) ? plan.segments : [],
  };
});

function normalizedCondition() {
  validationMessage.value = "";
  const targetDistanceKm = Number(String(query.targetDistanceKm).trim());
  if (!Number.isFinite(targetDistanceKm) || targetDistanceKm < 0.5 || targetDistanceKm > 100) {
    validationMessage.value = "목표 거리는 0.5~100km 사이로 입력해 주세요.";
    return null;
  }
  if (!Object.hasOwn(intensityLabels, query.intensity)) {
    validationMessage.value = "목표 운동 강도를 선택해 주세요.";
    return null;
  }
  return {
    targetDistanceKm: Math.round(targetDistanceKm * 100) / 100,
    intensity: query.intensity,
    genre: query.genre.trim(),
    page: 1,
  };
}

async function requestRecommendation() {
  const condition = normalizedCondition();
  if (!condition || querying.value) return;

  const sequence = ++querySequence;
  querying.value = true;
  queryStatus.value = "loading";
  queryError.value = null;
  result.value = null;
  savedPlaylist.value = null;
  saveMessage.value = "";

  try {
    const response = await recommendationApi.recommend(condition);
    if (sequence !== querySequence) return;
    if (!response?.log?.id || !Array.isArray(response?.music) || !response?.workoutPlan) {
      throw new Error("AI 추천 응답에 운동 계획 또는 음악 목록이 없습니다.");
    }
    result.value = response;
    queryStatus.value = "ready";
    await loadLogs();
    const createdLog = logs.value.find((log) => log.id === response.log.id);
    if (createdLog) {
      result.value = {
        ...response,
        log: {
          ...response.log,
          displayNumber: createdLog.displayNumber,
        },
      };
      selectedLog.value = createdLog;
    }
  } catch (caught) {
    if (sequence !== querySequence) return;
    queryError.value = caught;
    queryStatus.value = isApiError(caught)
      ? recommendationErrorState(caught)
      : "error";
  } finally {
    if (sequence === querySequence) querying.value = false;
  }
}

async function createCurationCopy(payload) {
  const logId = Number(result.value?.log?.id);
  if (!Number.isSafeInteger(logId) || logId < 1 || saving.value) return;

  saving.value = true;
  saveMessage.value = "";
  try {
    const response = await recommendationApi.save(logId, payload);
    const playlist = response?.playlist;
    if (!playlist?.id) {
      throw new Error("저장 응답에 플레이리스트 ID가 없습니다.");
    }
    savedPlaylist.value = playlist;
    saveMessageType.value = "success";
    saveMessage.value = "AI 운동 계획 플레이리스트를 내 목록에 추가했습니다.";
    await loadLogs();
  } catch (caught) {
    saveMessageType.value = "error";
    saveMessage.value = recommendationErrorMessage(caught);
    if (caught?.errorCode === "RECOMMENDATION_ALREADY_SAVED") {
      await loadLogs();
    }
  } finally {
    saving.value = false;
  }
}

async function loadLogs() {
  if (logsLoading.value) return;
  logsLoading.value = true;
  logsError.value = null;
  try {
    const response = await recommendationApi.getMyLogs();
    logs.value = Array.isArray(response) ? response : [];
    if (selectedLog.value) {
      selectedLog.value = logs.value.find((log) => log.id === selectedLog.value.id) || null;
    }
  } catch (caught) {
    logsError.value = caught;
  } finally {
    logsLoading.value = false;
  }
}

function selectLog(log) {
  selectedLog.value = selectedLog.value?.id === log.id ? null : log;
}

function displayLogNumber(log) {
  return log?.displayNumber || log?.id || "-";
}

function intensityLabel(value) {
  return intensityLabels[value] || value || "UNKNOWN";
}

function parseJsonObject(value) {
  if (!value) return {};
  if (typeof value === "object") return value;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed)
      ? parsed
      : {};
  } catch {
    return {};
  }
}

function formatDateTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

onMounted(loadLogs);
</script>
