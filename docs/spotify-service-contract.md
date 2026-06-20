# Spotify Service 연동 계약

이 문서는 팀원 B의 Controller, Request/Response DTO, Vue API 모듈 및 화면 구현을
위한 내부 Service 계약이다. Controller와 DTO의 구체적인 클래스명·URL은 팀원 B가
정하되, 아래 Service 입력·출력과 ErrorCode 의미는 변경하지 않는다.

## 1. 공통 전제

### 인증

- 애플리케이션 인증은 `HttpSession` 기반이다.
- 로그인 회원 ID는 `SessionConst.LOGIN_MEMBER_ID`에서 읽는다.
- Service에는 클라이언트가 보낸 회원 ID가 아니라 세션의 회원 ID를 전달한다.
- Spring Security와 JWT는 사용하지 않는다.
- 로그인 회원 ID가 없으면 `AUTH_REQUIRED`가 발생한다.

### 환경 변수

```text
SPOTIFY_CLIENT_ID
SPOTIFY_CLIENT_SECRET
SPOTIFY_REDIRECT_URI
```

- 세 값은 실행 환경에서만 주입한다.
- Client ID와 Client Secret은 코드, DTO, 응답, 로그, Git에 넣지 않는다.
- Redirect URI는 Spotify Developer Dashboard 등록값과 문자 단위로 일치해야 한다.
- 로컬 기본 예시는 `http://localhost:8080/api/spotify/oauth/callback`이다.
- `application.yml`에는 실제 값이나 Redirect URI 기본값을 두지 않는다.

### Spotify scope

현재 설정:

```text
user-read-private
playlist-modify-private
playlist-modify-public
```

- 비공개·공개 플레이리스트 생성을 모두 지원하므로 두 playlist scope가 필요하다.
- scope 변경 시 Spotify 재동의가 필요할 수 있다.

### 토큰 보안

- Access Token, Refresh Token, Authorization Code, Client Secret을 API 응답으로
  반환하지 않는다.
- `SpotifyConnection`을 그대로 Response DTO로 노출하지 않는다.
- 연결 상태 응답에는 연결 여부, Spotify 사용자 ID처럼 필요한 비민감 정보만 담는다.
- 토큰과 Authorization Code를 로그, 예외 메시지, Vue 상태 저장소에 기록하지 않는다.
- 실제 Spotify API 오류 본문을 그대로 클라이언트에 전달하지 않는다.

## 2. 기능별 Service 계약

### 2.1 Spotify 곡 검색

호출:

```java
List<SpotifyTrackMetadata> result =
        spotifyService.searchTracks(query, limit);
```

입력:

- `query`: 필수. 공백만 있는 값은 허용하지 않는다.
- `limit`: 선택. 기본값 10, 최대값 10이다. 10보다 크면 10으로 제한한다.

성공 결과:

- 검색 결과가 없으면 빈 목록을 반환한다.
- Track ID 또는 URI가 없는 Spotify 항목은 결과에서 제외한다.
- `title`, `artist`, `albumName`, `imageUrl`, `spotifyExternalUrl`은 `null`일 수 있다.
- `spotifyTrackId`, `spotifyUri`는 반환 항목의 필수 식별자다.

대표 ErrorCode:

- `INVALID_REQUEST`: 검색어가 비었거나 limit이 1보다 작음
- `SPOTIFY_SEARCH_FAILED`: Client Token 발급 또는 Spotify 검색 API 실패

Controller/DTO 주의:

- 빈 목록을 404로 바꾸지 않는다.
- nullable 표시 필드를 DTO 변환 중 강제로 역참조하지 않는다.

### 2.2 선택 Spotify 곡 DB 저장

호출:

```java
Music music = spotifyService.saveSelectedTrack(metadata);
```

