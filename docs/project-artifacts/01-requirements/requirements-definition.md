# SSAFIT Playlist 요구사항 정의서

본 문서는 `docs/project-artifacts/00-evidence/`의 근거 자료와 기존 명세성 문서(`README.md`, `docs/team-b-handover.md`, `docs/spotify-service-contract.md`, 러닝/추천 관련 설계·검증 문서)를 기준으로 작성한다. 목표 요구사항은 프로젝트가 제공하려는 기능 범위를 의미하며, 현재 구현 상태는 2026-06-25 현재 `develop` 브랜치의 실제 코드 근거로 판단한다.

## 1. 프로젝트 개요

### 프로젝트명

SSAFIT Playlist

### 문제 정의

러닝 사용자는 운동 강도, 케이던스, 심박수, 선호 장르, 음악 BPM을 함께 고려해 음악을 고르기 어렵다. 또한 러닝 기록, 러닝 모임, 음악 리뷰, 플레이리스트 저장/내보내기 기능이 분산되어 있으면 운동 경험과 음악 선택 경험이 이어지지 않는다.

### 해결 목표

- 회원의 러닝 프로필과 러닝 기록을 기반으로 운동 강도와 음악 조건을 계산한다.
- 음악 목록, 리뷰, 플레이리스트, 자동 큐레이션 저장 흐름을 하나의 서비스 안에서 제공한다.
- Spotify 검색, OAuth 연결, 플레이리스트 내보내기를 통해 외부 음악 플랫폼과 연동한다.
- Docker 기반 실행 구성을 통해 백엔드, 프론트엔드, MySQL 데모 환경을 재현 가능하게 한다.

### 주요 사용자

- 비회원: 공개 음악, 공개 플레이리스트, 로그인/회원가입 화면 접근 사용자
- 일반 회원: 러닝 기록/세션/프로필, 리뷰, 플레이리스트, 추천 기능을 사용하는 로그인 사용자
- Spotify 연동 회원: Spotify OAuth 연결 후 Spotify 검색, 가져오기, 내보내기를 사용하는 회원

### 핵심 가치

- 러닝 데이터와 음악 데이터를 연결한 개인화 추천
- 세션 기반 인증으로 JSP와 Vue 화면을 함께 사용하는 통합 사용자 흐름
- Spotify 외부 연동을 통한 실제 음악 서비스 확장성
- MyBatis/MySQL 기반의 명시적인 데이터 구조와 추적 가능한 Mapper SQL

## 2. 시스템 범위

### 포함 범위

- 회원가입, 로그인, 로그아웃, 내 정보 조회/수정
- 러닝 프로필 저장/조회, 러닝 기록 CRUD, 러닝 세션 CRUD 및 참여/취소
- 음악 조회·검색·정렬, 음악 상세, 음악 리뷰 CRUD
- 내부 플레이리스트 CRUD, 수록곡 추가/정렬/삭제, 저장한 플레이리스트 조회
- 케이던스·심박·운동 강도·BPM·장르 기반 추천 및 추천 결과 저장
- Spotify 곡 검색, 선택 곡 DB 저장, OAuth 연결, 내 Spotify 플레이리스트 조회/가져오기, 내부 플레이리스트 Spotify 내보내기
- AI 러닝 리포트 API와 카드 UI
- Docker Compose 기반 MySQL/Backend/Frontend 실행 구성

### 제외 범위

- Spring Security/JWT 기반 인증
- 확인된 관리자 권한 모델 및 역할 기반 접근 제어
- Swagger/OpenAPI 자동 문서
- Vue 전용 러닝 프로필 편집 화면
- Spotify 권한 철회 API 호출
- 실시간 운동 센서 연동 또는 의료적 운동 처방

### 외부 시스템 의존성

- Spotify Accounts API: OAuth 인증, 사용자 동의, scope 승인
- Spotify Web API: 트랙 검색, 사용자 플레이리스트 조회, 플레이리스트 생성/수록곡 추가
- OpenAI 호환 API: AI 러닝 리포트 및 추천 문장 생성 기능이 활성화될 경우 사용
- MySQL 8.x: CHECK 제약조건, FK, 인덱스가 전제된 스키마

