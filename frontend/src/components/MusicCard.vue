<template>
  <article class="content-card music-card">
    <img v-if="music.imageUrl" :src="music.imageUrl" :alt="`${music.title} 앨범 이미지`" />
    <div class="music-card-body">
      <div class="badge-row">
        <span class="eyebrow">{{ music.genre || "장르 미정" }}</span>
        <span v-if="isSpotify" class="source-badge">Spotify 기반</span>
      </div>
      <h2>
        <RouterLink :to="detailLocation">{{ music.title }}</RouterLink>
      </h2>
      <p>{{ music.artist }}</p>
      <small>{{ music.bpm ? `${music.bpm} BPM` : "BPM 정보 없음" }}</small>
      <div class="card-actions">
        <RouterLink class="text-link" :to="detailLocation">상세 보기</RouterLink>
        <a
          v-if="music.spotifyExternalUrl"
          class="text-link"
          :href="music.spotifyExternalUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          Spotify에서 열기
        </a>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  music: { type: Object, required: true },
  returnTo: { type: String, required: true },
});

const isSpotify = computed(() => props.music.sourceType === "SPOTIFY");
const detailLocation = computed(() => ({
  name: "music-detail",
  params: { musicId: props.music.id },
  query: { returnTo: props.returnTo },
}));
</script>
