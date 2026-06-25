# SSAFIT Playlist 화면 설계서

## 1. 작성 기준

- 기준 문서: `docs/project-artifacts/00-evidence/project-inventory.md`, `01-requirements/requirements-definition.md`, `02-usecase/use-case-specification.md`, `04-wbs/wbs.md`
- 실제 화면 근거: `frontend/src/router/index.js`, `frontend/src/views`, `frontend/src/components`, `frontend/src/api`, `ssafit/src/main/webapp/WEB-INF/views`, `ssafit/src/main/java/com/ssafy/ssafit/**/controller`
- 화면 상태 기준:
  - `IMPLEMENTED`: 실제 Route/JSP/Component와 연결 API가 확인됨
  - `PARTIAL`: 화면은 있으나 외부 OAuth, 권한, 실서비스 검증이 필요함
  - `UNVERIFIED`: 화면/코드는 있으나 실행 또는 외부 Provider 확인이 필요함
  - `NOT_IMPLEMENTED_SCREEN`: API 또는 요구사항은 있으나 화면 Route/Component가 확인되지 않음

## 2. JSP/Vue 혼합 구조

- 회원가입, 로그인, 로그아웃, 마이페이지, 러닝 프로필은 JSP 기반 화면이다.
- 음악, 플레이리스트, 러닝 기록, 러닝 세션, 추천, Spotify OAuth 완료 화면은 Vue Router 기반 화면이다.
- Vue 보호 라우트는 `authApi.getSession()`으로 `/api/members/me`를 호출하고, 인증 실패 시 `/login-required` 또는 JSP `/login`으로 이동한다.
- JSP 공통 네비게이션 `nav.jspf`는 `${frontendOrigin}`으로 Vue 화면(`/playlists`, `/recommendations`, `/running-records`, `/running-sessions`)에 진입한다.

## 화면 ID: SCR-001
### 화면명
메인 진입 및 인증 게이트
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원, 일반 회원
### URL 또는 Route
Vue `/`
### 화면 목적
접속자의 세션을 확인하고 실제 시작 화면을 결정한다.
### 진입 경로
브랜드 링크, 브라우저 직접 접근, 로그인 후 `returnTo=/`.
### 주요 UI 구성
실제 콘텐츠 화면이 아니라 라우터 가드다. 컴포넌트 fallback은 `NotFoundView.vue`.
### 입력 항목
없음
### 버튼 및 이벤트
라우터 `beforeEnter`에서 세션 확인.
### API 연동
`GET /api/members/me` via `authApi.getSession()`.
### 성공 상태
세션 확인 성공 시 `/music`으로 이동.
### 로딩 상태
명시 UI 없음.
### 빈 상태
해당 없음.
### 오류 상태
인증 필요 시 JSP `/login`, 기타 오류 시 `/access-check-failed`.
### 유효성 검증
세션 쿠키 기반 인증 확인.
### 화면 이동 규칙
회원은 `music-list`, 비회원은 `buildJspLoginUrl()`.
### 관련 요구사항
FR-002, FR-023
### 구현 근거
`frontend/src/router/index.js`, `frontend/src/api/authApi.js`
### 미구현 또는 개선 필요 항목
초기 세션 확인 중 표시할 로딩 화면은 별도 구현되어 있지 않다.

## 화면 ID: SCR-002
### 화면명
회원가입
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원
### URL 또는 Route
JSP `/join`
### 화면 목적
신규 회원 계정을 생성한다.
### 진입 경로
로그인 화면 하단 링크, 직접 URL.
### 주요 UI 구성
공통 네비게이션, 소개 문구, 회원가입 폼, Spotify 로그인 링크.
### 입력 항목
로그인 ID, 비밀번호, 이메일, 닉네임.
### 버튼 및 이벤트
`계정 만들기` 제출, `Spotify로 로그인`, 로그인 화면 이동.
### API 연동
JSP POST `/join`; REST `POST /api/members`는 API client만 확인.
### 성공 상태
회원 생성 후 로그인 화면으로 이동하는 Controller 흐름.
### 로딩 상태
JSP form submit 방식으로 별도 로딩 UI 없음.
### 빈 상태
해당 없음.
### 오류 상태
`form:errors`와 `alert-error`.
### 유효성 검증
필수 입력, 최대 길이, 이메일 타입, 서버 검증.
### 화면 이동 규칙
성공 시 `/login`, 기존 회원은 `/login`, Spotify 가입은 `/login/spotify`.
### 관련 요구사항
FR-001
### 구현 근거
`join.jsp`, `MemberViewController`, `MemberViewForms`, `memberApi.js`
### 미구현 또는 개선 필요 항목
Vue 전용 회원가입 화면은 확인되지 않는다.

## 화면 ID: SCR-003
### 화면명
로그인
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원
### URL 또는 Route
JSP `/login`
### 화면 목적
세션 기반 로그인을 수행하고 보호 화면으로 복귀한다.
### 진입 경로
Vue 인증 실패, 네비게이션 로그인 링크, 회원가입 완료.
### 주요 UI 구성
공통 네비게이션, 로그인 폼, 성공/오류 메시지, Spotify 로그인 링크.
### 입력 항목
로그인 ID, 비밀번호, hidden `returnTo`.
### 버튼 및 이벤트
로그인 submit, Spotify 로그인, 회원가입 이동.
### API 연동
JSP POST `/login`; REST `POST /api/auth/login`은 `authApi.js`에서 확인.
### 성공 상태
세션 생성 후 `returnTo` 또는 기본 화면으로 이동.
### 로딩 상태
JSP form submit 방식으로 별도 로딩 UI 없음.
### 빈 상태
해당 없음.
### 오류 상태
`form:errors`, 로그인 실패 메시지.
### 유효성 검증
로그인 ID/비밀번호 필수, 서버 인증 검증.
### 화면 이동 규칙
성공 시 `returnTo`, 회원가입은 `/join`, Spotify는 `/login/spotify`.
### 관련 요구사항
FR-002
### 구현 근거
`login.jsp`, `MemberViewController`, `AuthApiController`, `authApi.js`
### 미구현 또는 개선 필요 항목
로그인 후 Vue 세션 캐시와 JSP 세션의 동기화는 브라우저 회귀 검증 필요.

