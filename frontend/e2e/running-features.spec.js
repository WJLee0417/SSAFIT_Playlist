import { expect, test } from "@playwright/test";

const BACKEND_URL = "http://127.0.0.1:8080";
const PASSWORD = "e2e-password";

async function login(page, loginId, returnPath = "/running-records") {
  const returnTo = `http://127.0.0.1:5173${returnPath}`;
  await page.goto(`${BACKEND_URL}/login?returnTo=${encodeURIComponent(returnTo)}`);
  await page.getByLabel("로그인 ID").fill(loginId);
  await page.getByLabel("비밀번호").fill(PASSWORD);
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page).toHaveURL(new RegExp(returnPath.replaceAll("/", "\\/")));
}

async function api(page, method, path, data) {
  return page.request.fetch(`${BACKEND_URL}/api${path}`, {
    method,
    data,
    headers: { Accept: "application/json" },
  });
}

async function createSessionThroughUi(page, {
  title,
  capacity = "2",
  description = "Playwright 실제 브라우저 E2E 모임",
}) {
  await page.goto("/running-sessions/new");
  await page.getByLabel("모임 제목").fill(title);
  await page.getByLabel("설명").fill(description);
  await page.getByLabel("집결 장소").fill("한강공원 E2E 집결지");
  await page.getByLabel("모임 일시").fill("2027-06-23T09:30");
  await page.getByLabel("정원").fill(capacity);
  await page.getByRole("button", { name: "저장" }).click();
  await expect(page).toHaveURL(/\/running-sessions\/\d+/);
  await expect(page.getByRole("heading", { name: title })).toBeVisible();
  return page.url().match(/running-sessions\/(\d+)/)[1];
}

test("비로그인 보호 경로와 인증 확인 실패 흐름", async ({ page }) => {
  await page.goto("/running-records");
  await expect(page).toHaveURL(/\/login-required\?/);
  await expect(page.getByRole("heading", { name: "로그인이 필요합니다" })).toBeVisible();

  await page.goto("/music");
  await expect(page.getByRole("link", { name: "러닝 기록" })).toBeVisible();
  await page.route(
    `${BACKEND_URL}/api/members/me`,
    (route) => route.abort("connectionfailed"),
  );
  await page.getByRole("link", { name: "러닝 기록" }).click();
  await expect(page).toHaveURL(/\/access-check-failed\?/);
  await expect(page.getByRole("heading", { name: "로그인 상태를 확인하지 못했습니다" }))
    .toBeVisible();
});

test("러닝 기록 등록, 조회, 수정, 삭제", async ({ page }) => {
  await login(page, "e2e_host");
  await page.getByRole("link", { name: "새 기록" }).click();

  await page.getByLabel("러닝 날짜").fill("2026-06-23");
  await page.getByLabel("거리 (km)").fill("5.25");
  await page.getByLabel("분").fill("31");
  await page.getByLabel("초").fill("30");
  await page.getByLabel("케이던스").fill("172");
  await page.getByLabel("평균심박수(bpm)").fill("154");
  await page.getByLabel("메모").fill("E2E 최초 러닝 기록");
  await page.getByRole("button", { name: "저장" }).click();

  await expect(page.getByText("러닝 기록을 등록했습니다.")).toBeVisible();
  await expect(page.getByText("5.25 km")).toBeVisible();
  await expect(page.getByText("E2E 최초 러닝 기록")).toBeVisible();

  await page.getByRole("link", { name: "수정" }).click();
  await page.getByLabel("거리 (km)").fill("7.50");
  await page.getByLabel("분").fill("45");
  await page.getByLabel("초").fill("0");
  await page.getByLabel("메모").fill("E2E 수정 완료 기록");
  await page.getByRole("button", { name: "저장" }).click();

  await expect(page.getByText("러닝 기록을 수정했습니다.")).toBeVisible();
  await expect(page.getByText("7.5 km")).toBeVisible();
  await expect(page.getByText("E2E 수정 완료 기록")).toBeVisible();

  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "삭제" }).click();
  await expect(page).toHaveURL(/\/running-records$/);
  await expect(page.getByText("E2E 수정 완료 기록")).toHaveCount(0);
});

