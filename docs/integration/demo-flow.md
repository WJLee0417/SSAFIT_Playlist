# SSAFIT Playlist 발표 시연 흐름

## 1. 사전 준비

실행:

```powershell
cd ssafit
.\mvnw.cmd spring-boot:run

cd ..\frontend
npm.cmd run dev
```

준비 항목:

- MySQL `ssafit_playlist`와 schema/data/dummy 적용
- 시연용 회원 또는 현장에서 신규 회원가입
- Spotify 시연 시 Client ID, Client Secret, Redirect URI 등록
- Backend `http://localhost:8080`
- Vue `http://localhost:5173`

dummy 회원의 비밀번호 해시는 실제 로그인용이 아니므로, 발표에서는 새 회원을 가입한다.

## 2. 기본 MVP 시연 순서

### 1) 회원가입

- 화면: `http://localhost:8080/join`
- 입력: 고유 로그인 ID, 8자 이상 비밀번호, 이메일, 닉네임
- 기대: 로그인 화면으로 이동하며 가입 완료 메시지
- 확인: 중복 ID/이메일은 409 계약에 대응하는 필드 오류

### 2) JSP 로그인과 Vue 이동

- 화면: `/login`
- 기대: `JSESSIONID` 발급 후 마이페이지 또는 원래 Vue 경로 복귀
- 확인: 주소에 `;jsessionid`가 없어야 함
- 메뉴의 음악 또는 플레이리스트를 클릭해 Vue로 이동

### 3) 러닝 프로필

- 화면: `/members/me/running-profile`
- 강도, BPM 최소/최대, 장르 저장
- Vue 추천 화면에서 왔다면 저장 후 `/recommendations`로 복귀

### 4) 음악 목록과 상세

- 화면: `http://localhost:5173/music`
- 검색, 장르, BPM, 정렬, 페이징 확인
- 음악 카드 클릭 후 상세와 평가 평균/목록 확인

### 5) 음악 평가 CRUD

- 상세에서 평가 작성
- 같은 평가 수정
- 본인 평가 삭제
- 비로그인/타인 수정은 각각 401/403으로 구분됨을 설명

### 6) 플레이리스트

- `/playlists/new`에서 PUBLIC 플레이리스트 생성
- 상세에서 제목/설명 수정
- 수록곡 추가 및 순서 변경
- 중복 곡 추가 시 별도 충돌 메시지 확인

### 7) 공개 플레이리스트 참조 저장

- 다른 시연 계정으로 로그인
- 공개 플레이리스트 상세에서 “내 목록에 저장”
- `/playlists/saved`에 나타나는지 확인
- 개인 생성 목록과 참조 저장 목록은 다른 개념임을 설명

### 8) 자동 큐레이션 조회

- `/recommendations`
- 강도/장르/곡 수 입력 후 조회
- 로그는 생성되지만 개인 플레이리스트는 아직 생성되지 않음을 최근 로그에서 확인

### 9) 자동 큐레이션 저장

- 결과에서 개인 플레이리스트 생성
- 생성 결과가 `USER + CURATION_COPY`인지 상세에서 확인
- 최근 로그의 `savedPlaylistId` 링크 확인
- 참조 저장(`playlist_saves`)이 아니라 새 개인 플레이리스트임을 설명

## 3. Spotify 시연 순서

### 검색 및 내부 저장

1. 소유 플레이리스트 상세에서 “Spotify 곡 검색”
2. 실제 검색 결과 선택
3. 내부 `music` 저장 후 반환된 music ID로 수록곡 추가
4. 상세 재조회에서 곡이 표시되는지 확인

### OAuth

1. “Spotify 연결”
2. Backend에서 받은 `accounts.spotify.com/authorize` URL로 브라우저 전체 이동
3. 승인/거부
4. Backend callback이 `/oauth/spotify/complete`로 302 복귀
5. 완료 화면이 연결 상태 API를 다시 조회

URL에 Authorization Code, state, token이 남아 있으면 시연을 중단한다.

### Export

1. 연결됨 상태 확인
2. Spotify URI가 있는 곡만 포함된 소유 플레이리스트 준비
3. “Spotify로 내보내기”
4. 상세 재조회에서 `SUCCESS`와 Spotify URL 확인
5. “Spotify에서 열기” 링크 확인

## 4. Spotify 실패 시 대체 시연

실 계정/환경변수가 없으면 성공으로 설명하지 않는다.

대체 순서:

1. API Tester에서 검색 호출의 502 `SPOTIFY_SEARCH_FAILED` 확인
2. 환경변수 누락 시 OAuth의 500 `SPOTIFY_CONFIGURATION_ERROR` 확인
3. 미연결 Export의 401 `SPOTIFY_AUTH_REQUIRED` 확인
4. 코드에서 Token 비노출 DTO 확인
5. 이미 저장된 Spotify source 음악과 URI가 있는 수록곡 상세 시연
6. OAuth callback 단위 테스트의 안전한 Vue redirect 결과 제시

한계:

- 실제 검색 결과 정확성
- 실제 사용자 승인과 Token 교환
- 실제 Spotify Playlist 생성
- 실제 Spotify URL 표시

위 네 항목은 실 환경 없이는 미검증이다.

## 5. API Tester 보조 검증

파일:

```text
docs/integration/ssafit-api-tester.http
```

권장 구간:

- 비로그인 `/api/members/me` → 401 JSON
- 타인 플레이리스트 수정 → 403
- 없는 음악 → 404
- 중복 수록곡 → 409
- Spotify 설정 누락 → 500
- 자동 큐레이션 조회 전후와 저장 후 응답 비교

## 6. 실패 시 대체 경로

- Vue가 열리지 않음: `npm install` 후 `npm run dev`
- 로그인 상태 미전달: Cookie와 CORS credentials 확인
- 추천 데이터 부족: `demo-snapshot.sql`, 러닝 프로필 BPM 확인
- Spotify 검색 실패: 환경변수 확인 후 API Tester 오류 응답만 시연
- OAuth callback 실패: ErrorCode가 포함된 완료 화면 복귀와 연결 재시도 UI 확인

## 7. 예상 질문

- “추천 조회만 해도 플레이리스트가 생기나요?”
  - 아니오. 로그만 생기며 저장 요청에서만 CURATION_COPY가 생긴다.
- “내 저장 목록과 자동 큐레이션 저장은 같은가요?”
  - 아니다. 전자는 기존 플레이리스트 참조 관계, 후자는 새 USER 플레이리스트다.
- “Spotify Token은 Vue에 저장하나요?”
  - 아니다. Session과 Backend DB에서만 처리하며 DTO에 노출하지 않는다.
- “JWT를 사용하나요?”
  - 아니다. HttpSession과 JSESSIONID를 사용한다.
- “부분 Export 실패는 자동 재시도하나요?”
  - Spotify ID/URL이 남은 실패는 중복 방지를 위해 자동 재시도하지 않는다.