## 화면 ID: SCR-004
### 화면명
로그아웃 처리
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
JSP POST `/logout`, REST `POST /api/auth/logout`
### 화면 목적
세션을 무효화하고 로그인 화면으로 이동한다.
### 진입 경로
JSP 공통 네비게이션 로그아웃 버튼, Vue API 호출 가능.
### 주요 UI 구성
전용 화면 없음. `nav.jspf`의 POST form 버튼이 진입점이다.
### 입력 항목
없음
### 버튼 및 이벤트
`로그아웃` 버튼 submit.
### API 연동
`POST /logout`, `POST /api/auth/logout`.
### 성공 상태
로그인 화면 또는 비회원 상태로 이동.
### 로딩 상태
전용 UI 없음.
### 빈 상태
해당 없음.
### 오류 상태
전용 오류 화면 없음.
### 유효성 검증
세션 존재 여부.
### 화면 이동 규칙
로그아웃 후 보호 Vue route 접근 시 `/login-required` 또는 `/login`.
### 관련 요구사항
FR-003
### 구현 근거
`nav.jspf`, `MemberViewController`, `AuthApiController`, `authApi.js`
### 미구현 또는 개선 필요 항목
로그아웃 완료 전용 화면은 없다.

## 화면 ID: SCR-005
### 화면명
마이페이지
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
JSP `/members/me`
### 화면 목적
회원 정보, 러닝 프로필 요약, 러닝 기능 진입, 최근 러닝 차트를 제공한다.
### 진입 경로
JSP 네비게이션 `마이페이지`, 로그인 후 onboarding.
### 주요 UI 구성
히어로 영역, 러닝 프로필 타일, 러닝 기록/세션 링크, 최근 7일 러닝 차트, 온보딩 모달.
### 입력 항목
Spotify 신규 로그인 시 로그인 ID, 닉네임 입력 모달.
### 버튼 및 이벤트
프로필 등록, 플레이리스트 이동, 러닝 기록/세션 이동, 차트 지표 전환.
### API 연동
JSP 모델 조회, 브라우저 fetch `GET /api/running-records/me`.
### 성공 상태
회원 정보와 프로필/차트 표시.
### 로딩 상태
차트 영역에 “러닝 기록을 불러오는 중입니다.”
### 빈 상태
러닝 프로필 없음, 기록 없음 안내.
### 오류 상태
차트 fetch 실패 시 “최근 러닝 기록을 불러오지 못했습니다.”
### 유효성 검증
온보딩 identity form 서버 검증.
### 화면 이동 규칙
Vue `/playlists`, `/running-records`, `/running-sessions`, JSP `/members/me/running-profile`.
### 관련 요구사항
FR-004, FR-005, FR-006, FR-007
### 구현 근거
`my-page.jsp`, `MemberViewController`, `RunningRecordApiController`
### 미구현 또는 개선 필요 항목
마이페이지 자체는 JSP라 Vue SPA 내부 계정 설정 화면과 분리되어 있다.

## 화면 ID: SCR-006
### 화면명
러닝 프로필
### 구현 상태
PARTIAL
### 접근 권한
회원
### URL 또는 Route
JSP `/members/me/running-profile`
### 화면 목적
러닝 프로필과 Fitness Score, 자동 산출 목표 범위를 조회/수정한다.
### 진입 경로
마이페이지 타일, 추천 화면의 프로필 필요 안내.
### 주요 UI 구성
Fitness Score 패널, 성별/나이 입력, 자동 산출 요약, 선호 장르 입력.
### 입력 항목
성별, 나이, 선호 장르, hidden `returnTo`.
### 버튼 및 이벤트
프로필 저장, 마이페이지 이동.
### API 연동
JSP GET/POST `/members/me/running-profile`; REST `GET/PUT /api/members/me/running-profile` 존재.
### 성공 상태
저장 성공 메시지와 최신 프로필 표시.
### 로딩 상태
JSP server render라 별도 로딩 UI 없음.
### 빈 상태
기록이 없으면 목표 심박/케이던스 “기록 등록 후 자동 산출”.
### 오류 상태
`form:errors`, 서버 검증 오류.
### 유효성 검증
나이 10~100, 성별 enum, 장르 최대 길이, 서버 검증.
### 화면 이동 규칙
저장 후 `returnTo` 또는 마이페이지.
### 관련 요구사항
FR-005, FR-015
### 구현 근거
`running-profile.jsp`, `RunningProfileViewController`, `RunningProfileApiController`, `runningProfileApi.js`
### 미구현 또는 개선 필요 항목
Vue Router 기반 러닝 프로필 화면은 발표 범위에서 새로 구현하지 않는다. 현재 시연과 문서 기준은 JSP 화면이며, Vue 전용 화면은 후속 SPA 통합 과제로 분리한다.

## 화면 ID: SCR-007
### 화면명
로그인 필요 안내
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원
### URL 또는 Route
Vue `/login-required`
### 화면 목적
보호 Vue route 접근 시 로그인을 안내한다.
### 진입 경로
`router.beforeEach`의 `requiresAuth` 실패.
### 주요 UI 구성
안내 메시지, 로그인 이동 버튼.
### 입력 항목
없음
### 버튼 및 이벤트
JSP 로그인 이동.
### API 연동
진입 전 `GET /api/members/me`.
### 성공 상태
로그인 화면 이동.
### 로딩 상태
없음
### 빈 상태
해당 없음
### 오류 상태
해당 없음
### 유효성 검증
세션 확인 결과.
### 화면 이동 규칙
`returnTo` query를 로그인 URL에 전달.
### 관련 요구사항
FR-002, FR-023
### 구현 근거
`LoginRequiredView.vue`, `router/index.js`, `navigation.js`
### 미구현 또는 개선 필요 항목
로그인 완료 후 원래 화면 복귀는 브라우저 확인 필요.

