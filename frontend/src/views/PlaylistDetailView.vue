<template>
  <AppLayout>
    <RouterLink class="text-link back-link" :to="backLocation">목록으로 돌아가기</RouterLink>

    <PageStatePanel
      :status="status"
      :error="error"
      error-title="플레이리스트 상세를 불러오지 못했습니다."
      @retry="load"
    />

    <article v-if="status === 'ready'" class="detail-card playlist-detail">
      <div class="page-heading playlist-detail-heading">
        <div>
          <p class="playlist-detail-meta-line">
            {{ playlistCategoryLabel }} | {{ playlist.visibility || "PRIVATE" }}
          </p>
          <h1>{{ playlist.title }}</h1>
        </div>
        <div v-if="canManage" class="form-actions">
          <RouterLink
            class="secondary-button"
            :to="{ name: 'playlist-edit', params: { playlistId }, query: { returnTo: route.fullPath } }"
          >
            수정
          </RouterLink>
          <button type="button" class="danger-button" :disabled="actionBusy" @click="deletePlaylist">
            삭제
          </button>
        </div>
      </div>

      <p class="lead">{{ playlist.description || "설명이 없습니다." }}</p>

      <SaveStatus :type="actionType" :message="actionMessage" />

      <section v-if="showReferenceSavePanel" class="save-reference-panel save-reference-panel-compact">
        <button
          type="button"
          class="primary-button save-toggle-button"
          :class="{ active: playlist.saved }"
          :disabled="actionBusy"
          :aria-pressed="String(Boolean(playlist.saved))"
          @click="toggleSave"
        >
          {{ playlist.saved ? "내 목록에서 제거" : "내 목록에 저장" }}
        </button>
      </section>

      <section
        v-if="canManage"
        ref="spotifySearchRoot"
        class="playlist-spotify-inline-search"
        @focusout="closeSpotifyResultsSoon"
      >
        <form class="playlist-spotify-search-form" @submit.prevent="searchSpotifyTracks">
          <div class="playlist-search-control">
            <label class="sr-only" for="playlist-spotify-query">Spotify 곡 검색</label>
            <input
              id="playlist-spotify-query"
              v-model="spotifyQuery"
              type="search"
              autocomplete="off"
              placeholder="Spotify에서 추가할 곡 검색"
              @input="queueSpotifySearch"
              @focus="openSpotifyResults"
              @keydown.esc="spotifyResultsOpen = false"
            />

            <div v-if="shouldShowSpotifyDropdown" class="playlist-search-dropdown">
              <p v-if="spotifySearchMessage" class="playlist-search-state">{{ spotifySearchMessage }}</p>
              <button
                v-for="track in spotifyTracks"
                :key="track.spotifyTrackId"
                type="button"
                class="playlist-search-result playlist-search-result-button"
                :disabled="addingSpotifyTrackId === track.spotifyTrackId || actionBusy"
                @mousedown.prevent="addSpotifyTrack(track)"
                @click="addSpotifyTrack(track)"
              >
                <img v-if="track.imageUrl" :src="track.imageUrl" :alt="`${track.title} album cover`" />
                <span v-else class="playlist-result-placeholder">♪</span>
                <span class="playlist-result-copy">
                  <strong>{{ track.title }}</strong>
                  <span>{{ track.artist || "아티스트 정보 없음" }}</span>
                </span>
                <span class="playlist-result-duration">{{ formatDuration(track.durationMs) }}</span>
              </button>
            </div>
          </div>
          <button type="submit" :disabled="spotifySearching || !spotifyQuery.trim()">
            {{ spotifySearching ? "검색 중..." : "Spotify에서 추가" }}
          </button>
        </form>
      </section>

      <PlaylistItemList
        :items="items"
        :editable="canManage"
        :reviewable="canReviewItems"
        :busy="actionBusy"
        :current-member-id="currentMember?.id"
        @reorder="reorderItems"
        @remove="removeItem"
      />

      <div v-if="canExportToSpotify" class="playlist-spotify-export-row">
        <a
          v-if="playlist.spotifyPlaylistUrl"
          class="secondary-button spotify-export-capsule"
          :href="playlist.spotifyPlaylistUrl"
          target="_blank"
          rel="noreferrer"
        >
          Spotify에서 열기
        </a>
        <button
          v-else
          type="button"
          class="primary-button spotify-export-capsule"
          :disabled="spotifyExportBusy || spotifyExportBlocked || !items.length"
          @click="handleSpotifyExport"
        >
          {{ spotifyExportButtonLabel }}
        </button>
        <p v-if="spotifyExportHint" class="muted">{{ spotifyExportHint }}</p>
      </div>
    </article>
  </AppLayout>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { authApi } from "../api/authApi";
