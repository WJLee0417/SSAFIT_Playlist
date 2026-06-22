const SPOTIFY_RETURN_KEY = "ssafit.spotify.returnTo";

export function rememberSpotifyReturnPath(path) {
  if (typeof path === "string" && path.startsWith("/")) {
    sessionStorage.setItem(SPOTIFY_RETURN_KEY, path);
  }
}

export function consumeSpotifyReturnPath() {
  const path = sessionStorage.getItem(SPOTIFY_RETURN_KEY);
  sessionStorage.removeItem(SPOTIFY_RETURN_KEY);
  return path?.startsWith("/") ? path : null;
}
