# B UI/API 계약

## 1. 적용 기준

- 브랜치: `feature/b-ui-api`
- Backend: `http://localhost:8080`
- Vue/Vite: `http://localhost:5173`
- API prefix: `/api/**`
- 인증: `HttpSession`
- 로그인 회원 Session key: `LOGIN_MEMBER_ID`
- Vue API Client: `frontend/src/api/http.js`
- Controller는 Request 검증, Session 회원 확인, Service 호출, DTO 변환만 담당한다.
- DDL, Mapper, SQL, Service 비즈니스 로직은 이 단계에서 변경하지 않았다.

## 2. 공통 응답 계약

기존 오류 계약을 우선 적용하고 성공 응답을 새 공통 Wrapper로 통일했다.

성공:

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

오류:

```json
{
  "code": "AUTH_REQUIRED",
  "message": "로그인이 필요합니다."
}
```

페이징 데이터:

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "totalCount": 0,
  "totalPages": 0
}
```

공통 클래스:

| 클래스 | 역할 |
| --- | --- |
| `ApiResponse<T>` | 성공 응답 Wrapper |
| `ErrorResponse` | 기존 오류 응답 재사용 |
| `PageRequest` | page/size 기본값과 범위 검증 |
| `PageResponse<T>` | 목록과 전체 개수 조립 |
| `SessionMemberResolver` | 필수/선택 Session 회원 ID 추출 |

Vue는 HTTP status와 오류 응답의 `code`를 함께 사용해 401, 403, 404, 409, 502를 구분한다.

## 3. Session/CORS/Cookie

Spring CORS:

```text
대상: /api/**
허용 Origin: http://localhost:5173
Credentials: true
Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
```

Axios:

```javascript
axios.create({
  baseURL: "http://localhost:8080/api",
  withCredentials: true,
});
```

보호 API는 Request의 회원 ID를 인증 주체로 사용하지 않는다. 항상 `SessionMemberResolver.requireMemberId(session)` 결과를 Service에 전달한다.

## 4. 회원/인증 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/members` | POST | 불필요 | `MemberApiDtos.JoinRequest` | `MemberResponse` | `MemberService.register(Member, String)` | `DUPLICATE_LOGIN_ID`, `DUPLICATE_EMAIL`, `INVALID_REQUEST` |
| `/api/auth/login` | POST | 불필요 | `LoginRequest` | `LoginResponse` | `AuthService.login(HttpSession, String, String)` | `INVALID_LOGIN` |
| `/api/auth/logout` | POST | 필요 | 없음 | 없음 | `AuthService.logout(HttpSession)` | - |
| `/api/members/me` | GET | 필요 | 없음 | `MemberResponse` | `MemberService.getMyInfo(Long)` | `AUTH_REQUIRED`, `MEMBER_NOT_FOUND` |

`MemberResponse`에는 `passwordHash`가 없다.

## 5. 러닝 프로필 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/members/me/running-profile` | GET | 필요 | 없음 | `RunningProfileApiDtos.Response` | `RunningProfileService.getProfile(memberId, memberId)` | `AUTH_REQUIRED`, `PROFILE_NOT_FOUND` |
| `/api/members/me/running-profile` | PUT | 필요 | `SaveRequest` | `Response` | `RunningProfileService.saveOrUpdate(Long, RunningProfile)` | `INVALID_PROFILE_VALUE` |

## 6. 음악 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/music` | GET | 불필요 | `MusicApiDtos.SearchRequest` | `PageResponse<MusicResponse>` | `getMusicList`, `countMusicList` | `INVALID_BPM_VALUE`, `INVALID_BPM_RANGE`, `INVALID_REQUEST` |
| `/api/music/{musicId}` | GET | 불필요 | 없음 | `MusicApiDtos.DetailResponse` | `getMusicDetail`, 리뷰 목록/평균 Service | `MUSIC_NOT_FOUND` |
| `/api/music/from-spotify` | POST | 필요 | `SpotifyImportRequest` | `MusicResponse` | `SpotifyService.saveSelectedTrack` | `SPOTIFY_TRACK_NOT_FOUND`, `INVALID_BPM_VALUE` |

목록과 전체 개수는 실제 Service의 별도 메서드를 같은 검색 조건으로 호출해 페이징 응답으로 조립한다.

## 7. 음악 리뷰 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/music/{musicId}/reviews` | GET | 불필요 | 없음 | `List<ReviewResponse>` | `getReviewsByMusicId` | `MUSIC_NOT_FOUND` |
| `/api/music/{musicId}/reviews` | POST | 필요 | `CreateRequest` | `ReviewResponse` | `create(Long, MusicReview)` | `DUPLICATE_MUSIC_REVIEW`, `INVALID_SCORE` |
| `/api/music-reviews/{reviewId}` | PUT | 필요 | `UpdateRequest` | `ReviewResponse` | `update(Long, MusicReview)` | `MUSIC_REVIEW_NOT_FOUND`, `ACCESS_DENIED` |
| `/api/music-reviews/{reviewId}` | DELETE | 필요 | 없음 | 없음 | `delete(Long, Long)` | `MUSIC_REVIEW_NOT_FOUND`, `ACCESS_DENIED` |

## 8. 플레이리스트 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/playlists` | GET | 선택 | `ownerId` query | `List<PlaylistResponse>` | `PlaylistService.getList` | - |
| `/api/playlists/{playlistId}` | GET | 선택 | 없음 | `DetailResponse` | `getDetail`, `getItems`, 음악 상세 조회 | `PLAYLIST_NOT_FOUND`, `PRIVATE_PLAYLIST` |
| `/api/playlists` | POST | 필요 | `CreateRequest` | `PlaylistResponse` | `PlaylistService.create` | `INVALID_PLAYLIST_TITLE` |
| `/api/playlists/{playlistId}` | PUT | 필요 | `UpdateRequest` | `PlaylistResponse` | `PlaylistService.update` | `PLAYLIST_NOT_FOUND`, `ACCESS_DENIED` |
| `/api/playlists/{playlistId}` | DELETE | 필요 | 없음 | 없음 | `PlaylistService.delete` | `PLAYLIST_NOT_FOUND`, `ACCESS_DENIED` |

플레이리스트 상세 수록곡은 실제 `PlaylistItem`이 음악 ID만 제공하므로 Controller가 음악 상세를 DTO로 조립한다.

## 9. 플레이리스트 수록곡 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/playlists/{playlistId}/items` | GET | 선택 | 없음 | `List<ItemResponse>` | `PlaylistItemService.getItems` | `PRIVATE_PLAYLIST` |
| `/api/playlists/{playlistId}/items` | POST | 필요 | `ItemCreateRequest` | `ItemResponse` | `PlaylistItemService.add` | `DUPLICATE_PLAYLIST_ITEM`, `DUPLICATE_PLAYLIST_ORDER`, `SPOTIFY_URI_NOT_FOUND` |
| `/api/playlist-items/{itemId}/order` | PATCH | 필요 | `ItemOrderUpdateRequest` | `ItemResponse` | `updateOrder(Long, Long, Integer)` | `PLAYLIST_ITEM_NOT_FOUND`, `DUPLICATE_PLAYLIST_ORDER` |
| `/api/playlist-items/{itemId}` | DELETE | 필요 | 없음 | 없음 | `delete(Long, Long)` | `PLAYLIST_ITEM_NOT_FOUND`, `ACCESS_DENIED` |

## 10. 플레이리스트 저장 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/playlist-saves/me` | GET | 필요 | 없음 | `List<PlaylistResponse>` | `getMySavedList(Long)` | `AUTH_REQUIRED` |
| `/api/playlist-saves/{playlistId}` | POST | 필요 | 없음 | `SaveResponse` | `save(Long, Long)` | `DUPLICATE_SAVE`, `PRIVATE_PLAYLIST` |
| `/api/playlist-saves/{playlistId}` | DELETE | 필요 | 없음 | 없음 | `cancel(Long, Long)` | `ACCESS_DENIED` |

자동 큐레이션 저장은 이 API와 `playlist_saves`를 사용하지 않는다.

## 11. 자동 큐레이션 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/recommendations` | GET | 필요 | `RecommendRequest` | `ResultResponse` | `RecommendationService.recommend` | `PROFILE_NOT_FOUND`, `INVALID_INTENSITY`, `RECOMMENDATION_DATA_NOT_ENOUGH` |
| `/api/recommendations/logs/me` | GET | 필요 | 없음 | `List<LogResponse>` | `getMyRecommendationLogs(Long)` | `AUTH_REQUIRED` |
| `/api/recommendations/{logId}/save` | POST | 필요 | `SaveRequest` | `SaveResponse` | `saveRecommendation(Long, Long, String, PlaylistVisibility)` | `RECOMMENDATION_LOG_NOT_FOUND`, `RECOMMENDATION_ALREADY_SAVED`, `ACCESS_DENIED` |

조회 API는 강도, 장르, 개수만 받는다. BPM은 실제 Service 계약대로 회원 러닝 프로필 값을 사용한다. 저장 응답은 `USER + CURATION_COPY` 플레이리스트를 반환한다.

## 12. Spotify 검색/OAuth/연결 API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/spotify/tracks/search` | GET | 불필요 | query `query`, 선택 `limit` | `List<TrackResponse>` | `SpotifyService.searchTracks` | `SPOTIFY_SEARCH_FAILED`, `INVALID_REQUEST` |
| `/api/spotify/oauth/authorize` | GET | 필요 | 없음 | JSON `AuthorizationResponse(authorizationUrl)` | `buildAuthorizationUrl(HttpSession)` | `SPOTIFY_CONFIGURATION_ERROR`, `SPOTIFY_AUTH_URL_ERROR` |
| `/api/spotify/oauth/callback` | GET | 필요 | query `code`, `error`, 필수 `state` | JSON `ConnectionResponse` | `handleCallback(...)` | `SPOTIFY_AUTH_DENIED`, `SPOTIFY_OAUTH_STATE_MISMATCH`, `SPOTIFY_OAUTH_CALLBACK_ERROR`, `SPOTIFY_TOKEN_EXPIRED` |
| `/api/spotify/me/connection` | GET | 필요 | 없음 | `ConnectionResponse` | `getConnection(Long)` | `AUTH_REQUIRED` |
| `/api/spotify/me/connection` | DELETE | 필요 | 없음 | 없음 | `disconnect(Long)` | `SPOTIFY_AUTH_REQUIRED` |

`TrackResponse` 필드:

- `spotifyTrackId`, `spotifyUri`, `spotifyExternalUrl`
- `title`, `artist`, `albumName`, `imageUrl`
- `bpm`, `genre`

현재 Spotify 검색 Service는 최대 10곡으로 제한하며 Spotify 검색 응답의 BPM과 장르는 보강하지 않아 `null`일 수 있다.

OAuth authorize Endpoint는 302 Redirect가 아니라 JSON으로 인증 URL을 반환한다. Vue는 해당 URL이 `https://accounts.spotify.com/authorize`인지 검증한 후 브라우저 전체 이동을 수행한다.

OAuth callback도 Vue로 Redirect하지 않고 연결 결과 JSON을 반환한다. 따라서 `/oauth/spotify/complete` 자동 복귀는 현재 Backend 계약만으로 동작하지 않으며, callback 성공·실패 후 민감 query를 제거하고 내부 Vue 경로로 이동시키는 Backend Redirect adapter가 추가로 필요하다.

연결 정보가 없을 때 Service의 `SPOTIFY_AUTH_REQUIRED`를 Controller Adapter가 정상 응답 `connected=false`로 변환한다.

`ConnectionResponse`에는 Access Token과 Refresh Token이 없다.

`ConnectionResponse` 필드는 `connected`, `spotifyUserId`, `tokenExpiresAt`뿐이다.

### Spotify 음악 DB 저장

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/music/from-spotify` | POST | 필요 | `SpotifyImportRequest` | `MusicResponse` | `SpotifyService.saveSelectedTrack` | `SPOTIFY_TRACK_NOT_FOUND`, `INVALID_BPM_VALUE`, `AUTH_REQUIRED` |

`SpotifyImportRequest`는 Spotify 식별자와 표시 메타데이터를 받으며 `spotifyTrackId`, `spotifyUri`, `title`, `artist`는 필수다. 같은 `spotifyTrackId`가 이미 있으면 Service가 기존 음악을 조회해 보강하고 동일한 내부 `musicId`를 반환한다. 이 응답의 내부 `id`만 플레이리스트 수록곡 추가 API의 `musicId`로 사용한다.

## 13. Spotify Export API

| URI | Method | 인증 | Request DTO | Response DTO | 실제 Service | 대표 ErrorCode |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/playlists/{playlistId}/export/spotify` | POST | 필요 | 없음 | `PlaylistApiDtos.ExportResponse` | `PlaylistExportService.export` | `SPOTIFY_AUTH_REQUIRED`, `SPOTIFY_URI_NOT_FOUND`, `SPOTIFY_PLAYLIST_CREATE_FAILED`, `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED`, `SPOTIFY_ALREADY_EXPORTED` |

응답은 Service가 보장하는 Playlist ID, Spotify ID/URL, 상태, 오류 코드, 완료 시각만 제공한다. 곡별 성공/실패 수는 추정하지 않는다.

`ExportResponse` 필드:

- `playlistId`
- `spotifyPlaylistId`
- `spotifyPlaylistUrl`
- `spotifyExportStatus`
- `spotifyExportErrorCode`
- `spotifyExportedAt`

부분 성공 곡 수와 실패 곡 수는 응답에 없다. Spotify 플레이리스트 생성 후 곡 추가가 실패하면 Service는 `FAILED`, Spotify ID/URL, `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED`를 저장한 뒤 오류 응답을 반환할 수 있다. 이 상태는 자동 재시도가 금지되며 저장된 상세 상태와 URL을 표시해야 한다.

## 14. Controller 패키지 규칙

```text
<domain>/controller/api   REST API Controller
member/controller/view   JSP Controller 전용
<domain>/dto             Request/Response DTO
common/response          공통 성공/오류/페이징 응답
common/config            CORS 등 Web 설정
```

JSP Controller와 REST Controller를 같은 클래스에 혼합하지 않는다.

## 15. 팀원 A 확인 요청 사항

Service 변경 없이 현재 Controller Adapter로 동작하지만 다음 정책은 공동 확정이 필요하다.

1. 공통 오류 응답을 기존 `{code, message}`로 최종 고정할지 여부
2. 자동 큐레이션 화면에서 직접 BPM 입력을 제거할지 여부
3. Spotify 미연결을 200 `connected=false`로 최종 고정할지 여부
4. Export 응답에서 곡별 성공/실패 수를 MVP 제외할지 여부
5. 본인 소유 비공개 플레이리스트의 참조 저장 허용 여부
6. Spotify 검색 결과의 BPM/장르 보강 정책
7. 실패 후 Spotify ID/URL이 남은 Export의 복구 정책

현재 Controller 구축에 반드시 필요한 신규 Service 메서드는 없으므로 `a-interface-request.md`는 생성하지 않았다.

## 16. 검증 범위

- Controller/DTO 컴파일
- 공통 성공·오류 JSON 직렬화
- Spotify 음악 저장 DTO의 민감 필드 비노출
- Spotify 미연결 Adapter
- Vite origin의 credential CORS preflight
- 비로그인 보호 API의 401 `AUTH_REQUIRED`
- Vue/Vite production build
- 전체 기존 Backend 테스트 회귀

Swagger/OpenAPI는 기존 의존성과 설정이 없어 이번 단계에서 확인 대상이 없다.
