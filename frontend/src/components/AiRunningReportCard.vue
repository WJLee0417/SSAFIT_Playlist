<template>
  <section class="ai-running-report-card">
    <div class="ai-running-report-header">
      <div>
        <span class="eyebrow">AI RUNNING REPORT</span>
        <h2>최근 러닝 리포트</h2>
      </div>
      <div class="ai-running-report-actions">
        <span
          v-if="report"
          class="ai-running-report-badge"
          :class="{ fallback: isFallback }"
        >
          {{ sourceLabel }}
        </span>
        <button
          v-if="report"
          type="button"
          class="secondary-button"
          :disabled="busy"
          @click="generate(true)"
        >
          {{ busy ? "새로고침 중" : "새로고침" }}
        </button>
      </div>
    </div>

    <div v-if="loadingLatest" class="ai-running-report-state">
      <span class="spinner" aria-hidden="true"></span>
      <p>AI 러닝 리포트를 확인하는 중입니다.</p>
    </div>

    <div v-else-if="loginRequired" class="ai-running-report-state">
      <h3>로그인이 필요합니다</h3>
      <p>로그인 세션이 만료되었습니다. 다시 로그인한 뒤 리포트를 확인해 주세요.</p>
      <a class="primary-button" :href="loginUrl">로그인으로 이동</a>
    </div>

    <div v-else-if="loadError" class="ai-running-report-state error">
      <h3>리포트를 불러오지 못했습니다</h3>
      <p>{{ errorMessage }}</p>
      <button type="button" class="secondary-button" :disabled="busy" @click="loadLatest">
        다시 시도
      </button>
    </div>

    <div v-else-if="!report" class="ai-running-report-empty">
      <h3>아직 생성된 AI 러닝 리포트가 없습니다</h3>
      <p>최근 러닝 기록과 음악 추천 로그를 바탕으로 요약 카드를 만들 수 있습니다.</p>
      <button type="button" class="primary-button" :disabled="busy" @click="generate(false)">
        {{ busy ? "생성 중" : "리포트 생성" }}
      </button>
    </div>

    <template v-else>
      <div class="ai-running-report-meta">
        <div>
          <dt>분석 기간</dt>
          <dd>{{ analysisPeriod }}</dd>
        </div>
        <div>
          <dt>생성 시각</dt>
          <dd>{{ createdAtText }}</dd>
        </div>
        <div>
          <dt>상태</dt>
          <dd>{{ statusLabel }}</dd>
        </div>
      </div>

      <p v-if="isFallback" class="ai-running-report-note">
        현재 리포트는 AI 응답 대신 서버가 만든 안전 요약입니다.
      </p>
      <p v-if="report.dataInsufficient" class="ai-running-report-note warning">
        일부 데이터가 부족합니다. {{ missingItemsText }}
      </p>
      <RouterLink
        v-if="needsRecommendationHistory"
        class="secondary-button ai-running-report-recommendation-link"
        to="/recommendations"
      >
        음악 추천 받기
      </RouterLink>

      <div class="ai-running-report-sections">
        <article
          v-for="section in sections"
          :key="section.key"
          class="ai-running-report-section"
        >
          <h3>{{ section.title }}</h3>
          <p>{{ section.text }}</p>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { aiRunningReportApi } from "../api/aiRunningReportApi";
import { isApiError } from "../api/http";
import { buildJspLoginUrl } from "../utils/navigation";

const report = ref(null);
const loadingLatest = ref(false);
const busy = ref(false);
const loadError = ref(null);
const loginRequired = ref(false);

const content = computed(() => report.value?.reportContent || {});
const isFallback = computed(() => report.value?.generationSource === "FALLBACK");
const sourceLabel = computed(() => (isFallback.value ? "안전 요약" : "AI 생성"));
const statusLabel = computed(() => {
  if (!report.value?.status) return "-";
  return {
    COMPLETED: "완료",
    FAILED: "실패",
    PENDING: "생성 중",
  }[report.value.status] || report.value.status;
});
const analysisPeriod = computed(() => {
  const start = report.value?.analysisStartDate;
  const end = report.value?.analysisEndDate;
  if (!start && !end) return "데이터 부족";
  if (start && end) return `${start} ~ ${end}`;
  return start || end;
});
const createdAtText = computed(() => formatDateTime(report.value?.createdAt));
const missingItemsText = computed(() => {
  const items = report.value?.missingItems || [];
  if (!items.length) return "부족 항목이 명시되지 않았습니다.";
  return `부족 항목: ${items.map(formatMissingItem).join(", ")}`;
});
const needsRecommendationHistory = computed(() => {
  const items = report.value?.missingItems || [];
  return items.includes("RECOMMENDATION_HISTORY")
    || items.includes("RECOMMENDATION_LOGS");
});
const errorMessage = computed(() => (
  loadError.value?.message || "잠시 후 다시 시도해 주세요."
));
const loginUrl = computed(() => buildJspLoginUrl(window.location.href));
const sections = computed(() => [
  {
    key: "runningPatternSummary",
    title: "러닝 패턴",
    text: content.value.runningPatternSummary || "러닝 패턴을 판단할 데이터가 부족합니다.",
  },
  {
    key: "paceAndIntensityInsight",
    title: "페이스·강도",
    text: content.value.paceAndIntensityInsight || "페이스와 강도 해석을 준비할 수 없습니다.",
  },
  {
    key: "cadenceAndHeartRateInsight",
    title: "케이던스·심박 참고",
    text: content.value.cadenceAndHeartRateInsight || "케이던스 또는 심박 데이터가 부족합니다.",
  },
  {
    key: "musicRecommendationDirection",
    title: "음악 추천 방향",
    text: content.value.musicRecommendationDirection || "최근 음악 추천 로그가 부족합니다.",
  },
  {
    key: "nextRunningGoal",
    title: "다음 러닝 제안",
    text: content.value.nextRunningGoal || "먼저 가벼운 러닝 기록을 남겨 주세요.",
  },
]);

async function loadLatest() {
  loadingLatest.value = true;
  loadError.value = null;
  loginRequired.value = false;
  try {
    report.value = await aiRunningReportApi.getLatest();
  } catch (error) {
    if (isApiError(error) && error.loginRequired) {
      loginRequired.value = true;
      report.value = null;
    } else if (isApiError(error) && error.notFound) {
      report.value = null;
    } else {
      loadError.value = error;
    }
  } finally {
    loadingLatest.value = false;
  }
}

async function generate(forceRefresh) {
  if (busy.value) return;
  busy.value = true;
  loadError.value = null;
  loginRequired.value = false;
  try {
    report.value = await aiRunningReportApi.generate({ forceRefresh });
  } catch (error) {
    if (isApiError(error) && error.loginRequired) {
      loginRequired.value = true;
    } else {
      loadError.value = error;
    }
  } finally {
    busy.value = false;
  }
}

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatMissingItem(value) {
  return {
    RUNNING_RECORDS: "러닝 기록",
    ENOUGH_RUNNING_RECORDS: "충분한 러닝 기록",
    CADENCE: "케이던스",
    CADENCE_LIMITED: "케이던스 일부",
    HEART_RATE: "심박",
    HEART_RATE_LIMITED: "심박 일부",
    RECOMMENDATION_LOGS: "음악 추천 로그",
    RECOMMENDATION_HISTORY: "최근 음악 추천 이력",
  }[value] || value;
}

onMounted(loadLatest);
</script>
