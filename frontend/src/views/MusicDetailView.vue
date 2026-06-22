<template>
  <AppLayout>
    <RouterLink class="text-link back-link" :to="backLocation">← 목록으로 돌아가기</RouterLink>

    <PageStatePanel
      :status="status"
      :error="error"
      error-title="음악 상세를 불러오지 못했습니다"
      @retry="loadDetail"
    />

    <article v-if="status === 'ready'" class="detail-card music-detail">
      <div class="music-detail-header">
        <img
          v-if="data.music.imageUrl"
          :src="data.music.imageUrl"
          :alt="`${data.music.title} 앨범 이미지`"
        />
        <div>
          <div class="badge-row">
            <span class="eyebrow">{{ data.music.genre || "장르 미정" }}</span>
            <span v-if="isSpotify" class="source-badge">Spotify 기반</span>
          </div>
          <h1>{{ data.music.title }}</h1>
          <p class="lead">{{ data.music.artist }} · {{ data.music.albumName || "앨범 정보 없음" }}</p>
          <a
            v-if="data.music.spotifyExternalUrl"
            class="secondary-button inline-button"
            :href="data.music.spotifyExternalUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            Spotify에서 열기
          </a>
        </div>
      </div>

      <dl class="summary-list">
        <div><dt>BPM</dt><dd>{{ data.music.bpm || "정보 없음" }}</dd></div>
        <div>
          <dt>평균 선호도</dt>
          <dd>{{ formatAverage(data.reviewAverage?.averagePreferenceScore) }}</dd>
        </div>
        <div>
          <dt>평균 러닝 적합도</dt>
          <dd>{{ formatAverage(data.reviewAverage?.averageRunningFitScore) }}</dd>
        </div>
        <div><dt>리뷰</dt><dd>{{ data.reviewAverage?.reviewCount || 0 }}개</dd></div>
      </dl>

      <SaveStatus :type="actionType" :message="actionMessage" />

      <section class="review-section">
        <div class="section-heading">
          <div><span class="eyebrow">REVIEWS</span><h2>음악 평가</h2></div>
        </div>

        <section v-if="viewerStatus === 'anonymous'" class="review-callout">
          <p>리뷰를 작성하려면 로그인이 필요합니다.</p>
          <a class="primary-button" :href="loginUrl">JSP 로그인으로 이동</a>
        </section>
        <section v-else-if="viewerStatus === 'error'" class="review-callout">
          <p>로그인 상태를 확인하지 못해 리뷰 작성과 수정 기능을 표시하지 않습니다.</p>
        </section>
        <MusicReviewForm
          v-else-if="viewerStatus === 'authenticated' && !myReview"
          :busy="actionBusy"
          :error-message="formError"
          @submit="createReview"
        />

        <p v-if="!data.reviews.length" class="empty-inline">
          아직 작성된 리뷰가 없습니다. 첫 리뷰를 남겨 보세요.
        </p>

        <div v-else class="review-list">
          <article v-for="review in data.reviews" :key="review.id" class="review-card">
            <MusicReviewForm
              v-if="editingReviewId === review.id"
              mode="edit"
              :review="review"
              :busy="actionBusy"
              :error-message="formError"
              @submit="(payload) => updateReview(review.id, payload)"
              @cancel="cancelEdit"
            />
            <template v-else>
              <div class="review-card-heading">
                <div>
                  <strong>선호 {{ review.preferenceScore }} · 러닝 적합 {{ review.runningFitScore }}</strong>
                  <small>작성자 회원 #{{ review.memberId }}</small>
                </div>
                <div v-if="isMine(review)" class="review-actions">
                  <button type="button" class="secondary-button" @click="startEdit(review)">수정</button>
                  <button type="button" class="danger-button" :disabled="actionBusy" @click="deleteReview(review.id)">
                    삭제
                  </button>
                </div>
              </div>
              <p>{{ review.reviewContent || "내용 없음" }}</p>
            </template>
          </article>
        </div>
      </section>
    </article>
  </AppLayout>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { authApi } from "../api/authApi";
