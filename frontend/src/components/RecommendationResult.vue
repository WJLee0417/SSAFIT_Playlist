<template>
  <section class="detail-card recommendation-result">
    <div class="page-heading">
      <div>
        <span class="eyebrow">AI WORKOUT PLAN</span>
        <h2>{{ distanceLabel }} · {{ result.log.requestedIntensity }}</h2>
      </div>
      <div class="recommendation-result-header-actions">
        <button
          type="button"
          class="secondary-button inline-button recommendation-header-action"
          @click="$emit('close')"
        >
          결과 닫기
        </button>
        <span class="source-badge recommendation-header-action">로그 #{{ logNumber }}</span>
      </div>
    </div>

    <section class="recommendation-ai-summary">
      <h3>AI 요약</h3>
      <p>{{ plan.coachingSummary }}</p>
    </section>

    <dl class="summary-list recommendation-summary workout-plan-summary">
      <div>
        <dt>전체 러닝 시간</dt>
        <dd>{{ plan.totalDurationMinutes }}분</dd>
      </div>
      <div>
        <dt>목표 페이스</dt>
        <dd>{{ plan.targetPace }}</dd>
      </div>
      <div>
        <dt>케이던스 목표</dt>
        <dd>{{ plan.cadenceMinSpm }}~{{ plan.cadenceMaxSpm }} SPM</dd>
      </div>
      <div>
        <dt>심박 목표</dt>
        <dd>{{ plan.heartRateMinBpm }}~{{ plan.heartRateMaxBpm }} BPM</dd>
      </div>
      <div>
        <dt>음악 BPM</dt>
        <dd>{{ plan.musicBpmMin }}~{{ plan.musicBpmMax }} BPM</dd>
      </div>
      <div>
        <dt>예상 재생 시간</dt>
        <dd>{{ playlistDurationLabel }}</dd>
      </div>
    </dl>

    <section class="workout-segment-list" aria-label="러닝 구간별 BPM">
      <article v-for="segment in segments" :key="`${segment.name}-${segment.durationMinutes}`">
        <strong>{{ segment.name }}</strong>
        <span>{{ segment.durationMinutes }}분 · {{ segment.bpmMin }}~{{ segment.bpmMax }} BPM</span>
        <small>{{ segment.purpose }}</small>
      </article>
    </section>

    <div v-if="music.length" class="recommendation-music-list">
      <article v-for="(track, index) in music" :key="track.id" class="recommendation-track-row">
        <div class="recommendation-track-cover">
          <img v-if="track.imageUrl" :src="track.imageUrl" :alt="`${track.title} 앨범 커버`" />
          <span v-else>{{ index + 1 }}</span>
        </div>
        <div class="recommendation-track-main">
          <strong>{{ index + 1 }}. {{ track.title }}</strong>
          <small>{{ track.artist || "아티스트 미정" }}</small>
          <div class="recommendation-track-tags">
            <span v-for="keyword in trackKeywords(track)" :key="keyword">{{ keyword }}</span>
          </div>
        </div>
        <div class="recommendation-track-stats">
          <span>{{ formatDuration(track.durationMs) }}</span>
          <small>{{ track.musicBpm ? `${track.musicBpm} BPM` : "BPM 미정" }}</small>
        </div>
      </article>
    </div>
    <p v-else class="empty-inline">로컬 DB에서 추천할 음악을 찾지 못했습니다.</p>

    <section class="curation-save-panel">
      <div>
        <h3>이 운동 계획을 플레이리스트로 추가</h3>
        <p>
          수락하면 목표 강도에 맞는 사용자 큐레이션 플레이리스트가 생성되고,
          현재 트랙 순서 그대로 저장됩니다.
        </p>
      </div>

      <form class="curation-save-form" @submit.prevent="submitSave">
        <label>
          플레이리스트 제목
          <input
            v-model="saveForm.title"
            maxlength="200"
            :placeholder="defaultTitle"
            :disabled="saving || creationCompleted"
          />
        </label>
        <label>
          공개 범위
          <select v-model="saveForm.visibility" :disabled="saving || creationCompleted">
            <option value="PRIVATE">PRIVATE</option>
            <option value="PUBLIC">PUBLIC</option>
          </select>
        </label>
        <button
          type="submit"
          :disabled="!canSave || saving || creationCompleted"
        >
          {{ creationCompleted ? "플레이리스트 생성 완료" : saving ? "플레이리스트 생성 중..." : "플레이리스트 생성" }}
        </button>
      </form>
    </section>

    <SaveStatus :type="saveMessageType" :message="saveMessage" />

    <section v-if="savedPlaylist" class="curation-created-actions">
      <RouterLink
        class="primary-button"
        :to="{ name: 'playlist-detail', params: { playlistId: savedPlaylist.id }, query: { returnTo: '/recommendations' } }"
      >
        생성된 플레이리스트 보기
      </RouterLink>
      <RouterLink
        class="secondary-button"
        :to="{ name: 'playlist-list', query: { ownerId: savedPlaylist.ownerId } }"
      >
        플레이리스트 목록
      </RouterLink>
      <SpotifyExportPanel
        :playlist="savedPlaylist"
        :playlist-id="savedPlaylist.id"
        :editable="true"
        return-to="/recommendations"
        :items="exportItems"
        compact
      />
    </section>
  </section>
