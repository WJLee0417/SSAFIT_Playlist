<template>
  <AppLayout>
    <RouterLink class="text-link back-link running-editor-back-link" :to="backLocation">
      ← 돌아가기
    </RouterLink>
    <span class="eyebrow">RECORD</span>
    <h1>{{ isEdit ? "러닝 기록 수정" : "러닝 기록" }}</h1>

    <PageStatePanel
      v-if="status !== 'ready'"
      :status="status"
      :error="error"
      error-title="러닝 기록을 준비하지 못했습니다"
      @retry="initialize"
    />

    <form v-if="status === 'ready'" class="editor-form running-record-form" @submit.prevent="save">
      <div class="running-record-input-row running-record-primary-row">
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
      </div>

      <fieldset class="running-record-duration-field">
        <legend>운동 시간</legend>
        <div class="running-record-duration-inputs" aria-label="운동 시간">
          <input
            v-model="form.durationMinutes"
            aria-label="분"
            type="number"
            min="0"
            step="1"
            inputmode="numeric"
            placeholder="30"
          />
          <span>분</span>
          <input
            v-model="form.durationSeconds"
            aria-label="초"
            type="number"
            min="0"
            max="59"
            step="1"
            inputmode="numeric"
            placeholder="0"
          />
          <span>초</span>
        </div>
      </fieldset>

      <div class="running-record-input-row running-record-secondary-row">
        <label>
          케이던스
          <input
            v-model="form.averageCadence"
            type="number"
            min="60"
            max="240"
            step="1"
            inputmode="numeric"
            placeholder="예: 172"
          />
        </label>
        <label>
          평균심박수(bpm)
          <input
            v-model="form.averageHeartRateBpm"
            type="number"
            min="30"
            max="240"
            step="1"
            inputmode="numeric"
            placeholder="예: 154"
          />
        </label>
      </div>

      <dl class="running-record-pace-preview">
        <div>
          <dt>평균 페이스</dt>
          <dd>{{ averagePacePreview }}</dd>
        </div>
      </dl>

      <label>
        메모
        <textarea
          v-model="form.memo"
          maxlength="1000"
          placeholder="러닝 컨디션이나 코스를 기록해 보세요."
        ></textarea>
        <small class="muted">{{ form.memo.length }} / 1000</small>
      </label>

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
  calculatePaceSecondsPerKm,
  formatPace,
  runningRecordErrorMessage,
  runningRecordDurationSeconds,
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
  durationMinutes: "",
  durationSeconds: "",
  averageCadence: "",
  averageHeartRateBpm: "",
  memo: "",
});
const status = ref("loading");
const error = ref(null);
const saving = ref(false);
const validationMessage = ref("");
const saveMessage = ref("");
const saveType = ref("success");

const averagePacePreview = computed(() => {
  const pace = calculatePaceSecondsPerKm(
    form.distanceKm,
    runningRecordDurationSeconds(form),
  );
  return formatPace(pace);
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
  const durationSeconds = Number(record.durationSeconds || 0);
  Object.assign(form, {
    recordDate: record.recordDate || "",
    distanceKm: record.distanceKm ?? "",
    durationMinutes: durationSeconds > 0 ? Math.floor(durationSeconds / 60) : "",
    durationSeconds: durationSeconds > 0 ? durationSeconds % 60 : "",
    averageCadence: record.averageCadence ?? "",
    averageHeartRateBpm: record.averageHeartRateBpm ?? "",
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
