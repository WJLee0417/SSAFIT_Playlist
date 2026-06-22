<template>
  <AppLayout>
    <RouterLink class="text-link back-link" :to="{ name: 'running-session-list' }">
      ← 목록으로 돌아가기
    </RouterLink>

    <PageStatePanel
      :status="status"
      :error="error"
      error-title="러닝 모임 상세를 불러오지 못했습니다"
      @retry="load"
    />

    <article v-if="status === 'ready'" class="detail-card running-session-detail">
      <div class="page-heading">
        <div>
          <div class="badge-row">
            <span class="session-status-badge" :class="`session-status-${session.status?.toLowerCase()}`">
              {{ statusLabel(session.status) }}
            </span>
            <span class="visibility-badge">주최자 #{{ session.hostMemberId }}</span>
          </div>
          <h1>{{ session.title }}</h1>
        </div>
        <div v-if="isHost" class="form-actions">
          <RouterLink
            v-if="session.status !== 'CANCELLED'"
            class="secondary-button"
            :to="{
              name: 'running-session-edit',
              params: { runningSessionId },
            }"
          >
            수정
          </RouterLink>
          <button
            v-if="session.status !== 'CANCELLED'"
            type="button"
            class="danger-button"
            :disabled="actionBusy"
            @click="cancelSession"
          >
            모임 취소
          </button>
        </div>
      </div>

      <SaveStatus :type="actionType" :message="actionMessage" />

      <p class="lead">{{ session.description || "모임 설명이 없습니다." }}</p>
      <dl class="running-session-detail-metrics">
        <div><dt>일정</dt><dd>{{ formatSessionDateTime(session.scheduledAt) }}</dd></div>
        <div><dt>집결 장소</dt><dd>{{ session.meetingPlace }}</dd></div>
        <div><dt>참여 현황</dt><dd>{{ participantCount }} / {{ session.capacity }}명</dd></div>
      </dl>

      <section class="running-session-action-panel">
        <div>
          <span class="eyebrow">PARTICIPATION</span>
          <h2>{{ participationTitle }}</h2>
          <p>{{ participationDescription }}</p>
        </div>
        <div class="form-actions">
          <button
            v-if="canJoin"
            type="button"
            :disabled="actionBusy"
            @click="join"
          >
            {{ actionBusy ? "처리 중..." : "모임 참여" }}
          </button>
          <button
            v-if="isParticipating"
            type="button"
            class="secondary-button"
            :disabled="actionBusy"
            @click="leave"
          >
            {{ actionBusy ? "처리 중..." : "참여 취소" }}
          </button>
        </div>
      </section>

      <section v-if="isHost && session.status !== 'CANCELLED'" class="running-session-host-panel">
        <div>
          <span class="eyebrow">HOST CONTROLS</span>
          <h2>모집 상태 관리</h2>
          <p>정원 도달만으로 모집 상태가 자동 변경되지는 않습니다.</p>
        </div>
        <button
          type="button"
          class="secondary-button"
          :disabled="actionBusy"
          @click="toggleRecruitment"
        >
          {{ session.status === "OPEN" ? "모집 마감" : "모집 다시 열기" }}
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
        <div v-else class="participant-grid">
          <article
            v-for="participant in participants"
            :key="participant.id"
            class="participant-card"
            :class="{ mine: isMine(participant) }"
          >
            <strong>회원 #{{ participant.memberId }}</strong>
            <span v-if="isMine(participant)" class="saved-badge">나</span>
            <small>{{ formatSessionDateTime(participant.joinedAt) }}</small>
          </article>
        </div>
        <p class="contract-note">
          현재 Backend 응답에는 참여자 닉네임이 없어 회원 ID만 표시합니다.
        </p>
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
const canJoin = computed(() => (
  session.value.status === "OPEN" && !isParticipating.value
));
const participationTitle = computed(() => {
  if (isParticipating.value) return "이 모임에 참여 중입니다";
  if (session.value.status === "OPEN") return "모임에 참여할 수 있습니다";
  if (session.value.status === "CLOSED") return "모집이 마감되었습니다";
  return "취소된 모임입니다";
});
const participationDescription = computed(() => {
  if (isParticipating.value) {
    return "모집 상태와 관계없이 본인의 기존 참여를 취소할 수 있습니다.";
  }
  if (session.value.status === "OPEN") {
    return "정원 초과와 중복 참여 여부는 서버 응답을 최종 기준으로 확인합니다.";
  }
  return "현재 상태에서는 새로운 참여를 받을 수 없습니다.";
});

function isMine(participant) {
  return currentMember.value != null
    && String(currentMember.value.id) === String(participant.memberId);
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
      ? "러닝 모임을 생성했습니다."
      : (route.query.saved === "updated" ? "러닝 모임을 수정했습니다." : "");
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
    "러닝 모임에 참여했습니다.",
  );
}

function leave() {
  if (!window.confirm("이 러닝 모임의 참여를 취소하시겠습니까?")) return;
  return runAction(
    () => runningSessionApi.leave(props.runningSessionId),
    "러닝 모임 참여를 취소했습니다.",
  );
}

function toggleRecruitment() {
  const nextStatus = session.value.status === "OPEN" ? "CLOSED" : "OPEN";
  return runAction(
    () => runningSessionApi.changeStatus(props.runningSessionId, nextStatus),
    nextStatus === "OPEN" ? "모집을 다시 시작했습니다." : "모집을 마감했습니다.",
  );
}

function cancelSession() {
  if (!window.confirm(`"${session.value.title}" 모임을 취소하시겠습니까?`)) return;
  return runAction(
    () => runningSessionApi.cancel(props.runningSessionId),
    "러닝 모임을 취소했습니다.",
  );
}

watch(() => props.runningSessionId, () => load(), { immediate: true });
</script>
