# SSAFIT PlayList 기능 흐름·테스트·발표 증빙 매트릭스

이 문서는 `docs/final/00-project-audit.md`의 전수 점검 결과를 바탕으로, 최종 발표와 최종 보고서에 사용할 사용자 흐름, 코드 근거, 검증 근거, 캡처 지점을 정리한다. 코드는 수정하지 않았고 현재 구현과 통과된 환경/테스트 결과를 기준으로 작성한다.

## 1. 핵심 사용자 시나리오

### 시나리오 A. 기본 회원 흐름

1. 사용자는 `/join` JSP 화면에서 회원가입한다.
2. `/login` JSP 화면에서 로그인한다.
3. 로그인 성공 후 `/members/me` 또는 Vue 화면으로 이동한다.
4. Vue 라우터는 보호 화면 진입 시 `/api/members/me`로 세션을 확인한다.
5. 로그아웃은 JSP `/logout` 또는 REST `/api/auth/logout`으로 세션을 종료한다.

핵심 근거:

- Frontend/JSP: `ssafit/src/main/webapp/WEB-INF/views/member/join.jsp`, `login.jsp`, `my-page.jsp`
- Backend: `MemberViewController`, `MemberApiController`, `AuthApiController`, `AuthService`
- Session: `SessionConst.LOGIN_MEMBER_ID`, `SessionMemberResolver`

### 시나리오 B. 음악 탐색과 리뷰

1. 사용자는 `/music`에서 음악 목록을 검색/정렬한다.
2. `/music/:musicId`에서 상세 정보를 확인한다.
3. 로그인 사용자는 상세 화면에서 리뷰를 등록, 수정, 삭제한다.
4. 리뷰 평균과 개별 리뷰는 `music_reviews` 테이블에 저장된다.

핵심 근거:

- Frontend: `MusicListView.vue`, `MusicDetailView.vue`, `musicApi.js`, `reviewApi.js`
- Backend: `MusicApiController`, `MusicReviewApiController`
- Service/Mapper: `MusicService`, `MusicReviewService`, `MusicMapper.xml`, `MusicReviewMapper.xml`

### 시나리오 C. 플레이리스트 작성과 Spotify 내보내기

1. 사용자는 `/playlists/new`에서 플레이리스트를 생성한다.
2. `/playlists/:playlistId`에서 수록곡을 추가, 삭제, 순서 변경한다.
3. Spotify 연결이 되어 있고 수록곡에 Spotify URI가 있으면 Spotify export를 실행한다.
4. export 결과는 `spotify_export_status`, `spotify_playlist_id`, `spotify_playlist_url`로 저장된다.

핵심 근거:

- Frontend: `PlaylistFormView.vue`, `PlaylistDetailView.vue`, `SpotifyExportPanel.vue`, `playlistApi.js`, `spotifyApi.js`
- Backend: `PlaylistApiController`, `PlaylistItemApiController`, `PlaylistExportApiController`
- Service/Mapper: `PlaylistService`, `PlaylistItemService`, `PlaylistExportService`, `PlaylistMapper.xml`, `PlaylistItemMapper.xml`

### 시나리오 D. 러닝 기반 추천과 AI 리포트

1. 사용자는 `/members/me/running-profile` 또는 `/running-records`에서 러닝 기반 데이터를 준비한다.
2. `/recommendations`에서 강도, 목표 거리, 케이던스, 장르 조건으로 추천을 요청한다.
3. 추천 결과와 추천 로그가 저장된다.
4. 추천 결과를 플레이리스트로 저장할 수 있다.
5. AI 러닝 리포트는 `/api/ai/running-reports`에서 생성하고 `/api/ai/running-reports/latest`로 조회한다.

핵심 근거:

- Frontend: `RecommendationView.vue`, `AiRunningReportCard.vue`, `recommendationApi.js`, `aiRunningReportApi.js`
- Backend: `RecommendationApiController`, `AiRunningReportApiController`
- Service/Mapper: `RecommendationService`, `AiRunningReportGenerationService`, `OpenAiRunningReportProvider`, `RecommendationLogMapper.xml`, `AiRunningReportMapper.xml`

## 2. 기능별 증빙 매트릭스

