<template>
  <article
    class="content-card music-card"
    role="link"
    tabindex="0"
    @click="goDetail"
    @keydown.enter.prevent="goDetail"
    @keydown.space.prevent="goDetail"
  >
    <button
      type="button"
      class="playlist-icon-button heart-button music-card-like-button"
      :class="{ active: isLiked }"
      :disabled="likeBusy"
      :title="isLiked ? '좋아요 취소' : '좋아요'"
      @click.stop="toggleLike"
      @keydown.stop
    >
      {{ isLiked ? "♥" : "♡" }}
    </button>
    <img v-if="music.imageUrl" :src="music.imageUrl" :alt="`${music.title} 앨범 이미지`" />
    <div class="music-card-body">
      <div class="badge-row">
        <span class="eyebrow">{{ music.genre || "장르 미정" }}</span>
      </div>
      <h2>
        {{ music.title }}
      </h2>
      <p>{{ music.artist }}</p>
      <small>{{ music.musicBpm ? `${music.musicBpm} BPM` : "BPM 정보 없음" }}</small>
    </div>
  </article>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { reviewApi } from "../api/reviewApi";
import { buildJspLoginUrl } from "../utils/navigation";

const props = defineProps({
  music: { type: Object, required: true },
  returnTo: { type: String, required: true },
  currentMemberId: { type: [Number, String], default: null },
});

const router = useRouter();
const ownReview = ref(null);
const likeBusy = ref(false);

const detailLocation = computed(() => ({
  name: "music-detail",
  params: { musicId: props.music.id },
  query: { returnTo: props.returnTo },
}));
const isLiked = computed(() => Number(ownReview.value?.preferenceScore) >= 4);

watch(
  () => [props.currentMemberId, props.music.id],
  loadOwnReview,
  { immediate: true },
);

function goDetail() {
  router.push(detailLocation.value);
}

async function loadOwnReview() {
  ownReview.value = null;
  if (!props.currentMemberId || !props.music?.id) return;
  try {
    const reviews = await reviewApi.getByMusic(props.music.id);
    ownReview.value = Array.isArray(reviews)
      ? reviews.find((review) => String(review.memberId) === String(props.currentMemberId)) || null
      : null;
  } catch {
    ownReview.value = null;
  }
}

async function toggleLike() {
  if (!props.currentMemberId) {
    window.location.assign(buildJspLoginUrl(window.location.href));
    return;
  }
  if (likeBusy.value || !props.music?.id) return;

  likeBusy.value = true;
  const existing = ownReview.value;
  const payload = {
    preferenceScore: isLiked.value ? 1 : 5,
    runningFitScore: existing?.runningFitScore ?? 3,
    reviewContent: existing?.reviewContent || null,
  };
  try {
    ownReview.value = existing?.id
      ? await reviewApi.update(existing.id, payload)
      : await reviewApi.create(props.music.id, payload);
  } catch (error) {
    console.error("Failed to toggle music like", error);
  } finally {
    likeBusy.value = false;
  }
}
</script>
