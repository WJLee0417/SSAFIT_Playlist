<template>
  <AppLayout>
    <span class="eyebrow">AUTO CURATION</span>
    <h1>러닝 맞춤 자동 큐레이션</h1>
    <form class="filter-bar" @submit.prevent="recommend">
      <select v-model="form.intensity" aria-label="러닝 강도" required>
        <option disabled value="">강도 선택</option>
        <option value="LOW">LOW</option>
        <option value="MODERATE">MODERATE</option>
        <option value="HIGH">HIGH</option>
        <option value="INTERVAL">INTERVAL</option>
      </select>
      <input v-model.trim="form.genre" aria-label="장르" placeholder="장르(선택)" />
      <input v-model.number="form.size" type="number" min="1" max="50" aria-label="추천 곡 수" />
      <button type="submit">추천 받기</button>
    </form>

    <PageStatePanel
      v-if="status !== 'idle' && status !== 'ready'"
      :status="status"
      :error="error"
      empty-message="추천 결과가 없습니다."
      @retry="recommend"
    />

    <section v-if="status === 'ready'" class="detail-card">
      <h2>추천 결과</h2>
      <p>{{ data.log.recommendationReason }}</p>
      <div class="stack-list">
        <article v-for="music in data.music" :key="music.id" class="list-row">
          <div><strong>{{ music.title }}</strong><small>{{ music.artist }}</small></div>
          <span>{{ music.bpm ? `${music.bpm} BPM` : "BPM 미정" }}</span>
        </article>
      </div>
    </section>
  </AppLayout>
</template>

<script setup>
import { reactive } from "vue";
import { recommendationApi } from "../api/recommendationApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";

const form = reactive({ intensity: "", genre: "", size: 10 });
const { data, status, error, run } = usePageState();
const recommend = () => run(() => recommendationApi.recommend({
  intensity: form.intensity,
  genre: form.genre || undefined,
  size: form.size,
}));
</script>