### 기술적 제약

- Java 25 런타임이 필요하다.
- 기본 로컬 설정은 `spring.sql.init.mode=never`이므로 Docker 외 환경에서는 DB 스키마를 별도로 준비해야 한다.
- JSP와 Vue가 혼합되어 있으며 로그인/회원/러닝 프로필 일부는 JSP 흐름을 사용한다.
- Spotify와 AI 기능은 환경 변수와 외부 계정/키 설정이 없으면 실제 외부 호출을 완료할 수 없다.

## 3. 사용자 및 권한

### 비회원

- 공개 음악 목록과 음악 상세를 조회할 수 있다.
- 공개 또는 공식 플레이리스트를 조회할 수 있다.
- 회원가입 및 로그인 화면에 접근할 수 있다.
- 인증이 필요한 Vue 보호 경로 접근 시 로그인 필요 화면 또는 JSP 로그인으로 이동한다.

### 일반 회원

- 세션 기반 로그인 후 내 정보, 러닝 프로필, 러닝 기록, 러닝 세션, 리뷰, 플레이리스트, 추천 기능을 사용할 수 있다.
- 본인이 작성한 리뷰, 러닝 기록, 러닝 세션, 플레이리스트에 대해 수정/삭제 권한을 가진다.
- 공개/공식 플레이리스트를 저장 목록에 추가하거나 취소할 수 있다.

### Spotify 연동 회원

- 일반 회원 권한에 더해 Spotify OAuth 연결 상태를 가진다.
- Spotify 트랙 검색, Spotify 플레이리스트 조회/가져오기, 내부 플레이리스트 Spotify 내보내기를 수행할 수 있다.
- Access Token, Refresh Token, Authorization Code, Client Secret은 브라우저와 API 응답에 노출하지 않아야 한다.

### 관리자 역할

현재 `members` 테이블과 인증 코드에서 역할/권한 모델이 확인되지 않았다. 따라서 관리자 Actor는 본 요구사항 정의서의 공식 Actor로 포함하지 않는다. 단, `/api/music/internal` 계열 API는 존재하므로 권한 정책은 `UNVERIFIED` 위험 항목으로 추적한다.

## 4. 기능 요구사항

