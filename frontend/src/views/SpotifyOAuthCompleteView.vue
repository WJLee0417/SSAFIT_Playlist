<template>
  <AppLayout>
    <PageStatePanel :status="status" :error="error" @retry="checkConnection" />
    <section v-if="status === 'ready'" class="state-panel state-success">
      <h1>Spotify 연결을 확인했습니다</h1>
      <p>{{ data.spotifyUserId }} 계정이 연결되어 있습니다.</p>
      <RouterLink class="primary-button" :to="returnTo">이전 화면으로 돌아가기</RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { spotifyApi } from "../api/spotifyApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { consumeSpotifyReturnPath } from "../utils/oauthReturn";
import { usePageState } from "../utils/pageState";

const route = useRoute();
const rememberedReturnTo = consumeSpotifyReturnPath();
const returnTo = computed(() => {
  const candidate = route.query.returnTo;
  if (typeof candidate === "string" && candidate.startsWith("/")) {
    return candidate;
  }
  return rememberedReturnTo || "/playlists";
});
const { data, status, error, run } = usePageState();
const checkConnection = () => run(async () => {
  const connection = await spotifyApi.getConnection();
  if (!connection.connected) {
    return null;
  }
  return connection;
});

onMounted(checkConnection);
</script>
