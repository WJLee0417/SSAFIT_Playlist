<template>
  <AppLayout>
    <RouterLink class="text-link back-link" :to="{ name: 'running-record-list' }">
      ← 목록으로 돌아가기
    </RouterLink>

    <PageStatePanel
      :status="status"
      :error="error"
      error-title="러닝 기록 상세를 불러오지 못했습니다"
      @retry="load"
    />

    <article v-if="status === 'ready'" class="detail-card running-record-detail">
      <div class="page-heading">
        <div>
          <span class="eyebrow">RUNNING RECORD</span>
          <h1>{{ record.recordDate }}</h1>
        </div>
        <div class="form-actions">
          <RouterLink
            class="secondary-button"
            :to="{
              name: 'running-record-edit',
              params: { runningRecordId },
            }"
          >
            수정
          </RouterLink>
          <button
            type="button"
            class="danger-button"
            :disabled="actionBusy"
            @click="deleteRecord"
          >
            삭제
          </button>
        </div>
      </div>

      <SaveStatus :type="actionType" :message="actionMessage" />

      <dl class="running-record-detail-metrics">
        <div>
          <dt>거리</dt>
          <dd>{{ record.distanceKm }} km</dd>
        </div>
        <div>
          <dt>운동 시간</dt>
          <dd>{{ formatDuration(record.durationSeconds) }}</dd>
        </div>
        <div>
          <dt>평균 페이스</dt>
          <dd>{{ formatPace(record.paceSecondsPerKm) }}</dd>
        </div>
      </dl>

      <section class="running-record-note">
        <span class="eyebrow">MEMO</span>
        <h2>러닝 메모</h2>
        <p>{{ record.memo || "작성한 메모가 없습니다." }}</p>
      </section>

      <dl class="summary-list running-record-timestamps">
        <div><dt>등록 시각</dt><dd>{{ formatDateTime(record.createdAt) }}</dd></div>
        <div><dt>수정 시각</dt><dd>{{ formatDateTime(record.updatedAt) }}</dd></div>
      </dl>
    </article>
  </AppLayout>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { isApiError } from "../api/http";
import { runningRecordApi } from "../api/runningRecordApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import {
  formatDateTime,
  formatDuration,
  formatPace,
  runningRecordErrorMessage,
} from "../utils/runningRecord";

const props = defineProps({
  runningRecordId: { type: String, required: true },
});

const route = useRoute();
const router = useRouter();
const record = ref(null);
const status = ref("idle");
const error = ref(null);
const actionBusy = ref(false);
const actionType = ref("success");
const actionMessage = ref("");
let requestSequence = 0;

const savedNotice = computed(() => {
  if (route.query.saved === "created") return "러닝 기록을 등록했습니다.";
  if (route.query.saved === "updated") return "러닝 기록을 수정했습니다.";
  return "";
});

function consumeSavedNotice() {
  const notice = savedNotice.value;
  if (route.query.saved) {
    const { saved, ...query } = route.query;
    void router.replace({ query });
  }
  return notice;
}

async function load() {
  const sequence = ++requestSequence;
  status.value = "loading";
  error.value = null;
  actionMessage.value = consumeSavedNotice();
  actionType.value = "success";

  try {
    const response = await runningRecordApi.getDetail(props.runningRecordId);
    if (sequence !== requestSequence) return;
    record.value = response;
    status.value = "ready";
  } catch (caught) {
    if (sequence !== requestSequence) return;
    error.value = caught;
    if (isApiError(caught) && caught.loginRequired) {
      status.value = "loginRequired";
    } else if (isApiError(caught) && caught.unauthorized) {
      status.value = "unauthorized";
    } else {
      status.value = "error";
    }
  }
}

async function deleteRecord() {
  if (!window.confirm(`${record.value.recordDate} 러닝 기록을 삭제하시겠습니까?`)) return;
  actionBusy.value = true;
  actionMessage.value = "";
  try {
    await runningRecordApi.remove(props.runningRecordId);
    await router.push({ name: "running-record-list" });
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = runningRecordErrorMessage(caught);
    if (isApiError(caught) && caught.loginRequired) {
      error.value = caught;
      status.value = "loginRequired";
    } else if (isApiError(caught) && caught.unauthorized) {
      error.value = caught;
      status.value = "unauthorized";
    }
  } finally {
    actionBusy.value = false;
  }
}

watch(() => props.runningRecordId, load, { immediate: true });
</script>