| ID | 기능명 | 상세 설명 | Actor | 우선순위 | 현재 상태 | 구현 근거 | 인수 조건 |
|---|---|---|---|---|---|---|---|
| FR-001 | 회원가입 | 사용자는 로그인 ID, 비밀번호, 이메일, 닉네임으로 회원 계정을 생성한다. 목표는 JSP와 API 양쪽에서 가입을 지원하는 것이다. | 비회원 | High | IMPLEMENTED | `MemberApiController.POST /api/members`, `MemberViewController.GET/POST /join`, `MemberService.register`, `MemberMapper`, `members` | 중복 로그인 ID/이메일은 실패하고, 정상 가입 시 `members`에 ACTIVE 회원이 생성된다. |
| FR-002 | 로그인 | 회원은 로그인 ID와 비밀번호로 세션 로그인을 수행한다. | 비회원 | High | IMPLEMENTED | `AuthApiController.POST /api/auth/login`, `MemberViewController.POST /login`, `AuthService.login`, `SessionConst.LOGIN_MEMBER_ID` | 올바른 인증 정보면 세션에 로그인 회원 ID가 저장되고, 실패 시 `INVALID_LOGIN` 또는 오류 메시지가 반환된다. |
| FR-003 | 로그아웃 | 로그인 회원은 현재 세션을 종료한다. | 일반 회원 | High | IMPLEMENTED | `AuthApiController.POST /api/auth/logout`, `MemberViewController.POST /logout`, `AuthService.logout` | 로그아웃 후 보호 API/화면 접근 시 인증 필요 상태가 된다. |
| FR-004 | 회원 정보 조회 및 수정 | 로그인 회원은 내 정보를 조회하고 JSP 마이페이지에서 식별 정보를 수정한다. | 일반 회원 | High | IMPLEMENTED | `MemberApiController.GET /api/members/me`, `MemberViewController.GET /members/me`, `POST /members/me/identity`, `MemberService.getMyInfo`, `updateIdentity` | 내 정보 응답에는 비밀번호 해시가 포함되지 않으며, 수정 시 중복 이메일 등 검증이 수행된다. |
| FR-005 | 러닝 프로필 | 회원은 나이, 성별, 목표 BPM/케이던스 등 러닝 프로필을 저장·조회하고 자동 목표값을 활용한다. 목표는 프로필 설정 흐름을 제공하는 것이다. | 일반 회원 | High | PARTIAL | `RunningProfileApiController.GET/PUT /api/members/me/running-profile`, `RunningProfileViewController.GET/POST /members/me/running-profile`, `runningProfileApi.js`, `running_profiles` | 발표 및 현재 설계 기준에서는 JSP `/members/me/running-profile` 화면을 실제 구현 화면으로 사용한다. Vue Router 전용 프로필 편집 화면은 새로 구현하지 않고 후속 SPA 통합 과제로 분리한다. |
| FR-006 | 러닝 기록 CRUD | 회원은 자신의 러닝 날짜, 거리, 시간, 페이스, 평균 케이던스, 평균 심박, 메모를 등록·조회·수정·삭제한다. | 일반 회원 | High | IMPLEMENTED | `RunningRecordApiController`, `RunningRecordService.createRecord/getMyRecordList/getMyRecordDetail/updateMyRecord/deleteMyRecord`, `RunningRecordMapper.xml`, `RunningRecord*View.vue`, `running_records` | 본인 기록만 조회/수정/삭제 가능하며 삭제는 `deleted_at` 기반 soft delete로 처리된다. |
| FR-007 | 러닝 세션 | 회원은 러닝 모임을 만들고, 목록/상세 조회, 수정, 상태 변경, 취소, 참여/참여 취소를 수행한다. | 일반 회원 | High | IMPLEMENTED | `RunningSessionApiController`, `RunningSessionParticipantApiController`, `RunningSessionService`, `RunningSessionParticipantService`, `RunningSession*View.vue`, `running_sessions`, `running_session_participants` | 작성자 권한, 모집 상태, 정원, 중복 참여, 닫힌 세션 참여 제한이 적용된다. |
| FR-008 | 음악 조회·검색·정렬 | 사용자는 음악 목록을 검색하고 BPM, 장르, 정렬 조건 등을 이용해 조회한다. | 비회원, 일반 회원 | High | IMPLEMENTED | `MusicApiController.GET /api/music`, `MusicService`, `MusicMapper.xml`, `MusicListView.vue`, `musicApi.js`, `music` | 검색 조건이 유효하면 페이지 응답으로 음악 목록과 총 개수가 반환된다. |
| FR-009 | 음악 상세 | 사용자는 음악 상세 정보와 리뷰/평균 점수를 조회한다. | 비회원, 일반 회원 | High | IMPLEMENTED | `MusicApiController.GET /api/music/{musicId}`, `MusicService.getMusicDetail`, `MusicDetailView.vue`, `music_reviews` | 존재하는 음악 ID는 상세와 리뷰 데이터를 반환하고, 없는 ID는 `MUSIC_NOT_FOUND`로 처리된다. |
| FR-010 | 음악 리뷰 CRUD | 회원은 음악에 선호도/러닝 적합도 점수와 코멘트를 등록, 수정, 삭제한다. | 일반 회원 | Medium | IMPLEMENTED | `MusicReviewApiController`, `MusicReviewService.create/update/delete/getReviewsByMusicId`, `MusicReviewMapper.xml`, `MusicReviewForm.vue`, `reviewApi.js`, `music_reviews` | 점수는 1~5 범위여야 하며, 작성자만 수정/삭제할 수 있다. |
| FR-011 | 내부 플레이리스트 CRUD | 회원은 서비스 내부 플레이리스트를 생성, 조회, 수정, 삭제한다. 공개/비공개와 수동/큐레이션 복사 구분을 유지한다. | 일반 회원 | High | IMPLEMENTED | `PlaylistApiController`, `PlaylistService.create/getList/getDetail/update/delete`, `PlaylistListView.vue`, `PlaylistDetailView.vue`, `PlaylistFormView.vue`, `playlists` | 소유자만 수정/삭제 가능하고, 비공개 플레이리스트는 접근 제한이 적용된다. |
| FR-012 | 플레이리스트 수록곡 관리 | 회원은 자신의 플레이리스트에 음악을 추가하고 순서를 변경하거나 삭제한다. | 일반 회원 | High | IMPLEMENTED | `PlaylistItemApiController`, `PlaylistItemService.add/updateOrder/reorder/delete`, `PlaylistItemList.vue`, `playlist_items` | 중복 곡/중복 순서가 검증되고, 수록곡 순서가 API 응답에 반영된다. |
| FR-013 | 플레이리스트 저장 | 회원은 공개 또는 공식 플레이리스트를 내 저장 목록에 추가하거나 취소한다. | 일반 회원 | Medium | IMPLEMENTED | `PlaylistSaveApiController`, `PlaylistSaveService`, `SavedPlaylistView.vue`, `playlistSaveApi.js`, `playlist_saves` | 저장 목록 조회가 가능하며 중복 저장과 권한 없는 저장은 오류 처리된다. |
| FR-014 | 자동 큐레이션 | 회원은 러닝 조건과 음악 조건을 기반으로 추천 목록을 생성하고 추천 로그를 남긴다. | 일반 회원 | High | IMPLEMENTED | `RecommendationApiController.GET /api/recommendations`, `RecommendationService.recommend`, `RecommendationLogMapper.xml`, `RecommendationView.vue`, `recommendation_logs` | 추천 조회만으로 플레이리스트가 생성되지 않으며, 추천 결과와 로그 ID가 반환된다. |
| FR-015 | 케이던스·심박·운동 강도 기반 추천 | 시스템은 러닝 프로필과 최근 기록의 케이던스/심박/강도 정보를 활용해 BPM 범위와 운동 계획/추천 음악을 계산한다. | 일반 회원 | High | IMPLEMENTED | `CadenceResolutionService`, `RunningIntensityResolutionService`, `RecommendationService`, `CadenceMatchingPolicy`, `IntensityMatchingPolicy`, `running_profiles`, `running_records` | 유효한 프로필과 데이터가 있으면 강도/케이던스/BPM 근거가 포함된 추천 결과를 반환한다. 데이터 부족 시 정의된 오류 또는 fallback 정책을 따른다. |
| FR-016 | 추천 결과 저장 | 회원은 추천 결과를 개인 플레이리스트로 저장한다. 저장 결과는 `USER + CURATION_COPY` 성격의 내부 플레이리스트로 관리한다. | 일반 회원 | High | IMPLEMENTED | `RecommendationApiController.POST /api/recommendations/{logId}/save`, `RecommendationService.saveRecommendation`, `RecommendationResult.vue`, `playlists`, `playlist_items`, `recommendation_logs.saved_playlist_id` | 저장 버튼 호출 시 플레이리스트와 수록곡이 생성되고 추천 로그에 저장 플레이리스트 ID가 연결된다. |
| FR-017 | Spotify 곡 검색 | 사용자는 Spotify Web API를 통해 곡을 검색하고 선택한 곡을 내부 음악 DB에 저장할 수 있다. | 비회원, 일반 회원 | High | IMPLEMENTED | `SpotifyApiController.GET /api/spotify/tracks/search`, `MusicApiController.POST /api/music/from-spotify`, `SpotifyService`, `SpotifyRestApiClient`, `spotifyApi.js`, `music.spotify_track_id` | 검색어가 비어 있으면 실패하고, 검색 limit은 최대 10으로 제한된다. 선택 저장 시 Spotify Track ID/URI가 유지된다. |
| FR-018 | Spotify OAuth | 회원은 Spotify 계정을 OAuth로 연결하고 연결 상태를 조회/해제한다. | Spotify 연동 회원 | High | UNVERIFIED | `SpotifyApiController.GET /api/spotify/oauth/authorize`, `GET /api/spotify/oauth/callback`, `GET/DELETE /api/spotify/me/connection`, `SpotifyOAuthService`, `SpotifyConnectionMapper.xml`, `spotify_connections` | 코드상 구현은 확인되지만 실제 OAuth는 Spotify 앱 설정, redirect URI, 사용자 동의가 필요하므로 수동 통합 검증 전까지 외부 성공을 확정하지 않는다. |
| FR-019 | Spotify 플레이리스트 내보내기 | 회원은 본인 내부 플레이리스트를 Spotify 계정의 플레이리스트로 생성하고 수록곡을 추가한다. | Spotify 연동 회원 | High | UNVERIFIED | `PlaylistExportApiController.POST /api/playlists/{playlistId}/export/spotify`, `PlaylistExportService`, `PlaylistService.saveSpotifyExport*`, `SpotifyExportPanel.vue`, `playlists.spotify_export_status` | 모든 수록곡에 Spotify URI가 있어야 하며, Spotify 연결과 playlist-modify scope가 필요하다. 실제 외부 생성/추가는 수동 검증이 필요하다. |
| FR-020 | Spotify 플레이리스트 가져오기 | 회원은 자신의 Spotify 플레이리스트 목록을 조회하고 내부 플레이리스트로 가져온다. | Spotify 연동 회원 | Medium | UNVERIFIED | `SpotifyApiController.GET /api/spotify/me/playlists`, `POST /api/spotify/playlists/import`, `SpotifyPlaylistImportService`, `spotifyApi.getMyPlaylists/importPlaylist` | 코드상 API와 클라이언트는 있으나 실제 Spotify 계정 권한, playlist-read-private scope, 원격 데이터 상태에 따른 수동 확인이 필요하다. |
| FR-021 | AI 러닝 리포트 | 회원은 최근 러닝 기록과 추천 로그를 요약한 AI 러닝 리포트를 조회·생성한다. | 일반 회원 | Medium | UNVERIFIED | `AiRunningReportApiController.GET /api/ai/running-reports/latest`, `POST /api/ai/running-reports`, `AiRunningReport*Service`, `OpenAiRunningReportProvider`, `AiRunningReportCard.vue`, `ai_running_reports` | 저장/생성 로직과 fallback 구조는 존재하지만 실제 외부 provider 호출은 환경 변수와 API Key가 필요하다. |
| FR-022 | Docker 기반 실행 | 개발/시연 환경은 Docker Compose로 MySQL, Backend, Frontend를 함께 기동한다. | 개발자, 운영자 | High | IMPLEMENTED | `compose.yaml`, `ssafit/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`, `/health`, `schema.sql`, `demo-snapshot.sql` | `docker compose up --build` 시 MySQL schema/demo seed, backend healthcheck, frontend healthcheck 구성이 사용된다. 단, 이 문서 작성 단계에서는 실제 기동 검증을 수행하지 않았다. |
| FR-023 | 공통 API 오류 응답 | API는 성공 응답 wrapper와 오류 코드/메시지를 구분해 클라이언트가 상태별 화면 처리를 할 수 있게 한다. | 비회원, 일반 회원, Spotify 연동 회원 | Medium | IMPLEMENTED | `ApiResponse`, `ErrorResponse`, `GlobalExceptionHandler`, `ErrorCode`, `PageStatePanel.vue`, `spotifyError.js`, `playlistError.js` | 인증 필요, 접근 거부, 없음, 충돌, 외부 API 실패가 구분 가능한 오류 코드로 응답된다. |
| FR-024 | 내부 음악 관리 권한 | 목표상 내부 음악 생성/수정 API는 일반 사용자와 구분된 관리 정책을 가져야 한다. | 일반 회원 | Low | UNVERIFIED | `MusicApiController.POST /api/music/internal`, `PUT /api/music/internal/{musicId}`, `MusicService`, `music` | API는 존재하지만 관리자 역할/권한 모델이 확인되지 않았다. 최종 요구사항에서는 관리자 기능으로 확정하지 않는다. |

