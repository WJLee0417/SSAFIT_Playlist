<template>
  <AppLayout>
    <div class="page-heading playlist-page-heading">
      <div>
        <span class="eyebrow">PLAYLISTS</span>
        <h1>심장이 따라 반응하도록</h1>
      </div>
      <RouterLink class="primary-button" to="/playlists/new">새 플레이리스트</RouterLink>
    </div>

    <section class="playlist-spotify-discovery" aria-labelledby="spotify-search-title">
      <div class="playlist-search-copy">
        <span class="eyebrow">SPOTIFY TRACK SEARCH</span>
        <h2 id="spotify-search-title">음악을 찾고, 내 러닝 플로우에 담아보세요</h2>
      </div>

      <form class="playlist-spotify-search-form" @submit.prevent="searchSpotify">
        <div class="playlist-search-control">
          <input
            id="playlist-spotify-query"
            v-model="spotifyQuery"
            aria-label="Spotify 음악 검색"
            maxlength="200"
            placeholder="곡명 또는 아티스트를 검색하세요"
            autocomplete="off"
          />

          <div v-if="shouldShowSearchDropdown" class="playlist-search-dropdown">
            <p v-if="searchFeedbackMessage" class="playlist-search-state">
              {{ searchFeedbackMessage }}
            </p>

            <article
              v-for="track in spotifyTracks"
              :key="track.spotifyTrackId"
              class="playlist-search-result"
            >
              <img
                v-if="track.imageUrl"
                :src="track.imageUrl"
                :alt="`${track.title} 앨범 커버`"
              />
              <div v-else class="playlist-result-placeholder" aria-hidden="true">♫</div>

              <div class="playlist-result-copy">
                <strong>{{ track.title }}</strong>
                <span>{{ track.artist }}</span>
              </div>

              <div class="playlist-add-control">
                <button
                  type="button"
                  class="secondary-button playlist-add-trigger"
                  :class="{ active: isTrackAdded(track) }"
                  :aria-expanded="openTrackId === track.spotifyTrackId"
                  :disabled="addingTrackId != null || isTrackAdded(track)"
                  @click="togglePlaylistMenu(track.spotifyTrackId)"
                >
                  {{
                    isTrackAdded(track)
                      ? "추가됨"
                      : addingTrackId === track.spotifyTrackId
                        ? "추가 중..."
                        : "플레이리스트에 추가"
                  }}
                </button>

                <div
                  v-if="openTrackId === track.spotifyTrackId"
                  class="playlist-add-menu"
                >
                  <div class="playlist-add-menu-heading">
                    <strong>추가할 플레이리스트</strong>
                    <span>BPM과 장르가 가까운 순서예요.</span>
                  </div>

                  <button
                    v-for="candidate in compatiblePlaylists(track)"
                    :key="candidate.id"
                    type="button"
                    class="playlist-add-option"
                    :disabled="addingTrackId != null"
                    @click="addTrackToPlaylist(track, candidate)"
                  >
                    <span>
                      <strong>{{ candidate.title }}</strong>
                      <small>{{ candidate.genreLabel }} · {{ candidate.bpmLabel }}</small>
                    </span>
                    <em v-if="candidate.genreMatch">장르 일치</em>
                  </button>

                  <p v-if="!compatiblePlaylists(track).length" class="empty-inline">
                    이 곡을 추가할 수 있는 내 플레이리스트가 없습니다.
                  </p>
                </div>
              </div>
            </article>
          </div>
        </div>
        <button type="submit" :disabled="searching">
          {{ searching ? "검색 중..." : "Spotify 검색" }}
        </button>
      </form>

      <p v-if="searchValidationMessage" class="field-error" role="alert">
        {{ searchValidationMessage }}
      </p>
      <SaveStatus :type="actionType" :message="actionMessage" />

    </section>

    <PageStatePanel
      :status="status"
      :error="error"
      empty-title="플레이리스트가 없습니다"
      empty-message="새 플레이리스트를 추가해 보세요."
      @retry="load"
    />

    <div v-if="status === 'ready'" class="playlist-library-toolbar">
      <div class="playlist-library-title">
        <h2>
          <span>플레이리스트</span>
          <span class="playlist-library-separator" aria-hidden="true">|</span>
          <RouterLink class="playlist-library-link" to="/music">음악 둘러보기</RouterLink>
        </h2>
      </div>
      <div class="playlist-scope-toggle" role="group" aria-label="플레이리스트 조회 범위">
        <button
          v-for="option in FILTER_OPTIONS"
          :key="option.value"
          type="button"
          :class="{ active: activeFilter === option.value }"
          :aria-pressed="activeFilter === option.value"
          @click="activeFilter = option.value"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <section
      v-if="status === 'ready' && !filteredPlaylists.length"
      class="state-panel playlist-filter-empty"
    >
      <h2>{{ filterEmptyTitle }}</h2>
      <p>{{ filterEmptyMessage }}</p>
    </section>

    <div v-if="status === 'ready'" class="playlist-intensity-sections">
      <section
        v-for="group in visibleGroups"
        :key="group.key"
        class="playlist-intensity-section"
        :class="`playlist-intensity-${group.key.toLowerCase()}`"
      >
        <div class="playlist-section-heading">
          <div>
            <span class="eyebrow">{{ group.eyebrow }}</span>
            <h2>{{ group.title }}</h2>
          </div>
        </div>

        <div class="playlist-horizontal-scroll">
          <p v-if="!group.playlists.length" class="empty-inline playlist-section-empty">
            아직 사용자 플레이리스트가 없습니다.
          </p>
          <RouterLink
            v-for="playlist in group.playlists"
            :key="playlist.id"
            class="content-card playlist-library-card"
            :to="detailLocation(playlist)"
          >
            <div>
              <span class="eyebrow">{{ playlist.genreLabel }}</span>
              <h3>{{ playlist.title }}</h3>
            </div>
            <dl class="playlist-library-meta">
              <div>
                <dt>대표 장르</dt>
                <dd>{{ playlist.genreLabel }}</dd>
              </div>
            </dl>
          </RouterLink>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<script setup>
