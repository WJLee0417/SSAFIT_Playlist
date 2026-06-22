<template>
  <AppLayout>
    <section class="state-panel state-error">
      <h1>로그인 상태를 확인하지 못했습니다</h1>
      <p>인증 확인 API가 실패했으므로 로그인된 것으로 간주하지 않았습니다.</p>
      <dl class="error-meta">
        <div><dt>HTTP</dt><dd>{{ route.query.status || "응답 없음" }}</dd></div>
        <div><dt>오류 코드</dt><dd>{{ route.query.errorCode || "UNKNOWN_ERROR" }}</dd></div>
      </dl>
      <RouterLink class="primary-button" :to="returnTo">다시 확인</RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";
import AppLayout from "../components/AppLayout.vue";

const route = useRoute();
const returnTo = computed(() => {
  const candidate = route.query.returnTo;
  return typeof candidate === "string" && candidate.startsWith("/") ? candidate : "/";
});
</script>