import { isApiError } from "../api/http";
import { playlistApi } from "../api/playlistApi";
import { playlistSaveApi } from "../api/playlistSaveApi";
import { spotifyApi } from "../api/spotifyApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import PlaylistItemList from "../components/PlaylistItemList.vue";
import SaveStatus from "../components/SaveStatus.vue";
import { rememberSpotifyReturnPath } from "../utils/oauthReturn";
import { playlistErrorMessage } from "../utils/playlistError";
import { requiresSpotifyReconnect, spotifyErrorMessage, spotifyErrorState } from "../utils/spotifyError";

const props = defineProps({ playlistId: { type: String, required: true } });
const route = useRoute();
const router = useRouter();
const detail = ref(null);
const status = ref("idle");
const error = ref(null);
const currentMember = ref(null);
const viewerStatus = ref("checking");
const saveStateKnown = ref(false);
const actionBusy = ref(false);
const actionMessage = ref("");
const actionType = ref("success");
const spotifyQuery = ref("");
const spotifyTracks = ref([]);
const spotifySearching = ref(false);
const spotifySearchStatus = ref("idle");
const spotifySearchError = ref(null);
const spotifyResultsOpen = ref(false);
const spotifySearchRoot = ref(null);
const addingSpotifyTrackId = ref(null);
const spotifyConnectionStatus = ref("checking");
const spotifyConnection = ref(null);
const spotifyConnectionChecking = ref(false);
const spotifyExporting = ref(false);
let requestSequence = 0;
let spotifySearchSequence = 0;
let spotifySearchTimer = null;

const playlist = computed(() => detail.value?.playlist || {});
const items = computed(() => Array.isArray(detail.value?.items) ? detail.value.items : []);
const isOfficial = computed(() => playlist.value.playlistType === "OFFICIAL");
const isOwner = computed(() => (
  currentMember.value != null
  && playlist.value.ownerId != null
  && String(currentMember.value.id) === String(playlist.value.ownerId)
));
const canManage = computed(() => isOwner.value && !isOfficial.value);
const canExportToSpotify = computed(() => (
  viewerStatus.value === "authenticated"
  && (
    canManage.value
    || isOfficial.value
    || playlist.value.visibility === "PUBLIC"
  )
));
const canAttemptSave = computed(() => (
  viewerStatus.value === "authenticated"
  && !isOwner.value
  && (
    isOfficial.value
    || playlist.value.visibility === "PUBLIC"
  )
));
const showReferenceSavePanel = computed(() => (
  saveStateKnown.value
  && canAttemptSave.value
));
const canReviewItems = computed(() => viewerStatus.value === "authenticated");
const playlistCategoryLabel = computed(() => (
  isOfficial.value ? "공식 플레이리스트" : "사용자 플레이리스트"
));
const nextOrder = computed(() => (
  items.value.reduce(
    (maximum, item) => Math.max(maximum, Number(item.orderNo) || 0),
    0,
  ) + 1
));
const backLocation = computed(() => {
  const candidate = route.query.returnTo;
  return typeof candidate === "string" && candidate.startsWith("/playlists")
    ? candidate
    : "/playlists";
});
const spotifyExportBusy = computed(() => spotifyConnectionChecking.value || spotifyExporting.value);
const spotifyExportBlocked = computed(() => (
  playlist.value.spotifyExportStatus === "PENDING"
  || playlist.value.spotifyPlaylistId != null
  || (
    playlist.value.spotifyPlaylistUrl != null
    && playlist.value.spotifyExportStatus !== "SUCCESS"
  )
));
const spotifyExportButtonLabel = computed(() => {
  if (spotifyConnectionChecking.value) return "Spotify 확인 중...";
  if (spotifyExporting.value) return "내보내는 중...";
  if (spotifyConnectionStatus.value === "connected") return "Spotify로 내보내기";
  return "Spotify 연결";
});
const spotifyExportHint = computed(() => {
  if (!items.value.length) return "수록곡을 추가한 뒤 Spotify로 내보낼 수 있습니다.";
  if (spotifyExportBlocked.value) return "이미 Spotify 내보내기 기록이 있는 플레이리스트입니다.";
  if (spotifyConnectionStatus.value === "expired") return "Spotify 인증이 만료되었습니다. 다시 연결해 주세요.";
  if (spotifyConnectionStatus.value === "disconnected") return "Spotify 계정 연결 후 내보낼 수 있습니다.";
  return "";
});
const spotifySearchMessage = computed(() => {
  if (spotifySearchStatus.value === "idle") return "";
  if (spotifySearchStatus.value === "loading") return "Spotify에서 검색 중입니다.";
  if (spotifySearchStatus.value === "empty") return "검색 결과가 없습니다.";
  if (spotifySearchStatus.value === "loginRequired") return "로그인 후 Spotify 검색을 사용할 수 있습니다.";
  if (spotifySearchStatus.value === "unauthorized") return "Spotify 검색 권한이 없습니다.";
  if (spotifySearchStatus.value === "error") return spotifyErrorMessage(spotifySearchError.value);
  return "";
});
const shouldShowSpotifyDropdown = computed(() => (
  spotifyResultsOpen.value
  && (spotifyTracks.value.length > 0 || Boolean(spotifySearchMessage.value))
));