## 5. 비기능 요구사항

| ID | 항목 | 목표 요구사항 | 현재 구현 상태 및 근거 |
| --- | --- | --- | --- |
| NFR-001 | 보안 | 비밀번호와 외부 API 비밀 값은 안전하게 저장/처리하고 응답에 노출하지 않는다. | `PasswordHashService`가 PBKDF2를 사용하며, Spotify 계약 문서와 DTO 정책상 token/secret을 응답하지 않는다. |
| NFR-002 | 인증 및 세션 | 사용자 인증은 서버 세션 기반으로 처리하고 Vue API 요청은 쿠키를 포함한다. | `AuthService`, `SessionConst.LOGIN_MEMBER_ID`, `SessionMemberResolver`, `frontend/src/api/http.js`의 Axios 설정 근거가 있다. |
| NFR-003 | 데이터 정합성 | DB FK, CHECK, UNIQUE, soft delete 정책으로 주요 도메인 상태를 보호한다. | `schema.sql`의 `members`, `playlists`, `running_records`, `running_sessions`, `ai_running_reports` 제약과 Mapper 조건이 확인된다. |
| NFR-004 | 오류 처리 | 도메인 오류와 외부 API 오류는 공통 오류 응답으로 반환한다. | `GlobalExceptionHandler`, `ErrorCode`, `ErrorResponse`가 존재한다. |
| NFR-005 | 성능 | 음악 검색과 추천은 인덱스와 페이지 응답을 활용해 목록 조회 비용을 제한한다. | `idx_music_genre`, `idx_music_bpm`, `idx_music_source_bpm`, `ft_music_keyword`, `PageResponse`가 확인된다. 실제 부하 테스트는 미확인이다. |
| NFR-006 | 테스트 가능성 | 서비스/컨트롤러/Mapper/E2E 테스트로 핵심 흐름을 검증 가능해야 한다. | 37개 백엔드 테스트 파일과 `frontend/e2e/running-features.spec.js`가 확인된다. 이번 단계에서 테스트 실행은 하지 않았다. |
| NFR-007 | 외부 API 장애 대응 | Spotify/AI API 실패 시 내부 오류 코드와 fallback 또는 실패 상태를 제공한다. | Spotify 오류 코드, `spotify_export_status`, AI report fallback/provider 구성이 확인된다. 실 외부 장애 주입 검증은 미확인이다. |
| NFR-008 | 운영 및 배포 | Docker Compose로 DB, Backend, Frontend를 함께 실행할 수 있어야 한다. | `compose.yaml`의 MySQL/Backend/Frontend 서비스와 healthcheck가 확인된다. |

