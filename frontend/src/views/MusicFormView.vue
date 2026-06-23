<template>
  <AppLayout>
    <div class="page-heading">
      <div>
        <span class="eyebrow">INTERNAL MUSIC MANAGEMENT</span>
        <h1>{{ isEdit ? "내부 음악 수정" : "내부 음악 등록" }}</h1>
      </div>
      <RouterLink class="secondary-button" :to="cancelLocation">취소</RouterLink>
    </div>

    <PageStatePanel
      v-if="isEdit && loadStatus !== 'ready'"
      :status="loadStatus"
      :error="loadError"
      @retry="loadMusic"
    />

    <form
      v-if="!isEdit || loadStatus === 'ready'"
      class="editor-form content-card"
      @submit.prevent="submit"
    >
      <label>제목<input v-model.trim="form.title" maxlength="255" required /></label>
      <label>아티스트<input v-model.trim="form.artist" maxlength="255" required /></label>
      <label>앨범<input v-model.trim="form.albumName" maxlength="255" /></label>
      <label>장르<input v-model.trim="form.genre" maxlength="100" /></label>
      <label>이미지 URL<input v-model.trim="form.imageUrl" maxlength="500" type="url" /></label>
      <label>
        음악 템포 BPM
        <input v-model="form.musicBpm" type="number" min="60" max="220" />
        <small>케이던스 SPM과 평균 심박수 BPM이 아닌 곡의 템포입니다.</small>
      </label>
      <label>재생 시간(ms)<input v-model="form.durationMs" type="number" min="1" /></label>
      <label>
        음악 에너지 태그
        <select v-model="form.musicEnergyLevel">
          <option value="">미지정</option>
          <option value="CALM">CALM · 회복/가벼운 러닝</option>
          <option value="STEADY">STEADY · 안정적인 지속주</option>
          <option value="ENERGETIC">ENERGETIC · 템포런/강한 러닝</option>
          <option value="PEAK">PEAK · 고강도/인터벌</option>
        </select>
        <small>음악 BPM이나 평균 심박수로 자동 추론하지 않습니다.</small>
      </label>

      <p v-if="validationMessage" class="field-error" role="alert">
        {{ validationMessage }}
      </p>
      <SaveStatus :type="saveType" :message="saveMessage" />
      <div class="form-actions">
        <button type="submit" :disabled="saving">{{ saving ? "저장 중..." : "저장" }}</button>
        <RouterLink class="secondary-button" :to="cancelLocation">취소</RouterLink>
      </div>
    </form>
  </AppLayout>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { musicApi } from "../api/musicApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";

const props = defineProps({ musicId: { type: String, default: null } });
const route = useRoute();
const router = useRouter();
const isEdit = computed(() => route.meta.mode === "edit");
const form = reactive({
  title: "",
  artist: "",
  albumName: "",
  imageUrl: "",
  genre: "",
  musicBpm: "",
  durationMs: "",
  musicEnergyLevel: "",
});
const loadStatus = ref(isEdit.value ? "loading" : "ready");
const loadError = ref(null);
const validationMessage = ref("");
const saving = ref(false);
const saveMessage = ref("");
const saveType = ref("success");
const cancelLocation = computed(() => (
  isEdit.value && props.musicId
    ? { name: "music-detail", params: { musicId: props.musicId } }
    : { name: "music-list" }
));

function nullableNumber(value) {
  return value === "" || value == null ? null : Number(value);
}

function requestBody() {
  return {
    title: form.title.trim(),
    artist: form.artist.trim(),
    albumName: form.albumName.trim() || null,
    imageUrl: form.imageUrl.trim() || null,
    genre: form.genre.trim() || null,
    musicBpm: nullableNumber(form.musicBpm),
    durationMs: nullableNumber(form.durationMs),
    musicEnergyLevel: form.musicEnergyLevel || null,
  };
}

function validate() {
  if (!form.title.trim() || !form.artist.trim()) {
    return "제목과 아티스트를 입력해 주세요.";
  }
  const musicBpm = nullableNumber(form.musicBpm);
  if (musicBpm != null && (musicBpm < 60 || musicBpm > 220)) {
    return "음악 템포 BPM은 60~220 범위여야 합니다.";
  }
  const durationMs = nullableNumber(form.durationMs);
  if (durationMs != null && durationMs < 1) {
    return "재생 시간은 1ms 이상이어야 합니다.";
  }
  return "";
}

async function loadMusic() {
  if (!isEdit.value || !props.musicId) return;
  loadStatus.value = "loading";
  loadError.value = null;
  try {
    const detail = await musicApi.getDetail(props.musicId);
    const music = detail.music;
    Object.assign(form, {
      title: music.title || "",
      artist: music.artist || "",
      albumName: music.albumName || "",
      imageUrl: music.imageUrl || "",
      genre: music.genre || "",
      musicBpm: music.musicBpm ?? "",
      durationMs: music.durationMs ?? "",
      musicEnergyLevel: music.musicEnergyLevel || "",
    });
    loadStatus.value = "ready";
  } catch (caught) {
    loadError.value = caught;
    loadStatus.value = "error";
  }
}

async function submit() {
  validationMessage.value = validate();
  saveMessage.value = "";
  if (validationMessage.value || saving.value) return;
  saving.value = true;
  try {
    const saved = isEdit.value
      ? await musicApi.updateInternal(props.musicId, requestBody())
      : await musicApi.createInternal(requestBody());
    await router.push({ name: "music-detail", params: { musicId: saved.id } });
  } catch (caught) {
    saveType.value = "error";
    saveMessage.value = caught.message || "음악 저장에 실패했습니다.";
  } finally {
    saving.value = false;
  }
}

if (isEdit.value) loadMusic();
</script>
