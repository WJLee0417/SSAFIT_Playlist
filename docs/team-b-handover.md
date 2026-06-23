# SSAFIT PlayList 팀원 B 작업 인수인계서

## 0. 문서 목적

이 문서는 팀원 B가 아직 구현을 시작하지 않은 상태에서 바로 개발에 착수할 수 있도록
다음 자료를 하나로 합친 인수인계 문서다.

- `SSAFIT 플레이리스트 팀원 B 역할 명세서 - MVP + 추가 기능` PDF
- 현재 `feature/a-spotify-service` 브랜치의 실제 구현
- [Spotify Service 연동 계약](./spotify-service-contract.md)
- [Spotify 수동 통합 테스트 체크리스트](./spotify-manual-integration-checklist.md)
- 현재 Domain, Mapper, Service, ErrorCode, DB 스키마

이 문서의 기준 시점은 `feature/a-spotify-service` 브랜치 커밋
`21b3e6d (docs: document spotify service integration contract)`이다.

## 1. 가장 먼저 알아야 할 현재 상태

### 팀원 A가 완료한 영역

- Java 25, Spring Boot 4.0.6, Maven Wrapper 프로젝트
- MySQL 8 DDL과 제약조건
- Domain과 Enum
- MyBatis Mapper Interface/XML
- 회원, 인증, 러닝 프로필 Service
- 음악, 음악 평가 Service
- 플레이리스트, 수록곡, 참조 저장 Service
- 자동 큐레이션 Service
- Spotify 검색, 선택 곡 저장, OAuth, Token 갱신, Export Service
- 공통 ErrorCode와 `GlobalExceptionHandler`
- Mock 기반 Service 테스트
- Spotify 수동 통합 체크리스트와 Service 계약 문서

### 아직 구현되지 않은 팀원 B 영역

- 회원/인증/프로필 JSP
- 모든 업무 Controller와 REST API Controller
- Request/Response DTO
- Vue3 + Vite 프로젝트
- Vue Router
- Axios 또는 Fetch API 모듈
- 음악, 평가, 플레이리스트, 저장, 큐레이션 화면
- Spotify 검색/선택/OAuth/Export UI
- Swagger 또는 API Tester 문서
- Controller binding 및 화면 테스트
- 발표 시연 흐름

현재 저장소에는 실제 Frontend 디렉터리가 없고, Controller/DTO 패키지는 대부분
`.gitkeep`만 있는 상태다. 기존 Service 로직을 다시 작성하지 말고 웹 계층을 연결하는
것이 팀원 B의 핵심 작업이다.

## 2. 담당 범위와 금지 경계

### 팀원 B 담당

- JSP/JSTL 화면
- Vue3/Vite 화면과 컴포넌트
- Vue Router
- Frontend API 모듈
- Spring MVC Controller
- REST API Controller
- Request/Response DTO
- 세션 쿠키 기반 API 연동
- Swagger/API Tester 검증
- 브라우저 UI 테스트
- 발표 시연 흐름

### 건드리기 전에 팀원 A와 합의할 영역

- Domain 필드 추가·삭제
- Mapper SQL 변경
- Service 메서드 시그니처 변경
- ErrorCode 이름이나 HTTP 상태 변경
- DB 테이블·컬럼·제약조건 변경
- Spotify Token, state, Export 상태 전이 로직 변경

### 금지

- Spring Security 또는 JWT 도입
- Session 인증을 별도 인증 방식으로 교체
- Spotify Access Token이나 Refresh Token을 브라우저에 전달
- Client Secret을 Frontend, DTO, 응답, 로그에 포함
- Vue에서 Spotify Web API를 직접 호출
- Controller에서 OAuth state나 Token 갱신 로직을 재구현
- 기존 Service를 우회해 Mapper를 Controller에서 직접 호출

## 3. 기술 전제

| 영역 | 기준 |
| --- | --- |
| 인증 | `HttpSession` |
| 로그인 회원 키 | `SessionConst.LOGIN_MEMBER_ID` |
| Security/JWT | 미사용 |
| Server View | JSP/JSTL |
| Frontend | Vue3 + Vite |
| Routing | Vue Router |
| API Client | Axios 또는 Fetch |
| Backend Web | Spring MVC Controller |
| DB | MySQL 8 + MyBatis |
| 오류 응답 | `ErrorResponse(code, message)` |
| Spotify 호출 | Backend Service/Client에서만 수행 |

