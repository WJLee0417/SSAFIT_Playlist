<template>
  <div v-if="compact" class="spotify-export-compact" data-spotify-export>
    <button
      v-if="connectionStatus === 'disconnected' || connectionStatus === 'expired'"
      type="button"
      class="secondary-button"
      :disabled="busy"
      @click="connect"
    >
      Spotify 연결
    </button>
    <button
      v-else-if="connectionStatus === 'connected' && !alreadyExported"
      type="button"
      class="secondary-button"
      :disabled="busy || exportBlocked"
      @click="exportPlaylist"
    >
      {{ exporting ? "내보내는 중..." : "Spotify로 내보내기" }}
    </button>
    <button
      v-else-if="connectionStatus === 'unknown' || connectionStatus === 'checking'"
      type="button"
      class="secondary-button"
      :disabled="busy"
      @click="checkConnection"
    >
      Spotify 연결
    </button>
    <a
      v-else-if="exportState.spotifyPlaylistUrl"
      class="secondary-button"
      :href="exportState.spotifyPlaylistUrl"
      target="_blank"
      rel="noopener noreferrer"
    >
      Spotify에서 열기
    </a>
  </div>
  <section v-else class="spotify-extension-panel spotify-export-panel" data-spotify-export>
    <div>
      <span class="eyebrow">SPOTIFY EXPORT</span>
      <h2>Spotify 연결 및 내보내기</h2>
      <p>{{ connectionMessage }}</p>

      <dl class="playlist-meta">
        <div><dt>연결 상태</dt><dd>{{ connectionLabel }}</dd></div>
        <div><dt>내보내기 상태</dt><dd>{{ exportState.spotifyExportStatus || "NOT_EXPORTED" }}</dd></div>
        <div><dt>요청 곡 수</dt><dd>{{ exportState.requestedCount }}</dd></div>
        <div><dt>내보내기 가능</dt><dd>{{ exportableCount }}</dd></div>
        <div><dt>내보내기 성공</dt><dd>{{ exportState.exportedCount }}</dd></div>
        <div><dt>건너뜀</dt><dd>{{ exportState.skippedCount }}</dd></div>
        <div v-if="exportState.spotifyExportErrorCode">
          <dt>내보내기 오류</dt><dd>{{ exportState.spotifyExportErrorCode }}</dd>
        </div>
        <div v-if="exportState.failedChunkIndexes.length">
          <dt>실패 chunk</dt><dd>{{ exportState.failedChunkIndexes.join(", ") }}</dd>
        </div>
      </dl>
      <p v-if="exportState.skippedMusicIds.length" class="muted">
        건너뛴 내부 음악 ID: {{ exportState.skippedMusicIds.join(", ") }}
      </p>

      <SaveStatus :type="messageType" :message="message" />

      <a
        v-if="exportState.spotifyPlaylistUrl"
        class="secondary-button inline-button"
        :href="exportState.spotifyPlaylistUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        Spotify에서 열기
      </a>
    </div>

    <div v-if="editable" class="spotify-export-actions">
      <button
        v-if="connectionStatus === 'disconnected' || connectionStatus === 'expired'"
        type="button"
        :disabled="busy"
        @click="connect"
      >
        Spotify 연결
      </button>
      <button
        v-else-if="connectionStatus === 'connected' && !alreadyExported"
        type="button"
        :disabled="busy || exportBlocked"
        @click="exportPlaylist"
      >
        {{ exporting ? "Spotify로 내보내는 중..." : "Spotify로 내보내기" }}
      </button>
      <button
        v-else-if="connectionStatus === 'unknown'"
        type="button"
        class="secondary-button"
        :disabled="busy"
        @click="checkConnection"
      >
        연결 상태 다시 확인
      </button>
      <p v-if="alreadyExported" class="muted">이미 내보낸 플레이리스트입니다. 기존 링크를 사용해 주세요.</p>
      <p v-else-if="exportBlocked" class="muted">
        이전 내보내기 상태 또는 Spotify URL이 남아 있어 자동 재시도를 막았습니다.
      </p>
    </div>
    <p v-else class="muted">소유한 사용자 플레이리스트만 Spotify로 내보낼 수 있습니다.</p>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { spotifyApi } from "../api/spotifyApi";
import { rememberSpotifyReturnPath } from "../utils/oauthReturn";
import {
  requiresSpotifyReconnect,
  spotifyErrorMessage,
} from "../utils/spotifyError";
import SaveStatus from "./SaveStatus.vue";

const props = defineProps({
  playlist: { type: Object, required: true },
  playlistId: { type: [String, Number], required: true },
  editable: { type: Boolean, default: false },
  returnTo: { type: String, required: true },
  items: { type: Array, default: () => [] },
  compact: { type: Boolean, default: false },
});

