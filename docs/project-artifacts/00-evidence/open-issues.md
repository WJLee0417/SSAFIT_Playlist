# Open Issues Evidence

Generated from the current `develop` checkout on 2026-06-25.

## Implementation Mismatches

| Issue | Evidence | Impact Before Documentation |
| --- | --- | --- |
| JSP/Vue mixed auth and profile UX | JSP controllers/views exist for `/login`, `/join`, `/members/me`, `/members/me/running-profile`; Vue router covers most product domains and redirects to JSP login. | Use-case diagram and screen design must show both JSP and Vue paths, not a single pure SPA flow. |
| Running profile has API client but no dedicated Vue page | `frontend/src/api/runningProfileApi.js` exists; `frontend/src/router/index.js` has no `/running-profile` route. | Mark profile editing as JSP-connected, not Vue-screen-complete. |
| Internal music APIs have no confirmed role model | `MusicApiController` exposes `/api/music/internal` create/update, but `members` schema has no role column and no Spring Security config was found. | Admin/internal requirements should be marked unverified or out of scope unless manually confirmed. |
| Swagger/OpenAPI absent | No springdoc/swagger/openapi dependency in `ssafit/pom.xml`; no OpenAPI config found by search. | Do not claim generated API docs exist. |

## Connected API But Test Coverage Not Fully Confirmed

| Area | Evidence | Status |
| --- | --- | --- |
| Spotify OAuth real callback | `SpotifyApiController` and `SpotifyOAuthService` tests exist, but real credentials and redirect URI are environment-dependent. | API implemented; manual credentialed confirmation needed. |
| Spotify playlist import/export against live Spotify | `SpotifyPlaylistImportService`, `PlaylistExportService`, `SpotifyRestApiClient`, and Vue export/import UI exist. | Unit/controller coverage exists; live API limits/user permissions need manual confirmation. |
| AI running report provider | `OpenAiRunningReportProvider` exists and config points at `https://api.openai.com/v1`, but provider is disabled by default. | API/storage/generation logic tested; real provider call needs enabled env and manual confirmation. |
| Non-running frontend E2E | Playwright file focuses on running records/sessions/auth guard. | Music, playlist, review, Spotify, AI report screens need manual or additional E2E verification. |

## Unconfirmed DB Relationships Or Runtime Drift

| Issue | Evidence | Needed Check |
| --- | --- | --- |
| Runtime DB schema not queried in this inventory | Source DDL and migrations were inspected only. | Run safe `information_schema` checks against intended DB before final ERD. |
| Migrations plus full `schema.sql` may represent different setup paths | `schema.sql` already includes later fields; `migrations/*.sql` also alter/add them. | Document whether deployment uses fresh schema, migrations, or both. |
| `spring.sql.init.mode=never` outside Docker | `application.yml` disables SQL init; Docker mounts schema/demo snapshot into MySQL entrypoint. | Requirements should specify DB provisioning path explicitly. |

## Test Gaps

| Area | Evidence | Gap |
| --- | --- | --- |
| Vue unit/component tests | No frontend unit test framework found in `frontend/package.json`. | UI behavior mostly relies on code inspection and Playwright running-feature E2E. |
| Full backend test execution not run during this evidence generation | Inventory is read-only/source-based; no Maven test command was run in this task. | Use latest verified reports or rerun tests with Java 25 before final QA claims. |
| Browser verification not run during this evidence generation | No dev server/browser flow was launched. | Screen design should be validated against running app later. |

## Spotify Policy And API Constraints

- Spotify scopes in `application.yml`: `user-read-private`, `playlist-read-private`, `playlist-modify-private`, `playlist-modify-public`.
- Track search limit is capped in `frontend/src/api/spotifyApi.js` to max `10`; service tests include excessive limit capping.
- Playlist export depends on all selected tracks having Spotify IDs/URIs; `PlaylistItemService` checks exportability based on Spotify URI availability.
- Real behavior can be affected by Spotify app mode, redirect URI registration, user consent, access token expiry/refresh, playlist write permission, market availability, and Web API rate limits.

## Items To Confirm Before Writing Final Project Artifacts

1. Whether final requirements should include JSP member screens as first-class screens or treat them as legacy/auth support.
2. Whether running profile must be represented as JSP only, or whether a Vue screen is still expected.
3. Whether admin/internal music management requires roles; current source does not prove role-based authorization.
4. Whether the ERD should use `schema.sql` as canonical or live MySQL `information_schema` as canonical.
5. Whether Spotify and AI features should be marked "implemented but external-manual-verification required" in requirement acceptance criteria.
6. Whether Docker is the required demo runtime; it is currently the only path that auto-loads schema and demo snapshot.
