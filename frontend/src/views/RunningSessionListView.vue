<template>
  <AppLayout>
    <div class="page-heading">
      <div>
        <span class="eyebrow">RUNNING SESSIONS</span>
        <h1>러닝 모임</h1>
        <p class="lead">함께 달릴 모임을 찾거나 새로운 모임을 만들어 보세요.</p>
      </div>
      <RouterLink class="primary-button" to="/running-sessions/new">새 모임</RouterLink>
    </div>

    <PageStatePanel
      :status="status"
      :error="error"
      empty-title="등록된 러닝 모임이 없습니다"
      empty-message="첫 러닝 모임을 만들어 보세요."
      error-title="러닝 모임을 불러오지 못했습니다"
      @retry="load"
    >
      <template #empty-action>
        <RouterLink class="primary-button" to="/running-sessions/new">첫 모임 만들기</RouterLink>
      </template>
    </PageStatePanel>

    <section v-if="status === 'ready'" class="running-session-grid">
      <RouterLink
        v-for="session in data"
        :key="session.id"
        class="content-card running-session-card"
        :class="`running-session-${session.status?.toLowerCase()}`"
        :to="{ name: 'running-session-detail', params: { runningSessionId: session.id } }"
      >
        <div class="running-session-card-heading">
          <span class="session-status-badge" :class="`session-status-${session.status?.toLowerCase()}`">
            {{ statusLabel(session.status) }}
          </span>
          <small>주최자 #{{ session.hostMemberId }}</small>
        </div>
        <h2>{{ session.title }}</h2>
        <p class="running-session-description">
          {{ session.description || "모임 설명이 없습니다." }}
        </p>
        <dl class="running-session-card-meta">
          <div><dt>일정</dt><dd>{{ formatSessionDateTime(session.scheduledAt) }}</dd></div>
          <div><dt>장소</dt><dd>{{ session.meetingPlace }}</dd></div>
          <div><dt>모집 정원</dt><dd>{{ session.capacity }}명</dd></div>
        </dl>
      </RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { RouterLink } from "vue-router";
import { runningSessionApi } from "../api/runningSessionApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";
import { formatSessionDateTime, statusLabel } from "../utils/runningSession";

const { data, status, error, run } = usePageState([]);

function load() {
  return run(() => runningSessionApi.getList());
}

load();
</script>
