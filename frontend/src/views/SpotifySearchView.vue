<template>
  <AppLayout>
    <div class="page-heading">
      <div><span class="eyebrow">SPOTIFY</span><h1>Spotify 음악 검색</h1></div>
      <button type="button" class="secondary-button" @click="connect">계정 연결</button>
    </div>
    <p class="muted">계정 연결은 API에서 인증 URL만 받은 뒤 브라우저가 Spotify로 직접 이동합니다.</p>
    <form class="filter-bar" @submit.prevent="search">
      <input v-model.trim="query" required aria-label="Spotify 검색어" placeholder="곡 또는 아티스트" />
      <button type="submit">검색</button>
    </form>
    <SaveStatus :type="actionType" :message="actionMessage" />
    <PageStatePanel
      v-if="status !== 'idle'"
      :status="status"
      :error="error"
      empty-message="Spotify 검색 결과가 없습니다."
      @retry="search"
    />
    <section v-if="status === 'ready'" class="card-grid">
      <article v-for="track in data" :key="track.spotifyTrackId" class="content-card">
        <span class="eyebrow">{{ track.genre || "장르 미제공" }}</span>
        <h2>{{ track.title }}</h2>
        <p>{{ track.artist }}</p>
        <small>{{ track.bpm ? `${track.bpm} BPM` : "BPM 미제공" }}</small>
      </article>
    </section>
  </AppLayout>
</template>

<script setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { spotifyApi } from "../api/spotifyApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import { rememberSpotifyReturnPath } from "../utils/oauthReturn";
import { usePageState } from "../utils/pageState";

const route = useRoute();
const query = ref("");
const actionMessage = ref("");
const actionType = ref("error");
const { data, status, error, run } = usePageState([]);

const search = () => run(() => spotifyApi.searchTracks({ query: query.value, limit: 20 }));

async function connect() {
  actionMessage.value = "";
  try {
    rememberSpotifyReturnPath(route.fullPath);
    await spotifyApi.startAuthorization();
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = caught.message;
  }
}
</script>