Vue API 요청에는 세션 쿠키가 포함되어야 한다.

Axios 예시:

```javascript
const api = axios.create({
  baseURL: "/api",
  withCredentials: true,
});
```

동일 Origin 구성이라면 기본 쿠키 정책으로 동작할 수 있지만, 개발 서버 포트가
분리되면 CORS와 `withCredentials` 설정을 함께 확인해야 한다.

## 4. 권장 구현 순서

PDF 역할 명세의 MVP 우선순위를 현재 코드 상태에 맞춰 다음처럼 권장한다.

### 1단계: 공통 API 기반

1. 공통 성공 응답 형식 결정
2. 공통 페이징 응답 DTO 작성
3. Session 회원 ID 추출 방식 통일
4. ErrorResponse와 ErrorCode별 Frontend 처리기 작성
5. Swagger 또는 API Tester 기본 설정

현재 오류 응답은 이미 다음 형태다.

```json
{
  "code": "SPOTIFY_AUTH_REQUIRED",
  "message": "Spotify 계정 연결이 필요합니다."
}
```

### 2단계: JSP 회원/인증/프로필

1. 회원가입
2. 로그인
3. 로그아웃
4. 마이페이지
5. 러닝 프로필 등록/수정

### 3단계: Vue 기본 프로젝트와 음악

1. Vue3/Vite 프로젝트 생성
2. Vue Router
3. 공통 API Client
4. 음악 목록/상세
5. 검색/정렬/페이징
6. 음악 평가 작성/수정/삭제

### 4단계: 플레이리스트

1. 목록/상세
2. 생성/수정/삭제
3. 수록곡 추가/삭제/순서 변경
4. 공개·공식 플레이리스트 참조 저장/취소
5. 내 저장 목록

### 5단계: Spotify

1. Spotify 곡 검색
2. 선택 곡 DB 저장
3. 저장된 `musicId`로 플레이리스트 수록곡 추가
4. Spotify 연결 상태 조회
5. OAuth 인증 시작/callback
6. Spotify Export
7. Export 상태와 URL 표시

### 6단계: 자동 큐레이션

1. 추천 조건 입력
2. 임시 추천 결과 표시
3. 추천 로그 ID 유지
4. 저장 버튼으로 개인 플레이리스트 생성
5. 생성된 플레이리스트로 이동

### 7단계: 검증과 시연

1. Swagger/API Tester
2. 브라우저 오류 처리
3. Session 쿠키 확인
4. Spotify 수동 통합 테스트
5. 발표 시연 동선 고정

### 추가 기능

- 러닝 기록 CRUD
- 러닝 모임 CRUD
- 러닝 모임 참여/취소

현재 `runningrecord`, `runningsession`은 패키지 자리만 있고 Domain/Mapper/Service가
구현되지 않았다. 추가 기능은 팀원 B 단독으로 Controller/UI부터 만들 수 없으며,
백엔드 내부 구현 담당과 먼저 범위를 합의해야 한다.

## 5. JSP 담당 화면

| 화면 | 역할 명세 기준 URL | 주요 책임 |
| --- | --- | --- |
| 회원가입 | `GET /join` | 회원가입 폼, 중복 오류 |
| 로그인 | `GET /login` | 로그인 폼, 실패 메시지 |
| 로그아웃 | 팀 합의 | 세션 종료 후 redirect |
| 마이페이지 | `GET /members/me` | 내 정보, 프로필 요약, Vue 진입 링크 |
| 러닝 프로필 설정 | `GET /members/me/running-profile` | 프로필 등록/수정 |
| 공통 레이아웃 | 선택 | 로그인 상태, 메뉴, Vue 진입 링크 |

권장 Controller:

- `AuthController`
- `MemberController`
- `RunningProfileController`

JSP form 처리에서도 인증 상태는 Session을 사용한다.

## 6. Vue3 담당 화면과 컴포넌트

### 필수 Views

