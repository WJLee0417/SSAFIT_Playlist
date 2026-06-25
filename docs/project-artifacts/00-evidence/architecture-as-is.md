# Architecture As-Is Evidence

Generated from the current `develop` checkout on 2026-06-25.

## Overall Runtime

```mermaid
flowchart LR
    Browser["Browser"]
    Vue["Vue 3 SPA\nfrontend/src"]
    JSP["JSP Views\nWEB-INF/views/member"]
    API["Spring Boot API\nssafit/src/main/java"]
    MyBatis["MyBatis Mappers\nresources/mappers/**/*.xml"]
    MySQL["MySQL 8\nschema.sql"]
    Spotify["Spotify Accounts + Web API"]
    AI["OpenAI-compatible API\nAI_RUNNING_REPORT_*"]

    Browser --> Vue
    Browser --> JSP
    Vue -->|Axios /api/*| API
    JSP -->|form POST/GET| API
    API --> MyBatis
    MyBatis --> MySQL
    API -->|OAuth/Search/Playlist| Spotify
    API -->|Optional report generation| AI
```

## Backend Flow

```mermaid
flowchart TB
    Controllers["Controllers\nmember/music/musicreview/playlist/recommendation/running*/spotify/ai"]
    Services["Services\nbusiness rules and authorization"]
    Mappers["Mapper interfaces + XML SQL"]
    DB["MySQL tables"]
    Exceptions["GlobalExceptionHandler\nErrorCode/ErrorResponse"]
    Session["HttpSession\nSessionMemberResolver"]

    Controllers --> Session
    Controllers --> Services
    Services --> Mappers
    Mappers --> DB
    Services --> Exceptions
    Controllers --> Exceptions
```

Representative backend evidence:

- `SsafitApplication.java`: explicit `@MapperScan` allowlist for `airunningreport`, `member`, `music`, `musicreview`, `playlist`, `recommendation`, `runningprofile`, `runningrecord`, `runningsession`, `spotify`.
- API controllers: `MemberApiController`, `AuthApiController`, `MusicApiController`, `MusicReviewApiController`, `PlaylistApiController`, `RecommendationApiController`, `RunningRecordApiController`, `RunningSessionApiController`, `SpotifyApiController`, `AiRunningReportApiController`.
- JSP controllers: `MemberViewController`, `RunningProfileViewController`, `SpotifyLoginViewController`.
- Session authentication: `AuthService`, `SessionConst`, `SessionMemberResolver`.

## Database Flow

```mermaid
erDiagram
    members ||--o| running_profiles : has
    members ||--o{ music_reviews : writes
    members ||--o{ playlists : owns
    members ||--o{ playlist_saves : saves
    members ||--o{ recommendation_logs : requests
    members ||--o{ spotify_connections : connects
    members ||--o{ running_records : records
    members ||--o{ running_sessions : hosts
    members ||--o{ running_session_participants : joins
    members ||--o{ ai_running_reports : receives
    music ||--o{ music_reviews : reviewed
    music ||--o{ playlist_items : included
    playlists ||--o{ playlist_items : contains
    playlists ||--o{ playlist_saves : saved_by
    playlists ||--o{ recommendation_logs : saved_from
    running_sessions ||--o{ running_session_participants : has
```

Database evidence:

- `schema.sql` creates `members`, `running_profiles`, `music`, `music_reviews`, `playlists`, `playlist_items`, `playlist_saves`, `recommendation_logs`, `ai_running_reports`, `spotify_connections`, `running_records`, `running_sessions`, `running_session_participants`.
- Status/check values include `members.status` (`ACTIVE`, `INACTIVE`, `SUSPENDED`, `WITHDRAWN`), `playlists.spotify_export_status` (`NOT_EXPORTED`, `PENDING`, `SUCCESS`, `FAILED`, `PARTIAL`), `ai_running_reports.status` (`PENDING`, `COMPLETED`, `FAILED`), `running_sessions.status` (`OPEN`, `CLOSED`, `CANCELLED`).
- Soft delete is used for `members`, `playlists`, and `running_records` through `deleted_at`.

## Frontend And JSP Split

```mermaid
flowchart LR
    User["User"]
    VueRouter["Vue Router\nfrontend/src/router/index.js"]
    VuePages["Vue Pages\nmusic/playlists/running/recommendations/oauth"]
    ApiClients["API Clients\nfrontend/src/api/*.js"]
    JspLogin["JSP Login/Join/My Page\n/member/*.jsp"]
    SpringViews["Spring MVC View Controllers"]
    SpringApi["Spring REST API"]

    User --> VueRouter
    VueRouter --> VuePages
    VuePages --> ApiClients
    ApiClients --> SpringApi
    VueRouter -->|unauthenticated redirect| JspLogin
    User --> JspLogin
    JspLogin --> SpringViews
    SpringViews --> SpringApi
```