## 6. 제약사항 및 가정

### Spotify API 정책 제약

- Spotify OAuth와 Web API 사용은 Spotify Developer Dashboard의 앱 설정, redirect URI 일치, scope 승인에 의존한다.
- 필요한 scope는 `user-read-private`, `playlist-read-private`, `playlist-modify-private`, `playlist-modify-public`이다.
- 검색 결과, 플레이리스트 조회, 플레이리스트 생성/수록곡 추가는 Spotify API 정책, 앱 모드, 사용자 권한, rate limit에 영향을 받는다.

### 사용자 Spotify 계정 권한

- 사용자가 OAuth 동의를 거부하면 연결은 실패한다.
- private playlist 조회/생성/수정은 승인된 scope와 계정 상태가 필요하다.
- Access Token 갱신 실패 또는 Refresh Token 부재 시 재인증이 필요하다.

### BPM, 장르, 오디오 피처 데이터 제공 제한 가능성

- Spotify 검색 결과가 항상 BPM, 장르, 오디오 피처를 제공한다고 가정할 수 없다.
- 현재 코드에는 `SPOTIFY_AUDIO_FEATURE_ENRICHMENT_ENABLED` 설정과 `MusicTempoProvider` 추상화가 있으나, 기본은 비활성 또는 no-op 경로일 수 있다.
- 추천은 DB에 저장된 BPM/장르/케이던스/심박 데이터 품질에 영향을 받는다.

