<template>
  <AppLayout>
    <div class="page-heading music-page-heading">
      <div>
        <span class="eyebrow">MUSIC</span>
        <h1>음악 둘러보기</h1>
      </div>
    </div>

    <section
      ref="spotifySearchRoot"
      class="playlist-spotify-discovery music-spotify-discovery"
      aria-labelledby="music-spotify-search-title"
    >
      <div class="playlist-search-copy">
        <span class="eyebrow">SPOTIFY TRACK SEARCH</span>
        <h2 id="music-spotify-search-title">Spotify에서 음악 찾기</h2>
      </div>

      <form class="playlist-spotify-search-form" @submit.prevent="searchSpotify">
        <div class="playlist-search-control">
          <input
            id="music-spotify-query"
            v-model="spotifyQuery"
            aria-label="Spotify 음악 검색"
            maxlength="200"
            placeholder="곡명 또는 아티스트를 검색하세요"
            autocomplete="off"
            @focus="openSpotifyDropdown"
            @keydown.esc="closeSpotifyDropdown"
          />

          <div v-if="shouldShowSpotifyDropdown" class="playlist-search-dropdown">
            <p v-if="spotifySearchFeedbackMessage" class="playlist-search-state">
              {{ spotifySearchFeedbackMessage }}
            </p>
            <article
              v-for="track in spotifyTracks"
              :key="track.spotifyTrackId"
              class="playlist-search-result"
            >
              <img v-if="track.imageUrl" :src="track.imageUrl" :alt="`${track.title} 앨범 커버`" />
              <div v-else class="playlist-result-placeholder" aria-hidden="true">♫</div>
              <div class="playlist-result-copy">
                <strong>{{ track.title }}</strong>
                <span>{{ track.artist }}</span>
              </div>
              <button
                type="button"
                class="secondary-button playlist-add-trigger"
                :class="{ active: isSpotifyTrackSaved(track) }"
                :disabled="spotifySavingTrackId != null || isSpotifyTrackSaved(track)"
                @click="saveSpotifyTrack(track)"
              >
                {{
                  isSpotifyTrackSaved(track)
                    ? "저장됨"
                    : spotifySavingTrackId === track.spotifyTrackId
                      ? "저장 중..."
                      : "저장"
                }}
              </button>
            </article>
          </div>
        </div>
        <button type="submit" :disabled="spotifySearching">
          {{ spotifySearching ? "검색 중..." : "Spotify 검색" }}
        </button>
      </form>
      <p v-if="spotifyValidationMessage" class="field-error" role="alert">
        {{ spotifyValidationMessage }}
      </p>
      <SaveStatus :type="spotifyActionType" :message="spotifyActionMessage" />
    </section>

    <PageStatePanel
      v-if="status !== 'idle'"
      :status="status"
      :error="error"
      empty-message="현재 조건에 맞는 음악이 없습니다."
      @retry="loadFromRoute"
    />

    <div v-if="status === 'ready'" class="playlist-library-toolbar">
      <div class="playlist-library-title">
        <h2>
          <RouterLink class="playlist-library-link" to="/playlists">플레이리스트</RouterLink>
          <span class="playlist-library-separator" aria-hidden="true">|</span>
          <span class="playlist-library-active">음악 둘러보기</span>
        </h2>
      </div>
    </div>

    <section v-if="status === 'ready'" class="music-filter-section">
      <button
        type="button"
        class="music-filter-toggle"
        :aria-expanded="filterExpanded"
        @click="filterExpanded = !filterExpanded"
      >
        <span>검색 필터링</span>
        <small>{{ filterExpanded ? "접기" : "펼치기" }}</small>
      </button>
      <MusicSearchBar
        v-if="filterExpanded"
        :model="filters"
        :validation-message="validationMessage"
        @search="applyFilters"
        @sort-change="applySort"
        @reset="resetFilters"
      />
    </section>

    <template v-if="status === 'ready'">
      <p class="result-summary">총 {{ pageData.totalCount }}곡 · {{ pageData.page }}페이지</p>
      <section class="card-grid music-card-grid">
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { authApi } from "../api/authApi";
import { musicApi } from "../api/musicApi";
import { spotifyApi } from "../api/spotifyApi";
import AppLayout from "../components/AppLayout.vue";
import MusicCard from "../components/MusicCard.vue";
import MusicSearchBar from "../components/MusicSearchBar.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import {
  DEFAULT_MUSIC_QUERY,
  musicQueryFromRoute,
  musicQueryToApi,
  musicQueryToRoute,
  normalizeMusicPage,
  validateMusicQuery,
} from "../utils/musicQuery";
import { spotifyErrorMessage, spotifyErrorState } from "../utils/spotifyError";

