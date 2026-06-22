<template>
  <AppLayout>
    <section class="state-panel state-error">
      <span class="eyebrow">404</span>
      <h1>요청한 화면을 찾을 수 없습니다</h1>
      <p>주소를 다시 확인하거나 음악 목록으로 이동해 주세요.</p>
      <RouterLink class="primary-button" to="/music">음악 목록으로</RouterLink>
    </section>
  </AppLayout>
</template>

<script setup>
import { RouterLink } from "vue-router";
import AppLayout from "../components/AppLayout.vue";
</script>