## 화면 ID: SCR-008
### 화면명
접근 확인 실패
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원, 회원
### URL 또는 Route
Vue `/access-check-failed`
### 화면 목적
세션 확인 중 인증 실패가 아닌 오류를 안내한다.
### 진입 경로
`router.beforeEach`, `/` route guard.
### 주요 UI 구성
오류 메시지, 재시도/이동 안내.
### 입력 항목
없음
### 버튼 및 이벤트
이전 화면 또는 로그인 이동.
### API 연동
진입 전 `GET /api/members/me`.
### 성공 상태
해당 없음.
### 로딩 상태
없음
### 빈 상태
해당 없음.
### 오류 상태
query의 status/errorCode 표시.
### 유효성 검증
없음
### 화면 이동 규칙
`returnTo` 기반 복귀 가능.
### 관련 요구사항
FR-023
### 구현 근거
`AccessCheckFailedView.vue`, `router/index.js`
### 미구현 또는 개선 필요 항목
운영 오류 코드별 사용자 메시지 정교화 여지.

## 화면 ID: SCR-009
### 화면명
음악 목록 및 검색
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원, 회원
### URL 또는 Route
Vue `/music`
### 화면 목적
내부 음악 목록을 검색/정렬/페이지 이동하고 Spotify 곡을 검색해 저장한다.
### 진입 경로
`/` 세션 성공, 네비게이션, 플레이리스트/상세 링크.
### 주요 UI 구성
Spotify 검색 패널, 필터 토글, `MusicSearchBar`, 음악 카드 그리드, 페이지네이션.
### 입력 항목
Spotify 검색어, 제목/아티스트/장르/BPM/sort/page 필터.
### 버튼 및 이벤트
Spotify 검색, 저장, 필터 적용, 정렬 변경, 초기화, 페이지 이동, 상세 이동.
### API 연동
`GET /api/music`, `GET /api/spotify/tracks/search`, `POST /api/music/from-spotify`, `GET /api/members/me`.
### 성공 상태
검색 조건에 맞는 음악 카드 표시.
### 로딩 상태
`PageStatePanel` loading, Spotify “검색 중...”.
### 빈 상태
“현재 조건에 맞는 음악이 없습니다.”, Spotify “검색 결과가 없습니다.”
### 오류 상태
`PageStatePanel` error, `spotifyErrorMessage`.
### 유효성 검증
검색어 필수, Spotify query 최대 200, BPM/페이지 필터 검증.
### 화면 이동 규칙
카드 클릭 시 `/music/:musicId`, 플레이리스트 링크는 `/playlists`.
### 관련 요구사항
FR-008, FR-017
### 구현 근거
`MusicListView.vue`, `MusicSearchBar.vue`, `MusicCard.vue`, `musicApi.js`, `spotifyApi.js`
### 미구현 또는 개선 필요 항목
Spotify 검색은 외부 API 장애에 영향을 받는다.

## 화면 ID: SCR-010
### 화면명
음악 상세
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원, 회원
### URL 또는 Route
Vue `/music/:musicId`
### 화면 목적
음악 상세 정보, 평균 리뷰, 사용자 리뷰 CRUD를 제공한다.
### 진입 경로
음악 카드, 플레이리스트 수록곡.
### 주요 UI 구성
상세 카드, 메타 정보, 평균 점수, 리뷰 목록, 리뷰 폼.
### 입력 항목
리뷰 점수, 좋아요 여부, 러닝 적합도, 코멘트.
### 버튼 및 이벤트
리뷰 작성/수정/삭제, 좋아요 토글, 목록 이동.
### API 연동
`GET /api/music/{musicId}`, `GET/POST /api/music/{musicId}/reviews`, `PUT/DELETE /api/music-reviews/{reviewId}`.
### 성공 상태
상세와 리뷰 목록 표시, CRUD 후 목록 갱신.
### 로딩 상태
`PageStatePanel` loading.
### 빈 상태
리뷰 없음 안내.
### 오류 상태
상세/리뷰 API 오류 메시지.
### 유효성 검증
리뷰 점수 정수 범위, 코멘트 길이, 로그인 필요.
### 화면 이동 규칙
목록 또는 returnTo로 복귀.
### 관련 요구사항
FR-009, FR-010
### 구현 근거
`MusicDetailView.vue`, `MusicReviewForm.vue`, `reviewApi.js`
### 미구현 또는 개선 필요 항목
비회원 리뷰 작성 시 로그인 유도 흐름은 실제 브라우저 확인 필요.

## 화면 ID: SCR-011
### 화면명
음악 리뷰 작성 및 수정
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
단독 route 없음. `/music/:musicId`, `/playlists/:playlistId` 내 component.
### 화면 목적
음악에 대한 좋아요, 별점/점수, 러닝 적합도, 코멘트를 저장한다.
### 진입 경로
음악 상세 리뷰 영역, 플레이리스트 수록곡 리뷰 영역.
### 주요 UI 구성
점수 입력, 좋아요 토글, 코멘트 입력, 저장/취소 버튼.
### 입력 항목
score, liked, runningFitScore, comment.
### 버튼 및 이벤트
저장, 수정 시작, 취소, 삭제.
### API 연동
`POST /api/music/{musicId}/reviews`, `PUT /api/music-reviews/{reviewId}`, `DELETE /api/music-reviews/{reviewId}`.
### 성공 상태
리뷰 목록/내 리뷰 갱신.
### 로딩 상태
저장 중 버튼 disabled.
### 빈 상태
내 리뷰 없음.
### 오류 상태
`reviewErrorMessage`.
### 유효성 검증
정수 점수 검증, 서버 Validation.
### 화면 이동 규칙
단독 이동 없음.
### 관련 요구사항
FR-010
### 구현 근거
`MusicReviewForm.vue`, `MusicDetailView.vue`, `PlaylistItemList.vue`, `reviewApi.js`
### 미구현 또는 개선 필요 항목
리뷰 E2E는 WBS상 미확인 영역이다.

