# Spotify 검색·OAuth·내보내기 UI 흐름

## 1. 실제 계약 요약

| 기능 | Method / URI | 실제 응답 및 주의점 |
| --- | --- | --- |
| 곡 검색 | `GET /api/spotify/tracks/search?query=&limit=` | 인증 불필요, 최대 10곡 |
| 내부 음악 저장 | `POST /api/music/from-spotify` | Session 필요, `MusicResponse.id`가 내부 music ID |
| 수록곡 추가 | `POST /api/playlists/{playlistId}/items` | 내부 `musicId`, `orderNo` 사용 |
| OAuth URL 조회 | `GET /api/spotify/oauth/authorize` | 302가 아닌 JSON `authorizationUrl` |
| OAuth Callback | `GET /api/spotify/oauth/callback` | Vue Redirect가 아닌 JSON `ConnectionResponse` |
| 연결 상태 | `GET /api/spotify/me/connection` | 미연결도 200 `connected=false` |
| 내보내기 | `POST /api/playlists/{playlistId}/export/spotify` | 곡별 성공/실패 수 없음 |

## 2. 전체 UI 흐름

### 2.1 플레이리스트 상세 진입

1. 플레이리스트 상세과 Session 회원 정보를 조회한다.
2. 현재 회원 ID와 `playlist.ownerId`가 같고 공식 플레이리스트가 아닐 때만 Spotify 곡 추가와 내보내기 UI를 제공한다.
3. 상세 수록곡의 가장 큰 `orderNo + 1`을 검색 화면의 다음 추가 순서로 전달한다.

### 2.2 Spotify 곡 검색

1. 상세 화면에서 `/spotify/search?playlistId={id}&nextOrder={n}&returnTo={internalPath}`로 이동한다.
2. 검색어 앞뒤 공백을 제거한다.
3. 빈 검색어는 API 호출 전에 차단한다.
4. 검색 중 버튼을 비활성화하고 요청 순번으로 늦은 응답 덮어쓰기를 막는다.
5. 결과가 빈 배열이면 empty 상태, `SPOTIFY_SEARCH_FAILED`면 검색 오류 상태로 구분한다.
6. 실제 `TrackResponse`의 제목, 아티스트, 앨범, 이미지, Track ID, URI만 표시한다.
7. 이미지와 앨범이 없으면 대체 UI를 표시한다.

### 2.3 선택 곡 DB 저장

선택한 Track DTO를 다음 Body로 변환한다.

```json
{
  "spotifyTrackId": "Spotify Track ID",
  "spotifyUri": "spotify:track:...",
  "spotifyExternalUrl": "선택",
  "title": "곡명",
  "artist": "아티스트",
  "albumName": "선택",
  "imageUrl": "선택",
  "bpm": null,
  "genre": null
}
```

`POST /api/music/from-spotify` 응답의 `MusicResponse.id`를 내부 `musicId`로 사용한다. 동일 Spotify Track이 이미 저장되어 있으면 실제 Service가 기존 곡을 반환하므로 정상 재사용한다.

### 2.4 내부 music ID로 수록곡 추가

내부 음악 저장 성공 후에만 다음 요청을 실행한다.

```json
POST /api/playlists/{playlistId}/items
{
  "musicId": 123,
  "orderNo": 4
}
```

Spotify Track ID나 Spotify URI를 `musicId`로 전달하지 않는다.

- DB 저장 실패: 수록곡 추가 요청을 실행하지 않는다.
- DB 저장 성공 후 수록곡 추가 실패: 내부 music ID는 보존됐음을 별도 문구로 안내한다.
- `DUPLICATE_PLAYLIST_ITEM`: 곡 저장 실패가 아니라 이미 수록된 곡으로 안내한다.
- 성공: 보존된 `returnTo` 상세 경로로 이동하며 상세 API가 곡 목록과 개수를 다시 조회한다.

## 3. OAuth 연결 상태와 이동

### 3.1 연결 상태 확인

플레이리스트 상세의 소유자 UI에서 `GET /api/spotify/me/connection`을 호출한다.

- `connected=true`: 내보내기 가능 상태
- `connected=false`: Spotify 연결 필요
- `AUTH_REQUIRED`: SSAFIT 로그인 필요
- 기타 실패: 연결 상태 미확정이며 연결됨으로 추정하지 않음
- `SPOTIFY_TOKEN_EXPIRED`: 재인증 필요

### 3.2 OAuth 시작

현재 Backend authorize Endpoint는 302가 아니라 JSON URL 응답이다.

