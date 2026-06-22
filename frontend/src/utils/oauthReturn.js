const SPOTIFY_RETURN_KEY = "ssafit.spotify.returnTo";

export function isSafeSpotifyReturnPath(path) {
  if (typeof path !== "string" || !path.startsWith("/") || path.startsWith("//")) {
    return false;
  }
  try {
    const url = new URL(path, window.location.origin);
    return url.origin === window.location.origin
      && (
        url.pathname.startsWith("/playlists/")
        || url.pathname === "/spotify/search"
        || url.pathname === "/oauth/spotify/complete"
      );
  } catch {
    return false;
  }
}

export function rememberSpotifyReturnPath(path) {
  if (isSafeSpotifyReturnPath(path)) {
    sessionStorage.setItem(SPOTIFY_RETURN_KEY, path);
  }
}

export function consumeSpotifyReturnPath() {
  const path = sessionStorage.getItem(SPOTIFY_RETURN_KEY);
  sessionStorage.removeItem(SPOTIFY_RETURN_KEY);
  return isSafeSpotifyReturnPath(path) ? path : null;
}