## 화면 ID: SCR-012
### 화면명
내부 음악 등록 및 수정
### 구현 상태
UNVERIFIED
### 접근 권한
회원, 실제 관리자 권한 미확인
### URL 또는 Route
Vue `/music-manage/new`, `/music-manage/:musicId/edit`
### 화면 목적
내부 음악 데이터를 직접 생성 또는 수정한다.
### 진입 경로
라우트 직접 접근 또는 관리 링크가 필요하나 화면상 노출 경로는 제한적으로 확인됨.
### 주요 UI 구성
음악 입력 폼, 저장/취소 버튼.
### 입력 항목
제목, 아티스트, 앨범, 장르, BPM, 길이, energy 등 음악 메타데이터.
### 버튼 및 이벤트
저장, 취소, 수정 시 기존 정보 로드.
### API 연동
`GET /api/music/{musicId}`, `POST /api/music/internal`, `PUT /api/music/internal/{musicId}`.
### 성공 상태
저장 후 상세 또는 목록 이동.
### 로딩 상태
수정 시 상세 로딩.
### 빈 상태
해당 없음.
### 오류 상태
저장 실패 메시지.
### 유효성 검증
필수값, 숫자값 nullable 처리, 서버 검증.
### 화면 이동 규칙
취소/저장 후 음악 화면으로 이동.
### 관련 요구사항
FR-024
### 구현 근거
`MusicFormView.vue`, `MusicApiController`, `musicApi.js`
### 미구현 또는 개선 필요 항목
관리자 Actor/권한 정책이 확인되지 않아 발표 기능으로 단정하지 않는다.

## 화면 ID: SCR-013
### 화면명
플레이리스트 목록
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원, 회원
### URL 또는 Route
Vue `/playlists`
### 화면 목적
공개/공식/내 플레이리스트를 탐색하고 Spotify 곡을 호환 플레이리스트에 추가한다.
### 진입 경로
JSP nav, Vue nav, 마이페이지.
### 주요 UI 구성
플레이리스트 카드, 그룹/필터, Spotify 검색 드롭다운, 플레이리스트 추가 메뉴.
### 입력 항목
검색/필터, Spotify 검색어.
### 버튼 및 이벤트
상세 이동, 생성 이동, Spotify 검색, 곡 추가.
### API 연동
`GET /api/playlists`, `GET /api/spotify/tracks/search`, `POST /api/music/from-spotify`, `POST /api/playlists/{id}/items`.
### 성공 상태
플레이리스트 목록과 추가 성공 메시지.
### 로딩 상태
`PageStatePanel` loading, Spotify 검색 중.
### 빈 상태
플레이리스트 없음/검색 결과 없음.
### 오류 상태
playlist/spotify 오류 메시지.
### 유효성 검증
Spotify 검색어 필수, 추가 가능한 플레이리스트 조건.
### 화면 이동 규칙
`/playlists/:playlistId`, `/playlists/new`, `/music`.
### 관련 요구사항
FR-011, FR-012, FR-017
### 구현 근거
`PlaylistListView.vue`, `PlaylistCard.vue`, `playlistApi.js`, `spotifyApi.js`
### 미구현 또는 개선 필요 항목
Spotify 곡 추가는 외부 API 장애 시 실패 가능.

## 화면 ID: SCR-014
### 화면명
저장한 플레이리스트
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/playlists/saved`
### 화면 목적
회원이 저장한 공개/공식 플레이리스트를 조회한다.
### 진입 경로
플레이리스트 화면 또는 직접 접근.
### 주요 UI 구성
저장 목록 카드, 상태 패널.
### 입력 항목
없음
### 버튼 및 이벤트
상세 이동.
### API 연동
`GET /api/playlist-saves/me`.
### 성공 상태
저장 목록 표시.
### 로딩 상태
`PageStatePanel` loading.
### 빈 상태
저장한 플레이리스트 없음.
### 오류 상태
API 오류 표시.
### 유효성 검증
세션 인증.
### 화면 이동 규칙
상세는 `/playlists/:playlistId`.
### 관련 요구사항
FR-013
### 구현 근거
`SavedPlaylistView.vue`, `playlistSaveApi.js`, `router/index.js`
### 미구현 또는 개선 필요 항목
없음.

## 화면 ID: SCR-015
### 화면명
플레이리스트 상세
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원, 회원
### URL 또는 Route
Vue `/playlists/:playlistId`
### 화면 목적
플레이리스트 상세, 저장/취소, 소유자 관리, Spotify 검색/추가/내보내기를 제공한다.
### 진입 경로
플레이리스트 목록, 추천 저장 결과, 저장 목록.
### 주요 UI 구성
상세 헤더, 수정/삭제 버튼, 저장 버튼, Spotify inline 검색, 수록곡 목록, export 영역.
### 입력 항목
Spotify 검색어, 수록곡 순서 변경.
### 버튼 및 이벤트
수정, 삭제, 저장/취소, Spotify 검색/추가, 수록곡 삭제/정렬, Spotify export.
### API 연동
`GET /api/playlists/{id}`, `GET /api/playlist-saves/me`, `POST/DELETE /api/playlist-saves/{id}`, item APIs, `GET /api/spotify/me/connection`, `POST /api/playlists/{id}/export/spotify`.
### 성공 상태
상세/수록곡 표시, 작업 후 갱신.
### 로딩 상태
`PageStatePanel` loading, export checking/exporting.
### 빈 상태
수록곡 없음, Spotify 검색 결과 없음.
### 오류 상태
playlist/spotify 오류 메시지.
### 유효성 검증
소유자 여부, 공개/공식 저장 가능 여부, export 가능 상태, 수록곡 존재 여부.
### 화면 이동 규칙
목록으로 복귀, 수정 화면, Spotify OAuth로 이동 가능.
### 관련 요구사항
FR-011, FR-012, FR-013, FR-019
### 구현 근거
`PlaylistDetailView.vue`, `PlaylistItemList.vue`, `playlistApi.js`, `playlistSaveApi.js`, `spotifyApi.js`
### 미구현 또는 개선 필요 항목
Spotify export 실계정 성공 여부는 미확인이다.

## 화면 ID: SCR-016
### 화면명
플레이리스트 생성 및 수정
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/playlists/new`, `/playlists/:playlistId/edit`
### 화면 목적
내부 플레이리스트를 생성하거나 메타데이터를 수정한다.
### 진입 경로
플레이리스트 목록/상세.
### 주요 UI 구성
제목/설명/공개 설정 폼, 저장/취소.
### 입력 항목
title, description, visibility 등.
### 버튼 및 이벤트
저장, 취소, 수정 시 기존 상세 로드.
### API 연동
`GET /api/playlists/{id}`, `POST /api/playlists`, `PUT /api/playlists/{id}`.
### 성공 상태
저장 후 상세 화면 이동.
### 로딩 상태
수정 초기화 loading.
### 빈 상태
해당 없음.
### 오류 상태
저장/조회 오류 메시지.
### 유효성 검증
제목 필수, 길이, 공개값 서버 검증.
### 화면 이동 규칙
상세 또는 목록.
### 관련 요구사항
FR-011
### 구현 근거
`PlaylistFormView.vue`, `playlistApi.js`
### 미구현 또는 개선 필요 항목
없음.