`metadata` 입력 필드:

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `spotifyTrackId` | 필수 | Spotify Track ID, 중복 판정 기준 |
| `spotifyUri` | 필수 | Export에 사용하는 `spotify:track:...` URI |
| `title` | 필수 | 곡명 |
| `artist` | 필수 | 대표 아티스트 |
| `spotifyExternalUrl` | 선택 | Spotify 외부 URL |
| `albumName` | 선택 | 앨범명 |
| `imageUrl` | 선택 | 앨범 이미지 |
| `bpm` | 선택 | 60~220 |
| `genre` | 선택 | 장르 |

성공 결과:

- 최초 저장이면 `sourceType=SPOTIFY`인 `Music`을 생성한다.
- 동일 `spotifyTrackId`가 존재하면 기존 `Music`을 반환한다.
- 기존 행의 비어 있는 메타데이터는 입력값으로 보강할 수 있다.
- DB UNIQUE 제약과 Service 재조회로 동시 중복 insert를 방어한다.

대표 ErrorCode:

- `SPOTIFY_TRACK_NOT_FOUND`: 필수 Spotify 식별자·곡명·아티스트 누락
- `INVALID_BPM_VALUE`: BPM 범위 오류

### 2.3 Spotify OAuth 인증 URL 생성

호출:

```java
String authorizationUrl =
        spotifyOAuthService.buildAuthorizationUrl(httpSession);
```

입력:

- authorize 요청과 callback 요청 사이에 유지되는 `HttpSession`

성공 결과:

- Spotify authorize URL 문자열
- Service가 랜덤 state를 생성해 Session에 저장한다.
- Controller는 state를 직접 생성하거나 별도 속성으로 중복 관리하지 않는다.
- 성공 응답은 URL 문자열 또는 `{ authorizationUrl }` 형태로 감쌀 수 있다.

대표 ErrorCode:

- `SPOTIFY_CONFIGURATION_ERROR`: 필수 환경 변수 누락
- `SPOTIFY_AUTH_URL_ERROR`: 인증 URL 생성 실패

### 2.4 OAuth callback 처리

호출:

```java
SpotifyConnection connection = spotifyOAuthService.handleCallback(
        loginMemberId,
        authorizationCode,
        error,
        returnedState,
        httpSession);
```

입력:

- `loginMemberId`: Session에서 읽은 로그인 회원 ID
- `authorizationCode`: Spotify callback의 `code`
- `error`: Spotify callback의 `error`
- `returnedState`: Spotify callback의 `state`
- `httpSession`: authorize 요청과 동일한 Session

성공 결과:

- 회원별 Spotify 연결 정보 insert 또는 update
- callback state는 검증 후 제거되어 재사용할 수 없다.
- 같은 계정 재연결에서 새 Refresh Token이 없으면 기존 값을 유지한다.

Controller 응답 주의:

- 반환된 `SpotifyConnection`을 그대로 직렬화하지 않는다.
- 토큰을 제외한 연결 성공 여부만 응답한다.

대표 ErrorCode:

- `SPOTIFY_AUTH_DENIED`: 사용자가 권한 동의를 거부
- `SPOTIFY_OAUTH_STATE_MISMATCH`: state 누락·불일치·재사용
- `SPOTIFY_OAUTH_CALLBACK_ERROR`: Spotify callback 오류
- `SPOTIFY_AUTH_REQUIRED`: code 누락 또는 재인증 필요
- `SPOTIFY_TOKEN_EXPIRED`: 유효한 Token을 확보할 수 없음

### 2.5 Spotify 연결 상태 조회와 해제

조회:

```java
SpotifyConnection connection =
        spotifyOAuthService.getConnection(loginMemberId);
```

유효 Token이 필요한 내부 처리:

```java
SpotifyConnection connection =
        spotifyOAuthService.getValidConnection(loginMemberId);
```

연결 해제:

```java
spotifyOAuthService.disconnect(loginMemberId);
```

연결 상태 응답 권장 필드:

```text
connected
spotifyUserId
tokenExpiresAt
```

- Access Token과 Refresh Token은 제외한다.
- 일반 상태 조회에는 `getConnection`을 사용한다.
- Spotify API 호출 직전에는 만료 갱신이 포함된 `getValidConnection`을 사용한다.
- 연결 해제는 로컬 `spotify_connections` 행만 삭제한다.
- Spotify 계정 권한 철회 API는 현재 범위에 포함되지 않는다.

