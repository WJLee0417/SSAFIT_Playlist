<template>
  <AppLayout>
    <span class="eyebrow">PLAYLIST EDITOR</span>
    <h1>{{ isEdit ? "플레이리스트 수정" : "플레이리스트 생성" }}</h1>
    <PageStatePanel
      v-if="isEdit && status !== 'ready'"
      :status="status"
      :error="error"
      @retry="load"
    />
    <form v-if="!isEdit || status === 'ready'" class="editor-form" @submit.prevent="save">
      <label>제목<input v-model.trim="form.title" required maxlength="200" /></label>
      <label>설명<textarea v-model.trim="form.description" maxlength="2000"></textarea></label>
      <label>공개 범위
        <select v-model="form.visibility">
          <option value="PUBLIC">PUBLIC</option>
          <option value="PRIVATE">PRIVATE</option>
        </select>
      </label>
      <button type="submit" :disabled="saving">{{ saving ? "저장 중..." : "저장" }}</button>
      <SaveStatus :type="saveType" :message="saveMessage" />
    </form>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { playlistApi } from "../api/playlistApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import { usePageState } from "../utils/pageState";

const props = defineProps({ playlistId: { type: String, default: "" } });
const route = useRoute();
const router = useRouter();
const isEdit = computed(() => route.meta.mode === "edit");
const form = reactive({ title: "", description: "", visibility: "PRIVATE" });
const saving = ref(false);
const saveMessage = ref("");
const saveType = ref("success");
const { status, error, run } = usePageState();

async function load() {
  const detail = await run(
    () => playlistApi.getDetail(props.playlistId),
    { empty: { value: false } },
  );
  if (detail) {
    Object.assign(form, {
      title: detail.playlist.title,
      description: detail.playlist.description || "",
      visibility: detail.playlist.visibility || "PRIVATE",
    });
  }
}

async function save() {
  saving.value = true;
  saveMessage.value = "";
  try {
    const playlist = isEdit.value
      ? await playlistApi.update(props.playlistId, form)
      : await playlistApi.create(form);
    saveType.value = "success";
    saveMessage.value = "플레이리스트를 저장했습니다.";
    await router.push(`/playlists/${playlist.id}`);
  } catch (caught) {
    saveType.value = "error";
    saveMessage.value = caught.message;
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  if (isEdit.value) {
    load();
  }
});
</script>