| 파일 제안 | 책임 |
| --- | --- |
| `MusicListView.vue` | 검색, 정렬, 페이징, 음악 목록 |
| `MusicDetailView.vue` | 상세, 평균 평가, 평가 목록 |
| `PlaylistListView.vue` | 공식/사용자 목록, 저장 상태 |
| `PlaylistDetailView.vue` | 수록곡 관리, Spotify Export, URL |
| `PlaylistFormView.vue` | 생성/수정 |
| `SavedPlaylistView.vue` | 내 저장 목록, 저장 취소 |
| `RecommendationView.vue` | 추천 조건, 임시 결과, 저장 |
| `SpotifyTrackSearchView.vue` | Spotify 검색과 선택 저장 |
| `SpotifyExportResultView.vue` | 선택 사항. 상세 화면 내부 영역으로 대체 가능 |

### 필수 Components

| 파일 제안 | 책임 |
| --- | --- |
| `MusicSearchBar.vue` | 검색 조건 입력 |
| `MusicCard.vue` | 음악 요약 |
| `MusicReviewForm.vue` | 평가 작성/수정/삭제 |
| `PlaylistCard.vue` | 플레이리스트 요약 |
| `PlaylistItemList.vue` | 수록곡 표시와 순서 관리 |
| `RecommendationResult.vue` | 추천 음악과 추천 사유 |
| `SpotifyTrackSearchModal.vue` | 플레이리스트 상세 내 Spotify 검색 |
| `SpotifyExportButton.vue` | 연결 확인, OAuth, Export, 진행 상태 |

검색 전용 View와 Modal을 둘 다 만들 필요는 없다. UX에 맞춰 하나를 주 진입점으로
정하되 선택 곡 저장 후 `musicId`를 플레이리스트 추가 흐름으로 넘겨야 한다.

## 7. 권장 Frontend 구조

```text
frontend/
  package.json
  vite.config.js
  src/
    main.js
    App.vue
    router/
      index.js
    api/
      authApi.js
      memberApi.js
      runningProfileApi.js
      musicApi.js
      reviewApi.js
      playlistApi.js
      playlistSaveApi.js
      recommendationApi.js
      spotifyApi.js
    views/
    components/
```

추가 기능을 실제 구현하기로 합의한 경우에만 다음 모듈을 추가한다.

```text
runningRecordApi.js
runningSessionApi.js
```

## 8. 담당 Controller

### JSP Controller

- `AuthController`
- `MemberController`
- `RunningProfileController`

### REST Controller

- `MemberApiController`
- `AuthApiController`
- `RunningProfileApiController`
- `MusicApiController`
- `MusicReviewApiController`
- `PlaylistApiController`
- `PlaylistItemApiController`
- `PlaylistSaveApiController`
- `RecommendationApiController`
- `SpotifyApiController`
- `PlaylistExportApiController`

Spotify 검색/OAuth/Export를 `SpotifyApiController` 하나로 합칠 수는 있지만,
메서드와 DTO의 책임은 검색/OAuth/Export로 분명하게 구분한다.

## 9. DTO 목록

### 공통 DTO

- `ApiResponse<T>`: 선택. 도입 시 모든 성공 응답에 일관되게 적용
- 기존 `ErrorResponse`: 공통 오류 응답으로 재사용
- `PageResponse<T>`
- `PageRequest`

성공 응답 Wrapper를 도입할지 여부는 구현 초기에 팀 전체와 확정한다. 현재 Service
테스트와 Domain은 Wrapper를 전제하지 않는다.

### 회원/프로필

- `MemberJoinRequest`
- `LoginRequest`
- `MemberResponse`
- `LoginResponse`
- `RunningProfileRequest`
- `RunningProfileResponse`

### 음악/평가

- `MusicResponse`
- `MusicDetailResponse`
- `MusicReviewCreateRequest`
- `MusicReviewUpdateRequest`
- `MusicReviewResponse`

`MusicSearchCondition`은 현재 Domain에 존재한다. 웹 요청 DTO로 직접 받을지 별도
Request DTO를 둘지는 팀 규칙에 맞추되, Controller에서 Service 조건 객체로 변환한다.

### 플레이리스트

- `PlaylistCreateRequest`
- `PlaylistUpdateRequest`
- `PlaylistResponse`
- `PlaylistDetailResponse`
- `PlaylistItemCreateRequest`
- `PlaylistItemOrderUpdateRequest`
- `PlaylistItemResponse`
- `PlaylistSaveResponse`

`PlaylistResponse`에는 현재 로그인 조회자의 참조 저장 여부를 나타내는
`saved` boolean 필드를 포함한다. 일반 목록에서는 로그인 회원의
`playlist_saves` 존재 여부가 반영되고, 비로그인 조회는 `false`, 내 저장 목록은
항상 `true`다.

