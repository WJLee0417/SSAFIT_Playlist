<template>
  <AppLayout>
    <section class="state-panel state-loginRequired">
      <h1>로그인이 필요합니다</h1>
      <p>SSAFIT의 JSP 로그인 화면에서 로그인한 뒤 Vue 기능을 이용해 주세요.</p>
      <a class="primary-button" :href="loginUrl">JSP 로그인으로 이동</a>
      <RouterLink class="text-link" to="/music">공개 음악 목록 보기</RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";
import AppLayout from "../components/AppLayout.vue";
import { buildJspLoginUrl } from "../utils/navigation";

const route = useRoute();
const loginUrl = computed(() => buildJspLoginUrl(
  new URL(
    typeof route.query.returnTo === "string" ? route.query.returnTo : "/",
    window.location.origin,
  ).toString(),
));
</script>