```text
GET /api/spotify/oauth/authorize
→ authorizationUrl 수신
→ origin/path가 https://accounts.spotify.com/authorize인지 검증
→ window.location.assign(authorizationUrl)
```

브라우저 이탈 전에 인증 정보가 아닌 내부 `returnTo` 경로만 `sessionStorage`에 저장한다. 허용 경로는 플레이리스트 상세, Spotify 검색, OAuth 완료 화면으로 제한하며 외부 URL과 `//` 경로는 거부한다.

### 3.3 Callback 이후 Vue 복귀

Spotify 설정의 Redirect URI는 현재 `/api/spotify/oauth/callback`이다. Callback Controller는 `code`, `error`, `state`를 처리하고 연결 JSON을 반환하지만 Vue로 Redirect하지 않는다.

따라서 현재 실제 흐름은 다음 지점에서 멈춘다.

```text
Spotify
→ Backend /api/spotify/oauth/callback
→ 연결 처리
→ JSON ConnectionResponse 표시
```

Vue 자동 복귀를 위해 팀원 A/B가 확정해야 하는 Adapter:

```text
Backend callback 성공/실패 처리
→ code/state/token을 URL에서 제거
→ /oauth/spotify/complete 같은 고정 Vue 경로로 302 Redirect
→ Vue가 sessionStorage의 내부 returnTo 복원
→ GET /api/spotify/me/connection 재조회
→ 실제 연결 결과 표시
```

Vue의 `/oauth/spotify/complete` 화면은 연결 상태 재조회와 안전한 내부 복귀를 이미 준비하지만, Backend Redirect adapter가 없으므로 실 OAuth 자동 복귀 완료로 간주하지 않는다.

## 4. Spotify 내보내기

1. 소유자이며 사용자 플레이리스트인지 확인한다.
2. 연결 상태가 확인되기 전에는 내보내기 버튼을 제공하지 않는다.
3. `connected=true`에서만 `POST /api/playlists/{playlistId}/export/spotify`를 호출한다.
4. 요청 중 버튼을 비활성화한다.
5. 성공 응답 후 상세을 재조회하고 `spotifyPlaylistUrl`이 있을 때만 “Spotify에서 열기” 링크를 제공한다.
6. `SUCCESS` 상태는 다시 내보내지 않는다.
7. `PENDING` 또는 Spotify ID/URL이 남은 실패 상태는 자동 재시도를 차단한다.
8. `FAILED`이면서 ID/URL이 없는 생성 실패 상태만 실제 Service 정책상 재시도할 수 있다.

## 5. 부분 실패 표현

실제 `ExportResponse`에는 성공 곡 수와 실패 곡 수가 없다. 따라서 숫자를 추정하거나 표시하지 않는다.

- `SPOTIFY_PLAYLIST_CREATE_FAILED`: 외부 플레이리스트 생성 실패
- `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED`: 외부 플레이리스트는 생성됐지만 일부 또는 전체 곡 추가 실패 가능
- 실패 후 상세 응답의 Spotify ID/URL, `FAILED`, 오류 코드를 함께 표시
- 외부 URL이 존재하면 사용자가 실제 Spotify 상태를 확인할 수 있도록 링크 제공
- 불완전한 외부 플레이리스트에 자동 재내보내기하지 않음

## 6. ErrorCode별 UI

| ErrorCode | 안내 및 후속 행동 |
| --- | --- |
| `SPOTIFY_AUTH_REQUIRED` | Spotify 연결 후 재시도 |
| `SPOTIFY_AUTH_DENIED` | 인증 취소 안내, 재연결 가능 |
| `SPOTIFY_TOKEN_EXPIRED` | 연결 만료, 재인증 |
| `SPOTIFY_SEARCH_FAILED` | 검색 일시 실패, 재시도 |
| `SPOTIFY_TRACK_NOT_FOUND` | 선택 곡을 찾을 수 없음 |
| `SPOTIFY_URI_NOT_FOUND` | URI가 없어 수록/내보내기 불가 |
| `SPOTIFY_PLAYLIST_CREATE_FAILED` | 외부 플레이리스트 생성 실패 |
| `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED` | 외부 생성 후 곡 추가 실패, 자동 재시도 금지 |
| `SPOTIFY_ALREADY_EXPORTED` | 기존 내보내기 URL 사용 |
| `SPOTIFY_EXPORT_FAILED` | 이전 외부 상태가 남아 수동 확인 필요 |
| `DUPLICATE_PLAYLIST_ITEM` | 이미 플레이리스트에 포함된 곡 |

## 7. 민감 정보 비노출