| 기능 | 사용자 진입 화면/URL | 프론트엔드 근거 | 백엔드 API | 핵심 Service | DB 테이블/Mapper | 테스트 또는 실행 근거 | 상태 | 발표 캡처 추천 |
| -- | ------------- | -------- | ------- | ---------- | ------------- | ------------ | -- | -------- |
| 회원가입 | `/join`, REST 대안 `/api/members` | JSP `member/join.jsp`, `memberApi.join` | `POST /join`, `POST /api/members` | `MemberService.register` | `members`, `MemberMapper.xml` | `MemberServiceTest`, `MemberViewControllerTest`, `MapperIntegrationTest` | 구현 완료 | 가입 폼, 성공 후 로그인 화면 |
| 로그인 | `/login`, REST `/api/auth/login` | JSP `member/login.jsp`, `authApi.login` | `POST /login`, `POST /api/auth/login` | `AuthService.login` | `members`, `MemberMapper.xml` | `AuthServiceTest`, `MemberViewControllerTest`, E2E login helper | 구현 완료 | 로그인 폼, 로그인 후 `/members/me` |
| 로그아웃 | JSP form `/logout`, REST `/api/auth/logout` | `authApi.logout`, `AppLayout.vue` | `POST /logout`, `POST /api/auth/logout` | `AuthService.logout` | session invalidation | `AuthServiceTest`, `MemberViewControllerTest` | 구현 완료 | 로그아웃 버튼, 로그인 화면 복귀 |
| 회원 정보 조회/마이페이지 | `/members/me`, REST `/api/members/me` | JSP `member/my-page.jsp`, `authApi.getSession`, `memberApi.getMe` | `GET /members/me`, `GET /api/members/me` | `MemberService.getMyInfo` | `members`, `running_profiles` | `MemberViewControllerTest`, `RunningProfileViewControllerTest` | 구현 완료 | 마이페이지, 프로필 등록 유도 |
| 음악 목록 조회 | `/music` | `MusicListView.vue`, `MusicSearchBar.vue`, `musicApi.getList` | `GET /api/music` | `MusicService.getMusicList` | `music`, `MusicMapper.selectMusicList` | `MusicServiceTest`, `MusicApiControllerTest`, `MapperIntegrationTest` | 구현 완료 | 검색어/정렬/목록 카드 |
| 음악 상세 조회 | `/music/:musicId` | `MusicDetailView.vue`, `MusicCard.vue`, `musicApi.getDetail` | `GET /api/music/{musicId}` | `MusicService.getMusicDetail` | `music`, `MusicMapper.selectMusicDetail` | `MusicServiceTest`, `MusicApiControllerTest` | 구현 완료 | 상세 정보, 리뷰 영역 |
| 음악 리뷰 등록/수정/삭제 | `/music/:musicId` 상세 화면 | `MusicReviewForm.vue`, `reviewApi.create/update/remove` | `POST /api/music/{musicId}/reviews`, `PUT /api/music-reviews/{reviewId}`, `DELETE /api/music-reviews/{reviewId}` | `MusicReviewService` | `music_reviews`, `MusicReviewMapper.xml` | `MusicReviewServiceTest`, `MapperIntegrationTest` | 구현 완료 | 리뷰 작성 전/후, 수정/삭제 버튼 |
| 플레이리스트 생성 | `/playlists/new` | `PlaylistFormView.vue`, `playlistApi.create` | `POST /api/playlists` | `PlaylistService.create` | `playlists`, `PlaylistMapper.insert` | `PlaylistServiceTest`, `MapperIntegrationTest` | 구현 완료 | 생성 폼, 생성 후 상세 |
| 플레이리스트 수록곡 추가/삭제 | `/playlists/:playlistId` | `PlaylistDetailView.vue`, `PlaylistItemList.vue`, `playlistApi.addItem/removeItem` | `POST /api/playlists/{playlistId}/items`, `DELETE /api/playlist-items/{itemId}` | `PlaylistItemService.add/delete` | `playlist_items`, `PlaylistItemMapper.xml` | `PlaylistItemServiceTest`, `MapperIntegrationTest` | 구현 완료 | 수록곡 목록 변화 |
| 플레이리스트 순서 변경 | `/playlists/:playlistId` | `PlaylistItemList.vue`, `playlistApi.updateItemOrder/reorderItems` | `PATCH /api/playlist-items/{itemId}/order`, `PATCH /api/playlists/{playlistId}/items/order` | `PlaylistItemService.updateOrder/reorder` | `playlist_items`, `PlaylistItemMapper.updateOrder/bulkUpdateOrders` | `PlaylistItemServiceTest` | 구현 완료 | 순서 변경 UI 또는 API 결과 |
| 자동 큐레이션/러닝 조건 기반 추천 | `/recommendations` | `RecommendationView.vue`, `RecommendationResult.vue`, `recommendationApi.recommend` | `GET /api/recommendations` | `RecommendationService.recommend` | `recommendation_logs`, `music`, `running_profiles`, `running_records` | `RecommendationServiceTest`, `CadenceMatchingPolicyTest`, `IntensityMatchingPolicyTest` | 구현 완료 | 조건 입력, 추천 결과, 추천 사유 |
| 추천 결과 플레이리스트 저장 | `/recommendations` | `RecommendationView.vue`, `recommendationApi.save` | `POST /api/recommendations/{logId}/save` | `RecommendationService.saveRecommendation` | `playlists`, `playlist_items`, `recommendation_logs` | `RecommendationServiceTest`, `MapperIntegrationTest` | 구현 완료 | 추천 결과 저장 버튼, 생성된 플레이리스트 |
| 러닝 프로필 등록/수정 | `/members/me/running-profile`, REST `/api/members/me/running-profile` | JSP `member/running-profile.jsp`, `runningProfileApi.save` | `POST /members/me/running-profile`, `PUT /api/members/me/running-profile` | `RunningProfileService.saveOrUpdate` | `running_profiles`, `RunningProfileMapper.xml` | `RunningProfileServiceTest`, `RunningProfileViewControllerTest` | 구현 완료 | 프로필 폼, 자동 목표값 |
| 러닝 기록 저장 | `/running-records/new` | `RunningRecordFormView.vue`, `runningRecordApi.create` | `POST /api/running-records` | `RunningRecordService.createRecord` | `running_records`, `RunningRecordMapper.xml` | `RunningRecordServiceTest`, `RunningRecordApiControllerTest`, Playwright E2E | 구현 완료 | 기록 등록 폼, 목록 반영 |
| 러닝 세션 저장 | `/running-sessions/new` | `RunningSessionFormView.vue`, `runningSessionApi.create` | `POST /api/running-sessions` | `RunningSessionService.createSession` | `running_sessions`, `RunningSessionMapper.xml` | `RunningSessionServiceTest`, `RunningSessionApiControllerTest`, Playwright E2E | 구현 완료 | 모임 생성 폼, 상세 화면 |
| 러닝 세션 참여/취소 | `/running-sessions/:id` | `RunningSessionDetailView.vue`, `runningSessionApi.join/leave` | `POST /api/running-sessions/{id}/participants/me`, `DELETE /api/running-sessions/{id}/participants/me` | `RunningSessionParticipantService` | `running_session_participants`, `RunningSessionParticipantMapper.xml` | `RunningSessionParticipantServiceTest`, E2E 정원/중복 검증 | 구현 완료 | 참여 버튼, 정원 초과 메시지 |
| Spotify 음악 검색 | `/music` 또는 `/recommendations` 내 검색/가져오기 흐름 | `spotifyApi.searchTracks`, 관련 검색 UI | `GET /api/spotify/tracks/search` | `SpotifyService.searchTracks` | 검색 자체는 외부 API, 저장 시 `music` | `SpotifyServiceTest`, `SpotifyApiControllerTest`, 수동 API 검증 | 부분 구현 | Spotify 결과 카드, 앨범 이미지 |
| Spotify OAuth 연결 | `/login/spotify`, `/api/spotify/oauth/authorize`, `/oauth/spotify/complete` | `SpotifyOAuthCompleteView.vue`, `spotifyApi.startAuthorization` | `GET /api/spotify/oauth/authorize`, `GET /api/spotify/oauth/callback`, `GET /api/spotify/me/connection` | `SpotifyOAuthService` | `spotify_connections`, `SpotifyConnectionMapper.xml` | `SpotifyOAuthServiceTest`, `SpotifyApiControllerTest`, 수동 OAuth 검증 | 구현 완료 | 연결 시작, 완료 화면, 연결 상태 |
| Spotify 플레이리스트 내보내기 | `/playlists/:playlistId` | `SpotifyExportPanel.vue`, `playlistApi.exportToSpotify`, `spotifyApi.exportPlaylist` | `POST /api/playlists/{playlistId}/export/spotify` | `PlaylistExportService.exportWithResult` | `playlists`, `playlist_items`, `spotify_connections` | `PlaylistExportServiceTest`, 수동 Spotify export 검증 | 구현 완료 | export 버튼, 성공 URL/상태 |
| 생성형 AI 러닝 리포트 | `/recommendations` 또는 리포트 카드 영역 | `AiRunningReportCard.vue`, `aiRunningReportApi.generate/getLatest` | `POST /api/ai/running-reports`, `GET /api/ai/running-reports/latest` | `AiRunningReportGenerationService`, `OpenAiRunningReportProvider` | `ai_running_reports`, `AiRunningReportMapper.xml` | `AiRunningReportGenerationServiceTest`, `AiRunningReportApiControllerTest`, `AiRunningReportMapperIntegrationTest` | 구현 완료 | 리포트 생성 전/후, generation source |