const emit = defineEmits(["exported"]);
const connectionStatus = ref("checking");
const connection = ref(null);
const checking = ref(false);
const exporting = ref(false);
const message = ref("");
const messageType = ref("success");
const exportState = reactive({
  spotifyPlaylistId: props.playlist.spotifyPlaylistId || null,
  spotifyPlaylistUrl: props.playlist.spotifyPlaylistUrl || null,
  spotifyExportStatus: props.playlist.spotifyExportStatus || "NOT_EXPORTED",
  spotifyExportErrorCode: props.playlist.spotifyExportErrorCode || null,
  spotifyExportedAt: props.playlist.spotifyExportedAt || null,
  requestedCount: props.items.length,
  exportedCount: 0,
  skippedCount: props.items.filter((item) => !isExportableItem(item)).length,
  skippedMusicIds: props.items
    .filter((item) => !isExportableItem(item))
    .map((item) => item.musicId),
  failedChunkIndexes: [],
});

const busy = computed(() => checking.value || exporting.value);
const exportableCount = computed(() => (
  exportState.requestedCount - exportState.skippedCount
));
const alreadyExported = computed(() => (
  exportState.spotifyExportStatus === "SUCCESS"
  || exportState.spotifyExportStatus === "PARTIAL"
));
const exportBlocked = computed(() => (
  exportState.spotifyExportStatus === "PENDING"
  || exportState.spotifyPlaylistId != null
  || (
    exportState.spotifyPlaylistUrl != null
    && exportState.spotifyExportStatus !== "SUCCESS"
  )
));
const connectionLabel = computed(() => ({
  checking: "확인 중",
  connected: "연결됨",
  disconnected: "연결 필요",
  expired: "재인증 필요",
  unknown: "확인할 수 없음",
})[connectionStatus.value] || "확인할 수 없음");
const connectionMessage = computed(() => ({
  checking: "Spotify 연결 상태를 확인하고 있습니다.",
  connected: connection.value?.spotifyUserId
    ? `${connection.value.spotifyUserId} 계정이 연결되어 있습니다.`
    : "Spotify 계정이 연결되어 있습니다.",
  disconnected: "Spotify 계정을 연결해야 플레이리스트를 내보낼 수 있습니다.",
  expired: "Spotify 연결이 만료되었습니다. 다시 인증해 주세요.",
  unknown: "Spotify 연결 상태를 확인할 수 없습니다. 연결된 것으로 가정하지 않습니다.",
})[connectionStatus.value]);

async function checkConnection() {
  if (checking.value) return;
  checking.value = true;
  message.value = "";
  connectionStatus.value = "checking";
  try {
    const response = await spotifyApi.getConnection();
    connection.value = response;
    connectionStatus.value = response?.state === "RECONNECT_REQUIRED"
      ? "expired"
      : (response?.connected ? "connected" : "disconnected");
  } catch (caught) {
    connection.value = null;
    connectionStatus.value = requiresSpotifyReconnect(caught) ? "expired" : "unknown";
    messageType.value = "error";
    message.value = spotifyErrorMessage(caught);
  } finally {
    checking.value = false;
  }
}

async function connect() {
  if (busy.value) return;
  redirectToSpotifyAuthorization();
}

async function redirectToSpotifyAuthorization() {
  message.value = "";
  rememberSpotifyReturnPath(props.returnTo);
  try {
    await spotifyApi.startAuthorization(props.returnTo);
  } catch (caught) {
    messageType.value = "error";
    message.value = spotifyErrorMessage(caught);
  }
}

async function exportPlaylist() {
  if (busy.value || connectionStatus.value !== "connected" || exportBlocked.value) return;
  exporting.value = true;
  message.value = "";
  try {
    const response = await spotifyApi.exportPlaylist(props.playlistId);
    Object.assign(exportState, response);
    messageType.value = response.spotifyExportStatus === "PARTIAL"
      ? "error"
      : "success";
    message.value = response.spotifyExportStatus === "PARTIAL"
      ? `일부 곡만 내보냈습니다. 성공 ${response.exportedCount}곡, 실패 chunk ${response.failedChunkIndexes.join(", ")}`
      : `Spotify 플레이리스트로 ${response.exportedCount}곡을 내보냈습니다.`;
    emit("exported", {
      response,
      type: messageType.value,
      message: message.value,
      refresh: false,
    });
  } catch (caught) {
    messageType.value = "error";
    message.value = spotifyErrorMessage(caught);
    if (requiresSpotifyReconnect(caught)) {
      connectionStatus.value = "expired";
      await redirectToSpotifyAuthorization();
      return;
    }
    emit("exported", {
      type: "error",
      message: message.value,
      refresh: [
        "SPOTIFY_PLAYLIST_CREATE_FAILED",
        "SPOTIFY_PLAYLIST_ITEM_ADD_FAILED",
        "SPOTIFY_EXPORT_FAILED",
        "SPOTIFY_ALREADY_EXPORTED",
      ].includes(caught?.errorCode),
    });
  } finally {
    exporting.value = false;
  }
}

function isExportableItem(item) {
  return Boolean(
    item?.music?.spotifyTrackId?.trim?.()
    && item?.music?.spotifyUri?.trim?.(),
  );
}

onMounted(() => {
  if (props.editable) checkConnection();
  else connectionStatus.value = "unknown";
});
</script>
