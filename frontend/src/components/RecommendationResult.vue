<template>
  <section class="detail-card recommendation-result">
    <div class="page-heading">
      <div>
        <span class="eyebrow">TEMPORARY RESULT</span>
        <h2>추천 결과</h2>
      </div>
      <span class="source-badge">로그 #{{ result.log.id }}</span>
    </div>

    <p class="policy-callout">
      조회만으로는 플레이리스트가 생성되지 않습니다.
    </p>

    <p class="lead">{{ result.log.recommendationReason }}</p>
    <dl class="summary-list recommendation-summary">
      <div><dt>강도</dt><dd>{{ result.log.requestedIntensity }}</dd></div>
      <div>
        <dt>실제 적용 BPM</dt>
        <dd>{{ result.log.requestedBpmMin }}~{{ result.log.requestedBpmMax }}</dd>
      </div>
      <div><dt>장르</dt><dd>{{ result.log.requestedGenre || "제한 없음" }}</dd></div>
      <div><dt>추천 곡</dt><dd>{{ music.length }}곡</dd></div>
    </dl>

    <div v-if="music.length" class="stack-list recommendation-music-list">
      <article v-for="track in music" :key="track.id" class="list-row">
        <div>
          <strong>{{ track.title }}</strong>
          <small>{{ track.artist }}</small>
        </div>
        <span>{{ track.bpm ? `${track.bpm} BPM` : "BPM 미정" }}</span>
      </article>
    </div>
    <p v-else class="empty-inline">추천 결과에 표시할 음악이 없습니다.</p>

    <section class="curation-save-panel">
      <div>
        <span class="eyebrow">CREATE CURATION COPY</span>
        <h3>개인 플레이리스트로 생성</h3>
        <p>
          이 버튼을 눌러야만 <code>USER + CURATION_COPY</code> 플레이리스트와
          수록곡이 생성됩니다. <code>playlist_saves</code> 참조 저장과는 다른 기능입니다.
        </p>
      </div>

      <form class="curation-save-form" @submit.prevent="submitSave">
        <label>
          플레이리스트 제목
          <input
            v-model="saveForm.title"
            maxlength="200"
            placeholder="비우면 Backend 기본 제목 사용"
            :disabled="saving || savedPlaylist != null"
          />
        </label>
        <label>
          공개 범위
          <select v-model="saveForm.visibility" :disabled="saving || savedPlaylist != null">
            <option value="PRIVATE">PRIVATE</option>
            <option value="PUBLIC">PUBLIC</option>
          </select>
        </label>
        <button
          type="submit"
          :disabled="!canSave || saving || savedPlaylist != null"
        >
          {{ saving ? "개인 플레이리스트 생성 중..." : "개인 플레이리스트로 생성" }}
        </button>
      </form>
    </section>

    <SaveStatus :type="saveMessageType" :message="saveMessage" />

    <section v-if="savedPlaylist" class="curation-created-panel">
      <span class="eyebrow">CURATION COPY CREATED</span>
      <h3>{{ savedPlaylist.title }}</h3>
      <dl class="playlist-meta">
        <div><dt>플레이리스트 ID</dt><dd>{{ savedPlaylist.id }}</dd></div>
        <div><dt>유형</dt><dd>{{ savedPlaylist.playlistType }}</dd></div>
        <div><dt>생성 경로</dt><dd>{{ savedPlaylist.sourceType }}</dd></div>
        <div><dt>공개 범위</dt><dd>{{ savedPlaylist.visibility }}</dd></div>
      </dl>
      <div class="card-actions">
        <RouterLink
          class="primary-button"
          :to="{ name: 'playlist-detail', params: { playlistId: savedPlaylist.id }, query: { returnTo: '/recommendations' } }"
        >
          생성된 개인 플레이리스트 상세
        </RouterLink>
        <RouterLink
          class="secondary-button"
          :to="{ name: 'playlist-list', query: { ownerId: savedPlaylist.ownerId } }"
        >
          내 개인 플레이리스트 목록
        </RouterLink>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, reactive } from "vue";
import { RouterLink } from "vue-router";
import SaveStatus from "./SaveStatus.vue";

const props = defineProps({
  result: { type: Object, required: true },
  saving: { type: Boolean, default: false },
  savedPlaylist: { type: Object, default: null },
  saveMessage: { type: String, default: "" },
  saveMessageType: { type: String, default: "success" },
});

const emit = defineEmits(["save"]);
const saveForm = reactive({ title: "", visibility: "PRIVATE" });
const music = computed(() => (
  Array.isArray(props.result?.music) ? props.result.music : []
));
const canSave = computed(() => (
  Number.isSafeInteger(Number(props.result?.log?.id))
  && Number(props.result.log.id) >= 1
));

function submitSave() {
  if (!canSave.value || props.saving || props.savedPlaylist) return;
  emit("save", {
    title: saveForm.title,
    visibility: saveForm.visibility,
  });
}
</script>