## 3. 실제 실행 가능한 데모 시나리오

### 데모 1. 로그인 후 음악·리뷰·플레이리스트

1. `http://127.0.0.1:5173/login` 접속
2. 데모 계정으로 로그인
3. `/music`에서 음악 목록 조회 및 검색
4. 음악 상세로 이동해 리뷰 등록
5. `/playlists/new`에서 플레이리스트 생성
6. 플레이리스트 상세에서 수록곡 추가

증빙 포인트:

- 세션 인증: `/api/members/me`
- 음악 조회: `/api/music`
- 리뷰 저장: `/api/music/{musicId}/reviews`
- 플레이리스트 저장: `/api/playlists`, `/api/playlists/{playlistId}/items`

### 데모 2. 러닝 데이터 기반 추천 저장

1. `/members/me/running-profile`에서 러닝 프로필 등록 또는 확인
2. `/running-records/new`에서 거리, 시간, 케이던스, 심박 기록 저장
3. `/recommendations`에서 강도, 목표 거리, 장르 입력
4. 추천 결과와 추천 사유 확인
5. 추천 결과를 플레이리스트로 저장

증빙 포인트:

- 러닝 기록 저장 후 자동 페이스 계산
- 추천 결과에 BPM, 케이던스, 강도, 선호 장르 기반 사유 표시
- 추천 로그와 저장된 플레이리스트 연결