대표 ErrorCode:

- `SPOTIFY_AUTH_REQUIRED`: 연결 정보 없음
- `SPOTIFY_TOKEN_EXPIRED`: Refresh Token 없음 또는 갱신 실패

### 2.6 Spotify 플레이리스트 Export

호출:

```java
Playlist result =
        playlistExportService.export(loginMemberId, playlistId);
```

입력:

- `loginMemberId`: Session에서 읽은 로그인 회원 ID
- `playlistId`: 내보낼 사용자 플레이리스트 ID

사전 조건:

- 로그인 회원이 플레이리스트 소유자여야 한다.
- Spotify 연결과 유효한 Access Token이 있어야 한다.
- 모든 수록곡에 `spotifyUri`가 있어야 한다.
- 수록곡은 Spotify 제한에 맞춰 100개 단위로 추가한다.

상태 전이:

```text
NOT_EXPORTED → PENDING → SUCCESS
PENDING → FAILED
FAILED(외부 ID 없음) → PENDING 재시도 가능
FAILED(외부 ID 있음) → 자동 재시도 금지
```

성공 결과:

- 최신 `Playlist`
- `spotifyPlaylistId`, `spotifyPlaylistUrl`, `spotifyExportStatus=SUCCESS`,
  `spotifyExportedAt`이 설정된다.

대표 ErrorCode:

- `PLAYLIST_NOT_FOUND`: 대상 없음 또는 USER 플레이리스트가 아님
- `ACCESS_DENIED`: 소유자 아님
- `SPOTIFY_AUTH_REQUIRED`: Spotify 연결 없음
- `SPOTIFY_TOKEN_EXPIRED`: Token 갱신 실패
- `SPOTIFY_URI_NOT_FOUND`: 수록곡이 없거나 URI 누락 곡 존재
- `SPOTIFY_PLAYLIST_CREATE_FAILED`: Spotify Playlist 생성 실패
- `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED`: Playlist 생성 후 곡 추가 실패
- `SPOTIFY_EXPORT_FAILED`: 상태 선점 실패, 부분 실패 자동 재시도 차단 등 일반 Export 실패
- `SPOTIFY_ALREADY_EXPORTED`: 이미 성공한 Playlist 재요청

### 2.7 Spotify Export 결과 조회

별도 Spotify API 호출 없이 기존 플레이리스트 상세 조회 결과의 Export 필드를 사용한다.

예시 Service 흐름:

```java
Playlist playlist = playlistService.getDetail(loginMemberId, playlistId);
```

Controller는 기존 공개/비공개 조회 권한 정책을 유지해야 한다.

## 3. Export 결과 필드 계약

### 현재 Domain/DB 필드

| 필드 | 의미 |
| --- | --- |
| `spotifyPlaylistId` | Spotify에서 생성된 Playlist ID |
| `spotifyPlaylistUrl` | 사용자에게 표시할 Spotify Playlist URL |
| `spotifyExportStatus` | `NOT_EXPORTED`, `PENDING`, `SUCCESS`, `FAILED` |
| `spotifyExportErrorCode` | 실패 원인을 나타내는 비민감 ErrorCode 이름 |
| `spotifyExportedAt` | `SUCCESS` 완료 시각. 그 외 상태에서는 `null` |

부분 실패에서는 `spotifyExportStatus=FAILED`이면서 Spotify ID와 URL이 존재할 수
있다. 이는 외부 Playlist가 이미 생성되었음을 의미하므로 URL을 숨기지 말고 실패
안내와 함께 표시할 수 있다.

### 추가 성공·실패 곡 수

현재 Service와 DB는 배치별 추가 성공 곡 수·실패 곡 수를 별도로 저장하지 않는다.
현재 계약은 다음과 같다.

- `SUCCESS`: 검증된 URI 전부 추가 성공. 성공 곡 수는 Export 직전 URI 수와 같다.
- `FAILED`: 실패 배치 이전 일부 곡이 Spotify에 추가됐을 수 있으나 정확한 성공·실패
  수를 응답하지 않는다.
