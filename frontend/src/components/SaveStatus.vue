<template>
  <p v-if="message" class="save-status" :class="`save-${type}`" role="status">
    {{ message }}
  </p>
</template>

<script setup>
defineProps({
  type: { type: String, default: "success" },
  message: { type: String, default: "" },
});
</script>