### 데모 3. Spotify 검색·연결·내보내기

1. Spotify 환경 변수가 설정된 상태에서 `/music` 또는 Spotify 검색 UI 진입
2. 검색어 입력 후 Spotify 결과 확인
3. 필요한 경우 `/api/spotify/oauth/authorize` 또는 `/login/spotify`로 Spotify 연결
4. Spotify URI가 있는 곡들로 구성된 플레이리스트 상세 진입
5. Spotify 내보내기 실행
6. export 성공 상태와 Spotify playlist URL 확인

증빙 포인트:

- 외부 API 검색 결과의 앨범 이미지/아티스트/트랙명
- OAuth 완료 화면과 연결 상태
- export 결과의 `SUCCESS` 또는 `PARTIAL` 상태

### 데모 4. 생성형 AI 러닝 리포트

1. 러닝 기록과 추천 로그가 있는 계정으로 로그인
2. AI 러닝 리포트 카드 영역에서 최신 리포트 조회
3. 리포트 생성 버튼 실행
4. 리포트 요약, 페이스/강도 인사이트, 케이던스/심박 인사이트, 다음 러닝 목표 확인
5. 환경 설정에 따라 `LLM` 또는 `FALLBACK` source를 설명

증빙 포인트:

- API: `POST /api/ai/running-reports`
- 저장 테이블: `ai_running_reports`
- 실제 LLM 호출은 `AI_RUNNING_REPORT_ENABLED=true`, provider `openai`, API key 설정 시 발생

## 4. 부분 구현 또는 대체 구현 기능

| 항목 | 실제 상태 | 발표/보고서 표현 |
| --- | --- | --- |
| 음악 삭제 | 음악 삭제 API가 확인되지 않음 | “음악 조회, 검색, 등록, 수정, Spotify 저장 구현. 삭제는 제외” |
| Spotify BPM/장르 기반 검색 | Spotify Search 자체는 검색어 기반. tempo/genre는 별도 조회 시도 후 실패 시 null | “Spotify 검색 결과를 가져오고, 가능한 경우 tempo/genre 보강 후 로컬 추천 점수에 활용” |
| Spotify audio features | `/audio-features/{id}` 호출 코드는 있으나 정책/권한/API 실패 시 null 처리 | “외부 API 제한으로 오디오 특성은 선택적 보강 또는 대체 구현” |
| MusicTempoProvider | 기본 구현은 `NoOpMusicTempoProvider`, enrichment 기본값 false | “확장 가능한 provider 구조는 있으나 기본은 no-op” |
| AI 러닝 리포트 | 실제 OpenAI-compatible 호출 구현 있음. 기본 환경은 disabled/provider none | “설정 시 실 API 호출, 미설정/실패 시 fallback 리포트” |
| AI 운동 계획 추천 | provider 있으면 LLM 사용, 없으면 `fallbackWorkoutPlan` 사용 | “LLM 보조 + 서버 계산 fallback 구조” |
| Spotify 실 API 자동 테스트 | 단위/컨트롤러 테스트는 mock 중심, 실 API는 수동 환경 의존 | “수동 검증 및 환경 변수 기반 검증 필요” |
| Vue/JSP 혼재 | 로그인/회원가입/마이페이지 일부는 JSP, 주요 기능은 Vue | “JSP 인증 화면과 Vue 기능 화면을 Nginx proxy/returnTo로 연결” |

## 5. 발표 화면 캡처 목록

