<template>
  <AppLayout>
    <PageStatePanel :status="status" :error="error" @retry="load" />
    <article v-if="status === 'ready'" class="detail-card">
      <div class="page-heading">
        <div>
          <span class="eyebrow">{{ data.playlist.visibility }}</span>
          <h1>{{ data.playlist.title }}</h1>
        </div>
        <RouterLink class="secondary-button" :to="`/playlists/${playlistId}/edit`">수정</RouterLink>
      </div>
      <p class="lead">{{ data.playlist.description || "설명이 없습니다." }}</p>
      <h2>수록곡</h2>
      <div v-if="data.items.length" class="stack-list">
        <article v-for="item in data.items" :key="item.id" class="list-row">
          <span>{{ item.orderNo }}</span>
          <div><strong>{{ item.music.title }}</strong><small>{{ item.music.artist }}</small></div>
        </article>
      </div>
      <p v-else class="muted">아직 수록곡이 없습니다.</p>
    </article>
  </AppLayout>
</template>

<script setup>
import { onMounted } from "vue";
import { RouterLink } from "vue-router";
import { playlistApi } from "../api/playlistApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";

const props = defineProps({ playlistId: { type: String, required: true } });
const { data, status, error, run } = usePageState();
const load = () => run(() => playlistApi.getDetail(props.playlistId), { empty: { value: false } });
onMounted(load);
</script>
