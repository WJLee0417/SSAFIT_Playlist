<template>
  <AppLayout>
    <span class="eyebrow">MY LIBRARY</span>
    <h1>내 저장 플레이리스트</h1>
    <p class="muted">다른 플레이리스트를 참조 저장한 목록입니다. 자동 큐레이션 복사본과는 별도입니다.</p>
    <PageStatePanel
      :status="status"
      :error="error"
      empty-message="참조 저장한 플레이리스트가 없습니다."
      @retry="load"
    />
    <section v-if="status === 'ready'" class="card-grid">
      <PlaylistCard
        v-for="playlist in data"
        :key="playlist.id"
        :playlist="playlist"
        :return-to="route.fullPath"
      />
    </section>
  </AppLayout>
</template>

<script setup>
import { onMounted } from "vue";
import { useRoute } from "vue-router";
import { playlistSaveApi } from "../api/playlistSaveApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import PlaylistCard from "../components/PlaylistCard.vue";
import { usePageState } from "../utils/pageState";

const route = useRoute();
const { data, status, error, run } = usePageState([]);
const load = () => run(() => playlistSaveApi.getMine());
onMounted(load);
</script>
