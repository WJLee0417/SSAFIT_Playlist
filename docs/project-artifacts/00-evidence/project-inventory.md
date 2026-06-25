# Project Inventory Evidence

Generated from the current `develop` checkout on 2026-06-25. This file is evidence data only; no application source was changed.

## Repository Structure

| Path | Role | Evidence |
| --- | --- | --- |
| `ssafit/` | Spring Boot backend packaged as WAR | `ssafit/pom.xml` has `spring-boot-starter-web`, `tomcat-embed-jasper`, MyBatis, MySQL connector, validation, packaging `war`, Java `25`. |
| `ssafit/src/main/java/com/ssafy/ssafit/` | Backend application packages | `SsafitApplication.java` starts Spring Boot and extends `SpringBootServletInitializer`. |
| `ssafit/src/main/resources/mappers/` | MyBatis XML mapper SQL | Mapper XML exists for member, music, musicreview, playlist, recommendation, runningprofile, runningrecord, runningsession, spotify, airunningreport. |
| `ssafit/src/main/resources/sql/` | MySQL DDL, migrations, demo seed | `schema.sql`, `migrations/*.sql`, `demo-snapshot.sql`. |
| `ssafit/src/main/webapp/WEB-INF/views/` | JSP views | `member/login.jsp`, `member/join.jsp`, `member/my-page.jsp`, `member/running-profile.jsp`. |
| `frontend/` | Vue 3 SPA | `frontend/package.json` uses Vue, Vue Router, Axios, Vite, Playwright. |
| `docker/`, `compose.yaml` | Local container runtime | MySQL 8.4.4, backend, frontend services with health checks. |
| `docs/` | Existing design, verification, integration docs | Existing docs were read as context only and not overwritten. |

## Technology Stack

- Backend: Spring Boot `4.0.6`, Java `25`, Servlet/JSP support, Jakarta Validation, MyBatis `4.0.1`, MySQL Connector/J.
- Database: MySQL 8.x syntax and CHECK constraints; Docker image `mysql:8.4.4`.
- Frontend: Vue `3.5.13`, Vue Router `4.5.1`, Axios `1.7.9`, Vite `6.0.5`.
- Testing: JUnit/Spring Boot Test/MockMvc, mapper integration tests against MySQL, Playwright E2E for running features.
- External APIs: Spotify Accounts/API via `SpotifyRestApiClient`; OpenAI-compatible running report endpoint via `OpenAiRunningReportProvider`.
- Deployment: `compose.yaml` builds `ssafit/Dockerfile` and `frontend/Dockerfile`; frontend served through nginx config in `frontend/nginx.conf`.

## Actual Implemented Domains

| Domain | Backend Evidence | Frontend/JSP Evidence | DB Evidence | Status |
| --- | --- | --- | --- | --- |
| Member/Auth | `MemberApiController`, `AuthApiController`, `MemberViewController`, `MemberService`, `AuthService`, `MemberMapper` | JSP login/join/my-page, Vue auth guard uses `authApi.getMe()` | `members` | IMPLEMENTED |
| Music | `MusicApiController`, `MusicService`, `MusicMapper` | `MusicListView.vue`, `MusicDetailView.vue`, `MusicFormView.vue`, `musicApi.js` | `music` | IMPLEMENTED |
| Music Review | `MusicReviewApiController`, `MusicReviewService`, `MusicReviewMapper` | `MusicReviewForm.vue`, `MusicCard.vue`, `reviewApi.js` | `music_reviews` | IMPLEMENTED |
| Playlist | `PlaylistApiController`, `PlaylistItemApiController`, `PlaylistSaveApiController`, playlist services/mappers | playlist views/components, `playlistApi.js`, `playlistSaveApi.js` | `playlists`, `playlist_items`, `playlist_saves` | IMPLEMENTED |
| Running Profile | `RunningProfileApiController`, `RunningProfileViewController`, `RunningProfileService`, `RunningFitnessMetricService` | JSP running profile; Vue API client exists but no Vue route/page dedicated to profile editing | `running_profiles` | PARTIAL |
| Running Records | `RunningRecordApiController`, `RunningRecordService`, `RunningRecordMapper` | `RunningRecord*View.vue`, `runningRecordApi.js` | `running_records` | IMPLEMENTED |
| Running Sessions | `RunningSessionApiController`, `RunningSessionParticipantApiController`, services/mappers | `RunningSession*View.vue`, `runningSessionApi.js` | `running_sessions`, `running_session_participants` | IMPLEMENTED |
| Cadence/Heart-rate/Intensity Recommendation | `RecommendationApiController`, `RecommendationService`, `CadenceResolutionService`, `RunningIntensityResolutionService`, policies | `RecommendationView.vue`, `RecommendationResult.vue`, `recommendationApi.js` | `recommendation_logs`, cadence/HR fields in running tables | IMPLEMENTED |
| Spotify Search/OAuth/Import/Export | `SpotifyApiController`, `PlaylistExportApiController`, Spotify services/client/mapper | `spotifyApi.js`, `SpotifyOAuthCompleteView.vue`, `SpotifyExportPanel.vue`, Spotify search in `MusicListView.vue` and playlist import/export controls | `spotify_connections`, Spotify metadata columns on `music` and `playlists` | IMPLEMENTED, MANUAL_CONFIRMATION_NEEDED for real Spotify credentials/rate limits |
| AI Running Report | `AiRunningReportApiController`, services, mapper, provider | `AiRunningReportCard.vue`, `aiRunningReportApi.js` used from recommendation surface | `ai_running_reports` | IMPLEMENTED, MANUAL_CONFIRMATION_NEEDED for enabled real provider |

