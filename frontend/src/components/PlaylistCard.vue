<template>
  <article class="content-card playlist-card">
    <div class="badge-row">
      <span class="source-badge" :class="{ official: isOfficial }">
        {{ isOfficial ? "공식" : sourceLabel }}
      </span>
      <span class="visibility-badge">{{ playlist.visibility }}</span>
      <span v-if="playlist.saved" class="saved-badge">저장됨</span>
    </div>
    <h2>
      <RouterLink :to="detailLocation">{{ playlist.title }}</RouterLink>
    </h2>
    <p>{{ playlist.description || "설명이 없습니다." }}</p>
    <dl class="playlist-meta">
      <div><dt>유형</dt><dd>{{ playlist.playlistType }}</dd></div>
      <div><dt>Spotify</dt><dd>{{ playlist.spotifyExportStatus || "NOT_EXPORTED" }}</dd></div>
    </dl>
    <RouterLink class="text-link" :to="detailLocation">상세 보기</RouterLink>
  </article>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  playlist: { type: Object, required: true },
  returnTo: { type: String, required: true },
});

const isOfficial = computed(() => props.playlist.playlistType === "OFFICIAL");
const sourceLabel = computed(() => {
  if (props.playlist.sourceType === "CURATION_COPY") return "큐레이션 복사본";
  return "사용자";
});
const detailLocation = computed(() => ({
  name: "playlist-detail",
  params: { playlistId: props.playlist.id },
  query: { returnTo: props.returnTo },
}));
</script>