## 화면 ID: SCR-017
### 화면명
플레이리스트 수록곡 관리
### 구현 상태
IMPLEMENTED
### 접근 권한
소유 회원
### URL 또는 Route
단독 route 없음. `/playlists/:playlistId` 내 component.
### 화면 목적
플레이리스트 곡 순서를 바꾸고 삭제하며 곡별 리뷰를 관리한다.
### 진입 경로
플레이리스트 상세.
### 주요 UI 구성
수록곡 목록, drag reorder, 삭제 버튼, 리뷰 편집 영역.
### 입력 항목
순서, 리뷰 점수/코멘트.
### 버튼 및 이벤트
drag start/move/finish, 삭제, 리뷰 저장.
### API 연동
`PATCH /api/playlists/{id}/items/order`, `PATCH /api/playlist-items/{itemId}/order`, `DELETE /api/playlist-items/{itemId}`, review APIs.
### 성공 상태
순서/삭제 후 목록 갱신.
### 로딩 상태
busy 상태에서 버튼 disabled.
### 빈 상태
수록곡 없음.
### 오류 상태
상위 상세 화면 SaveStatus.
### 유효성 검증
소유자 여부, item id, review score.
### 화면 이동 규칙
단독 이동 없음.
### 관련 요구사항
FR-012, FR-010
### 구현 근거
`PlaylistItemList.vue`, `PlaylistDetailView.vue`, `playlistApi.js`
### 미구현 또는 개선 필요 항목
drag reorder 브라우저 호환성은 수동 검증 필요.

## 화면 ID: SCR-018
### 화면명
러닝 기록 목록
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/running-records`
### 화면 목적
내 러닝 기록을 조회하고 기간별 합산 거리/페이스를 확인한다.
### 진입 경로
JSP nav, 마이페이지, Vue nav.
### 주요 UI 구성
새 기록 버튼, 상태 패널, 전체/년/월 토글, 요약 지표, 기록 카드, AI 리포트 카드.
### 입력 항목
조회 범위, 연도, 월.
### 버튼 및 이벤트
새 기록, 범위 전환, 기록 상세 이동, AI 리포트 생성.
### API 연동
`GET /api/running-records/me`, AI report APIs.
### 성공 상태
기록 목록과 집계 표시.
### 로딩 상태
`PageStatePanel` loading.
### 빈 상태
“아직 러닝 기록이 없습니다”, 첫 기록 등록 버튼.
### 오류 상태
“러닝 기록을 불러오지 못했습니다”.
### 유효성 검증
세션 인증.
### 화면 이동 규칙
`/running-records/new`, `/running-records/:id`.
### 관련 요구사항
FR-006, FR-021
### 구현 근거
`RunningRecordListView.vue`, `AiRunningReportCard.vue`, `runningRecordApi.js`
### 미구현 또는 개선 필요 항목
AI 리포트 Provider 실검증 필요.

## 화면 ID: SCR-019
### 화면명
러닝 기록 상세
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/running-records/:runningRecordId`
### 화면 목적
러닝 기록 상세를 확인하고 수정/삭제한다.
### 진입 경로
러닝 기록 목록, 저장 후 redirect.
### 주요 UI 구성
상세 메트릭, 메모, 수정/삭제 버튼, 저장 알림.
### 입력 항목
없음
### 버튼 및 이벤트
수정, 삭제, 목록 이동.
### API 연동
`GET /api/running-records/{id}`, `DELETE /api/running-records/{id}`.
### 성공 상태
상세 표시, 삭제 후 목록 이동.
### 로딩 상태
`PageStatePanel` loading.
### 빈 상태
해당 없음.
### 오류 상태
not found/unauthorized/error 상태.
### 유효성 검증
소유자 권한.
### 화면 이동 규칙
수정은 `/running-records/:id/edit`, 삭제 후 `/running-records`.
### 관련 요구사항
FR-006
### 구현 근거
`RunningRecordDetailView.vue`, `runningRecordApi.js`
### 미구현 또는 개선 필요 항목
없음.

