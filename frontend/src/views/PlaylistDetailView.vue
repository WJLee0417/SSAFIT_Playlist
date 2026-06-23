<template>
  <AppLayout>
    <RouterLink class="text-link back-link" :to="backLocation">← 목록으로 돌아가기</RouterLink>

    <PageStatePanel
      :status="status"
      :error="error"
      error-title="플레이리스트 상세를 불러오지 못했습니다"
      @retry="load"
    />

    <article v-if="status === 'ready'" class="detail-card playlist-detail">
      <div class="page-heading">
        <div>
          <div class="badge-row">
            <span class="source-badge" :class="{ official: isOfficial }">
              {{ isOfficial ? "공식 플레이리스트" : sourceLabel }}
            </span>
            <span class="visibility-badge">{{ playlist.visibility }}</span>
            <span v-if="playlist.saved" class="saved-badge">저장됨</span>
          </div>
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
      <dl class="summary-list playlist-summary">
        <div><dt>유형</dt><dd>{{ playlist.playlistType }}</dd></div>
        <div><dt>생성 경로</dt><dd>{{ playlist.sourceType }}</dd></div>
        <div><dt>수록곡</dt><dd>{{ items.length }}곡</dd></div>
        <div><dt>Spotify 상태</dt><dd>{{ playlist.spotifyExportStatus || "NOT_EXPORTED" }}</dd></div>
      </dl>

      <SaveStatus :type="actionType" :message="actionMessage" />

      <section class="save-reference-panel">
        <div>
          <span class="eyebrow">REFERENCE SAVE</span>
          <h2>내 저장 목록</h2>
          <p>
            이 기능은 기존 플레이리스트를 참조 저장합니다.
            자동 큐레이션 복사본 생성이나 Spotify 내보내기와는 별도입니다.
          </p>
          <p v-if="isOwner && playlist.visibility === 'PRIVATE'" class="contract-note">
            현재 Service 계약은 소유자 본인의 비공개 플레이리스트 참조 저장도 허용합니다.
          </p>
        </div>
        <div>
          <a v-if="viewerStatus === 'anonymous'" class="primary-button" :href="loginUrl">로그인 후 저장</a>
          <p v-else-if="viewerStatus === 'error'" class="muted">로그인 상태를 확인하지 못해 저장 상태를 표시하지 않습니다.</p>
          <p v-else-if="!saveStateKnown" class="muted">내 저장 목록을 확인하지 못했습니다.</p>
          <button
            v-else-if="canAttemptSave"
            type="button"
            :class="playlist.saved ? 'secondary-button' : 'primary-button'"
            :disabled="actionBusy"
            @click="toggleSave"
          >
            {{ playlist.saved ? "저장 취소" : "내 목록에 저장" }}
          </button>
          <p v-else class="muted">현재 계약상 이 플레이리스트는 참조 저장할 수 없습니다.</p>
        </div>
      </section>

      <PlaylistItemList
        :items="items"
        :editable="canManage"
        :busy="actionBusy"
        @add="addItem"
        @change-order="changeItemOrder"
        @remove="removeItem"
      />

      <section v-if="canManage" class="spotify-track-add-panel">
        <div>
          <span class="eyebrow">SPOTIFY TRACKS</span>
          <h2>Spotify 곡 추가</h2>
          <p>검색 결과를 내부 음악으로 저장한 뒤 반환된 music ID로 수록곡에 추가합니다.</p>
        </div>
        <RouterLink
          class="primary-button"
          :to="{
            name: 'spotify-search',
            query: {
              playlistId,
              nextOrder,
              returnTo: route.fullPath,
            },
          }"
        >
          Spotify 곡 검색
        </RouterLink>
      </section>

      <SpotifyExportPanel
        :key="`${playlist.id}-${playlist.spotifyExportStatus}-${playlist.spotifyPlaylistUrl || ''}`"
        :playlist="playlist"
        :playlist-id="playlistId"
        :editable="canManage"
        :return-to="route.fullPath"
        :items="items"
        @exported="handleSpotifyExported"
      />
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
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import PlaylistItemList from "../components/PlaylistItemList.vue";
import SaveStatus from "../components/SaveStatus.vue";
import SpotifyExportPanel from "../components/SpotifyExportPanel.vue";
import { buildJspLoginUrl } from "../utils/navigation";
import { playlistErrorMessage } from "../utils/playlistError";

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
let requestSequence = 0;

const playlist = computed(() => detail.value?.playlist || {});
const items = computed(() => Array.isArray(detail.value?.items) ? detail.value.items : []);
const isOfficial = computed(() => playlist.value.playlistType === "OFFICIAL");
const isOwner = computed(() => (
  currentMember.value != null
  && playlist.value.ownerId != null
  && String(currentMember.value.id) === String(playlist.value.ownerId)
));
const canManage = computed(() => isOwner.value && !isOfficial.value);
const canAttemptSave = computed(() => (
  viewerStatus.value === "authenticated"
  && (
    isOfficial.value
    || playlist.value.visibility === "PUBLIC"
    || isOwner.value
  )
));
const sourceLabel = computed(() => (
  playlist.value.sourceType === "CURATION_COPY" ? "큐레이션 복사본" : "사용자 플레이리스트"
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
const loginUrl = computed(() => buildJspLoginUrl(window.location.href));

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
    if (detail.value?.playlist) detail.value.playlist.saved = saved;
    saveStateKnown.value = true;
  } catch {
    saveStateKnown.value = false;
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
  const action = playlist.value.saved
    ? () => playlistSaveApi.cancel(props.playlistId)
    : () => playlistSaveApi.save(props.playlistId);
  runAction(
    action,
    playlist.value.saved ? "저장을 취소했습니다." : "내 저장 목록에 추가했습니다.",
  );
}

function addItem(payload, callbacks) {
  runAction(
    () => playlistApi.addItem(props.playlistId, payload),
    "수록곡을 추가했습니다.",
    callbacks,
  );
}

function changeItemOrder(item, orderNo, callbacks) {
  runAction(
    () => playlistApi.updateItemOrder(item.id, { orderNo }),
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

async function handleSpotifyExported(result) {
  if (result?.refresh) {
    await load();
  }
  if (result?.message) {
    actionType.value = result.type || "success";
    actionMessage.value = result.message;
  }
}

watch(() => props.playlistId, load, { immediate: true });
</script>
