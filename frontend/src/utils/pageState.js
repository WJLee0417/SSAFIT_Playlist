import { computed, ref } from "vue";
import { isApiError } from "../api/http";

export function usePageState(initialData = null) {
  const data = ref(initialData);
  const status = ref("idle");
  const error = ref(null);
  const notice = ref("");

  const isEmpty = computed(() => {
    const value = data.value;
    if (Array.isArray(value)) {
      return value.length === 0;
    }
    if (value && Array.isArray(value.items)) {
      return value.items.length === 0;
    }
    return value == null;
  });

  async function run(request, { empty = isEmpty } = {}) {
    status.value = "loading";
    error.value = null;
    notice.value = "";
    try {
      data.value = await request();
      status.value = empty.value ? "empty" : "ready";
      return data.value;
    } catch (caught) {
      error.value = caught;
      if (isApiError(caught) && caught.loginRequired) {
        status.value = "loginRequired";
      } else if (isApiError(caught) && caught.unauthorized) {
        status.value = "unauthorized";
      } else {
        status.value = "error";
      }
      return null;
    }
  }

  return { data, status, error, notice, isEmpty, run };
}
