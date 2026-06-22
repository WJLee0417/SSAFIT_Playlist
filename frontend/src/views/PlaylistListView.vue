<template>
  <AppLayout>
    <div class="page-heading">
      <div>
        <span class="eyebrow">PLAYLISTS</span>
        <h1>플레이리스트</h1>
      </div>
      <RouterLink class="primary-button" to="/playlists/new">새 플레이리스트</RouterLink>
    </div>

    <form class="playlist-filter" @submit.prevent="applyOwnerFilter">
      <label>
        소유자 회원 ID
        <input v-model="ownerIdInput" inputmode="numeric" placeholder="전체 조회" />
      </label>
      <button type="submit">조회</button>
      <button type="button" class="secondary-button" @click="clearOwnerFilter">전체 보기</button>
    </form>
    <p v-if="filterMessage" class="field-error" role="alert">{{ filterMessage }}</p>

    <PageStatePanel
      :status="status"
      :error="error"
      empty-message="조회 가능한 플레이리스트가 없습니다."
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
import { ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { playlistApi } from "../api/playlistApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import PlaylistCard from "../components/PlaylistCard.vue";
import { usePageState } from "../utils/pageState";

const route = useRoute();
const router = useRouter();
const ownerIdInput = ref("");
const filterMessage = ref("");
const { data, status, error, run } = usePageState([]);

function routeOwnerId() {
  const raw = Array.isArray(route.query.ownerId)
    ? route.query.ownerId[0]
    : route.query.ownerId;
  if (raw == null || raw === "") return null;
  const value = Number(raw);
  return Number.isSafeInteger(value) && value >= 1 ? value : null;
}

function load() {
  const ownerId = routeOwnerId();
  ownerIdInput.value = ownerId == null ? "" : String(ownerId);
  return run(() => playlistApi.getList({ ownerId: ownerId ?? undefined }));
}

function applyOwnerFilter() {
  filterMessage.value = "";
  const raw = ownerIdInput.value.trim();
  if (!raw) {
    router.push({ name: "playlist-list" });
    return;
  }
  if (!/^\d+$/.test(raw) || Number(raw) < 1) {
    filterMessage.value = "소유자 회원 ID는 1 이상의 정수로 입력해 주세요.";
    return;
  }
  router.push({ name: "playlist-list", query: { ownerId: Number(raw) } });
}

function clearOwnerFilter() {
  filterMessage.value = "";
  ownerIdInput.value = "";
  router.push({ name: "playlist-list" });
}

watch(() => route.query, load, { immediate: true, deep: true });
</script>