### 자동 큐레이션

- `PlaylistRecommendRequest`
- `PlaylistRecommendResponse`
- `RecommendationLogResponse`
- `CurationSaveRequest`
- `CurationSaveResponse`

### Spotify

- `SpotifyTrackSearchRequest`
- `SpotifyTrackResponse`
- `SpotifyTrackImportRequest`
- `SpotifyConnectionResponse`
- `SpotifyPlaylistExportRequest`
- `SpotifyPlaylistExportResponse`

Spotify DTO에는 Access Token, Refresh Token, Authorization Code, Client Secret을
절대 포함하지 않는다.

## 10. 현재 사용 가능한 Service 호출표

### 인증/회원

| 목적 | Service 호출 |
| --- | --- |
| 회원가입 | `memberService.register(member, rawPassword)` |
| 로그인 | `authService.login(session, loginId, rawPassword)` |
| 로그아웃 | `authService.logout(session)` |
| Session 회원 ID | `authService.getLoginMemberId(session)` |
| 로그인 회원 조회 | `authService.getLoginMember(session)` |
| 내 정보 | `memberService.getMyInfo(loginMemberId)` |

### 러닝 프로필

| 목적 | Service 호출 |
| --- | --- |
| 프로필 조회 | `runningProfileService.getProfile(loginMemberId, targetMemberId)` |
| 등록/수정 | `runningProfileService.saveOrUpdate(loginMemberId, profile)` |

### 음악

| 목적 | Service 호출 |
| --- | --- |
| 목록 | `musicService.getMusicList(condition)` |
| 목록 개수 | `musicService.countMusicList(condition)` |
| 추천용 목록 | `musicService.getRecommendedMusic(condition)` |
| 상세 | `musicService.getMusicDetail(musicId)` |
| Spotify 선택 곡 저장 | `spotifyService.saveSelectedTrack(metadata)` |

### 음악 평가

| 목적 | Service 호출 |
| --- | --- |
| 작성 | `musicReviewService.create(loginMemberId, review)` |
| 수정 | `musicReviewService.update(loginMemberId, review)` |
| 삭제 | `musicReviewService.delete(loginMemberId, reviewId)` |
| 음악별 목록 | `musicReviewService.getReviewsByMusicId(musicId)` |
| 평균 | `musicReviewService.getAverageScores(musicId)` |

### 플레이리스트

| 목적 | Service 호출 |
| --- | --- |
| 생성 | `playlistService.create(loginMemberId, playlist)` |
| 목록 | `playlistService.getList(viewerMemberId, ownerId)` |
| 상세 | `playlistService.getDetail(viewerMemberId, playlistId)` |
| 수정 | `playlistService.update(loginMemberId, changes)` |
| 삭제 | `playlistService.delete(loginMemberId, playlistId)` |

### 수록곡

| 목적 | Service 호출 |
| --- | --- |
| 추가 | `playlistItemService.add(loginMemberId, item)` |
| 삭제 | `playlistItemService.delete(loginMemberId, itemId)` |
| 순서 변경 | `playlistItemService.updateOrder(loginMemberId, itemId, orderNo)` |
| 목록 | `playlistItemService.getItems(viewerMemberId, playlistId)` |

### 플레이리스트 참조 저장

| 목적 | Service 호출 |
| --- | --- |
| 저장 | `playlistSaveService.save(loginMemberId, playlistId)` |
| 취소 | `playlistSaveService.cancel(loginMemberId, playlistId)` |
| 내 저장 목록 | `playlistSaveService.getMySavedList(loginMemberId)` |

### 자동 큐레이션

| 목적 | Service 호출 |
| --- | --- |
| 추천 조회 | `recommendationService.recommend(loginMemberId, condition, reason)` |
| 추천 저장 | `recommendationService.saveRecommendation(loginMemberId, logId, title, visibility)` |
| 내 추천 로그 | `recommendationService.getMyRecommendationLogs(loginMemberId)` |

### Spotify