import { isApiError } from "../api/http";
import { musicApi } from "../api/musicApi";
import { reviewApi } from "../api/reviewApi";
import AppLayout from "../components/AppLayout.vue";
import MusicReviewForm from "../components/MusicReviewForm.vue";
import PageStatePanel from "../components/PageStatePanel.vue";
import SaveStatus from "../components/SaveStatus.vue";
import { buildJspLoginUrl } from "../utils/navigation";
import { reviewErrorMessage } from "../utils/reviewError";

const props = defineProps({ musicId: { type: String, required: true } });
const route = useRoute();
const data = ref(null);
const status = ref("idle");
const error = ref(null);
const currentMember = ref(null);
const viewerStatus = ref("checking");
const editingReviewId = ref(null);
const actionBusy = ref(false);
const formError = ref("");
const actionMessage = ref("");
const actionType = ref("success");
let loadSequence = 0;

const isSpotify = computed(() => data.value?.music?.sourceType === "SPOTIFY");
const myReview = computed(() => data.value?.reviews?.find(isMine) || null);
const backLocation = computed(() => {
  const candidate = route.query.returnTo;
  return typeof candidate === "string" && candidate.startsWith("/music")
    ? candidate
    : "/music";
});
const loginUrl = computed(() => buildJspLoginUrl(window.location.href));

function isMine(review) {
  return currentMember.value != null
    && String(currentMember.value.id) === String(review.memberId);
}

function formatAverage(value) {
  return value !== null && value !== undefined && Number.isFinite(Number(value))
    ? Number(value).toFixed(1)
    : "평가 없음";
}

async function resolveViewer() {
  viewerStatus.value = "checking";
  try {
    currentMember.value = await authApi.getSession();
    viewerStatus.value = "authenticated";
  } catch (caught) {
    currentMember.value = null;
    if (isApiError(caught) && caught.loginRequired) {
      viewerStatus.value = "anonymous";
    } else {
      viewerStatus.value = "error";
    }
  }
}

async function fetchDetail({ reset = false } = {}) {
  const sequence = ++loadSequence;
  if (reset) {
    data.value = null;
    status.value = "loading";
    error.value = null;
    editingReviewId.value = null;
    formError.value = "";
    actionMessage.value = "";
  }
  try {
    const detail = await musicApi.getDetail(props.musicId);
    if (sequence !== loadSequence) return false;
    data.value = {
      ...detail,
      reviews: Array.isArray(detail?.reviews) ? detail.reviews : [],
    };
    status.value = "ready";
    return true;
  } catch (caught) {
    if (sequence !== loadSequence) return false;
    error.value = caught;
    status.value = isApiError(caught) && caught.loginRequired
      ? "loginRequired"
      : (isApiError(caught) && caught.unauthorized ? "unauthorized" : "error");
    return false;
  }
}

async function loadDetail() {
  await Promise.all([fetchDetail({ reset: true }), resolveViewer()]);
}

async function runReviewAction(action, successMessage, { globalError = false } = {}) {
  actionBusy.value = true;
  formError.value = "";
  actionMessage.value = "";
  try {
    await action();
    await fetchDetail();
    editingReviewId.value = null;
    actionType.value = "success";
    actionMessage.value = successMessage;
  } catch (caught) {
    if (isApiError(caught) && caught.loginRequired) {
      authApi.clearSessionCache();
      currentMember.value = null;
      viewerStatus.value = "anonymous";
    }
    actionType.value = "error";
    formError.value = reviewErrorMessage(caught);
    actionMessage.value = globalError ? formError.value : "";
  } finally {
    actionBusy.value = false;
  }
}

function createReview(payload) {
  return runReviewAction(
    () => reviewApi.create(props.musicId, payload),
    "리뷰를 등록했습니다.",
  );
}

function updateReview(reviewId, payload) {
  return runReviewAction(
    () => reviewApi.update(reviewId, payload),
    "리뷰를 수정했습니다.",
  );
}

function startEdit(review) {
  formError.value = "";
  actionMessage.value = "";
  editingReviewId.value = review.id;
}

function cancelEdit() {
  formError.value = "";
  editingReviewId.value = null;
}

function deleteReview(reviewId) {
  if (!window.confirm("이 리뷰를 삭제하시겠습니까?")) return;
  runReviewAction(
    () => reviewApi.remove(reviewId),
    "리뷰를 삭제했습니다.",
    { globalError: true },
  );
}

watch(() => props.musicId, loadDetail, { immediate: true });
</script>
