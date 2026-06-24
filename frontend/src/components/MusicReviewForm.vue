<template>
  <form class="review-form" @submit.prevent="submit">
    <div class="review-score-grid">
      <label>
        러닝 적합도
        <select v-model.number="form.runningFitScore" required>
          <option disabled value="">점수 선택</option>
          <option v-for="score in scores" :key="score" :value="score">{{ score }}</option>
        </select>
      </label>
    </div>
    <label>
      리뷰 내용
      <textarea
        v-model="form.reviewContent"
        maxlength="2000"
        placeholder="러닝할 때 어떤 느낌이었는지 남겨 주세요."
      ></textarea>
    </label>
    <p v-if="validationMessage" class="field-error" role="alert">
      {{ validationMessage }}
    </p>
    <p v-if="errorMessage" class="save-status save-error" role="alert">
      {{ errorMessage }}
    </p>
    <div class="form-actions">
      <button type="submit" :disabled="busy">
        {{ busy ? "저장 중..." : submitLabel }}
      </button>
      <button
        v-if="mode === 'edit'"
        type="button"
        class="secondary-button"
        :disabled="busy"
        @click="$emit('cancel')"
      >
        취소
      </button>
    </div>
  </form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";

const props = defineProps({
  review: { type: Object, default: null },
  mode: { type: String, default: "create" },
  busy: { type: Boolean, default: false },
  errorMessage: { type: String, default: "" },
});

const emit = defineEmits(["submit", "cancel"]);
const scores = [1, 2, 3, 4, 5];
const validationMessage = ref("");
const form = reactive({
  runningFitScore: "",
  reviewContent: "",
});

watch(
  () => props.review,
  (review) => {
    form.runningFitScore = review?.runningFitScore ?? review?.preferenceScore ?? "";
    form.reviewContent = review?.reviewContent ?? "";
    validationMessage.value = "";
  },
  { immediate: true },
);

const submitLabel = props.mode === "edit" ? "수정 저장" : "리뷰 등록";

function validIntegerScore(value) {
  return Number.isInteger(value) && value >= 1 && value <= 5;
}

function submit() {
  validationMessage.value = "";
  if (!validIntegerScore(form.runningFitScore)) {
    validationMessage.value = "러닝 적합도 점수를 1부터 5 사이의 정수로 선택해 주세요.";
    return;
  }
  emit("submit", {
    runningFitScore: form.runningFitScore,
    reviewContent: form.reviewContent.trim() || null,
  });
}
</script>
