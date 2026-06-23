import { defineConfig } from "@playwright/test";

const requiredEnvironment = [
  "SSAFIT_TEST_DB_URL",
  "SSAFIT_TEST_DB_USERNAME",
  "SSAFIT_TEST_DB_PASSWORD",
];

for (const name of requiredEnvironment) {
  if (!process.env[name]) {
    throw new Error(
      `${name} is required. Configure a dedicated MySQL database whose name contains "test".`,
    );
  }
}

if (!/\/[^/?]*test[^/?]*(?:\?|$)/i.test(process.env.SSAFIT_TEST_DB_URL)) {
  throw new Error(
    "Refusing to run E2E tests: SSAFIT_TEST_DB_URL database name must contain \"test\".",
  );
}

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  timeout: 45_000,
  expect: {
    timeout: 8_000,
  },
  reporter: [
    ["list"],
    ["html", { outputFolder: "playwright-report", open: "never" }],
  ],
  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  webServer: [
    {
      command:
        ".\\mvnw.cmd spring-boot:test-run \"-Dspring-boot.run.profiles=e2e\"",
      cwd: "../ssafit",
      url: "http://localhost:8080/health",
      timeout: 120_000,
      reuseExistingServer: false,
    },
    {
      command: "npm run dev -- --host 127.0.0.1",
      cwd: ".",
      url: "http://localhost:5173",
      timeout: 60_000,
      reuseExistingServer: false,
    },
  ],
});