Vue 상태, Router query, 화면, 문서에 다음 값을 저장하거나 출력하지 않는다.

- Access Token
- Refresh Token
- Client Secret
- Authorization Code
- OAuth state 원문

연결 응답에서 사용하는 값은 `connected`, `spotifyUserId`, `tokenExpiresAt`뿐이다. OAuth 복귀 경로 저장에는 내부 Router 경로만 사용한다.

## 8. 실 API 검증 전제와 한계

필요 환경:

- `SPOTIFY_CLIENT_ID`
- `SPOTIFY_CLIENT_SECRET`
- `SPOTIFY_REDIRECT_URI=http://localhost:8080/api/spotify/oauth/callback`
- Spotify Developer Dashboard의 동일 Redirect URI 등록
- 테스트용 Spotify 계정
- Session 로그인과 Spotify URI가 있는 플레이리스트 수록곡

실 Spotify 환경이 없으면 검색 결과, OAuth 승인, 토큰 갱신, 외부 Playlist 생성과 곡 추가 성공을 검증 완료로 기록하지 않는다. 로컬 대체 서버를 사용하는 UI 검증은 요청 순서, 상태 전환, DTO 변환과 오류 분기를 확인하는 용도로만 사용하며 제품 코드에 포함하지 않는다.

## 9. 팀원 A 확인 요청

1. OAuth callback 성공·실패 후 Vue 완료 화면으로 Redirect하는 Adapter가 필요하다.
2. Redirect 시 OAuth code, state, token을 Vue URL에 포함하지 않아야 한다.
3. `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED` 응답에 성공/실패 곡 수를 추가할지, MVP에서 제외할지 확정이 필요하다.
4. 실패 후 Spotify ID/URL이 남은 내보내기의 삭제·복구·재시도 정책이 필요하다.
5. 연결 상태 API에서 만료된 연결을 `connected=true`로 반환할지, 유효성 검사 후 만료 상태를 제공할지 확정이 필요하다.
6. 검색 결과 BPM·장르를 계속 `null`로 둘지 보강 정책이 필요하다.

## 10. 이번 작업 검증 결과

로컬 대체 응답 서버로 완료:

- 빈 검색어 API 호출 전 차단
- 빈 검색 결과 화면 검증 및 `SPOTIFY_SEARCH_FAILED` 별도 오류 매핑 확인
- 이미지·앨범 정보가 없는 검색 결과 대체 UI
- 선택 Track을 `POST /api/music/from-spotify`로 저장하고 내부 `musicId=501` 확보
- 내부 `musicId`로만 수록곡 추가
- 동일 Track 재선택 시 같은 내부 music ID 재사용
- 동일 곡 수록 시 DB 저장 성공과 `DUPLICATE_PLAYLIST_ITEM` 실패를 분리해 안내
- 수록곡 추가 성공 후 상세 복귀 및 곡 개수 재조회
- `connected=false`, 연결 상태 조회 실패, OAuth 완료 화면의 연결 재조회
- OAuth 버튼 클릭 시 `accounts.spotify.com/authorize` 흐름으로 브라우저 전체 이동
- 정상 내보내기 후 `SUCCESS`와 Spotify URL 표시
- `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED` 후 `FAILED`, ErrorCode, 남은 URL 표시와 자동 재시도 차단
- `SPOTIFY_ALREADY_EXPORTED` 409 안내

실제 Service/기존 테스트 코드로 확인:

- 동일 Spotify Track ID는 기존 음악을 반환하고 필요한 메타데이터를 보강
- 내보내기 성공 상태 재실행은 `SPOTIFY_ALREADY_EXPORTED`
- Spotify 플레이리스트 생성 후 곡 추가 실패 시 실패 상태와 외부 ID/URL 저장
- Access/Refresh Token은 Response DTO에 포함되지 않음

환경 제약으로 미검증:

- 실제 Spotify 검색 API 결과
- 실제 Spotify 계정 승인·거부와 Authorization Code 교환
- 실제 Refresh Token 갱신과 만료 처리
- 실제 Spotify Playlist 생성 및 수록곡 추가
- Callback 이후 Vue 자동 복귀: Backend Redirect adapter 미구현
- 실제 Session Cookie와 MySQL을 사용하는 전체 브라우저 통합
- 부분 성공 곡 수: 실제 Response DTO가 제공하지 않음

빌드:

- Frontend `npm run build`: 성공
- Backend `mvnw test`: 현재 실행 Java 17이 프로젝트의 `release 25`를 지원하지 않아 컴파일 시작 단계에서 실패