| 목적 | Service 호출 |
| --- | --- |
| 곡 검색 | `spotifyService.searchTracks(query, limit)` |
| 선택 곡 저장 | `spotifyService.saveSelectedTrack(metadata)` |
| OAuth URL | `spotifyOAuthService.buildAuthorizationUrl(session)` |
| callback | `spotifyOAuthService.handleCallback(loginMemberId, code, error, state, session)` |
| 연결 조회 | `spotifyOAuthService.getConnection(loginMemberId)` |
| 유효 Token 연결 | `spotifyOAuthService.getValidConnection(loginMemberId)` |
| 연결 해제 | `spotifyOAuthService.disconnect(loginMemberId)` |
| Export | `playlistExportService.export(loginMemberId, playlistId)` |

Spotify의 상세 nullable 필드, 상태 전이와 ErrorCode는
[Spotify Service 연동 계약](./spotify-service-contract.md)을 반드시 함께 확인한다.

## 11. 권장 API 계약

아래 URI는 역할 명세서의 제안이다. 기존 Controller가 없으므로 구현 전에 팀 전체와
확정하되, 확정 후 Vue API 모듈·Swagger·문서에서 동일하게 사용한다.

### 회원/인증/프로필

| Method | URI | 목적 |
| --- | --- | --- |
| `POST` | `/api/members` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인 |
| `POST` | `/api/auth/logout` | 로그아웃 |
| `GET` | `/api/members/me` | 내 정보 |
| `GET` | `/api/members/me/running-profile` | 내 러닝 프로필 |
| `PUT` | `/api/members/me/running-profile` | 러닝 프로필 저장 |

### 음악/평가

| Method | URI | 목적 |
| --- | --- | --- |
| `GET` | `/api/music` | 목록/검색/정렬/페이징 |
| `GET` | `/api/music/{musicId}` | 상세 |
| `POST` | `/api/music/{musicId}/reviews` | 평가 작성 |
| `PUT` | `/api/music-reviews/{reviewId}` | 평가 수정 |
| `DELETE` | `/api/music-reviews/{reviewId}` | 평가 삭제 |

### 플레이리스트

| Method | URI | 목적 |
| --- | --- | --- |
| `POST` | `/api/playlists` | 생성 |
| `GET` | `/api/playlists` | 목록 |
| `GET` | `/api/playlists/{playlistId}` | 상세 |
| `PUT` | `/api/playlists/{playlistId}` | 수정 |
| `DELETE` | `/api/playlists/{playlistId}` | 삭제 |
| `POST` | `/api/playlists/{playlistId}/items` | 수록곡 추가 |
| `PATCH` | `/api/playlist-items/{itemId}/order` | 순서 변경 |
| `DELETE` | `/api/playlist-items/{itemId}` | 수록곡 삭제 |
| `POST` | `/api/playlists/{playlistId}/save` | 참조 저장 |
| `DELETE` | `/api/playlists/{playlistId}/save` | 저장 취소 |
| `GET` | `/api/playlists/saved/me` | 내 저장 목록 |

### 자동 큐레이션

| Method | URI | 목적 |
| --- | --- | --- |
| `POST` | `/api/recommendations` | 임시 추천 조회 |
| `GET` | `/api/recommendations/logs/me` | 내 추천 로그 |
| `POST` | `/api/recommendations/{logId}/save` | 추천 결과 저장 |

### Spotify

| Method | URI | 목적 |
| --- | --- | --- |
| `GET` | `/api/spotify/tracks/search` | Spotify 곡 검색 |
| `POST` | `/api/music/from-spotify` | 선택 곡 DB 저장 |
| `GET` | `/api/spotify/oauth/authorize` | OAuth URL 생성 또는 redirect |
| `GET` | `/api/spotify/oauth/callback` | OAuth callback |
| `GET` | `/api/spotify/me/connection` | 연결 상태 |
| `DELETE` | `/api/spotify/me/connection` | 연결 해제 |
| `POST` | `/api/playlists/{playlistId}/export/spotify` | Spotify Export |

OAuth 시작 API는 다음 중 한 방식으로 통일한다.

1. Backend가 Spotify authorize URL로 redirect
2. `{ authorizationUrl }`을 반환하고 Vue가 이동

두 방식을 섞지 않는다. 현재 Service는 URL 문자열을 반환한다.

## 12. Spotify UI 흐름

```text
Spotify 검색
→ 검색 결과 선택
→ 선택 곡 DB 저장
→ 반환된 musicId로 플레이리스트 수록곡 추가
→ Spotify 연결 상태 확인
→ 미연결이면 OAuth 진행
→ Spotify Export
→ spotifyPlaylistUrl 표시
```