import { computed, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { authApi } from "../api/authApi";
import { playlistApi } from "../api/playlistApi";
import { spotifyApi } from "../api/spotifyApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import { usePageState } from "../utils/pageState";
import { spotifyErrorMessage, spotifyErrorState } from "../utils/spotifyError";

const INTENSITY_GROUPS = [
  {
    key: "LOW",
    eyebrow: "LOW INTENSITY",
    title: "가볍게 리듬 찾기",
    min: 90,
    max: 130,
  },
  {
    key: "MODERATE",
    eyebrow: "MODERATE",
    title: "꾸준히 페이스 유지",
    min: 120,
    max: 160,
  },
  {
    key: "HIGH",
    eyebrow: "HIGH INTENSITY",
    title: "호흡을 끌어올리기",
    min: 150,
    max: 190,
  },
  {
    key: "INTERVAL",
    eyebrow: "INTERVAL",
    title: "폭발적 인터벌",
    min: 140,
    max: 200,
  },
  {
    key: "UNCLASSIFIED",
    eyebrow: "USER",
    title: "사용자 플레이리스트",
    min: null,
    max: null,
  },
];

const FILTER_OPTIONS = [
  { value: "MY", label: "MY" },
  { value: "PUBLIC", label: "PUBLIC" },
  { value: "ALL", label: "ALL" },
];

const route = useRoute();
const { data, status, error, run } = usePageState([]);
const currentMember = ref(null);
const activeFilter = ref("ALL");
const spotifyQuery = ref("");
const spotifyTracks = ref([]);
const searchStatus = ref("idle");
const searchError = ref(null);
const searchValidationMessage = ref("");
const searching = ref(false);
const openTrackId = ref(null);
const addingTrackId = ref(null);
const addedTrackIds = ref(new Set());
const actionMessage = ref("");
const actionType = ref("success");
let searchSequence = 0;

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
const searchFeedbackMessage = computed(() => {
  if (searchStatus.value === "loading") return "Spotify에서 검색 중입니다.";
  if (searchStatus.value === "empty") return "검색 결과가 없습니다. 다른 곡명이나 아티스트로 검색해 보세요.";
  if (searchStatus.value === "error") return searchDisplayError.value?.message || "검색에 실패했습니다.";
  if (["loginRequired", "unauthorized"].includes(searchStatus.value)) {
    return searchDisplayError.value?.message || "Spotify 검색 권한을 확인해 주세요.";
  }
  return "";
});
const shouldShowSearchDropdown = computed(() => (
  searchStatus.value !== "idle"
  && (spotifyTracks.value.length > 0 || Boolean(searchFeedbackMessage.value))
));

const filteredPlaylists = computed(() => {
  if (activeFilter.value === "MY") {
    return data.value.filter((playlist) => isOwnedPlaylist(playlist));
  }
  if (activeFilter.value === "PUBLIC") {
    return data.value.filter((playlist) => playlist.visibility === "PUBLIC");
  }
  return data.value;
});

const filterEmptyTitle = computed(() => (
  activeFilter.value === "MY"
    ? "내 플레이리스트가 없습니다"
    : "공개 플레이리스트가 없습니다"
));

const filterEmptyMessage = computed(() => (
  activeFilter.value === "MY"
    ? "새 플레이리스트를 만들거나 Spotify에서 가져와 보세요."
    : "현재 조회할 수 있는 공개 플레이리스트가 없습니다."
));

const visibleGroups = computed(() => INTENSITY_GROUPS
  .map((group) => ({
    ...group,
    playlists: sortGroupPlaylists(
      group,
      filteredPlaylists.value.filter(
        (playlist) => playlist.intensityKey === group.key,
      ),
    ),
  }))
  .filter((group) => (
    group.playlists.length
    || (group.key === "UNCLASSIFIED" && currentMember.value)
  )));

const manageablePlaylists = computed(() => data.value.filter((playlist) => isOwnedPlaylist(playlist)));

function normalizeGenre(value) {
  return String(value || "")
    .trim()
    .toLocaleLowerCase()
    .replace(/\s+/g, "");
}

function isOwnedPlaylist(playlist) {
  return Boolean(
    currentMember.value
      && playlist?.playlistType !== "OFFICIAL"
      && playlist?.ownerId != null
      && String(playlist.ownerId) === String(currentMember.value.id),
  );
}

function sortGroupPlaylists(group, playlists) {
  if (group.key !== "UNCLASSIFIED") return playlists;
  return [...playlists].sort((left, right) => (
    Number(isFavoritePlaylist(right)) - Number(isFavoritePlaylist(left))
  ));
}

function isFavoritePlaylist(playlist) {
  return playlist?.playlistType === "USER" && playlist?.title === "좋아요한 노래";
}

function summarizePlaylist(playlist, detail) {
  const items = Array.isArray(detail?.items) ? detail.items : [];
  const genres = items
    .map((item) => item.music?.genre?.trim())
    .filter(Boolean);
  const genreCounts = genres.reduce((counts, genre) => {
    const key = normalizeGenre(genre);
    const current = counts.get(key) || { label: genre, count: 0 };
    current.count += 1;
    counts.set(key, current);
    return counts;
  }, new Map());
  const representativeGenre = [...genreCounts.values()]
    .sort((left, right) => right.count - left.count)[0]?.label || null;
  const bpms = items
    .map((item) => Number(item.music?.musicBpm))
    .filter((bpm) => Number.isFinite(bpm) && bpm > 0);
  const bpmMin = bpms.length ? Math.min(...bpms) : null;
  const bpmMax = bpms.length ? Math.max(...bpms) : null;
  const bpmAverage = bpms.length
    ? bpms.reduce((sum, bpm) => sum + bpm, 0) / bpms.length
    : null;

  return {
    ...playlist,
    items,
    representativeGenre,
    genreLabel: representativeGenre || "장르 미정",
    bpmMin,
    bpmMax,
    bpmAverage,
    bpmLabel: bpmMin == null
      ? "BPM 미정"
      : bpmMin === bpmMax
        ? `${bpmMin} BPM`
        : `${bpmMin}–${bpmMax} BPM`,
    intensityKey: isUserSectionPlaylist(playlist)
      ? "UNCLASSIFIED"
      : resolveIntensityKey(bpmMin, bpmMax, bpmAverage),
  };
}

function isUserSectionPlaylist(playlist) {
  return isFavoritePlaylist(playlist)
    || (
      playlist?.playlistType === "USER"
      && playlist?.sourceType === "CURATION_COPY"
    );
}

function resolveIntensityKey(bpmMin, bpmMax, bpmAverage) {
  if (bpmAverage == null) return "UNCLASSIFIED";
  if (bpmMax - bpmMin >= 35 && bpmMax >= 170) return "INTERVAL";
  if (bpmAverage < 125) return "LOW";
  if (bpmAverage < 150) return "MODERATE";
  if (bpmAverage < 175) return "HIGH";
  return "INTERVAL";
}

async function load() {
  try {
    currentMember.value = await authApi.getSession();
  } catch {
    currentMember.value = null;
  }

  return run(async () => {
    const playlists = await playlistApi.getList();
    const details = await Promise.allSettled(
      playlists.map((playlist) => playlistApi.getDetail(playlist.id)),
    );

    return playlists.map((playlist, index) => summarizePlaylist(
      playlist,
      details[index].status === "fulfilled" ? details[index].value : null,
    ));
  });
}

function detailLocation(playlist) {
  return {
    name: "playlist-detail",
    params: { playlistId: playlist.id },
    query: { returnTo: route.fullPath },
  };
}

async function searchSpotify() {
  const normalizedQuery = spotifyQuery.value.trim();
  searchValidationMessage.value = "";
  actionMessage.value = "";
  openTrackId.value = null;
  if (!normalizedQuery) {
    searchValidationMessage.value = "검색할 곡명이나 아티스트를 입력해 주세요.";
    return;
  }
  if (searching.value) return;

  const sequence = ++searchSequence;
  searching.value = true;
  searchStatus.value = "loading";
  searchError.value = null;
  spotifyTracks.value = [];
  try {
    const response = await spotifyApi.searchTracks({
      query: normalizedQuery,
      limit: 10,
    });
    if (sequence !== searchSequence) return;
    spotifyTracks.value = Array.isArray(response) ? response : [];
    searchStatus.value = spotifyTracks.value.length ? "ready" : "empty";
  } catch (caught) {
    if (sequence !== searchSequence) return;
    searchError.value = caught;
    searchStatus.value = spotifyErrorState(caught);
  } finally {
    if (sequence === searchSequence) searching.value = false;
  }
}

function togglePlaylistMenu(trackId) {
  openTrackId.value = openTrackId.value === trackId ? null : trackId;
}

function playlistIntensityRange(playlist) {
  return INTENSITY_GROUPS.find((group) => group.key === playlist.intensityKey);
}

function compatiblePlaylists(track) {
  const trackBpm = Number(track.musicBpm);
  const hasTrackBpm = Number.isFinite(trackBpm) && trackBpm > 0;
  const trackGenre = normalizeGenre(track.genre);

  return manageablePlaylists.value
    .filter((playlist) => !playlist.items.some((item) => (
      item.music?.spotifyTrackId === track.spotifyTrackId
    )))
    .map((playlist) => {
      const range = playlistIntensityRange(playlist);
      const bpmMatch = !hasTrackBpm
        || playlist.bpmAverage == null
        || (
          range?.min != null
          && trackBpm >= range.min
          && trackBpm <= range.max
        );
      const genreMatch = Boolean(
        trackGenre
        && playlist.representativeGenre
        && normalizeGenre(playlist.representativeGenre) === trackGenre,
      );
      return { ...playlist, bpmMatch, genreMatch };
    })
    .filter((playlist) => playlist.bpmMatch)
    .sort((left, right) => (
      Number(right.genreMatch) - Number(left.genreMatch)
      || Number(left.bpmAverage == null) - Number(right.bpmAverage == null)
      || left.title.localeCompare(right.title, "ko")
    ));
}

async function addTrackToPlaylist(track, playlist) {
  if (addingTrackId.value != null) return;
  addingTrackId.value = track.spotifyTrackId;
  actionMessage.value = "";
  try {
    const music = await spotifyApi.importTrack(track);
    if (!music?.id) throw new Error("음악 정보를 저장하지 못했습니다.");

    const nextOrder = playlist.items.reduce(
      (maximum, item) => Math.max(maximum, Number(item.orderNo) || 0),
      0,
    ) + 1;
    await playlistApi.addItem(playlist.id, {
      musicId: music.id,
      orderNo: nextOrder,
    });

    actionType.value = "success";
    actionMessage.value = `"${track.title}"을(를) "${playlist.title}"에 추가했습니다.`;
    addedTrackIds.value = new Set([...addedTrackIds.value, track.spotifyTrackId]);
    openTrackId.value = null;
    await load();
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = spotifyErrorMessage(caught);
  } finally {
    addingTrackId.value = null;
  }
}

function isTrackAdded(track) {
  return Boolean(track?.spotifyTrackId && addedTrackIds.value.has(track.spotifyTrackId));
}

load();
</script>
