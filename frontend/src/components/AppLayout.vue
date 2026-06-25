<template>
  <div class="app-shell">
    <header class="site-header">
      <nav class="nav-shell" aria-label="주요 메뉴">
        <RouterLink class="brand" to="/playlists">
          <span class="brand-mark" aria-hidden="true"></span>
          <span class="brand-wordmark" aria-label="SSAFIT Playlist">
            <span class="brand-ssafit">SSAFIT</span>
            <span class="brand-playlist">Playlist</span>
          </span>
        </RouterLink>
        <div class="nav-links">
          <RouterLink to="/playlists">플레이리스트</RouterLink>
          <RouterLink to="/recommendations">음악 추천</RouterLink>
          <RouterLink to="/running-records">러닝 기록</RouterLink>
          <RouterLink to="/running-sessions">러닝 세션</RouterLink>
          <a :href="myPageUrl">마이페이지</a>
          <form class="nav-form" method="post" :action="logoutUrl">
            <button class="link-button" type="submit">로그아웃</button>
          </form>
        </div>
      </nav>
    </header>
    <main class="page-shell">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { RouterLink } from "vue-router";
import { buildJspUrl } from "../utils/navigation";

const myPageUrl = buildJspUrl("/members/me");
const logoutUrl = buildJspUrl("/logout");
</script>
