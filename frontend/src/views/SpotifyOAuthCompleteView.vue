<template>
  <AppLayout>
    <PageStatePanel
      v-if="status !== 'ready' && status !== 'disconnected'"
      :status="status"
      :error="displayError"
      error-title="Spotify 연결을 확인하지 못했습니다"
      @retry="checkConnection"
    />

    <section v-if="status === 'ready'" class="state-panel state-success">
      <h1>Spotify 연결을 확인했습니다</h1>
      <p v-if="connection.spotifyUserId">{{ connection.spotifyUserId }} 계정이 연결되어 있습니다.</p>
      <p v-else>Spotify 계정이 연결되어 있습니다.</p>
      <RouterLink class="primary-button" :to="returnTo">이전 화면으로 돌아가기</RouterLink>
    </section>

    <section v-else-if="status === 'disconnected'" class="state-panel state-error">
      <h1>Spotify 연결이 완료되지 않았습니다</h1>
      <p>
        현재 연결 상태 API에서 연결 정보를 확인하지 못했습니다.
        인증을 다시 시작하거나 이전 화면으로 돌아갈 수 있습니다.
      </p>
      <button type="button" :disabled="connecting" @click="connect">
        {{ connecting ? "Spotify로 이동 중..." : "Spotify 다시 연결" }}
      </button>
      <RouterLink class="secondary-button" :to="returnTo">이전 화면으로 돌아가기</RouterLink>
    </section>

    <p class="contract-note">
      현재 Backend OAuth Callback은 Vue로 Redirect하지 않고 JSON을 반환합니다.
      이 화면으로 자동 복귀하려면 Backend Redirect adapter 계약이 추가로 필요합니다.
    </p>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { spotifyApi } from "../api/spotifyApi";
import AppLayout from "../components/AppLayout.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import {
  consumeSpotifyReturnPath,
  isSafeSpotifyReturnPath,
  rememberSpotifyReturnPath,
} from "../utils/oauthReturn";
import { spotifyErrorMessage, spotifyErrorState } from "../utils/spotifyError";

const route = useRoute();
const router = useRouter();
const rememberedReturnTo = consumeSpotifyReturnPath();
const status = ref("loading");
const error = ref(null);
const connection = ref(null);
const connecting = ref(false);
const returnTo = computed(() => {
  const candidate = Array.isArray(route.query.returnTo)
    ? route.query.returnTo[0]
    : route.query.returnTo;
  if (isSafeSpotifyReturnPath(candidate)) return candidate;
  return rememberedReturnTo || "/playlists";
});
const displayError = computed(() => error.value ? {
  status: error.value.status,
  errorCode: error.value.errorCode,
  message: spotifyErrorMessage(error.value),
  notFound: error.value.notFound,
  conflict: error.value.conflict,
  upstreamUnavailable: error.value.upstreamUnavailable,
} : null);

async function checkConnection() {
  status.value = "loading";
  error.value = null;
  try {
    const response = await spotifyApi.getConnection();
    connection.value = response;
    status.value = response?.connected ? "ready" : "disconnected";
  } catch (caught) {
    error.value = caught;
    status.value = spotifyErrorState(caught);
  }
}

async function connect() {
  if (connecting.value) return;
  connecting.value = true;
  rememberSpotifyReturnPath(returnTo.value);
  try {
    await spotifyApi.startAuthorization();
  } catch (caught) {
    error.value = caught;
    status.value = spotifyErrorState(caught);
    connecting.value = false;
  }
}

onMounted(async () => {
  const candidate = Array.isArray(route.query.returnTo)
    ? route.query.returnTo[0]
    : route.query.returnTo;
  const safeReturnTo = isSafeSpotifyReturnPath(candidate) ? candidate : null;
  const hasSensitiveQuery = ["code", "state", "token", "access_token", "refresh_token"]
    .some((key) => Object.prototype.hasOwnProperty.call(route.query, key));
  if (hasSensitiveQuery || Object.keys(route.query).some((key) => key !== "returnTo")) {
    await router.replace({
      name: "spotify-oauth-complete",
      query: safeReturnTo ? { returnTo: safeReturnTo } : {},
    });
  }
  await checkConnection();
});
</script>
