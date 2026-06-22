<template>
  <section class="playlist-items-section">
    <div class="section-heading">
      <div>
        <span class="eyebrow">TRACKS</span>
        <h2>수록곡 {{ items.length }}곡</h2>
      </div>
    </div>

    <form v-if="editable" class="item-add-form" @submit.prevent="addItem">
      <label>
        음악 ID
        <input v-model="addForm.musicId" inputmode="numeric" placeholder="저장된 음악 ID" />
      </label>
      <label>
        순서
        <input v-model="addForm.orderNo" inputmode="numeric" placeholder="1 이상의 빈 순서" />
      </label>
      <button type="submit" :disabled="busy">수록곡 추가</button>
    </form>

    <p v-if="message" class="save-status" :class="messageType === 'error' ? 'save-error' : 'save-success'">
      {{ message }}
    </p>

    <p v-if="!items.length" class="empty-inline">아직 수록곡이 없습니다.</p>
    <div v-else class="stack-list">
      <article v-for="item in orderedItems" :key="item.id" class="playlist-item-row">
        <span class="order-chip">{{ item.orderNo }}</span>
        <div class="playlist-item-copy">
          <strong>{{ item.music?.title || `음악 #${item.musicId}` }}</strong>
          <small>{{ item.music?.artist || "아티스트 정보 없음" }}</small>
        </div>
        <div v-if="editable" class="item-controls">
          <label>
            순서 번호
            <input
              :value="orderDrafts[item.id]"
              inputmode="numeric"
              @input="orderDrafts[item.id] = $event.target.value"
            />
          </label>
          <button type="button" class="secondary-button" :disabled="busy" @click="changeOrder(item)">
            순서 변경
          </button>
          <button type="button" class="danger-button" :disabled="busy" @click="removeItem(item)">
            삭제
          </button>
        </div>
      </article>
    </div>

    <p v-if="editable" class="contract-note">
      현재 API는 한 곡의 순서 번호만 변경합니다. 이미 사용 중인 번호와 직접 교환하는 배치 재정렬은 지원하지 않습니다.
    </p>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { playlistErrorMessage } from "../utils/playlistError";

const props = defineProps({
  items: { type: Array, default: () => [] },
  editable: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
});

const emit = defineEmits(["add", "change-order", "remove"]);
const addForm = reactive({ musicId: "", orderNo: "" });
const orderDrafts = reactive({});
const message = ref("");
const messageType = ref("error");

const orderedItems = computed(() => [...props.items].sort(
  (left, right) => Number(left.orderNo) - Number(right.orderNo),
));

watch(
  () => props.items,
  (items) => {
    for (const item of items) orderDrafts[item.id] = String(item.orderNo);
  },
  { immediate: true, deep: true },
);

function positiveInteger(raw) {
  if (!/^\d+$/.test(String(raw).trim())) return null;
  const value = Number(raw);
  return Number.isSafeInteger(value) && value >= 1 ? value : null;
}

function addItem() {
  message.value = "";
  const musicId = positiveInteger(addForm.musicId);
  const orderNo = positiveInteger(addForm.orderNo);
  if (musicId == null) {
    message.value = "음악 ID는 1 이상의 정수로 입력해 주세요.";
    return;
  }
  if (orderNo == null) {
    message.value = "순서는 1 이상의 정수로 입력해 주세요.";
    return;
  }
  emit("add", { musicId, orderNo }, {
    success() {
      addForm.musicId = "";
      addForm.orderNo = "";
      messageType.value = "success";
      message.value = "수록곡을 추가했습니다.";
    },
    fail(error) {
      messageType.value = "error";
      message.value = playlistErrorMessage(error);
    },
  });
}

function changeOrder(item) {
  message.value = "";
  const orderNo = positiveInteger(orderDrafts[item.id]);
  if (orderNo == null) {
    message.value = "순서는 1 이상의 정수로 입력해 주세요.";
    return;
  }
  if (orderNo === Number(item.orderNo)) {
    message.value = "현재 순서와 다른 번호를 입력해 주세요.";
    return;
  }
  emit("change-order", item, orderNo, {
    success() {
      messageType.value = "success";
      message.value = "수록곡 순서를 변경했습니다.";
    },
    fail(error) {
      orderDrafts[item.id] = String(item.orderNo);
      messageType.value = "error";
      message.value = playlistErrorMessage(error);
    },
  });
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
