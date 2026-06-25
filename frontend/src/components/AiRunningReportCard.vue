<template>
  <section class="ai-running-report-card">
    <button
      type="button"
      class="ai-running-report-toggle"
      :aria-expanded="expanded"
      @click="expanded = !expanded"
    >
      <h2>AI 요약리포트</h2>
      <span aria-hidden="true">{{ expanded ? "접기" : "펼치기" }}</span>
    </button>

    <div v-if="expanded" class="ai-running-report-body">
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
      </div>

      <div v-else-if="!report" class="ai-running-report-empty">
        <h3>아직 생성된 AI 러닝 리포트가 없습니다</h3>
        <p>최근 러닝 기록을 바탕으로 요약 리포트를 만들 수 있습니다.</p>
        <button type="button" class="primary-button" :disabled="busy" @click="generate(false)">
          {{ busy ? "생성 중..." : "리포트 생성" }}
        </button>
      </div>

      <template v-else>
        <div class="ai-running-report-topline">
          <strong>{{ analysisPeriod }}</strong>
          <button
            type="button"
            class="secondary-button inline-button"
            :disabled="busy"
            @click="generate(true)"
          >
            {{ busy ? "새로 생성 중..." : "새로고침" }}
          </button>
        </div>

        <p v-if="report.dataInsufficient" class="ai-running-report-note warning">
          일부 데이터가 부족합니다. {{ missingItemsText }}
        </p>

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
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { aiRunningReportApi } from "../api/aiRunningReportApi";
import { isApiError } from "../api/http";
import { buildJspLoginUrl } from "../utils/navigation";

const report = ref(null);
const expanded = ref(false);
const loadingLatest = ref(false);
const busy = ref(false);
const loadError = ref(null);
const loginRequired = ref(false);

const content = computed(() => report.value?.reportContent || {});
const analysisPeriod = computed(() => {
  const start = report.value?.analysisStartDate;
  const end = report.value?.analysisEndDate;
  if (!start && !end) return "집계 기간 : 데이터 부족";
  if (start && end) return `집계 기간 : ${start} ~ ${end}`;
  return `집계 기간 : ${start || end}`;
});
const missingItemsText = computed(() => {
  const items = (report.value?.missingItems || [])
    .filter((item) => !["RECOMMENDATION_LOGS", "RECOMMENDATION_HISTORY"].includes(item));
  if (!items.length) return "부족 항목은 명시되지 않았습니다.";
  return `부족 항목: ${items.map(formatMissingItem).join(", ")}`;
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
    title: "페이스와 강도",
    text: content.value.paceAndIntensityInsight || "페이스와 강도 해석을 준비할 수 없습니다.",
  },
  {
    key: "cadenceAndHeartRateInsight",
    title: "케이던스와 심박",
    text: content.value.cadenceAndHeartRateInsight || "케이던스 또는 심박 데이터가 부족합니다.",
  },
  {
    key: "nextRunningGoal",
    title: "다음 러닝 제안",
    text: content.value.nextRunningGoal || "먼저 가벼운 러닝 기록을 쌓아 주세요.",
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

function formatMissingItem(value) {
  return {
    RUNNING_RECORDS: "러닝 기록",
    ENOUGH_RUNNING_RECORDS: "충분한 러닝 기록",
    CADENCE: "케이던스",
    CADENCE_LIMITED: "케이던스 일부",
    HEART_RATE: "심박",
    HEART_RATE_LIMITED: "심박 일부",
  }[value] || value;
}

onMounted(loadLatest);
</script>