## 화면 ID: SCR-020
### 화면명
러닝 기록 등록 및 수정
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/running-records/new`, `/running-records/:runningRecordId/edit`
### 화면 목적
러닝 날짜, 거리, 시간, 케이던스, 심박, 메모를 저장한다.
### 진입 경로
러닝 기록 목록/상세.
### 주요 UI 구성
러닝 기록 form, 평균 페이스 preview, 저장/취소, 상태 메시지.
### 입력 항목
recordDate, distanceKm, durationMinutes, durationSeconds, averageCadence, averageHeartRateBpm, memo.
### 버튼 및 이벤트
저장, 취소, 수정 초기 데이터 로드.
### API 연동
`GET /api/running-records/{id}`, `POST /api/running-records`, `PUT /api/running-records/{id}`.
### 성공 상태
상세 화면으로 이동하며 saved query 전달.
### 로딩 상태
수정 초기화 loading, 저장 중 버튼 disabled.
### 빈 상태
신규 등록 form.
### 오류 상태
API 오류와 SaveStatus.
### 유효성 검증
날짜 필수, 거리 0.01 이상, 초 0~59, 케이던스 60~240, 심박 30~240, 메모 1000자.
### 화면 이동 규칙
저장 후 상세, 취소는 목록 또는 상세.
### 관련 요구사항
FR-006, FR-015
### 구현 근거
`RunningRecordFormView.vue`, `runningRecordApi.js`, `utils/runningRecord.js`
### 미구현 또는 개선 필요 항목
없음.

## 화면 ID: SCR-021
### 화면명
러닝 세션 목록
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/running-sessions`
### 화면 목적
러닝 세션을 조회하고 모집 중 또는 내 세션으로 필터링한다.
### 진입 경로
JSP nav, 마이페이지, Vue nav.
### 주요 UI 구성
세션 생성 버튼, 상태 패널, 전체/모집 중/MY 필터, 세션 row 목록.
### 입력 항목
필터 선택.
### 버튼 및 이벤트
세션 생성, 필터 변경, 상세 이동.
### API 연동
`GET /api/running-sessions`.
### 성공 상태
정렬된 세션 목록 표시.
### 로딩 상태
`PageStatePanel` loading.
### 빈 상태
“등록된 러닝 세션이 없습니다”.
### 오류 상태
“러닝 세션을 불러오지 못했습니다”.
### 유효성 검증
세션 인증.
### 화면 이동 규칙
`/running-sessions/new`, `/running-sessions/:id`.
### 관련 요구사항
FR-007
### 구현 근거
`RunningSessionListView.vue`, `runningSessionApi.js`
### 미구현 또는 개선 필요 항목
없음.

## 화면 ID: SCR-022
### 화면명
러닝 세션 상세
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/running-sessions/:runningSessionId`
### 화면 목적
세션 상세, 참여자 목록, 참여/취소, 주최자 상태 관리를 제공한다.
### 진입 경로
러닝 세션 목록, 저장 후 redirect.
### 주요 UI 구성
상세 메트릭, 참여자 목록, 참여/참여 취소, 수정/취소/상태 변경 버튼.
### 입력 항목
상태 변경 값은 버튼 액션으로 처리.
### 버튼 및 이벤트
참여, 참여 취소, 모집 마감/재개, 수정, 취소, 삭제성 취소.
### API 연동
`GET /api/running-sessions/{id}`, `GET/POST/DELETE /api/running-sessions/{id}/participants`, `PATCH /api/running-sessions/{id}/status`, `POST /api/running-sessions/{id}/cancel`.
### 성공 상태
상세와 참여자 목록 갱신.
### 로딩 상태
`PageStatePanel` loading, action busy.
### 빈 상태
참여자 없음.
### 오류 상태
loginRequired, unauthorized, notFound, conflict.
### 유효성 검증
주최자 권한, 정원, 모집 상태, 중복 참여.
### 화면 이동 규칙
수정은 `/running-sessions/:id/edit`, 목록 복귀.
### 관련 요구사항
FR-007
### 구현 근거
`RunningSessionDetailView.vue`, `runningSessionApi.js`
### 미구현 또는 개선 필요 항목
실제 동시 참여 정원 충돌은 서버 검증 중심이다.

## 화면 ID: SCR-023
### 화면명
러닝 세션 생성 및 수정
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/running-sessions/new`, `/running-sessions/:runningSessionId/edit`
### 화면 목적
러닝 모임 일정과 목표를 생성/수정한다.
### 진입 경로
세션 목록/상세.
### 주요 UI 구성
세션 폼, 저장/취소, 오류 메시지.
### 입력 항목
title, capacity, meetingPlace, scheduledAt, targetDistanceKm, targetPaceMinutes, targetPaceSeconds, description.
### 버튼 및 이벤트
저장, 취소, 수정 초기화.
### API 연동
`GET /api/running-sessions/{id}`, `POST /api/running-sessions`, `PUT /api/running-sessions/{id}`.
### 성공 상태
상세 화면으로 이동.
### 로딩 상태
수정 초기화 loading, 저장 중 disabled.
### 빈 상태
신규 form.
### 오류 상태
권한 없음, 취소된 세션 수정 불가, API 오류.
### 유효성 검증
제목/장소/일시/정원/거리 필수, 설명 2000자, 정원 1 이상, 거리 0.01 이상.
### 화면 이동 규칙
저장 후 상세, 취소는 목록 또는 상세.
### 관련 요구사항
FR-007
### 구현 근거
`RunningSessionFormView.vue`, `runningSessionApi.js`, `utils/runningSession.js`
### 미구현 또는 개선 필요 항목
없음.

## 화면 ID: SCR-024
### 화면명
자동 큐레이션 조건 입력
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
Vue `/recommendations`
### 화면 목적
목표 거리, 운동 강도, 장르를 입력해 러닝 운동 계획과 음악 추천을 생성한다.
### 진입 경로
JSP nav, Vue nav.
### 주요 UI 구성
추천 조건 폼, 프로필 필요 안내, 상태 패널, 추천 히스토리.
### 입력 항목
targetDistanceKm, intensity, genre.
### 버튼 및 이벤트
운동 계획 생성, 로그 새로고침, 결과 보기, 로그 삭제.
### API 연동
`GET /api/recommendations`, `GET /api/recommendations/logs/me`, `GET /api/recommendations/{logId}`, `DELETE /api/recommendations/{logId}`.
### 성공 상태
추천 결과와 히스토리 표시.
### 로딩 상태
“AI 계획 생성 중...”, `PageStatePanel` loading.
### 빈 상태
추천 로그 없음.
### 오류 상태
프로필 없음, 추천 데이터 부족, invalid intensity, 서버 오류.
### 유효성 검증
목표 거리 0.5~100km, intensity 필수, 장르 trim.
### 화면 이동 규칙
프로필 없음 시 JSP 러닝 프로필 작성 링크.
### 관련 요구사항
FR-014, FR-015
### 구현 근거
`RecommendationView.vue`, `recommendationApi.js`
### 미구현 또는 개선 필요 항목
추천 품질은 로컬 음악 데이터와 러닝 프로필 품질에 의존한다.

