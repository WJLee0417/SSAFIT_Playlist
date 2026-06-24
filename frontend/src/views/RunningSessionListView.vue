<template>
  <AppLayout>
    <div class="page-heading running-record-page-heading">
      <div class="running-record-title-stack">
        <div class="running-record-title-row">
          <h1>러닝 세션</h1>
          <RouterLink class="primary-button running-record-create-link" to="/running-sessions/new">
            세션 생성
          </RouterLink>
        </div>
        <p class="lead running-session-lead">
          함께 달릴 세션을 찾거나 새로운 세션을 만들어 보세요
        </p>
      </div>
    </div>

    <PageStatePanel
      :status="status"
      :error="error"
      empty-title="등록된 러닝 세션이 없습니다"
      empty-message="첫 러닝 세션을 만들어 보세요."
      error-title="러닝 세션을 불러오지 못했습니다"
      @retry="load"
    >
      <template #empty-action>
        <RouterLink class="primary-button" to="/running-sessions/new">첫 모임 만들기</RouterLink>
      </template>
    </PageStatePanel>

    <section v-if="status === 'ready'" class="running-session-dashboard">
      <div class="running-record-scope-toggle" aria-label="러닝 세션 조회 기준">
        <button
          v-for="filter in filterOptions"
          :key="filter.value"
          type="button"
          :class="{ active: activeFilter === filter.value }"
          @click="activeFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>
    </section>

    <section v-if="status === 'ready'" class="running-session-list">
      <RouterLink
        v-for="session in filteredSessions"
        :key="session.id"
        class="running-session-list-row"
        :class="[
          `running-session-${session.status?.toLowerCase()}`,
          { 'running-session-inactive': isInactiveSession(session) },
        ]"
        :to="{ name: 'running-session-detail', params: { runningSessionId: session.id } }"
      >
        <div class="running-session-list-heading">
          <span class="session-status-badge" :class="`session-status-${session.status?.toLowerCase()}`">
            {{ statusLabel(session.status) }}
          </span>
          <strong>{{ session.title }}</strong>
        </div>
        <p class="running-session-list-meta">
          <span>{{ formatSessionListDateTime(session.scheduledAt) }}</span>
          <span>{{ session.meetingPlace }}</span>
          <span>모집 정원 ({{ participantCount(session) }} / {{ session.capacity }})</span>
        </p>
      </RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, ref } from "vue";
import { RouterLink } from "vue-router";
import { authApi } from "../api/authApi";
import { runningSessionApi } from "../api/runningSessionApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";
import { formatSessionListDateTime, statusLabel } from "../utils/runningSession";

const { data, status, error, run } = usePageState([]);
const activeFilter = ref("all");
const currentMember = ref(null);

const filterOptions = [
  { value: "all", label: "전체" },
  { value: "open", label: "모집 중" },
  { value: "my", label: "MY" },
];

const filteredSessions = computed(() => {
  const sessions = Array.isArray(data.value) ? data.value : [];
  if (activeFilter.value === "open") {
    return sessions.filter(isRecruitingSession);
  }
  if (activeFilter.value === "my") {
    return sessions.filter((session) => session.isMine);
  }
  return sessions;
});

function load() {
  return run(async () => {
    const [member, response] = await Promise.all([
      authApi.getSession(),
      runningSessionApi.getList(),
    ]);
    const sessions = Array.isArray(response) ? response : [];
    currentMember.value = member;

    const details = await Promise.all(
      sessions.map((session) => runningSessionApi.getDetail(session.id).catch(() => null)),
    );

    return sessions.map((session, index) => enrichSession(session, details[index]));
  });
}

function participantCount(session) {
  return Number(session.participantCount ?? session.currentParticipantCount ?? 0);
}

function enrichSession(session, detail) {
  const participants = Array.isArray(detail?.participants) ? detail.participants : [];
  const detailSession = detail?.session || {};
  return {
    ...session,
    participantCount: Number(detail?.participantCount ?? session.participantCount ?? participants.length),
    isMine: participants.some((participant) => (
      currentMember.value != null
      && String(participant.memberId) === String(currentMember.value.id)
    )),
    status: detailSession.status || session.status,
    capacity: detailSession.capacity ?? session.capacity,
    scheduledAt: detailSession.scheduledAt || session.scheduledAt,
  };
}

function isRecruitingSession(session) {
  return session.status === "OPEN"
    && !isPastSession(session)
    && participantCount(session) < Number(session.capacity || 0);
}

function isPastSession(session) {
  if (!session.scheduledAt) return false;
  const scheduledAt = new Date(session.scheduledAt);
  return !Number.isNaN(scheduledAt.getTime()) && scheduledAt.getTime() < Date.now();
}

function isInactiveSession(session) {
  return session.status === "CANCELLED"
    || session.status === "CLOSED"
    || isPastSession(session)
    || participantCount(session) >= Number(session.capacity || 0);
}

load();
</script>