async function resolveViewerAndSaveState(playlistIdValue) {
  saveStateKnown.value = false;
  try {
    currentMember.value = await authApi.getSession();
    viewerStatus.value = "authenticated";
  } catch (caught) {
    currentMember.value = null;
    viewerStatus.value = isApiError(caught) && caught.loginRequired
      ? "anonymous"
      : "error";
    return false;
  }

  try {
    const savedPlaylists = await playlistSaveApi.getMine();
    const saved = Array.isArray(savedPlaylists)
      && savedPlaylists.some((value) => String(value.id) === String(playlistIdValue));
    if (detail.value?.playlist) detail.value.playlist.saved = saved || isOwner.value;
    saveStateKnown.value = true;
  } catch {
    if (detail.value?.playlist && isOwner.value) {
      detail.value.playlist.saved = true;
      saveStateKnown.value = true;
    } else {
      saveStateKnown.value = false;
    }
  }
  return true;
}

async function load() {
  const sequence = ++requestSequence;
  detail.value = null;
  status.value = "loading";
  error.value = null;
  actionMessage.value = "";
  viewerStatus.value = "checking";

  try {
    const response = await playlistApi.getDetail(props.playlistId);
    if (sequence !== requestSequence) return;
    detail.value = {
      ...response,
      playlist: { ...response.playlist },
      items: Array.isArray(response?.items) ? response.items : [],
    };
    await resolveViewerAndSaveState(response.playlist.id);
    if (sequence !== requestSequence) return;
    status.value = "ready";
    if (canExportToSpotify.value) checkSpotifyConnection();
  } catch (caught) {
    if (sequence !== requestSequence) return;
    error.value = caught;
    if (isApiError(caught) && caught.loginRequired) {
      status.value = "loginRequired";
    } else if (isApiError(caught) && caught.unauthorized) {
      status.value = "unauthorized";
    } else {
      status.value = "error";
    }
  }
}

async function refreshAfterAction(message, callbacks) {
  await load();
  actionType.value = "success";
  actionMessage.value = message;
  callbacks?.success?.();
}

async function runAction(action, successMessage, callbacks) {
  actionBusy.value = true;
  actionMessage.value = "";
  try {
    await action();
    await refreshAfterAction(successMessage, callbacks);
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = playlistErrorMessage(caught);
    callbacks?.fail?.(caught);
    if (isApiError(caught) && caught.loginRequired) {
      authApi.clearSessionCache();
      currentMember.value = null;
      viewerStatus.value = "anonymous";
    }
  } finally {
    actionBusy.value = false;
  }
}

function toggleSave() {
  if (!canAttemptSave.value) return;
  const saved = Boolean(playlist.value.saved);
  runAction(
    () => (saved
      ? playlistSaveApi.cancel(props.playlistId)
      : playlistSaveApi.save(props.playlistId)),
    saved
      ? "내 목록에서 제거했습니다."
      : "내 목록에 저장했습니다.",
  );
}

function reorderItems(payload, callbacks) {
  runAction(
    () => playlistApi.reorderItems(props.playlistId, payload),
    "수록곡 순서를 변경했습니다.",
    callbacks,
  );
}

function removeItem(item, callbacks) {
  runAction(
    () => playlistApi.removeItem(item.id),
    "수록곡을 삭제했습니다.",
    callbacks,
  );
}

async function deletePlaylist() {
  if (!window.confirm(`"${playlist.value.title}" 플레이리스트를 삭제하시겠습니까?`)) return;
  actionBusy.value = true;
  actionMessage.value = "";
  try {
    await playlistApi.remove(props.playlistId);
    await router.push(backLocation.value);
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = playlistErrorMessage(caught);
  } finally {
    actionBusy.value = false;
  }
}

function queueSpotifySearch() {
  spotifyResultsOpen.value = true;
  window.clearTimeout(spotifySearchTimer);
  spotifySearchTimer = window.setTimeout(() => {
    searchSpotifyTracks();
  }, 320);
}

function openSpotifyResults() {
  if (spotifyTracks.value.length || spotifySearchMessage.value) {
    spotifyResultsOpen.value = true;
  }
}

function closeSpotifyResultsSoon() {
  window.setTimeout(() => {
    if (!spotifySearchRoot.value?.contains(document.activeElement)) {
      spotifyResultsOpen.value = false;
    }
  }, 120);
}

