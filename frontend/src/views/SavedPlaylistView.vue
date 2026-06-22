<template>
  <AppLayout>
    <span class="eyebrow">MY LIBRARY</span>
    <h1>내 저장 플레이리스트</h1>
    <PageStatePanel
      :status="status"
      :error="error"
      empty-message="저장한 플레이리스트가 없습니다."
      @retry="load"
    />
    <section v-if="status === 'ready'" class="card-grid">
      <RouterLink
        v-for="playlist in data"
        :key="playlist.id"
        class="content-card"
        :to="`/playlists/${playlist.id}`"
      >
        <h2>{{ playlist.title }}</h2>
        <p>{{ playlist.description || "설명이 없습니다." }}</p>
      </RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { onMounted } from "vue";
import { RouterLink } from "vue-router";
import { playlistSaveApi } from "../api/playlistSaveApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";

const { data, status, error, run } = usePageState([]);
const load = () => run(() => playlistSaveApi.getMine());
onMounted(load);
</script>
