export const JSP_BASE_URL =
  import.meta.env.VITE_JSP_BASE_URL || "http://127.0.0.1:8080";

export function buildJspUrl(path) {
  return new URL(path, JSP_BASE_URL).toString();
}

export function buildJspLoginUrl(returnTo) {
  const url = new URL("/login", JSP_BASE_URL);
  if (returnTo) {
    url.searchParams.set("returnTo", returnTo);
  }
  return url.toString();
}

export function buildJspRunningProfileUrl(returnTo) {
  const url = new URL("/members/me/running-profile", JSP_BASE_URL);
  if (returnTo) {
    url.searchParams.set("returnTo", returnTo);
  }
  return url.toString();
}

export function currentReturnPath(route) {
  return new URL(route.fullPath, window.location.origin).toString();
}