- 실패 사유는 `spotifyExportErrorCode`로 제공한다.

팀원 B가 Response DTO에 아래 필드를 두는 것은 가능하지만, 현재 값을 추정해서
채우면 안 된다.

```text
addedTrackCount     // SUCCESS일 때만 전체 URI 수로 채울 수 있음
failedTrackCount    // 현재 정확한 값 미지원, 실패 시 null 권장
failureReason       // spotifyExportErrorCode 사용
```

정확한 배치별 카운트가 필요하면 별도 DB 컬럼 또는 Export 이력 테이블을 추가하는
후속 합의가 필요하다.

## 4. 공유 ErrorCode

| ErrorCode | HTTP 상태 | UI 처리 기준 |
| --- | --- | --- |
| `SPOTIFY_AUTH_REQUIRED` | 401 | Spotify 연결 버튼 표시 |
| `SPOTIFY_AUTH_DENIED` | 403 | 사용자가 동의를 거부했음을 안내 |
| `SPOTIFY_TOKEN_EXPIRED` | 401 | 재인증 유도 |
| `SPOTIFY_SEARCH_FAILED` | 502 | 잠시 후 검색 재시도 안내 |
| `SPOTIFY_TRACK_NOT_FOUND` | 404 | 선택 곡 정보 재확인 |
| `SPOTIFY_PLAYLIST_CREATE_FAILED` | 502 | Playlist 생성 실패 안내 |
| `SPOTIFY_PLAYLIST_ITEM_ADD_FAILED` | 502 | 부분 실패 가능성과 URL 확인 안내 |
| `SPOTIFY_ALREADY_EXPORTED` | 409 | 저장된 Spotify URL 표시 |

함께 처리할 관련 코드:

- `SPOTIFY_EXPORT_FAILED`
- `SPOTIFY_URI_NOT_FOUND`
- `SPOTIFY_OAUTH_STATE_MISMATCH`
- `SPOTIFY_OAUTH_CALLBACK_ERROR`
- `SPOTIFY_CONFIGURATION_ERROR`
- `SPOTIFY_AUTH_URL_ERROR`
- `AUTH_REQUIRED`
- `ACCESS_DENIED`
- `PLAYLIST_NOT_FOUND`

## 5. 팀원 B API/UI 흐름

```text
Spotify 검색
→ 선택 곡 DB 저장
→ 서비스 플레이리스트 수록곡 추가
→ Spotify OAuth 연결
→ Spotify로 내보내기
→ Spotify URL 표시
```

권장 화면 동작:

1. 검색 결과에서 곡을 선택하면 먼저 DB 저장 Service를 호출한다.
2. 반환된 `music.id`로 기존 플레이리스트 수록곡 추가 흐름을 호출한다.
3. Export 전에 연결 상태를 조회한다.
4. 미연결이면 OAuth URL을 받아 같은 브라우저 Session에서 인증을 진행한다.
5. callback 성공 후 원래 Export 화면으로 이동한다.
6. Export 성공 시 `spotifyPlaylistUrl` 링크를 표시한다.
7. 실패 상태에 ID/URL이 있으면 부분 실패임을 알리고 URL과 실패 코드를 함께 표시한다.
8. 성공 건을 재요청하지 않고 기존 URL을 사용한다.

## 6. 변경 금지 경계

- Controller, Request DTO, Response DTO, Vue API Module은 이 문서 작업 범위에서
  구현하거나 수정하지 않는다.
- Controller는 Token이나 Secret을 Service에서 꺼내 응답하지 않는다.
- Controller에서 OAuth state 또는 Token 만료 로직을 중복 구현하지 않는다.
- Vue에서 Spotify Access Token을 저장하거나 Spotify API를 직접 호출하지 않는다.

실제 계정을 사용하는 검증은
[`spotify-manual-integration-checklist.md`](./spotify-manual-integration-checklist.md)를
따른다.