### 캡처 1. 회원가입 화면

- 목적: 기본 회원 기능과 JSP 기반 인증 진입점 증빙
- 화면 경로: `/join`
- 사전 조건: 비로그인 상태
- 반드시 보여야 하는 요소:
  - 로그인 ID 입력
  - 이메일 입력
  - 닉네임 입력
  - 비밀번호 입력
  - 가입 버튼
- 연결 코드:
  - Frontend/JSP: `ssafit/src/main/webapp/WEB-INF/views/member/join.jsp`
  - Backend: `MemberViewController.join`, `MemberService.register`
- 유의 사항:
  - Docker에서는 `http://127.0.0.1:5173/join`으로 접근하는 것이 프록시 흐름 증빙에 좋다.

### 캡처 2. 로그인 및 마이페이지

- 목적: 세션 기반 인증과 회원 정보 조회 증빙
- 화면 경로: `/login`, `/members/me`
- 사전 조건: 가입된 계정
- 반드시 보여야 하는 요소:
  - 로그인 폼
  - 로그인 후 사용자 닉네임 또는 계정 정보
  - 러닝 프로필 등록 상태
- 연결 코드:
  - Frontend/JSP: `login.jsp`, `my-page.jsp`
  - Backend: `MemberViewController.login`, `AuthService.login`, `MemberService.getMyInfo`
- 유의 사항:
  - Vue 보호 라우트 진입 시 `/api/members/me` 세션 확인도 함께 설명할 수 있다.

### 캡처 3. 음악 목록 및 검색 결과 화면

- 목적: 음악 목록 조회, 검색, 정렬 기능 증빙
- 화면 경로: `/music`
- 사전 조건: demo snapshot 또는 음악 데이터 존재
- 반드시 보여야 하는 요소:
  - 검색어 입력
  - 장르/BPM/정렬 조건
  - 음악 카드 목록
  - 제목, 아티스트, BPM 또는 장르
- 연결 코드:
  - Frontend: `MusicListView.vue`, `MusicSearchBar.vue`, `musicApi.getList`
  - Backend: `MusicApiController.getMusicList`, `MusicService.getMusicList`, `MusicMapper.selectMusicList`
- 유의 사항:
  - Spotify 데이터가 아닌 seed/admin 데이터인 경우 내부 데이터임을 설명한다.

### 캡처 4. 음악 상세와 리뷰 작성

- 목적: 음악 상세 조회와 사용자 리뷰 CRUD 증빙
- 화면 경로: `/music/:musicId`
- 사전 조건: 로그인 상태, 음악 데이터 존재
- 반드시 보여야 하는 요소:
  - 음악 상세 정보
  - 리뷰 입력 폼
  - 선호 점수/러닝 적합도 점수
  - 등록된 리뷰
  - 수정/삭제 버튼
- 연결 코드:
  - Frontend: `MusicDetailView.vue`, `MusicReviewForm.vue`, `reviewApi.js`
  - Backend: `MusicReviewApiController`, `MusicReviewService`, `MusicReviewMapper.xml`
- 유의 사항:
  - 한 회원은 같은 음악에 하나의 리뷰만 작성 가능하다.

### 캡처 5. 플레이리스트 생성 화면

- 목적: 사용자 플레이리스트 CRUD 증빙
- 화면 경로: `/playlists/new`
- 사전 조건: 로그인 상태
- 반드시 보여야 하는 요소:
  - 제목
  - 공개/비공개
  - 설명
  - 저장 버튼
- 연결 코드:
  - Frontend: `PlaylistFormView.vue`, `playlistApi.create`
  - Backend: `PlaylistApiController.create`, `PlaylistService.create`, `PlaylistMapper.insert`
- 유의 사항:
  - 생성 후 `/playlists/:playlistId` 상세로 이동하는 흐름을 함께 캡처하면 좋다.

### 캡처 6. 플레이리스트 수록곡 관리

- 목적: 수록곡 추가/삭제/순서 관리 증빙
- 화면 경로: `/playlists/:playlistId`
- 사전 조건: 로그인 상태, 소유한 플레이리스트와 음악 데이터 존재
- 반드시 보여야 하는 요소:
  - 수록곡 목록
  - 추가된 음악
  - 삭제 버튼
  - 순서 정보
- 연결 코드:
  - Frontend: `PlaylistDetailView.vue`, `PlaylistItemList.vue`, `playlistApi.addItem/removeItem/reorderItems`
  - Backend: `PlaylistItemApiController`, `PlaylistItemService`, `PlaylistItemMapper.xml`
- 유의 사항:
  - 같은 플레이리스트에 같은 곡 중복 추가는 제한된다.

