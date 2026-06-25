# 통합 추적표

기준: `requirements-definition.md`의 `FR-*`를 중심으로 Use Case, Screen, API/Controller, DB Table, WBS, Gantt를 연결한다. 구현 상태는 요구사항 정의서와 근거 데이터의 현재 상태를 우선한다.

| Requirement ID | Use Case ID | Screen ID | API/Controller | DB Table | WBS ID | Gantt Task ID | 구현 상태 |
|---|---|---|---|---|---|---|---|
| FR-001 | UC-001 | SCR-002 | `MemberViewController /join`, `MemberApiController POST /api/members` | `members` | 2.1, 2.2, 2.3 | G-101, G-102, G-103 | IMPLEMENTED |
| FR-002 | UC-002 | SCR-001, SCR-003, SCR-007 | `MemberViewController /login`, `AuthApiController POST /api/auth/login`, Vue router guard | `members` | 2.1, 2.2, 2.3, 2.6 | G-101, G-102, G-103, G-105 | IMPLEMENTED |
| FR-003 | UC-003 | SCR-004 | `MemberViewController POST /logout`, `AuthApiController POST /api/auth/logout` | - | 2.1, 2.2, 2.3 | G-101, G-102, G-103 | IMPLEMENTED |
| FR-004 | UC-004 | SCR-005 | `MemberViewController /members/me`, `MemberApiController GET /api/members/me` | `members` | 2.1, 2.2, 2.3 | G-101, G-102, G-103 | IMPLEMENTED |
| FR-005 | UC-005 | SCR-006 | `RunningProfileViewController /members/me/running-profile`, `RunningProfileApiController /api/members/me/running-profile` | `running_profiles` | 2.4, 2.5 | G-104, G-106 | PARTIAL: JSP 화면 기준 구현, Vue 전용 화면은 후속 통합 |
| FR-006 | UC-006 | SCR-018, SCR-019, SCR-020 | `RunningRecordApiController /api/running-records` | `running_records` | 6.1, 6.4, 6.5, 6.6 | G-401, G-402, G-406 | IMPLEMENTED |
| FR-007 | UC-007 | SCR-021, SCR-022, SCR-023 | `RunningSessionApiController`, `RunningSessionParticipantApiController` | `running_sessions`, `running_session_participants` | 6.2, 6.3, 6.4, 6.5, 6.6 | G-401, G-402, G-406 | IMPLEMENTED |
| FR-008 | UC-008 | SCR-009 | `MusicApiController GET /api/music` | `music` | 3.1, 3.2, 3.3 | G-201, G-203 | IMPLEMENTED |
| FR-009 | UC-009 | SCR-010 | `MusicApiController GET /api/music/{musicId}` | `music`, `music_reviews` | 3.1, 3.2, 3.3 | G-201, G-203 | IMPLEMENTED |
| FR-010 | UC-010 | SCR-010, SCR-011 | `MusicReviewApiController` | `music_reviews` | 4.1, 4.2, 4.3, 4.4 | G-202, G-204, G-206 | IMPLEMENTED |
| FR-011 | UC-011 | SCR-013, SCR-015, SCR-016 | `PlaylistApiController /api/playlists` | `playlists` | 5.1, 5.4, 5.5 | G-301, G-303 | IMPLEMENTED |
| FR-012 | UC-012 | SCR-013, SCR-015, SCR-017 | `PlaylistItemApiController` | `playlist_items` | 5.2, 5.4, 5.5 | G-301, G-303, G-305 | IMPLEMENTED |
| FR-013 | UC-013 | SCR-014, SCR-015 | `PlaylistSaveApiController /api/playlist-saves` | `playlist_saves` | 5.3, 5.4, 5.5 | G-302, G-303 | IMPLEMENTED |
| FR-014 | UC-014 | SCR-024 | `RecommendationApiController GET /api/recommendations` | `recommendation_logs` | 7.1, 7.2, 7.3 | G-403 | IMPLEMENTED |
| FR-015 | UC-015 | SCR-006, SCR-024 | `RecommendationApiController`, `CadenceResolutionService`, `RunningIntensityResolutionService` | `running_profiles`, `running_records`, `music`, `recommendation_logs` | 2.4, 6.1, 7.1 | G-403 | IMPLEMENTED |
| FR-016 | UC-016 | SCR-025 | `RecommendationApiController POST /api/recommendations/{logId}/save` | `playlists`, `playlist_items`, `recommendation_logs` | 5.6, 7.2, 7.3 | G-304 | IMPLEMENTED |
| FR-017 | UC-017 | SCR-009, SCR-013, SCR-027 | `SpotifyApiController GET /api/spotify/tracks/search`, `MusicApiController POST /api/music/from-spotify` | `music` | 8.1, 8.2, 8.3 | G-501, G-502, G-503 | IMPLEMENTED |
| FR-018 | UC-018 | SCR-028, SCR-029 | `SpotifyApiController oauth/connection`, `SpotifyLoginViewController /login/spotify` | `spotify_connections` | 8.1, 8.2, 8.3, 8.4 | G-501, G-502, G-504 | UNVERIFIED |
| FR-019 | UC-019, UC-020 | SCR-015, SCR-030 | `PlaylistExportApiController POST /api/playlists/{playlistId}/export/spotify` | `playlists`, `playlist_items`, `music`, `spotify_connections` | 8.1, 8.2, 8.3, 8.5 | G-501, G-502, G-503, G-505 | UNVERIFIED |
| FR-020 | UC-021 | SCR-032 | `SpotifyApiController GET /api/spotify/me/playlists`, `POST /api/spotify/playlists/import` | `spotify_connections`, `playlists`, `playlist_items`, `music` | 8.1, 8.2, 8.3, 8.5 | G-501, G-502, G-503, G-505 | UNVERIFIED |
| FR-021 | UC-024 | SCR-018, SCR-026 | `AiRunningReportApiController /api/ai/running-reports` | `ai_running_reports`, `running_records`, `recommendation_logs` | 7.4, 7.5 | G-404, G-405 | UNVERIFIED |
| FR-022 | UC-022 | - | `HealthController /health`, Docker Compose | MySQL schema init | 1.1, 11.1, 11.2, 11.3, 11.4 | G-002, G-703, G-704, G-706 | IMPLEMENTED |
| FR-023 | UC-023 | SCR-001, SCR-007, SCR-008, SCR-031 | `GlobalExceptionHandler`, `ApiResponse`, `ErrorResponse` | - | 1.2, 1.5, 2.6, 9.1 | G-004, G-005, G-105, G-601 | IMPLEMENTED |
| FR-024 | UC-025 | SCR-012 | `MusicApiController POST/PUT /api/music/internal` | `music` | 3.2, 3.3, 3.4 | G-203, G-205 | UNVERIFIED |

## 검증 결과

- 모든 `FR-*` 요구사항은 최소 1개 이상의 Use Case와 연결되어 있다.
- 모든 화면이 필요한 핵심 사용자 기능은 Screen ID로 연결되어 있다.
- `FR-020`은 API는 있으나 전용 화면이 없어 `SCR-032 NOT_IMPLEMENTED_SCREEN`으로 추적한다.
- `FR-022`는 실행 환경 요구사항이라 사용자 화면 ID가 없다.
