<template>
  <AppLayout>
    <div class="page-heading">
      <div>
        <span class="eyebrow">RUNNING RECORDS</span>
        <h1>내 러닝 기록</h1>
        <p class="lead">거리와 시간을 기록하면 1km 페이스를 자동으로 계산합니다.</p>
      </div>
      <RouterLink class="primary-button" to="/running-records/new">새 기록</RouterLink>
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

    <section v-if="status === 'ready'" class="running-record-grid">
      <RouterLink
        v-for="record in data"
        :key="record.id"
        class="content-card running-record-card"
        :to="{ name: 'running-record-detail', params: { runningRecordId: record.id } }"
      >
        <div class="running-record-card-heading">
          <span class="eyebrow">{{ record.recordDate }}</span>
          <strong>{{ record.distanceKm }} km</strong>
        </div>
        <dl class="running-record-metrics">
          <div><dt>운동 시간</dt><dd>{{ formatDuration(record.durationSeconds) }}</dd></div>
          <div><dt>평균 페이스</dt><dd>{{ formatPace(record.paceSecondsPerKm) }}</dd></div>
        </dl>
        <p class="running-record-memo">{{ record.memo || "메모가 없습니다." }}</p>
      </RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { RouterLink } from "vue-router";
import { runningRecordApi } from "../api/runningRecordApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";
import { formatDuration, formatPace } from "../utils/runningRecord";

const { data, status, error, run } = usePageState([]);

function load() {
  return run(() => runningRecordApi.getMine());
}

load();
</script>
