<template>
  <AppLayout>
    <div class="page-heading">
      <div>
        <span class="eyebrow">SPOTIFY TRACK SEARCH</span>
        <h1>Spotify 음악 검색</h1>
      </div>
      <RouterLink v-if="returnTo" class="secondary-button" :to="returnTo">
        플레이리스트 상세로 돌아가기
      </RouterLink>
    </div>

    <p class="lead">
      Spotify 검색 결과를 먼저 내부 음악으로 저장한 뒤, 반환된 내부 music ID로 플레이리스트에 추가합니다.
    </p>
    <p v-if="!playlistId" class="contract-note">
      플레이리스트 ID가 없어 곡 검색만 가능합니다. 수록곡 추가는 플레이리스트 상세에서 이 화면으로 진입해 주세요.
    </p>

    <form class="spotify-search-form" @submit.prevent="search">
      <label>
        검색어
        <input v-model="query" maxlength="200" placeholder="곡 또는 아티스트" />
      </label>
      <button type="submit" :disabled="searching">
        {{ searching ? "검색 중..." : "검색" }}
      </button>
    </form>
    <p v-if="validationMessage" class="field-error" role="alert">{{ validationMessage }}</p>
    <SaveStatus :type="actionType" :message="actionMessage" />

    <PageStatePanel
      v-if="searchStatus !== 'idle' && searchStatus !== 'ready'"
      :status="searchStatus"
      :error="searchDisplayError"
      empty-title="Spotify 검색 결과가 없습니다"
      empty-message="다른 곡명이나 아티스트로 검색해 보세요."
      :retryable="searchStatus === 'error'"
      @retry="search"
    />

    <section v-if="searchStatus === 'ready'" class="spotify-track-grid">
      <article
        v-for="track in tracks"
        :key="track.spotifyTrackId"
        class="spotify-track-card"
      >
        <img
          v-if="track.imageUrl"
          :src="track.imageUrl"
          :alt="`${track.title} 앨범 이미지`"
        />
        <div v-else class="spotify-image-placeholder" aria-hidden="true">♫</div>
        <div class="spotify-track-body">
          <span class="eyebrow">{{ track.albumName || "앨범 정보 없음" }}</span>
          <h2>{{ track.title }}</h2>
          <p>{{ track.artist }}</p>
          <dl class="playlist-meta">
            <div><dt>장르</dt><dd>{{ track.genre || "정보 없음" }}</dd></div>
            <div><dt>BPM</dt><dd>{{ track.musicBpm ? `${track.musicBpm} BPM` : "정보 없음" }}</dd></div>
            <div><dt>재생 시간</dt><dd>{{ formatDuration(track.durationMs) }}</dd></div>
            <div><dt>Track ID</dt><dd>{{ track.spotifyTrackId }}</dd></div>
            <div><dt>URI</dt><dd>{{ track.spotifyUri }}</dd></div>
          </dl>
          <div class="card-actions">
            <button
              v-if="playlistId"
              type="button"
              :disabled="selectedTrackId != null"
              @click="saveAndAdd(track)"
            >
              {{ selectedTrackId === track.spotifyTrackId ? "저장 및 추가 중..." : "이 곡 추가" }}
            </button>
            <a
              v-if="track.spotifyExternalUrl"
              class="text-link"
              :href="track.spotifyExternalUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              Spotify에서 확인
            </a>
          </div>
        </div>
      </article>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { playlistApi } from "../api/playlistApi";
import { spotifyApi } from "../api/spotifyApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import { isSafeSpotifyReturnPath } from "../utils/oauthReturn";
import { spotifyErrorMessage, spotifyErrorState } from "../utils/spotifyError";

const route = useRoute();
const router = useRouter();
const query = ref("");
const tracks = ref([]);
const searchStatus = ref("idle");
const searchError = ref(null);
const validationMessage = ref("");
const searching = ref(false);
const selectedTrackId = ref(null);
const actionMessage = ref("");
const actionType = ref("success");
let searchSequence = 0;

const playlistId = computed(() => {
  const raw = Array.isArray(route.query.playlistId)
    ? route.query.playlistId[0]
    : route.query.playlistId;
  const value = Number(raw);
  return Number.isSafeInteger(value) && value >= 1 ? value : null;
});
const nextOrder = computed(() => {
  const raw = Array.isArray(route.query.nextOrder)
    ? route.query.nextOrder[0]
    : route.query.nextOrder;
  const value = Number(raw);
  return Number.isSafeInteger(value) && value >= 1 ? value : 1;
});
const returnTo = computed(() => {
  const candidate = Array.isArray(route.query.returnTo)
    ? route.query.returnTo[0]
    : route.query.returnTo;
  return isSafeSpotifyReturnPath(candidate) ? candidate : null;
});
const searchDisplayError = computed(() => {
  if (!searchError.value) return null;
  return {
    status: searchError.value.status,
    errorCode: searchError.value.errorCode,
    message: spotifyErrorMessage(searchError.value),
    notFound: searchError.value.notFound,
    conflict: searchError.value.conflict,
    upstreamUnavailable: searchError.value.upstreamUnavailable,
  };
});

async function search() {
  const normalizedQuery = query.value.trim();
  validationMessage.value = "";
  actionMessage.value = "";
  if (!normalizedQuery) {
    validationMessage.value = "Spotify 검색어를 입력해 주세요.";
    return;
  }
  if (searching.value) return;

  const sequence = ++searchSequence;
  searching.value = true;
  searchStatus.value = "loading";
  searchError.value = null;
  tracks.value = [];
  try {
    const response = await spotifyApi.searchTracks({
      query: normalizedQuery,
      limit: 10,
    });
    if (sequence !== searchSequence) return;
    tracks.value = Array.isArray(response) ? response : [];
    searchStatus.value = tracks.value.length ? "ready" : "empty";
  } catch (caught) {
    if (sequence !== searchSequence) return;
    searchError.value = caught;
    searchStatus.value = spotifyErrorState(caught);
  } finally {
    if (sequence === searchSequence) searching.value = false;
  }
}

async function saveAndAdd(track) {
  if (!playlistId.value || selectedTrackId.value != null) return;
  selectedTrackId.value = track.spotifyTrackId;
  actionMessage.value = "";
  try {
    const music = await spotifyApi.importTrack(track);
    if (!music?.id) {
      throw new Error("내부 음악 ID를 확인하지 못했습니다.");
    }
    try {
      await playlistApi.addItem(playlistId.value, {
        musicId: music.id,
        orderNo: nextOrder.value,
      });
    } catch (caught) {
      actionType.value = "error";
      actionMessage.value = `곡은 내부 음악 #${music.id}로 저장됐지만 플레이리스트 추가는 실패했습니다. ${spotifyErrorMessage(caught)}`;
      return;
    }

    actionType.value = "success";
    actionMessage.value = `내부 음악 #${music.id}를 플레이리스트에 추가했습니다.`;
    if (returnTo.value) {
      await router.push(returnTo.value);
    }
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = spotifyErrorMessage(caught);
  } finally {
    selectedTrackId.value = null;
  }
}

function formatDuration(durationMs) {
  const value = Number(durationMs);
  if (!Number.isFinite(value) || value <= 0) return "정보 없음";
  const totalSeconds = Math.round(value / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}
</script>
