<template>
  <AppLayout>
    <PageStatePanel :status="status" :error="error" @retry="load" />
    <article v-if="status === 'ready'" class="detail-card">
      <span class="eyebrow">{{ data.music.genre || "장르 미정" }}</span>
      <h1>{{ data.music.title }}</h1>
      <p class="lead">{{ data.music.artist }} · {{ data.music.albumName || "앨범 정보 없음" }}</p>
      <dl class="summary-list">
        <div><dt>BPM</dt><dd>{{ data.music.bpm || "정보 없음" }}</dd></div>
        <div><dt>리뷰</dt><dd>{{ data.reviewAverage?.reviewCount || 0 }}개</dd></div>
      </dl>
      <section>
        <h2>리뷰</h2>
        <p v-if="!data.reviews.length">아직 작성된 리뷰가 없습니다.</p>
        <article v-for="review in data.reviews" :key="review.id" class="review-row">
          <strong>선호 {{ review.preferenceScore }} · 러닝 적합 {{ review.runningFitScore }}</strong>
          <p>{{ review.reviewContent || "내용 없음" }}</p>
        </article>
      </section>
    </article>
  </AppLayout>
</template>

<script setup>
import { onMounted } from "vue";
import { musicApi } from "../api/musicApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import { usePageState } from "../utils/pageState";

const props = defineProps({ musicId: { type: String, required: true } });
const { data, status, error, run } = usePageState();
const load = () => run(() => musicApi.getDetail(props.musicId), { empty: { value: false } });

onMounted(load);
</script>