const route = useRoute();
const router = useRouter();
const filters = reactive({ ...DEFAULT_MUSIC_QUERY });
const pageData = ref(normalizeMusicPage(null, DEFAULT_MUSIC_QUERY));
const status = ref("idle");
const error = ref(null);
const currentMember = ref(null);
const validationMessage = ref("");
const filterExpanded = ref(false);
const spotifyQuery = ref("");
const spotifyTracks = ref([]);
const spotifySearchStatus = ref("idle");
const spotifySearchError = ref(null);
const spotifyValidationMessage = ref("");
const spotifySearching = ref(false);
const spotifyDropdownOpen = ref(false);
const spotifySearchRoot = ref(null);
const spotifySavingTrackId = ref(null);
const savedSpotifyTrackIds = ref(new Set());
const spotifyActionMessage = ref("");
const spotifyActionType = ref("success");
let requestSequence = 0;
let spotifySearchSequence = 0;

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

const spotifySearchFeedbackMessage = computed(() => {
  if (spotifySearchStatus.value === "loading") return "Spotify에서 검색 중입니다.";
  if (spotifySearchStatus.value === "empty") return "검색 결과가 없습니다. 다른 곡명이나 아티스트로 검색해 보세요.";
  if (spotifySearchStatus.value === "error") return spotifyErrorMessage(spotifySearchError.value);
  if (["loginRequired", "unauthorized"].includes(spotifySearchStatus.value)) {
    return spotifyErrorMessage(spotifySearchError.value);
  }
  return "";
});
const shouldShowSpotifyDropdown = computed(() => (
  spotifyDropdownOpen.value
  && spotifySearchStatus.value !== "idle"
  && (spotifyTracks.value.length > 0 || Boolean(spotifySearchFeedbackMessage.value))
));

async function searchSpotify() {
  const normalizedQuery = spotifyQuery.value.trim();
  spotifyValidationMessage.value = "";
  spotifyActionMessage.value = "";
  if (!normalizedQuery) {
    spotifyValidationMessage.value = "검색할 곡명이나 아티스트를 입력해 주세요.";
    return;
  }
  if (spotifySearching.value) return;

  const sequence = ++spotifySearchSequence;
  spotifySearching.value = true;
  spotifyDropdownOpen.value = true;
  spotifySearchStatus.value = "loading";
  spotifySearchError.value = null;
  spotifyTracks.value = [];
  try {
    const response = await spotifyApi.searchTracks({ query: normalizedQuery, limit: 10 });
    if (sequence !== spotifySearchSequence) return;
    spotifyTracks.value = Array.isArray(response) ? response : [];
    spotifySearchStatus.value = spotifyTracks.value.length ? "ready" : "empty";
  } catch (caught) {
    if (sequence !== spotifySearchSequence) return;
    spotifySearchError.value = caught;
    spotifySearchStatus.value = spotifyErrorState(caught);
  } finally {
    if (sequence === spotifySearchSequence) spotifySearching.value = false;
  }
}

function openSpotifyDropdown() {
  if (spotifyTracks.value.length || spotifySearchFeedbackMessage.value) {
    spotifyDropdownOpen.value = true;
  }
}

function closeSpotifyDropdown() {
  spotifyDropdownOpen.value = false;
}

function handleDocumentPointerDown(event) {
  if (spotifySearchRoot.value?.contains(event.target)) return;
  closeSpotifyDropdown();
}

async function saveSpotifyTrack(track) {
  if (spotifySavingTrackId.value != null || !track?.spotifyTrackId) return;
  spotifySavingTrackId.value = track.spotifyTrackId;
  spotifyActionMessage.value = "";
  try {
    await spotifyApi.importTrack(track);
    savedSpotifyTrackIds.value = new Set([...savedSpotifyTrackIds.value, track.spotifyTrackId]);
    spotifyActionType.value = "success";
    spotifyActionMessage.value = `"${track.title}"을(를) 음악 목록에 저장했습니다.`;
    await loadFromRoute();
  } catch (caught) {
    spotifyActionType.value = "error";
    spotifyActionMessage.value = spotifyErrorMessage(caught);
  } finally {
    spotifySavingTrackId.value = null;
  }
}

function isSpotifyTrackSaved(track) {
  return Boolean(track?.spotifyTrackId && savedSpotifyTrackIds.value.has(track.spotifyTrackId));
}

watch(() => route.query, loadFromRoute, { immediate: true, deep: true });
onMounted(() => {
  resolveViewer();
  document.addEventListener("pointerdown", handleDocumentPointerDown);
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", handleDocumentPointerDown);
});
</script>
