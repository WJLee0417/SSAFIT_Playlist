<template>
  <AppLayout>
    <span class="eyebrow">PLAYLIST EDITOR</span>
    <h1>{{ isEdit ? "플레이리스트 수정" : "플레이리스트 생성" }}</h1>

    <PageStatePanel
      v-if="status !== 'ready'"
      :status="status"
      :error="error"
      @retry="initialize"
    />

    <form v-if="status === 'ready'" class="editor-form" @submit.prevent="save">
      <label>
        제목
        <input v-model="form.title" maxlength="200" required />
      </label>
      <label>
        설명
        <textarea v-model="form.description" maxlength="2000"></textarea>
      </label>
      <label>
        공개 범위
        <select v-model="form.visibility">
          <option value="PUBLIC">PUBLIC</option>
          <option value="PRIVATE">PRIVATE</option>
        </select>
      </label>

      <aside class="contract-note">
        현재 Backend 플레이리스트 DTO에는 목표 강도와 BPM 범위 필드가 없습니다.
        저장되지 않는 입력값을 화면에 추가하지 않았습니다.
      </aside>

      <p v-if="validationMessage" class="field-error" role="alert">
        {{ validationMessage }}
      </p>
      <button type="submit" :disabled="saving">{{ saving ? "저장 중..." : "저장" }}</button>
      <SaveStatus :type="saveType" :message="saveMessage" />
    </form>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi } from "../api/authApi";
import { isApiError } from "../api/http";
import { playlistApi } from "../api/playlistApi";
import { playlistSaveApi } from "../api/playlistSaveApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import { playlistErrorMessage } from "../utils/playlistError";

const props = defineProps({ playlistId: { type: String, default: "" } });
const route = useRoute();
const router = useRouter();
const isEdit = computed(() => route.meta.mode === "edit");
const form = reactive({ title: "", description: "", visibility: "PRIVATE" });
const saving = ref(false);
const saveMessage = ref("");
const saveType = ref("success");
const validationMessage = ref("");
const status = ref("loading");
const error = ref(null);
const currentMember = ref(null);

async function initialize() {
  status.value = "loading";
  error.value = null;
  saveMessage.value = "";
  try {
    currentMember.value = await authApi.getSession();
    if (!isEdit.value) {
      status.value = "ready";
      return;
    }

    const detail = await playlistApi.getDetail(props.playlistId);
    const playlist = detail.playlist;
    const isOwner = String(currentMember.value.id) === String(playlist.ownerId);
    if (playlist.playlistType === "OFFICIAL" || !isOwner) {
      error.value = {
        status: 403,
        errorCode: "ACCESS_DENIED",
        message: "소유한 사용자 플레이리스트만 수정할 수 있습니다.",
      };
      status.value = "unauthorized";
      return;
    }
    Object.assign(form, {
      title: playlist.title || "",
      description: playlist.description || "",
      visibility: playlist.visibility || "PRIVATE",
    });
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

function payload() {
  return {
    title: form.title.trim(),
    description: form.description.trim() || null,
    visibility: form.visibility,
  };
}

async function save() {
  validationMessage.value = "";
  saveMessage.value = "";
  if (!form.title.trim()) {
    validationMessage.value = "제목을 입력해 주세요.";
    return;
  }

  saving.value = true;
  try {
    const playlist = isEdit.value
      ? await playlistApi.update(props.playlistId, payload())
      : await playlistApi.create(payload());
    if (!isEdit.value) {
      await playlistSaveApi.save(playlist.id);
    }
    saveType.value = "success";
    saveMessage.value = "플레이리스트를 저장했습니다.";
    await router.push({
      name: "playlist-detail",
      params: { playlistId: playlist.id },
      query: { returnTo: "/playlists" },
    });
  } catch (caught) {
    saveType.value = "error";
    saveMessage.value = playlistErrorMessage(caught);
    if (isApiError(caught) && caught.loginRequired) {
      status.value = "loginRequired";
      error.value = caught;
    } else if (isApiError(caught) && caught.unauthorized) {
      status.value = "unauthorized";
      error.value = caught;
    }
  } finally {
    saving.value = false;
  }
}

onMounted(initialize);
</script>
