<template>
  <AppLayout>
    <RouterLink class="text-link back-link running-editor-back-link" :to="backLocation">
      ← 돌아가기
    </RouterLink>
    <span class="eyebrow">RUNNING SESSION EDITOR</span>
    <h1>{{ isEdit ? "러닝 세션 수정" : "러닝 세션 생성" }}</h1>

    <PageStatePanel
      v-if="status !== 'ready'"
      :status="status"
      :error="error"
      error-title="러닝 세션을 준비하지 못했습니다"
      @retry="initialize"
    />

    <form v-if="status === 'ready'" class="editor-form running-session-form" @submit.prevent="save">
      <label>
        모임 제목
        <input v-model="form.title" maxlength="200" required />
      </label>
      <label>
        설명
        <textarea
          v-model="form.description"
          maxlength="2000"
          placeholder="코스, 예상 거리, 준비물 등을 안내해 주세요."
        ></textarea>
        <small class="muted">{{ form.description.length }} / 2000</small>
      </label>
      <label>
        집결 장소
        <input v-model="form.meetingPlace" maxlength="255" required />
      </label>
      <label>
        모임 일시
        <input v-model="form.scheduledAt" type="datetime-local" required />
      </label>
      <label>
        정원
        <input
          v-model="form.capacity"
          type="number"
          min="1"
          step="1"
          inputmode="numeric"
          required
        />
      </label>

      <aside class="contract-note">
        일정의 과거·미래 제한과 현재 참여 인원보다 작은 정원으로의 수정 제한은
        현재 Backend 정책에 없습니다.
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
import { authApi } from "../api/authApi";
import { isApiError } from "../api/http";
import { runningSessionApi } from "../api/runningSessionApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import {
  runningSessionErrorMessage,
  runningSessionPayload,
  toDateTimeLocalValue,
  validateRunningSessionForm,
} from "../utils/runningSession";

const props = defineProps({
  runningSessionId: { type: String, default: "" },
});

const route = useRoute();
const router = useRouter();
const isEdit = computed(() => route.meta.mode === "edit");
const form = reactive({
  title: "",
  description: "",
  meetingPlace: "",
  scheduledAt: "",
  capacity: "",
});
const status = ref("loading");
const error = ref(null);
const saving = ref(false);
const validationMessage = ref("");
const saveMessage = ref("");
const saveType = ref("success");

const backLocation = computed(() => {
  if (isEdit.value && props.runningSessionId) {
    return {
      name: "running-session-detail",
      params: { runningSessionId: props.runningSessionId },
    };
  }
  return { name: "running-session-list" };
});

function applySession(session) {
  Object.assign(form, {
    title: session.title || "",
    description: session.description || "",
    meetingPlace: session.meetingPlace || "",
    scheduledAt: toDateTimeLocalValue(session.scheduledAt),
    capacity: session.capacity ?? "",
  });
}

function setStateFromError(caught) {
  error.value = caught;
  if (isApiError(caught) && caught.loginRequired) {
    status.value = "loginRequired";
  } else if (isApiError(caught) && caught.unauthorized) {
    status.value = "unauthorized";
  } else {
    status.value = "error";
  }
}

async function initialize() {
  status.value = "loading";
  error.value = null;
  validationMessage.value = "";
  saveMessage.value = "";

  if (!isEdit.value) {
    status.value = "ready";
    return;
  }

  try {
    const [member, detail] = await Promise.all([
      authApi.getSession(),
      runningSessionApi.getDetail(props.runningSessionId),
    ]);
    const session = detail.session;
    if (String(member.id) !== String(session.hostMemberId)) {
      error.value = {
        status: 403,
        errorCode: "ACCESS_DENIED",
        message: "주최한 러닝 세션만 수정할 수 있습니다.",
      };
      status.value = "unauthorized";
      return;
    }
    if (session.status === "CANCELLED") {
      error.value = {
        status: 409,
        errorCode: "SESSION_CLOSED",
        message: "취소된 러닝 세션은 수정할 수 없습니다.",
      };
      status.value = "error";
      return;
    }
    applySession(session);
    status.value = "ready";
  } catch (caught) {
    setStateFromError(caught);
  }
}

async function save() {
  validationMessage.value = validateRunningSessionForm(form);
  saveMessage.value = "";
  if (validationMessage.value) return;

  saving.value = true;
  try {
    const payload = runningSessionPayload(form);
    const session = isEdit.value
      ? await runningSessionApi.update(props.runningSessionId, payload)
      : await runningSessionApi.create(payload);
    await router.push({
      name: "running-session-detail",
      params: { runningSessionId: session.id },
      query: { saved: isEdit.value ? "updated" : "created" },
    });
  } catch (caught) {
    saveType.value = "error";
    saveMessage.value = runningSessionErrorMessage(caught);
    if (
      isApiError(caught)
      && (caught.loginRequired || caught.unauthorized || caught.notFound)
    ) {
      setStateFromError(caught);
    }
  } finally {
    saving.value = false;
  }
}

onMounted(initialize);
</script>