test("러닝 모임 작성자 CRUD, 권한, 참여 예외 흐름", async ({ browser }) => {
  const suffix = Date.now();
  const hostContext = await browser.newContext();
  const participantContext = await browser.newContext();
  const fullContext = await browser.newContext();
  const hostPage = await hostContext.newPage();
  const participantPage = await participantContext.newPage();
  const fullPage = await fullContext.newPage();

  try {
    await login(hostPage, "e2e_host", "/running-sessions");
    const sessionId = await createSessionThroughUi(hostPage, {
      title: `E2E 모임 ${suffix}`,
    });

    await hostPage.getByRole("link", { name: "수정" }).click();
    await hostPage.getByLabel("모임 제목").fill(`E2E 수정 모임 ${suffix}`);
    await hostPage.getByRole("button", { name: "저장" }).click();
    await expect(hostPage.getByRole("heading", { name: `E2E 수정 모임 ${suffix}` }))
      .toBeVisible();

    await login(participantPage, "e2e_participant", `/running-sessions/${sessionId}`);
    await expect(participantPage.getByRole("link", { name: "수정" })).toHaveCount(0);
    await expect(participantPage.getByRole("button", { name: "모임 취소" })).toHaveCount(0);

    await participantPage.getByRole("button", { name: "모임 참여" }).click();
    await expect(participantPage.getByText("러닝 모임에 참여했습니다.")).toBeVisible();
    await expect(participantPage.getByRole("button", { name: "참여 취소" })).toBeVisible();

    const duplicate = await api(
      participantPage,
      "POST",
      `/running-sessions/${sessionId}/participants/me`,
    );
    expect(duplicate.status()).toBe(409);
    expect((await duplicate.json()).code).toBe("DUPLICATE_SESSION_JOIN");

    participantPage.once("dialog", (dialog) => dialog.accept());
    await participantPage.getByRole("button", { name: "참여 취소" }).click();
    await expect(participantPage.getByText("러닝 모임 참여를 취소했습니다.")).toBeVisible();

    await hostPage.reload();
    await hostPage.getByRole("button", { name: "모집 마감" }).click();
    await expect(hostPage.getByText("모집을 마감했습니다.")).toBeVisible();
    await participantPage.reload();
    await expect(participantPage.getByText("모집이 마감되었습니다")).toBeVisible();
    await expect(participantPage.getByRole("button", { name: "모임 참여" })).toHaveCount(0);
    const closedJoin = await api(
      participantPage,
      "POST",
      `/running-sessions/${sessionId}/participants/me`,
    );
    expect(closedJoin.status()).toBe(409);
    expect((await closedJoin.json()).code).toBe("SESSION_CLOSED");

    await hostPage.getByRole("button", { name: "모집 다시 열기" }).click();
    await expect(hostPage.getByText("모집을 다시 시작했습니다.")).toBeVisible();

    const fullSessionId = await createSessionThroughUi(hostPage, {
      title: `E2E 정원 모임 ${suffix}`,
      capacity: "1",
    });
    await login(fullPage, "e2e_full_member", `/running-sessions/${fullSessionId}`);
    await fullPage.getByRole("button", { name: "모임 참여" }).click();
    await expect(fullPage.getByText("러닝 모임에 참여했습니다.")).toBeVisible();

    await participantPage.goto(`/running-sessions/${fullSessionId}`);
    await participantPage.getByRole("button", { name: "모임 참여" }).click();
    await expect(participantPage.getByText("러닝 모임 정원이 모두 찼습니다.")).toBeVisible();

    fullPage.once("dialog", (dialog) => dialog.accept());
    await fullPage.getByRole("button", { name: "참여 취소" }).click();
    await expect(fullPage.getByText("러닝 모임 참여를 취소했습니다.")).toBeVisible();
    hostPage.once("dialog", (dialog) => dialog.accept());
    await hostPage.goto(`/running-sessions/${fullSessionId}`);
    await hostPage.getByRole("button", { name: "모임 취소" }).click();
    await expect(hostPage.getByText("러닝 모임을 취소했습니다.")).toBeVisible();

    hostPage.once("dialog", (dialog) => dialog.accept());
    await hostPage.goto(`/running-sessions/${sessionId}`);
    await hostPage.getByRole("button", { name: "모임 취소" }).click();
    await expect(hostPage.getByText("러닝 모임을 취소했습니다.")).toBeVisible();
    await expect(hostPage.getByText("취소됨")).toBeVisible();
  } finally {
    await Promise.all([
      hostContext.close(),
      participantContext.close(),
      fullContext.close(),
    ]);
  }
});