Vue routes confirmed in `frontend/src/router/index.js`:

- `/music`, `/music/:musicId`, `/music-manage/new`, `/music-manage/:musicId/edit`
- `/playlists`, `/playlists/saved`, `/playlists/new`, `/playlists/:playlistId`, `/playlists/:playlistId/edit`
- `/running-records`, `/running-records/new`, `/running-records/:runningRecordId`, `/running-records/:runningRecordId/edit`
- `/running-sessions`, `/running-sessions/new`, `/running-sessions/:runningSessionId`, `/running-sessions/:runningSessionId/edit`
- `/recommendations`, `/oauth/spotify/complete`, `/login-required`, `/login/spotify`, `/access-check-failed`

JSP routes confirmed in view controllers:

- `MemberViewController`: `/join`, `/login`, `/logout`, `/members/me`, `/members/me/identity`
- `RunningProfileViewController`: `/members/me/running-profile`
- `SpotifyLoginViewController`: `/login/spotify`

## External API Flow

```mermaid
sequenceDiagram
    participant V as Vue/JSP
    participant S as Spring SpotifyApiController
    participant O as SpotifyOAuthService
    participant C as SpotifyRestApiClient
    participant P as Spotify
    participant D as spotify_connections

    V->>S: GET /api/spotify/oauth/authorize
    S->>O: buildAuthorizationUrl()
    O-->>V: redirect to accounts.spotify.com
    P-->>S: GET /api/spotify/oauth/callback?code&state
    S->>O: handle callback and token exchange
    O->>C: token/profile requests
    C->>P: accounts/api calls
    O->>D: save token connection
    S-->>V: redirect to /oauth/spotify/complete
```

Spotify evidence:

- Config: `application.yml` has `spotify.client-id`, `client-secret`, `redirect-uri`, accounts base URL, API base URL, scopes for private read and playlist write.
- Client: `SpotifyRestApiClient` uses Spotify HTTP endpoints.
- Services: `SpotifyOAuthService`, `SpotifyService`, `SpotifyPlaylistImportService`, `PlaylistExportService`.
- DB: `spotify_connections`, Spotify metadata columns on `music`, export status columns on `playlists`.

AI report evidence:

- Config: `ai.running-report.*` in `application.yml` and `application-docker.yml`.
- Provider: `OpenAiRunningReportProvider`.
- API: `GET /api/ai/running-reports/latest`, `POST /api/ai/running-reports`.
- DB: `ai_running_reports`.

## Docker Flow

```mermaid
flowchart TB
    Compose["compose.yaml"]
    MysqlSvc["mysql\nmysql:8.4.4"]
    BackendSvc["backend\nssafit/Dockerfile"]
    FrontendSvc["frontend\nfrontend/Dockerfile + nginx"]
    Schema["schema.sql + demo-snapshot.sql"]
    Health["/health and container healthchecks"]

    Compose --> MysqlSvc
    Compose --> BackendSvc
    Compose --> FrontendSvc
    Schema --> MysqlSvc
    MysqlSvc -->|healthy| BackendSvc
    BackendSvc -->|healthy| FrontendSvc
    BackendSvc --> Health
```

## Actual Request Flow Examples

### Vue Running Record CRUD

```mermaid
sequenceDiagram
    participant Page as RunningRecordFormView.vue
    participant Client as runningRecordApi.js
    participant Controller as RunningRecordApiController
    participant Service as RunningRecordService
    participant Mapper as RunningRecordMapper.xml
    participant DB as running_records

    Page->>Client: create(payload)
    Client->>Controller: POST /api/running-records
    Controller->>Service: createRecord(session, request)
    Service->>Mapper: insert(runningRecord)
    Mapper->>DB: INSERT running_records
    DB-->>Page: ApiResponse<Response>
```

### Vue Recommendation Save To Playlist

```mermaid
sequenceDiagram
    participant Page as RecommendationView.vue
    participant Client as recommendationApi.js
    participant Controller as RecommendationApiController
    participant Service as RecommendationService
    participant DB as recommendation_logs/playlists/playlist_items

    Page->>Client: save(logId, title/visibility)
    Client->>Controller: POST /api/recommendations/{logId}/save
    Controller->>Service: saveRecommendation(memberId, logId, request)
    Service->>DB: read log and create playlist/items
    DB-->>Page: SaveResponse
```

### JSP Login Then Vue Return

```mermaid
sequenceDiagram
    participant Vue as Vue guarded route
    participant JSP as /login JSP
    participant VC as MemberViewController
    participant Auth as AuthService
    participant Session as HttpSession

    Vue->>JSP: redirect with returnTo
    JSP->>VC: POST /login
    VC->>Auth: login(session, loginId, password)
    Auth->>Session: set login member id
    VC-->>Vue: redirect to safe return URL
```
