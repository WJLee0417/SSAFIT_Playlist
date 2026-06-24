<template>
  <AppLayout>
    <div class="page-heading running-record-page-heading">
      <div class="running-record-title-stack">
        <div class="running-record-title-row">
          <h1>내 러닝 기록</h1>
          <RouterLink class="primary-button running-record-create-link" to="/running-records/new">
            새 기록
          </RouterLink>
        </div>
        <p class="lead running-records-lead">
          꾸준한 기록으로 러닝 퍼포먼스를 추적하세요
        </p>
      </div>
    </div>

    <PageStatePanel
      :status="status"
      :error="error"
      empty-title="아직 러닝 기록이 없습니다"
      empty-message="첫 러닝 기록을 등록해 보세요."
      error-title="러닝 기록을 불러오지 못했습니다"
      @retry="load"
    >
      <template #empty-action>
        <RouterLink class="primary-button" to="/running-records/new">첫 기록 등록</RouterLink>
      </template>
    </PageStatePanel>

    <section v-if="status === 'ready'" class="running-record-dashboard">
      <div class="running-record-scope-toggle" aria-label="러닝 기록 조회 기준">
        <button
          v-for="scope in scopeOptions"
          :key="scope.value"
          type="button"
          :class="{ active: activeScope === scope.value }"
          @click="setScope(scope.value)"
        >
          {{ scope.label }}
        </button>
      </div>

      <div v-if="activeScope !== 'all'" class="running-record-scope-detail">
        <label v-if="activeScope === 'year'">
          <span>조회 연도</span>
          <select v-model="selectedYear">
            <option
              v-for="year in availableYears"
              :key="year"
              :value="year"
            >
              {{ year }}
            </option>
          </select>
        </label>
        <label v-else>
          <span>조회 월</span>
          <select v-model="selectedMonth">
            <option
              v-for="month in availableMonths"
              :key="month"
              :value="month"
            >
              {{ formatMonthOption(month) }}
            </option>
          </select>
        </label>
      </div>

      <dl class="running-record-summary">
        <div>
          <dt>{{ activeScopeLabel }} 합산 거리</dt>
          <dd>{{ totalDistanceText }}</dd>
        </div>
        <div>
          <dt>{{ activeScopeLabel }} 평균 페이스</dt>
          <dd>{{ averagePaceText }}</dd>
        </div>
      </dl>
    </section>

    <section v-if="status === 'ready'" class="running-record-grid">
      <RouterLink
        v-for="record in filteredRecords"
        :key="record.id"
        class="content-card running-record-card"
        :to="{ name: 'running-record-detail', params: { runningRecordId: record.id } }"
      >
        <div class="running-record-card-heading">
          <time class="eyebrow" :datetime="record.recordDate">{{ record.recordDate }}</time>
          <strong>{{ record.distanceKm }} km</strong>
        </div>
        <dl class="running-record-metrics">
          <div><dt>운동 시간</dt><dd>{{ formatDuration(record.durationSeconds) }}</dd></div>
          <div><dt>평균 페이스</dt><dd>{{ formatPace(record.paceSecondsPerKm) }}</dd></div>
        </dl>
        <p class="running-record-memo">{{ record.memo || "메모 없음" }}</p>
      </RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, ref } from "vue";
import { RouterLink } from "vue-router";
import { runningRecordApi } from "../api/runningRecordApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";
import { formatDuration, formatPace } from "../utils/runningRecord";

const { data, status, error, run } = usePageState([]);
const activeScope = ref("all");
const today = new Date();
const currentYear = today.getFullYear();
const currentMonth = String(today.getMonth() + 1).padStart(2, "0");
const selectedYear = ref(String(currentYear));
const selectedMonth = ref(`${currentYear}-${currentMonth}`);

const scopeOptions = [
  { value: "all", label: "전체" },
  { value: "year", label: "년" },
  { value: "month", label: "월" },
];

const activeScopeLabel = computed(() => (
  {
    all: "전체",
    year: selectedYear.value,
    month: formatMonthOption(selectedMonth.value),
  }[activeScope.value] || "전체"
));
const records = computed(() => (Array.isArray(data.value) ? data.value : []));
const availableYears = computed(() => {
  const years = records.value
    .map((record) => record.recordDate?.slice(0, 4))
    .filter(Boolean);
  return [...new Set([...years, String(currentYear)])].sort((a, b) => Number(b) - Number(a));
});
const availableMonths = computed(() => {
  const months = records.value
    .map((record) => record.recordDate?.slice(0, 7))
    .filter(Boolean);
  return [...new Set([...months, `${currentYear}-${currentMonth}`])].sort().reverse();
});

const filteredRecords = computed(() => {
  if (activeScope.value === "year") {
    return records.value.filter((record) => record.recordDate?.startsWith(`${selectedYear.value}-`));
  }
  if (activeScope.value === "month") {
    return records.value.filter((record) => (
      record.recordDate?.startsWith(`${selectedMonth.value}-`)
    ));
  }
  return records.value;
});

const totalDistance = computed(() => filteredRecords.value.reduce(
  (total, record) => total + Number(record.distanceKm || 0),
  0,
));
const totalDurationSeconds = computed(() => filteredRecords.value.reduce(
  (total, record) => total + Number(record.durationSeconds || 0),
  0,
));

const totalDistanceText = computed(() => `${formatDistance(totalDistance.value)} km`);
const averagePaceText = computed(() => {
  if (totalDistance.value <= 0 || totalDurationSeconds.value <= 0) return "-";
  return formatPace(Math.round(totalDurationSeconds.value / totalDistance.value));
});

function formatDistance(value) {
  return new Intl.NumberFormat("ko-KR", {
    maximumFractionDigits: 2,
  }).format(value);
}

function formatMonthOption(value) {
  if (!value) return "";
  const [year, month] = value.split("-");
  return `${year}.${month}`;
}

function setScope(scope) {
  activeScope.value = scope;
  if (scope === "year" && !availableYears.value.includes(selectedYear.value)) {
    [selectedYear.value] = availableYears.value;
  }
  if (scope === "month" && !availableMonths.value.includes(selectedMonth.value)) {
    [selectedMonth.value] = availableMonths.value;
  }
}

function load() {
  return run(() => runningRecordApi.getMine());
}

load();
</script>