### 캡처 7. 러닝 추천 결과 화면

- 목적: 러닝 조건 기반 자동 큐레이션 증빙
- 화면 경로: `/recommendations`
- 사전 조건: 로그인 상태, 러닝 프로필/음악 데이터 존재
- 반드시 보여야 하는 요소:
  - 러닝 강도
  - 목표 거리 또는 케이던스
  - 장르 조건
  - 추천 음악 목록
  - 추천 사유
  - 저장 버튼
- 연결 코드:
  - Frontend: `RecommendationView.vue`, `RecommendationResult.vue`, `recommendationApi.recommend`
  - Backend: `RecommendationApiController`, `RecommendationService.recommend`, `RecommendationLogMapper.xml`
- 유의 사항:
  - “AI 추천”이라고 단정하기보다 러닝 조건과 서버 점수화 기반 추천이며, 목표 거리 운동 계획은 LLM provider 설정 시 보조된다고 설명한다.

### 캡처 8. 러닝 프로필 등록/수정 화면

- 목적: 러닝 개인화 데이터 입력 증빙
- 화면 경로: `/members/me/running-profile`
- 사전 조건: 로그인 상태
- 반드시 보여야 하는 요소:
  - 나이/성별
  - 선호 장르
  - 목표 심박 또는 케이던스 관련 정보
  - 저장 버튼
- 연결 코드:
  - Frontend/JSP: `member/running-profile.jsp`, `runningProfileApi.js`
  - Backend: `RunningProfileViewController`, `RunningProfileApiController`, `RunningProfileService`
- 유의 사항:
  - 러닝 기록 저장 후 자동 목표값 갱신 흐름과 연결해 설명하면 좋다.

### 캡처 9. 러닝 기록 저장 화면

- 목적: 러닝 기록 CRUD와 페이스 계산 증빙
- 화면 경로: `/running-records/new`
- 사전 조건: 로그인 상태
- 반드시 보여야 하는 요소:
  - 날짜
  - 거리
  - 시간
  - 케이던스
  - 평균 심박
  - 저장 후 목록 반영
- 연결 코드:
  - Frontend: `RunningRecordFormView.vue`, `runningRecordApi.create`
  - Backend: `RunningRecordApiController`, `RunningRecordService.createRecord`, `RunningRecordMapper.xml`
- 유의 사항:
  - 페이스는 입력이 아니라 거리/시간 기반 계산값임을 보여주면 좋다.

### 캡처 10. 러닝 세션 생성과 참여

- 목적: 러닝 모임 생성, 상태 변경, 참여/정원 검증 증빙
- 화면 경로: `/running-sessions/new`, `/running-sessions/:runningSessionId`
- 사전 조건: 로그인 계정 2개 이상
- 반드시 보여야 하는 요소:
  - 모임 제목
  - 집결 장소
  - 정원
  - 참여 버튼
  - 참여자 수 또는 정원 초과 메시지
- 연결 코드:
  - Frontend: `RunningSessionFormView.vue`, `RunningSessionDetailView.vue`, `runningSessionApi.js`
  - Backend: `RunningSessionApiController`, `RunningSessionParticipantApiController`, `RunningSessionParticipantService.joinSession`
- 유의 사항:
  - `selectByIdForUpdate` 기반 동시성 보완은 코드 캡처로 설명하는 것이 적합하다.

### 캡처 11. Spotify 검색 결과 화면

- 목적: 외부 API 기반 음악 검색 기능 증빙
- 화면 경로: Spotify 검색 UI가 있는 `/music` 또는 관련 화면
- 사전 조건: `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET` 설정
- 반드시 보여야 하는 요소:
  - 검색어
  - 검색 결과
  - 앨범 이미지 또는 아티스트 정보
  - 내부 음악 저장 또는 플레이리스트 추가 버튼
- 연결 코드:
  - Frontend: `spotifyApi.searchTracks`, `spotifyApi.importTrack`
  - Backend: `SpotifyApiController.searchTracks`, `SpotifyService.searchTracks`, `SpotifyRestApiClient.searchTracks`
- 유의 사항:
  - 실제 Spotify 데이터가 아닌 seed/demo 데이터인 경우 더미 데이터 여부를 명시한다.
  - BPM/장르는 Spotify API 조회 실패 시 비어 있을 수 있다.

### 캡처 12. Spotify OAuth 연결 완료

- 목적: Spotify OAuth와 계정 연결 증빙
- 화면 경로: `/login/spotify`, `/oauth/spotify/complete`
- 사전 조건: Spotify Developer Dashboard redirect URI 등록
- 반드시 보여야 하는 요소:
  - Spotify 연결 시작 버튼 또는 경로
  - OAuth 완료 화면
  - 연결 상태 조회 결과
