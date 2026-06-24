<template>
  <AppLayout>
    <RouterLink class="text-link back-link" :to="{ name: 'running-session-list' }">
      ← 목록으로 돌아가기
    </RouterLink>

    <PageStatePanel
      :status="status"
      :error="error"
      error-title="러닝 세션 상세를 불러오지 못했습니다"
      @retry="load"
    />

    <article v-if="status === 'ready'" class="detail-card running-session-detail">
      <div class="page-heading">
        <div class="running-session-detail-title">
          <div class="badge-row">
            <span class="session-status-badge" :class="`session-status-${session.status?.toLowerCase()}`">
              {{ statusLabel(session.status) }}
            </span>
          </div>
          <h1>{{ session.title }}</h1>
        </div>
        <div v-if="isHost && session.status !== 'CANCELLED'" class="form-actions">
          <RouterLink
            class="secondary-button running-session-edit-link"
            :to="{
              name: 'running-session-edit',
              params: { runningSessionId },
            }"
          >
            수정
          </RouterLink>
          <button
            type="button"
            class="danger-button"
            :disabled="actionBusy"
            @click="deleteSession"
          >
            삭제
          </button>
        </div>
      </div>

      <SaveStatus :type="actionType" :message="actionMessage" />

      <p class="lead">{{ session.description || "모임 설명이 없습니다." }}</p>
      <dl class="running-session-detail-metrics">
        <div><dt>호스트</dt><dd>{{ session.hostNickname || `회원 #${session.hostMemberId}` }}</dd></div>
        <div><dt>일정</dt><dd>{{ formatSessionSchedule(session.scheduledAt) }}</dd></div>
        <div><dt>집결 장소</dt><dd>{{ session.meetingPlace }}</dd></div>
        <div><dt>참여 현황</dt><dd>{{ participantCount }} / {{ session.capacity }}명</dd></div>
      </dl>
      <dl class="running-session-goal-metrics">
        <div>
          <dt>목표 거리</dt>
          <dd>{{ formatDistance(session.targetDistanceKm) }}</dd>
        </div>
        <div>
          <dt>목표 페이스</dt>
          <dd>{{ formatSessionPace(session.targetPaceSecondsPerKm) }}</dd>
        </div>
      </dl>

      <section class="running-session-management">
        <h2>세션 관리</h2>
        <button
          v-if="isHost && session.status !== 'CANCELLED'"
          type="button"
          class="secondary-button"
          :disabled="actionBusy"
          @click="toggleRecruitment"
        >
          {{ session.status === "OPEN" ? "모집 마감" : "모집 다시 열기" }}
        </button>
        <button
          v-else-if="canJoin"
          type="button"
          :disabled="actionBusy"
          @click="join"
        >
          {{ actionBusy ? "처리 중..." : "세션 참여" }}
        </button>
        <button
          v-else-if="isParticipating"
          type="button"
          class="secondary-button"
          :disabled="actionBusy"
          @click="leave"
        >
          {{ actionBusy ? "처리 중..." : "참여 취소" }}
        </button>
        <button
          v-else-if="!isHost"
          type="button"
          disabled
        >
          세션 참여
        </button>
      </section>

      <section class="running-session-participants">
        <div class="section-heading">
          <div>
            <span class="eyebrow">PARTICIPANTS</span>
            <h2>참여자 {{ participantCount }}명</h2>
          </div>
        </div>
        <p v-if="!participants.length" class="empty-inline">
          아직 참여자가 없습니다.
        </p>
        <div v-else class="running-session-participant-list">
          <article
            v-for="participant in participants"
            :key="participant.id"
            class="running-session-participant-row"
            :class="{ mine: isMine(participant) }"
          >
            <strong>{{ participant.memberNickname || `회원 #${participant.memberId}` }}</strong>
            <span>{{ formatSessionSchedule(participant.joinedAt) }}</span>
          </article>
        </div>
      </section>

      <dl class="summary-list running-session-timestamps">
        <div><dt>생성 시각</dt><dd>{{ formatSessionDateTime(session.createdAt) }}</dd></div>
        <div><dt>수정 시각</dt><dd>{{ formatSessionDateTime(session.updatedAt) }}</dd></div>
        <div v-if="session.cancelledAt">
          <dt>취소 시각</dt><dd>{{ formatSessionDateTime(session.cancelledAt) }}</dd>
        </div>
      </dl>
    </article>
  </AppLayout>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { authApi } from "../api/authApi";
import { isApiError } from "../api/http";
import { runningSessionApi } from "../api/runningSessionApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import {
  formatSessionDateTime,
  formatSessionPace,
  formatSessionSchedule,
  runningSessionErrorMessage,
  statusLabel,
} from "../utils/runningSession";