### JSP/Vue 혼합 화면 구조

- 회원가입, 로그인, 마이페이지, 러닝 프로필은 JSP 흐름이 확인된다.
- 음악, 플레이리스트, 러닝 기록, 러닝 세션, 추천, Spotify OAuth 완료는 Vue Router 중심이다.
- Use Case Diagram과 화면 설계서에서는 JSP 화면과 Vue 화면을 같은 프론트엔드로 단순 병합하지 말고 분리해야 한다.

### 외부 API 응답 실패 가능성

- Spotify 검색/토큰/플레이리스트 생성/수록곡 추가 실패가 발생할 수 있으며, 실패 상태는 내부 오류 코드와 export 상태로 표현된다.
- AI provider는 설정이 없거나 실패할 수 있으며, fallback 또는 실패 상태 처리가 필요하다.

## 7. 요구사항 추적성

| Requirement ID | Controller | Service | Mapper | Vue Route / Component | JSP | Table |
| --- | --- | --- | --- | --- | --- | --- |
| FR-001 | `MemberApiController`, `MemberViewController` | `MemberService` | `MemberMapper` | - | `/join`, `member/join.jsp` | `members` |
| FR-002 | `AuthApiController`, `MemberViewController` | `AuthService` | `MemberMapper` | Vue auth guard | `/login`, `member/login.jsp` | `members` |
| FR-003 | `AuthApiController`, `MemberViewController` | `AuthService` | - | Vue auth state | `/logout` | - |
| FR-004 | `MemberApiController`, `MemberViewController` | `MemberService` | `MemberMapper` | `authApi.js`, `memberApi.js` | `/members/me`, `member/my-page.jsp` | `members` |
| FR-005 | `RunningProfileApiController`, `RunningProfileViewController` | `RunningProfileService`, `RunningFitnessMetricService` | `RunningProfileMapper` | `runningProfileApi.js` | `/members/me/running-profile`, `member/running-profile.jsp` | `running_profiles` |
| FR-006 | `RunningRecordApiController` | `RunningRecordService` | `RunningRecordMapper` | `/running-records`, `RunningRecord*View.vue` | - | `running_records` |
| FR-007 | `RunningSessionApiController`, `RunningSessionParticipantApiController` | `RunningSessionService`, `RunningSessionParticipantService` | `RunningSessionMapper`, `RunningSessionParticipantMapper` | `/running-sessions`, `RunningSession*View.vue` | - | `running_sessions`, `running_session_participants` |
| FR-008 | `MusicApiController` | `MusicService` | `MusicMapper` | `/music`, `MusicListView.vue` | - | `music` |
| FR-009 | `MusicApiController` | `MusicService`, `MusicReviewService` | `MusicMapper`, `MusicReviewMapper` | `/music/:musicId`, `MusicDetailView.vue` | - | `music`, `music_reviews` |
| FR-010 | `MusicReviewApiController` | `MusicReviewService` | `MusicReviewMapper` | `MusicReviewForm.vue`, `reviewApi.js` | - | `music_reviews` |
| FR-011 | `PlaylistApiController` | `PlaylistService` | `PlaylistMapper` | `/playlists`, `/playlists/:playlistId`, `Playlist*View.vue` | - | `playlists` |
| FR-012 | `PlaylistItemApiController` | `PlaylistItemService` | `PlaylistItemMapper` | `PlaylistItemList.vue`, `playlistApi.js` | - | `playlist_items` |
| FR-013 | `PlaylistSaveApiController` | `PlaylistSaveService` | `PlaylistSaveMapper` | `/playlists/saved`, `SavedPlaylistView.vue` | - | `playlist_saves` |
| FR-014 | `RecommendationApiController` | `RecommendationService` | `RecommendationLogMapper` | `/recommendations`, `RecommendationView.vue` | - | `recommendation_logs` |
| FR-015 | `RecommendationApiController` | `RecommendationService`, `CadenceResolutionService`, `RunningIntensityResolutionService` | `MusicMapper`, `RunningProfileMapper`, `RunningRecordMapper` | `RecommendationResult.vue` | - | `music`, `running_profiles`, `running_records`, `recommendation_logs` |
| FR-016 | `RecommendationApiController` | `RecommendationService`, `PlaylistService`, `PlaylistItemService` | `RecommendationLogMapper`, `PlaylistMapper`, `PlaylistItemMapper` | `RecommendationResult.vue` | - | `playlists`, `playlist_items`, `recommendation_logs` |
| FR-017 | `SpotifyApiController`, `MusicApiController` | `SpotifyService` | `MusicMapper` | `spotifyApi.js`, `MusicListView.vue` | - | `music` |
| FR-018 | `SpotifyApiController`, `SpotifyLoginViewController` | `SpotifyOAuthService` | `SpotifyConnectionMapper` | `/oauth/spotify/complete`, `/login/spotify`, `SpotifyOAuthCompleteView.vue` | `/login/spotify` | `spotify_connections` |
| FR-019 | `PlaylistExportApiController` | `PlaylistExportService`, `PlaylistService` | `PlaylistMapper`, `PlaylistItemMapper`, `SpotifyConnectionMapper` | `SpotifyExportPanel.vue`, `spotifyApi.js` | - | `playlists`, `playlist_items`, `spotify_connections` |
| FR-020 | `SpotifyApiController` | `SpotifyOAuthService`, `SpotifyPlaylistImportService` | `SpotifyConnectionMapper`, `PlaylistMapper`, `PlaylistItemMapper`, `MusicMapper` | `spotifyApi.js`, playlist views | - | `spotify_connections`, `playlists`, `playlist_items`, `music` |
| FR-021 | `AiRunningReportApiController` | `AiRunningReportAggregationService`, `AiRunningReportGenerationService`, `AiRunningReportStorageService` | `AiRunningReportMapper` | `AiRunningReportCard.vue` | - | `ai_running_reports`, `running_records`, `recommendation_logs` |
| FR-022 | `HealthController` | - | - | Docker frontend/nginx | - | MySQL schema init |
| FR-023 | 모든 API Controller | 도메인 Service | - | `PageStatePanel.vue`, error utils | - | - |
| FR-024 | `MusicApiController` | `MusicService` | `MusicMapper` | `MusicFormView.vue` | - | `music` |

