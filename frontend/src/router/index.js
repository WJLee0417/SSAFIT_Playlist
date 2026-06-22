import { createRouter, createWebHistory } from "vue-router";
import { authApi } from "../api/authApi";
import { isApiError } from "../api/http";

const routes = [
  { path: "/", redirect: "/music" },
  {
    path: "/music",
    name: "music-list",
    component: () => import("../views/MusicListView.vue"),
  },
  {
    path: "/music/:musicId",
    name: "music-detail",
    component: () => import("../views/MusicDetailView.vue"),
    props: true,
  },
  {
    path: "/playlists",
    name: "playlist-list",
    component: () => import("../views/PlaylistListView.vue"),
  },
  {
    path: "/playlists/saved",
    name: "saved-playlists",
    component: () => import("../views/SavedPlaylistView.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/playlists/new",
    name: "playlist-create",
    component: () => import("../views/PlaylistFormView.vue"),
    meta: { requiresAuth: true, mode: "create" },
  },
  {
    path: "/playlists/:playlistId/edit",
    name: "playlist-edit",
    component: () => import("../views/PlaylistFormView.vue"),
    meta: { requiresAuth: true, mode: "edit" },
    props: true,
  },
  {
    path: "/playlists/:playlistId",
    name: "playlist-detail",
    component: () => import("../views/PlaylistDetailView.vue"),
    props: true,
  },
  {
    path: "/recommendations",
    name: "recommendations",
    component: () => import("../views/RecommendationView.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/spotify/search",
    name: "spotify-search",
    component: () => import("../views/SpotifySearchView.vue"),
  },
  {
    path: "/oauth/spotify/complete",
    name: "spotify-oauth-complete",
    component: () => import("../views/SpotifyOAuthCompleteView.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/login-required",
    name: "login-required",
    component: () => import("../views/LoginRequiredView.vue"),
  },
  {
    path: "/access-check-failed",
    name: "access-check-failed",
    component: () => import("../views/AccessCheckFailedView.vue"),
  },
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: () => import("../views/NotFoundView.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: (to, from, savedPosition) => savedPosition || { top: 0 },
});

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) {
    return true;
  }

  try {
    await authApi.getSession();
    return true;
  } catch (error) {
    if (isApiError(error) && error.loginRequired) {
      authApi.clearSessionCache();
      return {
        name: "login-required",
        query: { returnTo: to.fullPath },
      };
    }
    return {
      name: "access-check-failed",
      query: {
        returnTo: to.fullPath,
        status: error.status,
        errorCode: error.errorCode,
      },
    };
  }
});

export default router;
