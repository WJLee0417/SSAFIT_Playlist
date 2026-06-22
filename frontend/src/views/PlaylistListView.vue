<template>
  <AppLayout>
    <div class="page-heading">
      <div><span class="eyebrow">PLAYLISTS</span><h1>플레이리스트</h1></div>
      <RouterLink class="primary-button" to="/playlists/new">새 플레이리스트</RouterLink>
    </div>
    <PageStatePanel
      :status="status"
      :error="error"
      empty-message="조회 가능한 플레이리스트가 없습니다."
      @retry="load"
    />
    <section v-if="status === 'ready'" class="card-grid">
      <RouterLink
        v-for="playlist in data"
        :key="playlist.id"
        class="content-card"
        :to="`/playlists/${playlist.id}`"
      >
        <span class="eyebrow">{{ playlist.visibility }}</span>
        <h2>{{ playlist.title }}</h2>
        <p>{{ playlist.description || "설명이 없습니다." }}</p>
      </RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { onMounted } from "vue";
import { RouterLink } from "vue-router";
import { playlistApi } from "../api/playlistApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";

const { data, status, error, run } = usePageState([]);
const load = () => run(() => playlistApi.getList());
onMounted(load);
</script>