async function searchSpotifyTracks() {
  const normalizedQuery = spotifyQuery.value.trim();
  if (!normalizedQuery) {
    spotifyTracks.value = [];
    spotifySearchStatus.value = "idle";
    spotifySearchError.value = null;
    spotifyResultsOpen.value = false;
    return;
  }
  const sequence = ++spotifySearchSequence;
  spotifySearching.value = true;
  spotifyResultsOpen.value = true;
  spotifySearchStatus.value = "loading";
  spotifySearchError.value = null;
  try {
    const response = await spotifyApi.searchTracks({
      query: normalizedQuery,
      limit: 10,
    });
    if (sequence !== spotifySearchSequence) return;
    spotifyTracks.value = Array.isArray(response) ? response : [];
    spotifySearchStatus.value = spotifyTracks.value.length ? "ready" : "empty";
  } catch (caught) {
    if (sequence !== spotifySearchSequence) return;
    spotifyTracks.value = [];
    spotifySearchError.value = caught;
    spotifySearchStatus.value = spotifyErrorState(caught);
  } finally {
    if (sequence === spotifySearchSequence) spotifySearching.value = false;
  }
}

async function addSpotifyTrack(track) {
  if (addingSpotifyTrackId.value != null || actionBusy.value) return;
  addingSpotifyTrackId.value = track.spotifyTrackId;
  actionBusy.value = true;
  actionMessage.value = "";
  try {
    const music = await spotifyApi.importTrack(track);
    if (!music?.id) {
      throw new Error("내부 음악 ID를 확인하지 못했습니다.");
    }
    await playlistApi.addItem(props.playlistId, {
      musicId: music.id,
      orderNo: nextOrder.value,
    });
    spotifyQuery.value = "";
    spotifyTracks.value = [];
    spotifyResultsOpen.value = false;
    await refreshAfterAction("Spotify 곡을 플레이리스트에 추가했습니다.");
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = spotifyErrorMessage(caught);
  } finally {
    addingSpotifyTrackId.value = null;
    actionBusy.value = false;
  }
}

async function checkSpotifyConnection() {
  if (spotifyConnectionChecking.value) return;
  spotifyConnectionChecking.value = true;
  spotifyConnectionStatus.value = "checking";
  try {
    const response = await spotifyApi.getConnection();
    spotifyConnection.value = response;
    spotifyConnectionStatus.value = response?.state === "RECONNECT_REQUIRED"
      ? "expired"
      : (response?.connected ? "connected" : "disconnected");
  } catch (caught) {
    spotifyConnection.value = null;
    spotifyConnectionStatus.value = requiresSpotifyReconnect(caught) ? "expired" : "unknown";
  } finally {
    spotifyConnectionChecking.value = false;
  }
}

async function handleSpotifyExport() {
  if (spotifyExportBusy.value || spotifyExportBlocked.value || !items.value.length) return;
  if (spotifyConnectionStatus.value !== "connected") {
    rememberSpotifyReturnPath(route.fullPath);
    try {
      await spotifyApi.startAuthorization();
    } catch (caught) {
      actionType.value = "error";
      actionMessage.value = spotifyErrorMessage(caught);
    }
    return;
  }

  spotifyExporting.value = true;
  actionMessage.value = "";
  try {
    const response = await spotifyApi.exportPlaylist(props.playlistId);
    if (detail.value?.playlist) {
      Object.assign(detail.value.playlist, response);
    }
    actionType.value = response.spotifyExportStatus === "PARTIAL" ? "error" : "success";
    actionMessage.value = response.spotifyExportStatus === "PARTIAL"
      ? `일부 곡만 내보냈습니다. 성공 ${response.exportedCount}곡, 실패 chunk ${response.failedChunkIndexes.join(", ")}`
      : `Spotify 플레이리스트로 ${response.exportedCount}곡을 내보냈습니다.`;
  } catch (caught) {
    actionType.value = "error";
    actionMessage.value = spotifyErrorMessage(caught);
    if (requiresSpotifyReconnect(caught)) {
      spotifyConnectionStatus.value = "expired";
    }
    if ([
      "SPOTIFY_PLAYLIST_CREATE_FAILED",
      "SPOTIFY_PLAYLIST_ITEM_ADD_FAILED",
      "SPOTIFY_EXPORT_FAILED",
      "SPOTIFY_ALREADY_EXPORTED",
    ].includes(caught?.errorCode)) {
      await load();
    }
  } finally {
    spotifyExporting.value = false;
  }
}

function formatDuration(durationMs) {
  const value = Number(durationMs);
  if (!Number.isFinite(value) || value <= 0) return "";
  const totalSeconds = Math.round(value / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

watch(() => props.playlistId, load, { immediate: true });
</script>
