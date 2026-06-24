<template>
  <AppLayout>
    <div class="page-heading">
      <div>
        <span class="eyebrow">MUSIC</span>
        <h1>음악 둘러보기</h1>
      </div>
      <RouterLink class="secondary-button" to="/spotify/search">Spotify 검색</RouterLink>
    </div>

    <div class="form-actions">
      <RouterLink class="secondary-button" to="/music-manage/new">내부 음악 등록</RouterLink>
    </div>

    <MusicSearchBar
      :model="filters"
      :validation-message="validationMessage"
      @search="applyFilters"
      @sort-change="applySort"
      @reset="resetFilters"
    />

    <PageStatePanel
      v-if="status !== 'idle'"
      :status="status"
      :error="error"
      empty-message="현재 조건에 맞는 음악이 없습니다."
      @retry="loadFromRoute"
    />

    <template v-if="status === 'ready'">
      <p class="result-summary">총 {{ pageData.totalCount }}곡 · {{ pageData.page }}페이지</p>
      <section class="card-grid">
        <MusicCard
          v-for="music in pageData.items"
          :key="music.id"
          :music="music"
          :return-to="route.fullPath"
          :current-member-id="currentMember?.id"
        />
      </section>
      <nav v-if="pageData.totalPages > 1" class="pagination" aria-label="음악 목록 페이지">
        <button
          type="button"
          class="secondary-button"
          :disabled="pageData.page <= 1"
          @click="movePage(pageData.page - 1)"
        >
          이전
        </button>
        <span>{{ pageData.page }} / {{ pageData.totalPages }}</span>
        <button
          type="button"
          class="secondary-button"
          :disabled="pageData.page >= pageData.totalPages"
          @click="movePage(pageData.page + 1)"
        >
          다음
        </button>
      </nav>
    </template>
  </AppLayout>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { authApi } from "../api/authApi";
import { musicApi } from "../api/musicApi";
import AppLayout from "../components/AppLayout.vue";
import MusicCard from "../components/MusicCard.vue";
import MusicSearchBar from "../components/MusicSearchBar.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import {
  DEFAULT_MUSIC_QUERY,
  musicQueryFromRoute,
  musicQueryToApi,
  musicQueryToRoute,
  normalizeMusicPage,
  validateMusicQuery,
} from "../utils/musicQuery";

const route = useRoute();
const router = useRouter();
const filters = reactive({ ...DEFAULT_MUSIC_QUERY });
const pageData = ref(normalizeMusicPage(null, DEFAULT_MUSIC_QUERY));
const status = ref("idle");
const error = ref(null);
const currentMember = ref(null);
const validationMessage = ref("");
let requestSequence = 0;

function assignFilters(query) {
  Object.assign(filters, musicQueryFromRoute(query));
}

async function loadFromRoute() {
  const sequence = ++requestSequence;
  assignFilters(route.query);
  validationMessage.value = validateMusicQuery(filters);
  if (validationMessage.value) {
    status.value = "idle";
    pageData.value = normalizeMusicPage(null, filters);
    return;
  }

  status.value = "loading";
  error.value = null;
  try {
    const response = await musicApi.getList(musicQueryToApi(filters));
    if (sequence !== requestSequence) return;
    pageData.value = normalizeMusicPage(response, filters);
    status.value = pageData.value.items.length ? "ready" : "empty";
  } catch (caught) {
    if (sequence !== requestSequence) return;
    error.value = caught;
    status.value = "error";
  }
}

async function resolveViewer() {
  try {
    currentMember.value = await authApi.getSession();
  } catch {
    currentMember.value = null;
  }
}

function updateRoute(nextFilters, replace = false) {
  const navigation = {
    name: "music-list",
    query: musicQueryToRoute(nextFilters),
  };
  return replace ? router.replace(navigation) : router.push(navigation);
}

function applyFilters() {
  validationMessage.value = validateMusicQuery(filters);
  if (validationMessage.value) {
    status.value = "idle";
    return;
  }
  updateRoute({ ...filters, page: 1 });
}

function applySort() {
  validationMessage.value = validateMusicQuery(filters);
  if (validationMessage.value) {
    status.value = "idle";
    return;
  }
  updateRoute({ ...filters, page: 1 });
}

function resetFilters() {
  validationMessage.value = "";
  updateRoute({ ...DEFAULT_MUSIC_QUERY, page: 1 });
}

function movePage(page) {
  if (page < 1 || page > pageData.value.totalPages) return;
  updateRoute({ ...filters, page });
}

watch(() => route.query, loadFromRoute, { immediate: true, deep: true });
onMounted(resolveViewer);
</script>