## 화면 ID: SCR-025
### 화면명
러닝 음악 추천 결과
### 구현 상태
IMPLEMENTED
### 접근 권한
회원
### URL 또는 Route
단독 route 없음. `/recommendations` 내 component.
### 화면 목적
운동 계획과 추천 음악을 표시하고 결과를 플레이리스트로 저장한다.
### 진입 경로
추천 조건 제출, 추천 히스토리 결과 보기.
### 주요 UI 구성
운동 계획, 추천 곡 목록, 저장 폼/버튼, 닫기.
### 입력 항목
저장할 플레이리스트 제목/설명 등 component payload.
### 버튼 및 이벤트
저장, 닫기.
### API 연동
`POST /api/recommendations/{logId}/save`, 저장 후 `GET /api/playlists/{id}` 가능.
### 성공 상태
큐레이션 복사 플레이리스트 생성 및 상세 링크 표시.
### 로딩 상태
저장 중 disabled.
### 빈 상태
추천 결과 없음.
### 오류 상태
이미 저장됨, 저장 실패.
### 유효성 검증
logId 유효성, 서버 중복 저장 검증.
### 화면 이동 규칙
저장된 플레이리스트 상세로 이동 가능.
### 관련 요구사항
FR-016
### 구현 근거
`RecommendationResult.vue`, `RecommendationView.vue`, `recommendationApi.js`
### 미구현 또는 개선 필요 항목
없음.

## 화면 ID: SCR-026
### 화면명
AI 러닝 리포트
### 구현 상태
UNVERIFIED
### 접근 권한
회원
### URL 또는 Route
단독 route 없음. `/running-records` 내 component.
### 화면 목적
최근 러닝 기록을 바탕으로 AI 리포트를 조회/생성한다.
### 진입 경로
러닝 기록 목록.
### 주요 UI 구성
최신 리포트 카드, 생성/새로고침 버튼, 누락 데이터 안내.
### 입력 항목
강제 새로고침 여부.
### 버튼 및 이벤트
최신 조회, 생성, force refresh.
### API 연동
`GET /api/ai/running-reports/latest`, `POST /api/ai/running-reports`.
### 성공 상태
리포트 요약 표시.
### 로딩 상태
조회/생성 중 상태.
### 빈 상태
최신 리포트 없음.
### 오류 상태
Provider 실패, 데이터 부족, 서버 오류.
### 유효성 검증
로그인, 러닝 기록 데이터 필요.
### 화면 이동 규칙
단독 이동 없음.
### 관련 요구사항
FR-021
### 구현 근거
`AiRunningReportCard.vue`, `AiRunningReportApiController`, `aiRunningReportApi.js`
### 미구현 또는 개선 필요 항목
외부 AI Provider 실호출은 환경 의존으로 미확인이다.

## 화면 ID: SCR-027
### 화면명
Spotify 검색
### 구현 상태
IMPLEMENTED
### 접근 권한
회원 중심, 일부 화면은 비회원 접근 후 오류 처리
### URL 또는 Route
단독 route 없음. `/music`, `/playlists`, `/playlists/:playlistId` 내 검색 패널.
### 화면 목적
Spotify 곡을 검색하고 내부 음악 또는 플레이리스트 수록곡으로 저장한다.
### 진입 경로
음악 목록, 플레이리스트 목록/상세.
### 주요 UI 구성
검색 input, 드롭다운 결과, 앨범 이미지, 저장/추가 버튼.
### 입력 항목
query, limit.
### 버튼 및 이벤트
검색, 저장, 플레이리스트 추가.
### API 연동
`GET /api/spotify/tracks/search`, `POST /api/music/from-spotify`, `POST /api/playlists/{id}/items`.
### 성공 상태
검색 결과 표시, 저장/추가 성공 메시지.
### 로딩 상태
“Spotify에서 검색 중입니다.”, 버튼 “검색 중...”.
### 빈 상태
검색 결과 없음.
### 오류 상태
로그인 필요, 권한 없음, Spotify API 오류.
### 유효성 검증
검색어 필수, 최대 길이, limit 10 사용.
### 화면 이동 규칙
단독 이동 없음.
### 관련 요구사항
FR-017
### 구현 근거
`MusicListView.vue`, `PlaylistListView.vue`, `PlaylistDetailView.vue`, `spotifyApi.js`
### 미구현 또는 개선 필요 항목
외부 API 장애 시 대체 결과는 없다.

## 화면 ID: SCR-028
### 화면명
Spotify 연동 시작
### 구현 상태
PARTIAL
### 접근 권한
회원
### URL 또는 Route
Vue `/login/spotify` redirect, JSP Controller `/login/spotify`
### 화면 목적
Spotify OAuth authorize URL로 사용자를 보낸다.
### 진입 경로
JSP 로그인/회원가입 Spotify 버튼, Vue export 연결 버튼.
### 주요 UI 구성
전용 화면 없음. redirect 처리.
### 입력 항목
returnTo.
### 버튼 및 이벤트
Spotify 로그인/연결 클릭.
### API 연동
`GET /api/spotify/oauth/authorize`.
### 성공 상태
Spotify 동의 화면으로 redirect.
### 로딩 상태
전용 UI 없음.
### 빈 상태
해당 없음.
### 오류 상태
OAuth 시작 실패 시 상위 화면 오류 메시지.
### 유효성 검증
세션, state, returnTo.
### 화면 이동 규칙
Spotify 외부 페이지 → callback → `/oauth/spotify/complete`.
### 관련 요구사항
FR-018
### 구현 근거
`router/index.js`, `SpotifyLoginViewController`, `SpotifyApiController`, `spotifyApi.js`
### 미구현 또는 개선 필요 항목
Spotify Dashboard redirect URI와 scope 실검증 필요.

