<template>
  <section class="playlist-items-section">
    <div class="section-heading">
      <div>
        <span class="eyebrow">TRACKS</span>
        <h2>수록곡 {{ localItems.length }}곡</h2>
      </div>
    </div>

    <p v-if="message" class="save-status" :class="messageType === 'error' ? 'save-error' : 'save-success'">
      {{ message }}
    </p>

    <p v-if="!localItems.length" class="empty-inline playlist-items-empty">
      아직 수록곡이 없습니다.
    </p>
    <div v-else class="playlist-track-list">
      <article
        v-for="item in localItems"
        :key="item.id"
        class="playlist-item-row"
        :class="{ dragging: draggedItemId === item.id }"
        :draggable="editable"
        @dragstart="startDrag(item, $event)"
        @dragover.prevent="moveDraggedItem(item)"
        @dragend="finishDrag"
        @drop.prevent="finishDrag"
      >
        <button
          v-if="editable"
          type="button"
          class="drag-handle"
          draggable="true"
          title="드래그해서 순서 변경"
          @dragstart="startDrag(item, $event)"
        >
          <span></span>
          <span></span>
        </button>
        <span v-else class="playlist-item-position">{{ item.orderNo }}</span>

        <div class="playlist-item-copy">
          <strong>{{ item.music?.title || `음악 #${item.musicId}` }}</strong>
          <small>
            {{ item.music?.artist || "아티스트 정보 없음" }}
            <template v-if="item.music?.musicBpm"> · {{ item.music.musicBpm }} BPM</template>
          </small>
        </div>

        <div v-if="editable" class="playlist-item-actions">
          <button
            type="button"
            class="playlist-icon-button heart-button"
            :class="{ active: isLiked(item) }"
            :disabled="isReviewBusy(item)"
            :title="isLiked(item) ? '좋아요 취소' : '좋아요'"
            @click="toggleLike(item)"
          >
            {{ isLiked(item) ? "♥" : "♡" }}
          </button>

          <div class="fit-score-control" title="러닝 적합도">
            <span>FIT</span>
            <button
              v-for="score in scoreOptions"
              :key="score"
              type="button"
              class="fit-score-button"
              :class="{ active: runningFitScore(item) === score }"
              :disabled="isReviewBusy(item)"
              :aria-label="`러닝 적합도 ${score}점`"
              @click="setRunningFitScore(item, score)"
            >
              {{ score }}
            </button>
          </div>

          <button
            type="button"
            class="playlist-icon-button delete-track-button"
            :disabled="busy"
            title="삭제"
            @click="removeItem(item)"
          >
            ×
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { reviewApi } from "../api/reviewApi";
import { playlistErrorMessage } from "../utils/playlistError";

const props = defineProps({
  items: { type: Array, default: () => [] },
  editable: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
  currentMemberId: { type: [Number, String], default: null },
});

const emit = defineEmits(["reorder", "remove"]);
const localItems = ref([]);
const reviewByMusicId = reactive({});
const reviewBusyMusicIds = reactive({});
const message = ref("");
const messageType = ref("error");
const draggedItemId = ref(null);
const dragChanged = ref(false);
const scoreOptions = Object.freeze([1, 2, 3, 4, 5]);

const orderedItems = computed(() => [...props.items].sort(
  (left, right) => Number(left.orderNo) - Number(right.orderNo),
));

watch(
  orderedItems,
  (items) => {
    localItems.value = items.map((item, index) => ({ ...item, orderNo: index + 1 }));
  },
  { immediate: true },
);

watch(
  () => [props.currentMemberId, props.items.map((item) => item.musicId).join(",")],
  loadOwnReviews,
  { immediate: true },
);

function startDrag(item, event) {
  if (!props.editable || props.busy) return;
  draggedItemId.value = item.id;
  dragChanged.value = false;
  event.dataTransfer.effectAllowed = "move";
  event.dataTransfer.setData("text/plain", String(item.id));
}

