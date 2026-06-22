<template>
  <AppLayout>
    <RouterLink class="text-link back-link running-editor-back-link" :to="backLocation">
      ← 돌아가기
    </RouterLink>
    <span class="eyebrow">RUNNING RECORD EDITOR</span>
    <h1>{{ isEdit ? "러닝 기록 수정" : "러닝 기록 등록" }}</h1>

    <PageStatePanel
      v-if="status !== 'ready'"
      :status="status"
      :error="error"
      error-title="러닝 기록을 준비하지 못했습니다"
      @retry="initialize"
    />

    <form v-if="status === 'ready'" class="editor-form running-record-form" @submit.prevent="save">
      <label>
        러닝 날짜
        <input v-model="form.recordDate" type="date" required />
      </label>
      <label>
        거리 (km)
        <input
          v-model="form.distanceKm"
          type="number"
          min="0.01"
          max="99999999.99"
          step="0.01"
          inputmode="decimal"
          placeholder="예: 5.25"
          required
        />
      </label>
      <label>
        운동 시간 (초)
        <input
          v-model="form.durationSeconds"
          type="number"
          min="1"
          step="1"
          inputmode="numeric"
          placeholder="예: 1800"
          required
        />
        <small v-if="durationPreview" class="muted">입력 시간: {{ durationPreview }}</small>
      </label>
      <label>
        메모
        <textarea
          v-model="form.memo"
          maxlength="1000"
          placeholder="러닝 컨디션이나 코스를 기록해 보세요."
        ></textarea>
        <small class="muted">{{ form.memo.length }} / 1000</small>
      </label>

      <aside class="contract-note">
        평균 페이스는 거리와 운동 시간을 기준으로 Backend에서 자동 계산합니다.
      </aside>

      <p v-if="validationMessage" class="field-error" role="alert">
        {{ validationMessage }}
      </p>

      <div class="form-actions">
        <button type="submit" :disabled="saving">
          {{ saving ? "저장 중..." : "저장" }}
        </button>
        <RouterLink class="secondary-button" :to="backLocation">취소</RouterLink>
      </div>
      <SaveStatus :type="saveType" :message="saveMessage" />
    </form>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { isApiError } from "../api/http";
import { runningRecordApi } from "../api/runningRecordApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import {
  formatDuration,
  runningRecordErrorMessage,
  runningRecordPayload,
  validateRunningRecordForm,
} from "../utils/runningRecord";

const props = defineProps({
  runningRecordId: { type: String, default: "" },
});

const route = useRoute();
const router = useRouter();
const isEdit = computed(() => route.meta.mode === "edit");
const form = reactive({
  recordDate: "",
  distanceKm: "",
  durationSeconds: "",
  memo: "",
});
const status = ref("loading");
const error = ref(null);
const saving = ref(false);
const validationMessage = ref("");
const saveMessage = ref("");
const saveType = ref("success");

const durationPreview = computed(() => {
  const value = Number(form.durationSeconds);
  return Number.isSafeInteger(value) && value > 0 ? formatDuration(value) : "";
});
const backLocation = computed(() => {
  if (isEdit.value && props.runningRecordId) {
    return {
      name: "running-record-detail",
      params: { runningRecordId: props.runningRecordId },
    };
  }
  return { name: "running-record-list" };
});

function applyRecord(record) {
  Object.assign(form, {
    recordDate: record.recordDate || "",
    distanceKm: record.distanceKm ?? "",
    durationSeconds: record.durationSeconds ?? "",
    memo: record.memo || "",
  });
}

async function initialize() {
  status.value = "loading";
  error.value = null;
  saveMessage.value = "";
  validationMessage.value = "";

  if (!isEdit.value) {
    status.value = "ready";
    return;
  }

  try {
    applyRecord(await runningRecordApi.getDetail(props.runningRecordId));
    status.value = "ready";
  } catch (caught) {
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

async function save() {
  validationMessage.value = validateRunningRecordForm(form);
  saveMessage.value = "";
  if (validationMessage.value) return;

  saving.value = true;
  try {
    const payload = runningRecordPayload(form);
    const record = isEdit.value
      ? await runningRecordApi.update(props.runningRecordId, payload)
      : await runningRecordApi.create(payload);
    await router.push({
      name: "running-record-detail",
      params: { runningRecordId: record.id },
      query: { saved: isEdit.value ? "updated" : "created" },
    });
  } catch (caught) {
    saveType.value = "error";
    saveMessage.value = runningRecordErrorMessage(caught);
    if (isApiError(caught) && caught.loginRequired) {
      error.value = caught;
      status.value = "loginRequired";
    } else if (isApiError(caught) && caught.unauthorized) {
      error.value = caught;
      status.value = "unauthorized";
    }
  } finally {
    saving.value = false;
  }
}

onMounted(initialize);
</script>