## 화면 ID: SCR-029
### 화면명
Spotify OAuth 완료
### 구현 상태
PARTIAL
### 접근 권한
회원
### URL 또는 Route
Vue `/oauth/spotify/complete`
### 화면 목적
OAuth callback 이후 연결 상태를 확인하고 원래 화면으로 복귀시킨다.
### 진입 경로
Spotify callback 후 redirect.
### 주요 UI 구성
연결 확인 메시지, 성공/실패 안내, 복귀 버튼.
### 입력 항목
callback 결과 query 또는 저장된 return path.
### 버튼 및 이벤트
연결 상태 확인, 원래 화면 이동.
### API 연동
`GET /api/spotify/me/connection`.
### 성공 상태
연결됨 상태 확인.
### 로딩 상태
연결 확인 중.
### 빈 상태
해당 없음.
### 오류 상태
연결 확인 실패, 재연결 필요.
### 유효성 검증
OAuth state/token은 서버에서 검증.
### 화면 이동 규칙
저장된 return path 또는 기본 화면으로 이동.
### 관련 요구사항
FR-018
### 구현 근거
`SpotifyOAuthCompleteView.vue`, `SpotifyApiController`, `oauthReturn.js`
### 미구현 또는 개선 필요 항목
실제 OAuth 성공 경로는 외부 계정으로 검증해야 한다.

## 화면 ID: SCR-030
### 화면명
Spotify 플레이리스트 내보내기
### 구현 상태
PARTIAL
### 접근 권한
회원
### URL 또는 Route
단독 route 없음. `/playlists/:playlistId` 내 export 영역 및 `SpotifyExportPanel.vue`.
### 화면 목적
내부 플레이리스트를 Spotify 계정에 생성/내보낸다.
### 진입 경로
플레이리스트 상세.
### 주요 UI 구성
연결 상태, Spotify 연결/내보내기 버튼, Spotify에서 열기 링크, hint.
### 입력 항목
없음. 대상은 현재 playlistId.
### 버튼 및 이벤트
연결 확인, OAuth redirect, export 실행.
### API 연동
`GET /api/spotify/me/connection`, `POST /api/playlists/{playlistId}/export/spotify`.
### 성공 상태
내보낸 곡 수와 Spotify URL 표시.
### 로딩 상태
“Spotify 확인 중...”, “내보내는 중...”.
### 빈 상태
수록곡 없으면 export 불가 안내.
### 오류 상태
인증 만료, 생성 실패, 일부 실패, 이미 내보냄.
### 유효성 검증
수록곡 존재, Spotify 연결, export status, 공개/소유/공식 조건.
### 화면 이동 규칙
연결 필요 시 OAuth 시작 화면으로 이동.
### 관련 요구사항
FR-019
### 구현 근거
`PlaylistDetailView.vue`, `SpotifyExportPanel.vue`, `PlaylistExportApiController`, `spotifyApi.js`
### 미구현 또는 개선 필요 항목
실제 Spotify playlist 생성과 scope 권한은 미확인이다.

## 화면 ID: SCR-031
### 화면명
404 Not Found
### 구현 상태
IMPLEMENTED
### 접근 권한
비회원, 회원
### URL 또는 Route
Vue `/:pathMatch(.*)*`
### 화면 목적
정의되지 않은 Vue 경로 접근을 안내한다.
### 진입 경로
잘못된 URL 직접 접근.
### 주요 UI 구성
Not Found 안내, 홈/목록 이동 링크.
### 입력 항목
없음
### 버튼 및 이벤트
기본 화면 이동.
### API 연동
없음
### 성공 상태
안내 표시.
### 로딩 상태
없음
### 빈 상태
해당 없음
### 오류 상태
해당 화면 자체가 오류 상태.
### 유효성 검증
없음
### 화면 이동 규칙
사용자가 선택한 링크로 이동.
### 관련 요구사항
FR-023
### 구현 근거
`NotFoundView.vue`, `router/index.js`
### 미구현 또는 개선 필요 항목
없음.

## 화면 ID: SCR-032
### 화면명
Spotify 플레이리스트 가져오기
### 구현 상태
NOT_IMPLEMENTED_SCREEN
### 접근 권한
회원
### URL 또는 Route
전용 Vue route/JSP 없음
### 화면 목적
Spotify 계정의 플레이리스트를 내부 플레이리스트로 가져오는 목표 기능.
### 진입 경로
확인되지 않음.
### 주요 UI 구성
화면 미구현.
### 입력 항목
Spotify playlist 선택, 공개 여부, 내부 제목 등이 필요할 수 있으나 실제 화면 없음.
### 버튼 및 이벤트
화면 미구현.
### API 연동
`GET /api/spotify/me/playlists`, `POST /api/spotify/playlists/import`.
### 성공 상태
API 기준 내부 플레이리스트 생성 가능성이 있으나 화면 없음.
### 로딩 상태
화면 미구현.
### 빈 상태
화면 미구현.
### 오류 상태
화면 미구현.
### 유효성 검증
Spotify 연결, playlist id, scope가 필요하나 화면 검증 없음.
### 화면 이동 규칙
없음.
### 관련 요구사항
FR-020
### 구현 근거
`spotifyApi.js`, `SpotifyApiController`
### 미구현 또는 개선 필요 항목
가져오기 화면과 시연 동선이 필요하다.

## 3. 화면 수 요약

| 구분 | 수 |
|---|---:|
| 전체 식별 화면/화면 기능 | 32 |
| Vue route 화면 | 19 |
| Vue component 내장 화면 기능 | 7 |
| JSP 화면 | 4 |
| Controller redirect/처리 화면 | 2 |
| 화면 미구현 기능 | 1 |
