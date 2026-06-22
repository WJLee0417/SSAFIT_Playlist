<template>
  <AppLayout>
    <div class="page-heading">
      <div><span class="eyebrow">MUSIC</span><h1>음악 둘러보기</h1></div>
      <RouterLink class="secondary-button" to="/spotify/search">Spotify 검색</RouterLink>
    </div>

    <form class="filter-bar" @submit.prevent="load">
      <input v-model.trim="filters.keyword" aria-label="검색어" placeholder="제목 또는 아티스트" />
      <input v-model.trim="filters.genre" aria-label="장르" placeholder="장르" />
      <button type="submit">검색</button>
    </form>

    <PageStatePanel
      :status="status"
      :error="error"
      empty-message="현재 조건에 맞는 음악이 없습니다."
      @retry="load"
    />

    <section v-if="status === 'ready'" class="card-grid">
      <RouterLink
        v-for="music in data.items"
        :key="music.id"
        class="content-card"
        :to="`/music/${music.id}`"
      >
        <span class="eyebrow">{{ music.genre || "장르 미정" }}</span>
        <h2>{{ music.title }}</h2>
        <p>{{ music.artist }}</p>
        <small>{{ music.bpm ? `${music.bpm} BPM` : "BPM 정보 없음" }}</small>
      </RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import { RouterLink } from "vue-router";
import { musicApi } from "../api/musicApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";

const filters = reactive({ keyword: "", genre: "", page: 1, size: 20 });
const { data, status, error, run } = usePageState({ items: [] });
const load = () => run(() => musicApi.getList(filters));

onMounted(load);
</script>
