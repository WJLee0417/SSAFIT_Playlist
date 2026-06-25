# Spotify 로컬 연동 설정

## 1. Spotify Developer Dashboard 확인

Spotify Developer Dashboard에서 프로젝트용 App을 열고 다음을 확인한다.

- Client ID가 로컬 실행에 사용할 App의 값인지
- Client Secret이 활성 상태인지
- App Status가 Development Mode인지
- Development Mode라면 발표용 Spotify 계정이 Users Management 허용 목록에 등록되어 있는지
- Redirect URI가 아래 로컬 주소와 정확히 일치하는지

```text
http://127.0.0.1:8080/api/spotify/oauth/callback
```

신규 Spotify App의 loopback Redirect URI에는 `localhost` 대신 명시적인
`127.0.0.1`을 사용한다. 프로토콜, IP, 포트, 경로와 마지막 슬래시 여부까지
Dashboard 등록값과 환경 변수 값이 동일해야 한다.

## 2. 환경 변수

실제 값은 Git 추적 파일에 기록하지 않는다. Backend를 실행하는 STS Run
Configuration의 Environment 탭 또는 동일한 PowerShell 프로세스에 다음 값을
설정한다.

```text
SPOTIFY_CLIENT_ID=
SPOTIFY_CLIENT_SECRET=
SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/api/spotify/oauth/callback
```

PowerShell 예시:

```powershell
$env:SPOTIFY_CLIENT_ID="<로컬 Client ID>"
$env:SPOTIFY_CLIENT_SECRET="<로컬 Client Secret>"
$env:SPOTIFY_REDIRECT_URI="http://127.0.0.1:8080/api/spotify/oauth/callback"
```

Frontend도 동일한 loopback host를 사용한다.

```text
Frontend: http://127.0.0.1:5173
Backend:  http://127.0.0.1:8080
```

## 3. OAuth callback 흐름

```text
Vue Spotify 연결 버튼
→ 브라우저 전체 이동: GET /api/spotify/oauth/authorize
→ Backend가 state 생성 후 HttpSession 저장
→ Spotify /authorize로 302 redirect
→ 사용자 승인 또는 취소
→ GET /api/spotify/oauth/callback
→ Backend가 state 일치 여부 검증 및 일회 소비
→ authorization code를 access/refresh token으로 교환
→ /me 응답의 Spotify 사용자 ID를 프로젝트 로그인 회원 ID와 연결
→ token은 서버 DB에만 저장
→ http://127.0.0.1:5173/oauth/spotify/complete 로 redirect
→ Vue가 연결 상태 API를 다시 조회
```

callback URL에는 access token이나 refresh token을 포함하지 않는다. 실패 시에도
공개 가능한 ErrorCode만 Vue 완료 화면으로 전달한다.

## 4. 요청 scope

```text
user-read-private
playlist-read-private
playlist-modify-private
playlist-modify-public
```

- `user-read-private`: Spotify 로그인 및 계정 연결 시 현재 사용자 profile 조회
- `playlist-read-private`: Spotify private 플레이리스트 목록 조회
- `playlist-modify-private`: 비공개 Spotify 플레이리스트 생성·수정
- `playlist-modify-public`: 공개 Spotify 플레이리스트 생성·수정

현재 사용자 식별은 승인된 사용자 token으로 `/me`의 stable account ID를 우선 사용한다.
이메일, 재생 이력, 라이브러리 정보는 요청하지 않으므로 관련 scope를 추가하지 않는다.

## 5. Client Secret 관리

- Vue, JSP, JavaScript bundle, 정적 파일과 브라우저 저장소에 넣지 않는다.
- `application.yml`, 문서, 테스트 fixture, 로그와 화면 캡처에 실제 값을 남기지 않는다.
- `.env.example`에는 키 이름과 공개 가능한 Redirect URI만 둔다.
- 토큰 교환은 Backend에서만 수행한다.
- access token과 refresh token은 API 응답으로 반환하지 않는다.
- 커밋 전 `SPOTIFY_CLIENT_SECRET` 주변에 실제 값이 포함되지 않았는지 확인한다.

## 6. 연결 상태와 오류

- `CONNECTED`: 유효한 연결이며 필요 시 refresh까지 완료됨
- `DISCONNECTED`: 프로젝트 회원에 연결된 Spotify 정보가 없음
- `RECONNECT_REQUIRED`: refresh token이 없거나 갱신에 실패함

구분되는 주요 오류:

- `SPOTIFY_NOT_CONNECTED`
- `SPOTIFY_AUTHORIZATION_FAILED`
- `SPOTIFY_AUTHORIZATION_CANCELLED`
- `SPOTIFY_STATE_MISMATCH`
- `SPOTIFY_TOKEN_REFRESH_FAILED`
- `SPOTIFY_FORBIDDEN`
- `SPOTIFY_RATE_LIMITED`
- `SPOTIFY_API_FAILED`

사용자 token API가 401을 반환하면 서버 공통 실행 경로는 refresh 후 같은 작업을
정확히 한 번만 재시도한다. 두 번째 인증 실패는 재연결 필요 오류로 종료하며 무한
재시도하지 않는다.

## 7. 발표 계정 연결 전 체크리스트

- [ ] 발표용 Spotify 계정 이메일을 Development Mode Users Management에 등록했다.
- [ ] App 소유자와 발표 계정의 Spotify 이용 조건을 확인했다.
- [ ] Dashboard Redirect URI와 환경 변수 값이 정확히 같다.
- [ ] Frontend와 Backend를 모두 `127.0.0.1`로 접속한다.
- [ ] SSAFIT 회원으로 먼저 로그인한다.
- [ ] OAuth 동의 화면에 플레이리스트 수정 권한만 표시되는지 확인한다.
- [ ] 취소 시 연결 실패 상태가 표시되는지 확인한다.
- [ ] callback 이후 URL에 code, state, token이 남지 않는지 확인한다.
- [ ] 연결 상태에 Spotify 사용자 ID가 표시되는지 확인한다.
- [ ] 연결 해제 후 `DISCONNECTED`로 표시되는지 확인한다.
- [ ] 만료 또는 refresh 실패 시 `RECONNECT_REQUIRED`가 표시되는지 확인한다.
- [ ] Client Secret과 token이 콘솔, 네트워크 응답, 화면에 노출되지 않는지 확인한다.
