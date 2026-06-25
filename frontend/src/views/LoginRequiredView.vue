<template>
  <AppLayout>
    <section class="state-panel state-loginRequired">
      <p>로그인 후 이용해 주세요.</p>
      <a class="primary-button" :href="loginUrl">로그인으로 이동</a>
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