### 검색

- 빈 결과는 오류가 아니라 빈 배열이다.
- Track ID 또는 URI 없는 검색 결과는 Service에서 제외된다.
- 제목, 아티스트, 앨범, 이미지, 외부 URL은 nullable일 수 있다.

### 연결

`SpotifyConnectionResponse` 권장 필드:

```text
connected
spotifyUserId
tokenExpiresAt
```

Token 필드는 제외한다.

### Export

표시 필드:

```text
spotifyPlaylistId
spotifyPlaylistUrl
spotifyExportStatus
spotifyExportErrorCode
spotifyExportedAt
```

상태:

```text
NOT_EXPORTED
PENDING
SUCCESS
FAILED
```

`FAILED`인데 ID와 URL이 있으면 Spotify Playlist 생성 후 수록곡 추가가 실패한
부분 실패일 수 있다. URL을 표시하면서 자동 재시도가 차단된 이유를 안내한다.

현재 정확한 추가 성공/실패 곡 수는 저장하지 않는다.

- 성공 시 전체 URI가 추가된 것으로 처리 가능
- 실패 시 성공/실패 곡 수는 `null` 권장
- 실패 사유는 `spotifyExportErrorCode`

## 13. 주요 ErrorCode와 UI 처리

| ErrorCode | 권장 UI |
| --- | --- |
| `AUTH_REQUIRED` | 로그인 화면 이동 |
| `ACCESS_DENIED` | 권한 없음 안내 |
| `PRIVATE_PLAYLIST` | 비공개 접근 불가 |
| `MUSIC_NOT_FOUND` | 음악 없음 |
| `PLAYLIST_NOT_FOUND` | 플레이리스트 없음 |
| `DUPLICATE_PLAYLIST_ITEM` | 이미 추가된 곡 |
| `DUPLICATE_PLAYLIST_ORDER` | 순서 충돌 |
| `SPOTIFY_AUTH_REQUIRED` | Spotify 연결 버튼 |
| `SPOTIFY_AUTH_DENIED` | 권한 동의 거부 안내 |
| `SPOTIFY_TOKEN_EXPIRED` | Spotify 재인증 |
| `SPOTIFY_SEARCH_FAILED` | 잠시 후 재검색 |
| `SPOTIFY_TRACK_NOT_FOUND` | 선택 곡 정보 재확인 |
| `SPOTIFY_URI_NOT_FOUND` | URI 없는 수록곡 안내 |
| `SPOTIFY_PLAYLIST_CREATE_FAILED` | Spotify Playlist 생성 실패 |
| `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED` | 부분 실패와 URL 확인 |
| `SPOTIFY_EXPORT_FAILED` | 상태 충돌 또는 재시도 제한 |
| `SPOTIFY_ALREADY_EXPORTED` | 기존 Spotify URL 표시 |

Frontend는 HTTP 상태만 보지 말고 `error.response.data.code`를 우선 사용한다.

## 14. 자동 큐레이션 UI 주의사항

- 추천 조회만으로 플레이리스트가 생성되지 않는다.
- 추천 결과와 추천 로그 ID를 화면 상태로 유지한다.
- 사용자가 저장 버튼을 눌러야 개인 플레이리스트가 생성된다.
- 저장 결과는 `USER + CURATION_COPY` 플레이리스트다.
- `playlist_saves`의 참조 저장 목록과 큐레이션으로 생성한 내 플레이리스트를
  동일 개념으로 표시하지 않는다.

## 15. 테스트 책임

### Controller

- Request DTO binding
- Validation 오류
- Session 로그인 회원 ID 전달
- Response DTO 변환
- ErrorCode HTTP 응답

### JSP

- form submit
- redirect
- 로그인 실패 및 중복 오류 메시지
- Session 상태별 메뉴

### Vue

- API 호출
- 세션 쿠키
- 검색 조건과 페이징 유지
- 로딩/성공/실패 상태
- 401/403/404/409/422/502 UI
- Spotify OAuth 이동과 callback 이후 상태 갱신
- Export URL 표시

### Swagger/API Tester

- 인증/회원
- 러닝 프로필
- 음악/평가
- 플레이리스트/수록곡/저장
- 추천 조회/저장
- Spotify 검색/저장/OAuth/연결/Export

