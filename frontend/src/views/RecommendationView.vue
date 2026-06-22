<template>
  <AppLayout>
    <span class="eyebrow">AUTO CURATION</span>
    <h1>러닝 맞춤 자동 큐레이션</h1>
    <p class="lead">
      추천 조회는 임시 결과와 추천 로그만 생성합니다. 개인 플레이리스트는 결과의 생성 버튼을 누른 뒤에만 만들어집니다.
    </p>

    <aside class="policy-callout">
      <strong>조회만으로는 플레이리스트가 생성되지 않습니다.</strong>
      <span>
        실제 조회 API는 BPM 값을 받지 않습니다. 러닝 프로필의 유효한 BPM 범위를 우선 사용하고,
        없으면 선택한 강도의 기본 범위를 적용합니다.
      </span>
    </aside>

    <form class="recommendation-query-form" @submit.prevent="requestRecommendation">
      <label>
        목표 강도
        <select v-model="query.intensity" required>
          <option disabled value="">강도 선택</option>
          <option value="LOW">LOW</option>
          <option value="MODERATE">MODERATE</option>
          <option value="HIGH">HIGH</option>
          <option value="INTERVAL">INTERVAL</option>
        </select>
      </label>
      <label>
        선호 장르
        <input v-model="query.genre" maxlength="100" placeholder="비우면 프로필 장르 사용" />
      </label>
      <label>
        추천 곡 수
        <input v-model="query.size" inputmode="numeric" placeholder="1~50" />
      </label>
      <div class="bpm-contract-field">
        <span>적용 BPM 조건</span>
        <strong>{{ expectedBpmLabel }}</strong>
        <small>조회 완료 후 응답 로그의 실제 BPM 범위를 표시합니다.</small>
      </div>
      <button type="submit" :disabled="querying">
        {{ querying ? "추천 조회 중..." : "추천 조회" }}
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
      <p>BPM 범위와 선호 장르를 저장한 뒤 같은 화면으로 돌아와 다시 조회할 수 있습니다.</p>
      <a class="primary-button" :href="profileUrl">JSP 러닝 프로필 작성</a>
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
          <h2>최근 추천 로그</h2>
        </div>
        <button type="button" class="secondary-button" :disabled="logsLoading" @click="loadLogs">
          {{ logsLoading ? "불러오는 중..." : "로그 새로고침" }}
        </button>
      </div>
      <p class="muted">
        로그의 `savedPlaylistId`는 개인 CURATION_COPY 생성 여부이며 참조 저장 여부가 아닙니다.
      </p>
      <p v-if="logsError" class="save-status save-error">{{ recommendationErrorMessage(logsError) }}</p>
      <p v-else-if="!logsLoading && logs.length === 0" class="empty-inline">추천 로그가 없습니다.</p>
      <div v-else class="stack-list">
        <article v-for="log in logs" :key="log.id" class="recommendation-log-row">
          <div>
            <strong>로그 #{{ log.id }} · {{ log.requestedIntensity }}</strong>
            <small>
              {{ log.requestedBpmMin }}~{{ log.requestedBpmMax }} BPM ·
              {{ log.requestedGenre || "장르 제한 없음" }}
            </small>
          </div>
          <RouterLink
            v-if="log.savedPlaylistId"
            class="text-link"
            :to="{ name: 'playlist-detail', params: { playlistId: log.savedPlaylistId }, query: { returnTo: '/recommendations' } }"
          >
            생성된 플레이리스트 #{{ log.savedPlaylistId }}
          </RouterLink>
          <span v-else class="muted">아직 개인 플레이리스트 미생성</span>
        </article>
      </div>
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
  intensity: "",
  genre: "",
  size: "10",
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
let querySequence = 0;

const defaultBpmRanges = {
  LOW: [90, 130],
  MODERATE: [120, 160],
  HIGH: [150, 190],
  INTERVAL: [140, 200],
};
const expectedBpmLabel = computed(() => {
  if (!query.intensity) return "강도를 선택하면 기본 범위를 확인할 수 있습니다.";
  const range = defaultBpmRanges[query.intensity];
  return `프로필 BPM 우선 · 기본 ${range[0]}~${range[1]} BPM`;
});
const profileUrl = computed(() => (
  buildJspRunningProfileUrl(currentReturnPath(route))
));
const queryErrorTitle = computed(() => {
  if (queryError.value?.errorCode === "RECOMMENDATION_DATA_NOT_ENOUGH") {
    return "추천 데이터가 부족합니다";
  }
  if (queryError.value?.errorCode === "INVALID_INTENSITY") {
    return "목표 강도를 확인해 주세요";
  }
  if (queryError.value?.errorCode === "PROFILE_NOT_FOUND") {
    return "러닝 프로필이 필요합니다";
  }
  return "추천 결과를 불러오지 못했습니다";
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

function normalizedCondition() {
  validationMessage.value = "";
  if (!Object.hasOwn(defaultBpmRanges, query.intensity)) {
    validationMessage.value = "목표 강도를 선택해 주세요.";
    return null;
  }
  const sizeText = String(query.size).trim();
  if (!/^\d+$/.test(sizeText)) {
    validationMessage.value = "추천 곡 수는 1~50 사이 정수로 입력해 주세요.";
    return null;
  }
  const size = Number(sizeText);
  if (!Number.isSafeInteger(size) || size < 1 || size > 50) {
    validationMessage.value = "추천 곡 수는 1~50 사이 정수로 입력해 주세요.";
    return null;
  }
  return {
    intensity: query.intensity,
    genre: query.genre.trim(),
    size,
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
    if (!response?.log?.id || !Array.isArray(response?.music)) {
      throw new Error("추천 응답에 로그 ID 또는 음악 목록이 없습니다.");
    }
    result.value = response;
    queryStatus.value = response.music.length ? "ready" : "empty";
    await loadLogs();
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
      throw new Error("저장 응답에 생성된 플레이리스트 ID가 없습니다.");
    }
    savedPlaylist.value = playlist;
    saveMessageType.value = "success";
    saveMessage.value = "추천 결과를 새 개인 CURATION_COPY 플레이리스트로 생성했습니다.";
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
  } catch (caught) {
    logsError.value = caught;
  } finally {
    logsLoading.value = false;
  }
}

onMounted(loadLogs);
</script>