## Backend Packages And Responsibilities

- `common.auth`: session constants/resolution and PBKDF2 password hashing (`SessionConst`, `SessionMemberResolver`, `PasswordHashService`).
- `common.config`: CORS/static resource configuration (`WebConfig`).
- `common.exception`: structured application errors (`GlobalExceptionHandler`, `ErrorCode`, `CustomException`).
- `common.response`: API envelope/pagination DTOs.
- `member`: session login, API login/logout/join/me, JSP member views.
- `music`, `musicreview`: music catalog/search/import/internal-save and review CRUD.
- `playlist`: playlist CRUD, item ordering, saved playlist, Spotify export status fields.
- `recommendation`: cadence/intensity resolution, AI workout plan/recommendation result/log/save flow.
- `runningprofile`, `runningrecord`, `runningsession`: runner profile, personal run records, group run session/participants.
- `spotify`: OAuth, connection persistence, search, user playlists, playlist import/export.
- `airunningreport`: report aggregation/generation/storage and optional OpenAI-compatible provider.

## Execution Structure

- Local backend default: `ssafit/src/main/resources/application.yml` uses port `8080`, JSP prefix `/WEB-INF/views/`, MySQL URL `jdbc:mysql://localhost:3306/ssafit_playlist`, `spring.sql.init.mode=never`.
- Local frontend: `frontend/package.json` has `npm run dev`, `npm run build`, `npm run e2e`; `frontend/src/api/http.js` sets Axios base URL from `VITE_API_BASE_URL` or `/api`.
- Docker: `compose.yaml` starts MySQL, backend, frontend. MySQL initializes with `schema.sql` and `demo-snapshot.sql`. Backend uses `SPRING_PROFILES_ACTIVE=docker`. Frontend maps host port `5173` to container port `80`.
- MyBatis scanning is explicitly restricted in `SsafitApplication.java` to real mapper packages, including `spotify.mapper` but excluding non-mapper Spotify client interfaces.

## Architecture Issues Found

- JSP and Vue coexist: authentication/member profile flows are still available as JSP, while most domain workflows are Vue routes. Vue redirects unauthenticated users to JSP login via helper URLs.
- Running profile is split: API client exists in Vue, but the visible profile editing flow is JSP (`/members/me/running-profile`) rather than a Vue Router page.
- Swagger/OpenAPI is not implemented: no springdoc/swagger dependency or generated OpenAPI config was found.
- `spring.sql.init.mode=never` means local DB schema is not automatically applied outside Docker/test setup.
- External integrations are environment-gated: Spotify requires client credentials/redirect URI; AI report requires `AI_RUNNING_REPORT_ENABLED` and provider/API key.
- Frontend E2E coverage is concentrated on running features; Spotify OAuth/export and AI provider behavior require manual or credentialed verification.