- 연결 코드:
  - Frontend: `SpotifyOAuthCompleteView.vue`, `spotifyApi.startAuthorization`, `spotifyApi.getConnection`
  - Backend: `SpotifyApiController.authorize/callback/getConnection`, `SpotifyOAuthService`
- 유의 사항:
  - 개발 모드 계정 허용 여부와 redirect URI가 맞아야 한다.

### 캡처 13. Spotify 플레이리스트 내보내기

- 목적: SSAFIT 플레이리스트를 Spotify로 내보내는 외부 API 활용 증빙
- 화면 경로: `/playlists/:playlistId`
- 사전 조건: Spotify OAuth 연결, Spotify URI가 있는 수록곡 포함
- 반드시 보여야 하는 요소:
  - export 버튼
  - export 상태
  - Spotify playlist URL
  - 성공/부분 성공/실패 메시지
- 연결 코드:
  - Frontend: `SpotifyExportPanel.vue`, `playlistApi.exportToSpotify`
  - Backend: `PlaylistExportApiController.exportToSpotify`, `PlaylistExportService.exportWithResult`, `SpotifyRestApiClient.createPlaylist/addTracks`
- 유의 사항:
  - Spotify URI가 없는 곡은 skipped 처리될 수 있으므로 전체 곡 export 보장처럼 표현하지 않는다.

### 캡처 14. AI 러닝 리포트

- 목적: 생성형 AI 또는 fallback 기반 데이터 요약 기능 증빙
- 화면 경로: AI 리포트 카드가 표시되는 화면, 예: `/recommendations`
- 사전 조건: 로그인 상태, 러닝 기록 또는 추천 로그 존재
- 반드시 보여야 하는 요소:
  - 리포트 생성 버튼
  - 러닝 패턴 요약
  - 페이스/강도 인사이트
  - 케이던스/심박 인사이트
  - 다음 목표
- 연결 코드:
  - Frontend: `AiRunningReportCard.vue`, `aiRunningReportApi.generate/getLatest`
  - Backend: `AiRunningReportApiController`, `AiRunningReportGenerationService`, `OpenAiRunningReportProvider`
- 유의 사항:
  - `AI_RUNNING_REPORT_ENABLED=false`면 외부 LLM이 아니라 fallback 리포트임을 명시한다.
  - 실제 API 호출 시 provider/model/source 정보를 함께 보여주면 증빙력이 높다.

## 6. 코드 캡처 목록

| 캡처 대상 | 파일 | 보여줄 지점 | 목적 |
| --- | --- | --- | --- |
| 세션 로그인 저장 | `AuthService.java` | `login`, `logout`, `getLoginMemberId` | 세션 기반 인증 증빙 |
| Vue 보호 라우트 | `frontend/src/router/index.js` | `router.beforeEach` | 로그인 필요 화면 보호 증빙 |
| 음악 검색 | `MusicApiController.java`, `MusicService.java`, `MusicMapper.xml` | `getMusicList`, `selectMusicList` | 검색/정렬 구현 증빙 |
| 리뷰 CRUD | `MusicReviewApiController.java`, `MusicReviewService.java` | `create`, `update`, `delete` | 사용자 리뷰 기능 증빙 |
| 플레이리스트 수록곡 | `PlaylistItemApiController.java`, `PlaylistItemService.java` | `add`, `reorder`, `delete` | 수록곡 관리 증빙 |
| 러닝 추천 | `RecommendationService.java` | `recommend`, `scoreMusic`, `saveRecommendation` | 추천 알고리즘/로그/저장 증빙 |
| 러닝 기록 페이스 계산 | `RunningRecordService.java` | `calculatePaceSecondsPerKm` | 러닝 기록 계산 로직 증빙 |
| 러닝 세션 동시성 | `RunningSessionParticipantService.java`, `RunningSessionMapper.xml` | `joinSession`, `selectByIdForUpdate` | 정원 초과 방지 증빙 |
| Spotify 실제 API 호출 | `SpotifyRestApiClient.java` | `searchTracks`, `exchangeAuthorizationCode`, `createPlaylist`, `addTracks` | 외부 API 활용 증빙 |
| Spotify export 상태 저장 | `PlaylistExportService.java`, `PlaylistMapper.xml` | `exportWithResult`, `updateSpotifyExportResult` | 내보내기 상태 관리 증빙 |
| AI 실제 API 호출 | `OpenAiRunningReportProvider.java` | `generate` | 생성형 AI API 호출 증빙 |
| AI fallback | `AiRunningReportGenerationService.java` | `generateLocked`, `saveFallback` | mock/미설정/실패 대응 증빙 |
| Docker proxy | `frontend/nginx.conf` | `/api`, `/login`, `/members` location | Vue/JSP 혼재 연결 증빙 |