</template>

<script setup>
import { computed, reactive } from "vue";
import { RouterLink } from "vue-router";
import SaveStatus from "./SaveStatus.vue";
import SpotifyExportPanel from "./SpotifyExportPanel.vue";

const props = defineProps({
  result: { type: Object, required: true },
  saving: { type: Boolean, default: false },
  savedPlaylist: { type: Object, default: null },
  saveMessage: { type: String, default: "" },
  saveMessageType: { type: String, default: "success" },
});

const emit = defineEmits(["save", "close"]);
const saveForm = reactive({ title: "", visibility: "PRIVATE" });
const plan = computed(() => props.result.workoutPlan || {});
const segments = computed(() => (
  Array.isArray(plan.value.segments) ? plan.value.segments : []
));
const music = computed(() => (
  Array.isArray(props.result?.music) ? props.result.music : []
));
const exportItems = computed(() => music.value.map((track) => ({
  musicId: track.id,
  music: track,
})));
const canSave = computed(() => (
  Number.isSafeInteger(Number(props.result?.log?.id))
  && Number(props.result.log.id) >= 1
  && music.value.length > 0
));
const creationCompleted = computed(() => props.savedPlaylist != null);
const distanceLabel = computed(() => (
  props.result.targetDistanceKm
    ? `${props.result.targetDistanceKm}km`
    : "목표 거리"
));
const defaultTitle = computed(() => (
  `${distanceLabel.value} ${props.result.log.requestedIntensity} 러닝`
));
const logNumber = computed(() => (
  props.result?.log?.displayNumber || props.result?.log?.id
));
const playlistDurationLabel = computed(() => {
  const totalMs = music.value.reduce((sum, track) => (
    sum + (Number(track.durationMs) > 0 ? Number(track.durationMs) : 210000)
  ), 0);
  return formatDuration(totalMs);
});

function submitSave() {
  if (!canSave.value || props.saving || creationCompleted.value) return;
  emit("save", {
    title: saveForm.title.trim() || defaultTitle.value,
    visibility: saveForm.visibility,
  });
}

function formatDuration(durationMs) {
  const seconds = Math.max(0, Math.round((Number(durationMs) || 0) / 1000));
  const minutes = Math.floor(seconds / 60);
  const restSeconds = seconds % 60;
  return `${minutes}:${String(restSeconds).padStart(2, "0")}`;
}

function trackKeywords(track) {
  const reasonKeywords = String(track?.recommendationReason || "")
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
  const values = [
    track?.genre || null,
    track?.musicBpm ? `${track.musicBpm} BPM` : null,
    plan.value.cadenceMinSpm && plan.value.cadenceMaxSpm
      ? `${plan.value.cadenceMinSpm}~${plan.value.cadenceMaxSpm} SPM`
      : null,
    ...reasonKeywords,
  ];
  return [...new Set(values.filter(Boolean))].slice(0, 4);
}
</script>
