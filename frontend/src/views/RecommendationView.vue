<template>
  <AppLayout>
    <span class="eyebrow">AUTO CURATION</span>
    <h1>러닝 맞춤 자동 큐레이션</h1>
    <p class="lead">
      추천 조회는 임시 결과와 추천 로그만 생성합니다. 개인 플레이리스트는 결과의 생성 버튼을 누른 뒤에만 만들어집니다.
    </p>

    <aside class="policy-callout">
      <strong>Spotify 검색 결과의 장르와 BPM은 자동으로 채워지지 않습니다.</strong>
      <span>
        프로필의 장르와 BPM 범위는 추천 기준과 로그로 남기며, 곡에 해당 메타데이터가 있을 때만
        매칭 점수에 반영합니다. 빈 음악 목록은 Spotify 장르 검색 필터로 후보를 확보하지만,
        가져온 곡의 장르나 BPM을 추측해서 저장하지는 않습니다.
      </span>
    </aside>

    <form class="recommendation-query-form" @submit.prevent="requestRecommendation(1)">
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
        운동 강도 목표
        <select v-model="query.runningIntensityGoal">
          <option value="">최근 평균 심박수 이력 참고</option>
          <option value="EASY">EASY · 회복주/가벼운 러닝</option>
          <option value="MODERATE">MODERATE · 지속주/일반 러닝</option>
          <option value="HIGH">HIGH · 템포런/인터벌</option>
        </select>
      </label>
      <label>
        목표 케이던스(SPM)
        <input
          v-model="query.targetCadenceSpm"
          inputmode="numeric"
          placeholder="비우면 프로필/최근 기록 사용"
        />
      </label>
      <label>
        선호 장르
        <input v-model="query.genre" maxlength="100" placeholder="비우면 프로필 장르 사용" />
      </label>
      <label>
        페이지당 곡 수
        <input v-model="query.limit" inputmode="numeric" placeholder="1~10" />
      </label>
      <div class="bpm-contract-field">
        <span>적용 BPM 조건</span>
        <strong>{{ expectedBpmLabel }}</strong>
        <small>곡 BPM이 저장된 경우에만 템포 매칭 점수에 반영됩니다.</small>
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
      @retry="requestRecommendation(query.page)"
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
      @page-change="requestRecommendation"
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
  targetCadenceSpm: "",
  runningIntensityGoal: "",
  genre: "",
  limit: "10",
  page: 1,
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
  const cadenceText = String(query.targetCadenceSpm).trim();
  if (cadenceText && !/^\d+$/.test(cadenceText)) {
    validationMessage.value = "목표 케이던스는 60~240 SPM 정수로 입력해 주세요.";
    return null;
  }
  const targetCadenceSpm = cadenceText ? Number(cadenceText) : undefined;
  if (targetCadenceSpm != null
      && (!Number.isSafeInteger(targetCadenceSpm)
      || targetCadenceSpm < 60
      || targetCadenceSpm > 240)) {
    validationMessage.value = "목표 케이던스는 60~240 SPM 정수로 입력해 주세요.";
    return null;
  }
  const limitText = String(query.limit).trim();
  if (!/^\d+$/.test(limitText)) {
    validationMessage.value = "페이지당 곡 수는 1~10 사이 정수로 입력해 주세요.";
    return null;
  }
  const limit = Number(limitText);
  if (!Number.isSafeInteger(limit) || limit < 1 || limit > 10) {
    validationMessage.value = "페이지당 곡 수는 1~10 사이 정수로 입력해 주세요.";
    return null;
  }
  return {
    intensity: query.intensity,
    targetCadenceSpm,
    runningIntensityGoal: query.runningIntensityGoal || undefined,
    genre: query.genre.trim(),
    limit,
    page: query.page,
  };
}

async function requestRecommendation(page = 1) {
  const targetPage = Number(page);
  if (!Number.isSafeInteger(targetPage) || targetPage < 1 || targetPage > 5) return;
  query.page = targetPage;
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
    queryStatus.value = "ready";
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