## 7. 테스트 결과 캡처 목록

| 캡처 대상 | 파일 또는 명령 | 보여줄 지점 | 목적 |
| --- | --- | --- | --- |
| 전체 백엔드 테스트 통과 | STS Maven test 결과 또는 `docs/verification/mysql-mapper-integration-test-report.md` | `Tests run: 259, Failures: 0, Errors: 0, Skipped: 0` | 전체 안정성 증빙 |
| MySQL Mapper 통합 테스트 | `docs/verification/mysql-mapper-integration-test-report.md` | `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0` | 실제 MySQL/MyBatis 검증 |
| Docker Compose 검증 | `docker compose config --quiet` 결과 또는 Docker Desktop | mysql/backend/frontend healthy | 실행 환경 증빙 |
| 러닝 기능 E2E | `docs/verification/running-features-e2e-result.md`, `frontend/e2e/running-features.spec.js` | 러닝 기록/세션 시나리오 | 사용자 흐름 검증 |
| Spotify 서비스 테스트 | `SpotifyServiceTest.java`, `SpotifyOAuthServiceTest.java`, `PlaylistExportServiceTest.java` | 테스트 클래스와 성공 결과 | 외부 API 연동 로직 검증 |
| AI 리포트 테스트 | `AiRunningReportGenerationServiceTest.java`, `AiRunningReportApiControllerTest.java`, `AiRunningReportMapperIntegrationTest.java` | fallback, provider, 저장 검증 | 생성형 AI 기능 검증 |
| 테스트 DB 안전장치 | `TestDatabaseSafetyTest.java`, `TestDatabaseGuard.java` | DB명에 `test` 포함 검증 | 개발 DB 오염 방지 증빙 |
| Playwright 설정 | `frontend/playwright.config.js` | required env, webServer 설정 | E2E 실행 조건 증빙 |

## 8. 발표 시 주의할 표현

- “음악 CRUD 완전 구현” 대신 “음악 조회, 검색, 상세, 등록, 수정, Spotify 저장 구현. 삭제 API는 현재 범위 밖”이라고 표현한다.
- “Spotify BPM 기반 검색” 대신 “Spotify 검색 결과를 가져온 뒤 가능한 경우 tempo/genre 메타데이터를 보강하고, 로컬 추천 점수에 활용”이라고 표현한다.
- “Spotify 오디오 특성 완전 활용” 대신 “정책/API 제한을 고려해 실패 시 null 처리 및 로컬 데이터 기반 대체”라고 표현한다.
- “항상 OpenAI 호출” 대신 “환경 변수로 provider를 활성화하면 실제 API를 호출하고, 기본/실패 시 fallback 리포트를 생성”이라고 표현한다.
- “AI가 건강 진단을 제공” 대신 “러닝 기록을 바탕으로 참고용 요약과 다음 러닝 방향을 제안”한다고 표현한다.
- “Spotify export는 모든 곡을 보낸다” 대신 “Spotify URI가 있는 곡을 export하고, URI 없는 곡은 skipped 또는 부분 성공으로 처리”한다고 표현한다.
- “Vue 단일 앱” 대신 “JSP 인증 화면과 Vue 기능 화면이 공존하며 Nginx proxy와 returnTo로 연결”한다고 표현한다.
- “실 API까지 자동 테스트 완료” 대신 “핵심 로직은 mock/unit/integration 테스트로 검증했고, Spotify 실 API는 환경 변수와 계정 설정이 필요한 수동 검증 대상”이라고 표현한다.

## 프로젝트 핵심 성과 5개

1. Session 기반 회원 인증과 JSP/Vue 혼재 구조를 Nginx proxy, Vue Router guard, 안전한 return URL 처리로 연결했다.
2. 음악, 리뷰, 플레이리스트, 수록곡 관리, 저장 목록까지 MyBatis 기반 CRUD 흐름을 구현하고 단위/통합 테스트 근거를 확보했다.
3. 러닝 프로필, 러닝 기록, 러닝 세션, 참여/정원 검증을 구현하고 Playwright E2E와 MySQL 통합 테스트로 사용자 흐름 검증 근거를 마련했다.
4. Spotify 검색, OAuth, 플레이리스트 가져오기, SSAFIT 플레이리스트 내보내기까지 실제 외부 API 호출 구조를 구현했다.
5. 러닝 데이터와 추천 로그를 바탕으로 AI 러닝 리포트와 목표 거리 기반 운동 계획을 생성하는 구조를 만들고, 실제 LLM provider와 안전한 fallback 경로를 함께 구현했다.