const props = defineProps({
  runningSessionId: { type: String, required: true },
});

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const currentMember = ref(null);
const status = ref("idle");
const error = ref(null);
const actionBusy = ref(false);
const actionMessage = ref("");
const actionType = ref("success");
let requestSequence = 0;

const session = computed(() => detail.value?.session || {});
const participants = computed(() => (
  Array.isArray(detail.value?.participants) ? detail.value.participants : []
));
const participantCount = computed(() => (
  Number(detail.value?.participantCount) || participants.value.length
));
const isHost = computed(() => (
  currentMember.value != null
  && String(currentMember.value.id) === String(session.value.hostMemberId)
));
const isParticipating = computed(() => participants.value.some(isMine));
const isExpired = computed(() => {
  if (typeof session.value.expired === "boolean") {
    return session.value.expired;
  }
  if (!session.value.scheduledAt) return false;
  const scheduledAt = new Date(session.value.scheduledAt);
  return !Number.isNaN(scheduledAt.getTime()) && scheduledAt.getTime() < Date.now();
});
const isFull = computed(() => (
  participantCount.value >= Number(session.value.capacity || 0)
));
const canJoin = computed(() => (
  session.value.status === "OPEN"
  && !isParticipating.value
  && !isExpired.value
  && !isFull.value
));
function isMine(participant) {
  return currentMember.value != null
    && String(currentMember.value.id) === String(participant.memberId);
}

function formatDistance(distanceKm) {
  const distance = Number(distanceKm);
  if (!Number.isFinite(distance) || distance <= 0) return "-";
  return `${distance.toFixed(distance % 1 === 0 ? 0 : 2)} km`;
}

function setStateFromError(caught) {
  error.value = caught;
  if (isApiError(caught) && caught.loginRequired) {
    authApi.clearSessionCache();
    status.value = "loginRequired";
  } else if (isApiError(caught) && caught.unauthorized) {
    status.value = "unauthorized";
  } else {
    status.value = "error";
  }
}

async function load({ preserveMessage = false } = {}) {
  const sequence = ++requestSequence;
  status.value = "loading";
  error.value = null;
  if (!preserveMessage) {
    actionMessage.value = route.query.saved === "created"
      ? "러닝 세션을 생성했습니다."
      : (route.query.saved === "updated" ? "러닝 세션을 수정했습니다." : "");
    if (route.query.saved) {
      const { saved, ...query } = route.query;
      void router.replace({ query });
    }
    actionType.value = "success";
  }

  try {
    const [member, response] = await Promise.all([
      authApi.getSession(),
      runningSessionApi.getDetail(props.runningSessionId),
    ]);
    if (sequence !== requestSequence) return;
    currentMember.value = member;
    detail.value = {
      ...response,
      participants: Array.isArray(response?.participants)
        ? response.participants
        : [],
    };
    status.value = "ready";
  } catch (caught) {
    if (sequence !== requestSequence) return;
    setStateFromError(caught);
  }
}

async function runAction(action, successMessage) {
  if (actionBusy.value) return;
  actionBusy.value = true;
  actionMessage.value = "";
  try {
    await action();
    actionType.value = "success";
    actionMessage.value = successMessage;
    await load({ preserveMessage: true });
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = runningSessionErrorMessage(caught);
    if (isApiError(caught) && caught.loginRequired) {
      setStateFromError(caught);
    }
  } finally {
    actionBusy.value = false;
  }
}

function join() {
  return runAction(
    () => runningSessionApi.join(props.runningSessionId),
    "러닝 세션에 참여했습니다.",
  );
}

function leave() {
  if (!window.confirm("이 러닝 세션의 참여를 취소하시겠습니까?")) return;
  return runAction(
    () => runningSessionApi.leave(props.runningSessionId),
    "러닝 세션 참여를 취소했습니다.",
  );
}

function toggleRecruitment() {
  const nextStatus = session.value.status === "OPEN" ? "CLOSED" : "OPEN";
  return runAction(
    () => runningSessionApi.changeStatus(props.runningSessionId, nextStatus),
    nextStatus === "OPEN" ? "모집을 다시 시작했습니다." : "모집을 마감했습니다.",
  );
}

async function deleteSession() {
  if (!window.confirm(`"${session.value.title}" 러닝 세션을 삭제하시겠습니까?`)) return;
  if (actionBusy.value) return;
  actionBusy.value = true;
  actionMessage.value = "";
  try {
    await runningSessionApi.cancel(props.runningSessionId);
    await router.push({ name: "running-session-list" });
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = runningSessionErrorMessage(caught);
    if (isApiError(caught) && caught.loginRequired) {
      setStateFromError(caught);
    }
  } finally {
    actionBusy.value = false;
  }
}

watch(() => props.runningSessionId, () => load(), { immediate: true });
</script>