## 8. 요약

- 요구사항 총 개수: 24개
- 구현 완료: 18개
- 부분 구현: 1개
- 미구현: 0개
- 미확인: 5개

### 가장 위험한 요구사항 불일치 5개

1. `FR-018 Spotify OAuth`: 코드 구현은 있으나 실제 Spotify 앱 설정, redirect URI, 사용자 동의가 없으면 성공을 확정할 수 없다.
2. `FR-019 Spotify 플레이리스트 내보내기`: 내부 API/상태 저장은 있으나 실제 Spotify 생성/수록곡 추가는 외부 정책과 권한에 의존한다.
3. `FR-005 러닝 프로필`: API와 JSP 화면은 있으나 Vue 전용 화면은 없다. 발표 기준 구현 화면은 JSP로 확정하고 Vue 전환은 후속 SPA 통합 과제로 분리한다.
4. `FR-024 내부 음악 관리 권한`: API는 있으나 관리자 역할 모델이 확인되지 않아 관리자 기능으로 문서화하면 안 된다.
5. `FR-021 AI 러닝 리포트`: 코드와 DB는 있으나 provider 실 호출은 설정과 API Key에 의존한다.

### 다음 Use Case Diagram 단계에서 분리해야 할 기능 모듈

- 회원/JSP 인증 모듈
- 러닝 프로필/JSP 설정 모듈
- Vue 음악·리뷰 모듈
- Vue 플레이리스트·수록곡·저장 모듈
- 러닝 기록·러닝 세션 모듈
- 추천·자동 큐레이션·AI 러닝 리포트 모듈
- Spotify 검색/OAuth/가져오기/내보내기 모듈
- Docker/운영 환경 모듈