### 기존 Backend 테스트

```bash
cd ssafit
mvnw.cmd clean test
mvnw.cmd -o clean test
```

현재 기준 140건 중 132건 통과, DB 통합 테스트 8건은 기본 실행에서 제외된다.
Spotify 테스트는 Mock 기반이며 실제 API를 호출하지 않는다.

## 16. 실제 Spotify 수동 검증

실제 계정 검증은
[Spotify 수동 통합 테스트 체크리스트](./spotify-manual-integration-checklist.md)를
따른다.

필수 확인:

- 환경 변수
- Redirect URI
- OAuth 동의/거부
- Session 유지
- 연결 정보 저장
- 검색
- 선택 곡 저장
- 수록곡 추가
- Export
- Spotify URL
- 부분 실패
- Token Refresh
- 연결 해제 후 재인증

실제 Client ID, Secret, Token, Authorization Code를 문서, 로그, 캡처에 남기지 않는다.

## 17. 발표 시연 권장 흐름

```text
JSP 회원가입
→ JSP 로그인
→ JSP 러닝 프로필 설정
→ Vue 음악 목록/검색
→ 음악 상세/평가
→ 플레이리스트 생성
→ Spotify 곡 검색
→ 선택 곡 DB 저장
→ 플레이리스트 수록곡 추가
→ Spotify OAuth 연결
→ Spotify Export
→ Spotify에서 열기
→ 자동 큐레이션 조회
→ 추천 결과 저장
→ 내 플레이리스트 확인
→ Swagger/API Tester 보조 시연
```

시연 전 테스트 계정, Spotify 연결 상태, 기존 Export 상태를 초기화한다.

## 18. 완료 기준

- [ ] JSP 회원가입/로그인/마이페이지/프로필 화면
- [ ] Vue3/Vite 실행 가능
- [ ] Vue Router 구성
- [ ] 공통 API Client와 세션 쿠키 처리
- [ ] 음악/평가 화면
- [ ] 플레이리스트 CRUD와 수록곡 관리
- [ ] 저장/취소/내 저장 목록
- [ ] 자동 큐레이션 조회와 저장 분리
- [ ] Spotify 검색/선택 저장
- [ ] Spotify 기반 수록곡 추가
- [ ] OAuth 연결 상태와 인증 시작
- [ ] Export 버튼과 진행 상태
- [ ] 성공 URL 및 부분 실패 표시
- [ ] Controller와 DTO
- [ ] ErrorCode별 UI 처리
- [ ] Swagger/API Tester
- [ ] Controller/UI 테스트
- [ ] 수동 Spotify 체크리스트 완료
- [ ] 발표 시연 가능

## 19. 시작 전 팀 합의가 필요한 항목

1. 공통 성공 응답 Wrapper 도입 여부
2. 최종 REST URI
3. Vue 개발 서버와 Backend의 Origin/CORS 구성
4. OAuth 시작 API의 redirect 방식 또는 URL 반환 방식
5. OAuth callback 성공/실패 후 이동할 Frontend 경로
6. Export 부분 실패 URL을 어떤 문구로 표시할지
7. Swagger 라이브러리 도입 여부
8. 추가 기능인 러닝 기록/모임을 MVP에 포함할지

## 20. 알려진 제한 사항

- Access Token과 Refresh Token은 현재 DB에 평문 저장된다.
- 실제 Spotify 권한 철회 API는 구현하지 않았다.
- Export 외부 API와 DB는 분산 트랜잭션이 아니다.
- 부분 실패의 정확한 성공/실패 곡 수는 저장하지 않는다.
- 외부 Spotify Playlist ID가 남은 `FAILED` 건은 자동 재시도하지 않는다.
- 실제 MySQL Mapper 통합 테스트는 기본 Maven 테스트에서 제외된다.
- 러닝 기록과 러닝 모임 추가 기능의 내부 Backend는 미구현이다.

## 21. 참고 문서

- [Spotify Service 연동 계약](./spotify-service-contract.md)
- [Spotify 수동 통합 테스트 체크리스트](./spotify-manual-integration-checklist.md)
- [프로젝트 README](../README.md)
- 원본 역할 명세서:
  `C:/Users/dnjsw/Downloads/SSAFIT 플레이리스트 팀원 B 역할 명세서.pdf`