function moveDraggedItem(targetItem) {
  if (draggedItemId.value == null || draggedItemId.value === targetItem.id) return;
  const items = [...localItems.value];
  const fromIndex = items.findIndex((item) => item.id === draggedItemId.value);
  const toIndex = items.findIndex((item) => item.id === targetItem.id);
  if (fromIndex < 0 || toIndex < 0) return;
  const [moved] = items.splice(fromIndex, 1);
  items.splice(toIndex, 0, moved);
  localItems.value = items.map((item, index) => ({ ...item, orderNo: index + 1 }));
  dragChanged.value = true;
}

function finishDrag() {
  if (draggedItemId.value == null) return;
  const changed = dragChanged.value;
  draggedItemId.value = null;
  dragChanged.value = false;
  if (!changed) return;

  emit("reorder", {
    items: localItems.value.map((item, index) => ({
      itemId: item.id,
      orderNo: index + 1,
    })),
  }, {
    success() {
      messageType.value = "success";
      message.value = "수록곡 순서를 변경했습니다.";
    },
    fail(error) {
      localItems.value = orderedItems.value.map((item, index) => ({ ...item, orderNo: index + 1 }));
      messageType.value = "error";
      message.value = playlistErrorMessage(error);
    },
  });
}

async function loadOwnReviews() {
  if (!props.currentMemberId || !props.items.length) {
    Object.keys(reviewByMusicId).forEach((musicId) => {
      delete reviewByMusicId[musicId];
    });
    return;
  }
  const musicIds = [...new Set(props.items.map((item) => item.musicId).filter(Boolean))];
  await Promise.allSettled(musicIds.map(async (musicId) => {
    const reviews = await reviewApi.getByMusic(musicId);
    reviewByMusicId[musicId] = Array.isArray(reviews)
      ? reviews.find((review) => String(review.memberId) === String(props.currentMemberId)) || null
      : null;
  }));
}

function ownReview(item) {
  return reviewByMusicId[item.musicId] || null;
}

function isLiked(item) {
  return Number(ownReview(item)?.preferenceScore) >= 4;
}

function runningFitScore(item) {
  return Number(ownReview(item)?.runningFitScore) || null;
}

function isReviewBusy(item) {
  return Boolean(reviewBusyMusicIds[item.musicId]);
}

async function toggleLike(item) {
  await saveReview(item, {
    preferenceScore: isLiked(item) ? 1 : 5,
  });
}

async function setRunningFitScore(item, score) {
  await saveReview(item, { runningFitScore: score });
}

async function saveReview(item, patch) {
  if (!props.currentMemberId || !item.musicId || isReviewBusy(item)) return;
  reviewBusyMusicIds[item.musicId] = true;
  message.value = "";
  const existing = ownReview(item);
  const payload = {
    preferenceScore: patch.preferenceScore ?? existing?.preferenceScore ?? 3,
    runningFitScore: patch.runningFitScore ?? existing?.runningFitScore ?? 3,
    reviewContent: existing?.reviewContent || null,
  };
  try {
    reviewByMusicId[item.musicId] = existing?.id
      ? await reviewApi.update(existing.id, payload)
      : await reviewApi.create(item.musicId, payload);
    messageType.value = "success";
    message.value = "곡 평가를 저장했습니다.";
  } catch (caught) {
    messageType.value = "error";
    message.value = playlistErrorMessage(caught);
  } finally {
    reviewBusyMusicIds[item.musicId] = false;
  }
}

function removeItem(item) {
  const title = item.music?.title || `음악 #${item.musicId}`;
  if (!window.confirm(`"${title}"을(를) 플레이리스트에서 삭제하시겠습니까?`)) return;
  emit("remove", item, {
    success() {
      messageType.value = "success";
      message.value = "수록곡을 삭제했습니다.";
    },
    fail(error) {
      messageType.value = "error";
      message.value = playlistErrorMessage(error);
    },
  });
}
</script>
