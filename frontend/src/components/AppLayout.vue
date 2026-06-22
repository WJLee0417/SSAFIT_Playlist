<template>
  <div class="app-shell">
    <header class="site-header">
      <RouterLink class="brand" to="/music">SSAFIT Playlist</RouterLink>
      <nav aria-label="주요 메뉴">
        <RouterLink to="/music">음악</RouterLink>
        <RouterLink to="/playlists">플레이리스트</RouterLink>
        <RouterLink to="/playlists/saved">내 저장 목록</RouterLink>
        <RouterLink to="/recommendations">자동 큐레이션</RouterLink>
        <RouterLink to="/spotify/search">Spotify 검색</RouterLink>
      </nav>
    </header>
    <main class="page-shell">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { RouterLink } from "vue-router";
</script>
