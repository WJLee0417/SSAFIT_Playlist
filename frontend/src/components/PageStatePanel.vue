<template>
  <section v-if="status !== 'ready'" class="state-panel" :class="`state-${status}`">
    <template v-if="status === 'loading'">
      <span class="spinner" aria-hidden="true"></span>
      <p>데이터를 불러오는 중입니다.</p>
    </template>

    <template v-else-if="status === 'empty'">
      <h2>{{ emptyTitle }}</h2>
      <p>{{ emptyMessage }}</p>
      <slot name="empty-action" />
    </template>

    <template v-else-if="status === 'loginRequired'">
      <h2>로그인이 필요합니다</h2>
      <p>JSP 로그인 후 이 화면으로 돌아올 수 있도록 현재 경로를 함께 전달합니다.</p>
      <a class="primary-button" :href="loginUrl">JSP 로그인으로 이동</a>
    </template>

    <template v-else-if="status === 'unauthorized'">
      <h2>접근 권한이 없습니다</h2>
      <p>{{ error?.message || "이 작업을 수행할 권한이 없습니다." }}</p>
    </template>

    <template v-else-if="status === 'error'">
      <h2>{{ resolvedErrorTitle }}</h2>
      <p>{{ resolvedErrorMessage }}</p>
      <dl v-if="error" class="error-meta">
        <div><dt>HTTP</dt><dd>{{ error.status ?? "응답 없음" }}</dd></div>
        <div><dt>오류 코드</dt><dd>{{ error.errorCode ?? "UNKNOWN_ERROR" }}</dd></div>
      </dl>
      <button v-if="retryable" type="button" @click="$emit('retry')">다시 시도</button>
    </template>
  </section>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  status: { type: String, required: true },
  error: { type: Object, default: null },
  loginUrl: { type: String, default: "http://127.0.0.1:8080/login" },
  emptyTitle: { type: String, default: "표시할 데이터가 없습니다" },
  emptyMessage: { type: String, default: "조건을 바꾸거나 새 항목을 추가해 보세요." },
  errorTitle: { type: String, default: "요청을 완료하지 못했습니다" },
  retryable: { type: Boolean, default: true },
});

defineEmits(["retry"]);

const resolvedErrorTitle = computed(() => {
  if (props.error?.notFound) return "요청한 정보를 찾을 수 없습니다";
  if (props.error?.conflict) return "현재 상태와 요청이 충돌했습니다";
  if (props.error?.upstreamUnavailable) return "외부 서비스 연결이 원활하지 않습니다";
  return props.errorTitle;
});

const resolvedErrorMessage = computed(
  () => props.error?.message || "서버 연결 상태를 확인해 주세요.",
);
</script>
