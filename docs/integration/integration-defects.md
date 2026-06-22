# UI/API 통합 결함 목록

## [P1][해결됨] Frontend lock file에 vue-router 누락

- 구분: Team B 수정 필요
- 발생 조건: 새 환경에서 `npm run build`
- 재현 절차: 기존 `package-lock.json` 기준 install/build
- 기대 결과: Router 의존성 설치 및 build 성공
- 실제 결과: `vue-router` import resolve 실패
- 영향 범위: Frontend 실행 및 발표 시연 전체
- 관련 API 또는 파일: `frontend/package.json`, `frontend/package-lock.json`
- 임시 우회 가능 여부: `npm install`로 가능
- 수정 또는 확인 요청 사항: lock file 동기화 완료, build 재검증 완료

## [P1][해결됨] JSP 로그인 Redirect에 Session ID 노출

- 구분: Team B 수정 필요
- 발생 조건: 신규 Session으로 JSP 로그인
- 재현 절차: `/login` POST 응답의 Location 확인
- 기대 결과: Cookie 기반 Session만 사용
- 실제 결과: `/members/me;jsessionid=...`로 redirect
- 영향 범위: Session ID URL/로그/화면 노출 가능성
- 관련 API 또는 파일: `application.yml`
- 임시 우회 가능 여부: 없음
- 수정 또는 확인 요청 사항: session tracking mode를 cookie로 제한, 실제 HTTP 재검증 완료

## [P1][해결됨] Vue returnTo가 JSP 로그인과 프로필 저장에서 무시됨

- 구분: Team B 수정 필요
- 발생 조건: 인증 필요 Vue 화면에서 JSP 로그인/프로필 화면 이동
- 재현 절차: `returnTo=http://localhost:5173/...`를 전달하고 저장
- 기대 결과: 원래 Vue 내부 화면으로 복귀
- 실제 결과: 마이페이지 또는 JSP 프로필 화면에 머묾
- 영향 범위: 발표 시연 흐름과 사용자 작업 연속성
- 관련 API 또는 파일: `MemberViewController`, `RunningProfileViewController`, JSP form
- 임시 우회 가능 여부: 사용자가 Vue 주소를 다시 입력
- 수정 또는 확인 요청 사항: localhost:5173 allowlist 적용, 외부 redirect 차단 테스트 완료

## [P1][해결됨] Spotify OAuth callback이 JSON에서 종료됨

- 구분: Team B 수정 필요
- 발생 조건: Spotify 승인 후 Backend callback
- 재현 절차: `/api/spotify/oauth/callback`
- 기대 결과: 민감 query 제거 후 Vue 완료 화면 복귀
- 실제 결과: Connection JSON이 브라우저에 직접 표시
- 영향 범위: OAuth 발표 시연 흐름 단절
- 관련 API 또는 파일: `SpotifyApiController`, `SpotifyOAuthCompleteView.vue`
- 임시 우회 가능 여부: 사용자가 Vue 주소로 수동 이동
- 수정 또는 확인 요청 사항: 성공/실패 모두 고정 Vue URL로 302, 공개 ErrorCode만 전달

## [P1][해결됨] 기본 오류 응답에 stack trace 노출

- 구분: Team B 수정 필요
- 발생 조건: 미지원 HTTP method 또는 처리되지 않은 예외
- 재현 절차: 존재하는 API에 잘못된 method 호출
- 기대 결과: 계약된 최소 JSON 오류
- 실제 결과: Spring 기본 오류 JSON에 trace 포함
- 영향 범위: 내부 구현 정보 노출 및 Vue 오류 계약 불일치
- 관련 API 또는 파일: `GlobalExceptionHandler`, `application.yml`
- 임시 우회 가능 여부: 없음
- 수정 또는 확인 요청 사항: trace 비활성화, 404/405/500 공통 JSON handler와 테스트 추가

## [P1][환경 의존] 실 Spotify 검색/OAuth/Export 미검증

- 구분: 환경 이슈
- 발생 조건: Spotify Client ID/Secret/Redirect URI 미설정
- 재현 절차: 검색, authorize, export 호출
- 기대 결과: 실제 Spotify API 성공 및 URL 표시
- 실제 결과: 검색 502, authorize 500, 미연결 export 401
- 영향 범위: Spotify 외부 성공 시연
- 관련 API 또는 파일: `.env.example`, Spotify Controller/Service/Vue
- 임시 우회 가능 여부: 오류 분기와 내부 DB 저장 구조 시연만 가능
- 수정 또는 확인 요청 사항: 실제 테스트 계정과 Dashboard Redirect URI 준비 후 수동 검증

## [P2][환경 의존] 실제 브라우저 클릭 기반 검증 미완료

- 구분: 환경 이슈
- 발생 조건: Codex 인앱 브라우저 초기화
- 재현 절차: 브라우저 테스트 환경 연결
- 기대 결과: JSP/Vue 화면 클릭과 시각 상태 확인
- 실제 결과: 브라우저 런타임 초기화 실패
- 영향 범위: 시각 UI와 DevTools Cookie 확인
- 관련 API 또는 파일: 없음
- 임시 우회 가능 여부: HTTP, Cookie jar, Controller 테스트, production build로 보조 검증
- 수정 또는 확인 요청 사항: 발표 전 사람 기준 브라우저 체크리스트 실행

## Team A 확인 요청

1. Spotify 검색 결과 BPM/장르 보강 여부
2. Export 부분 실패의 곡별 성공/실패 수를 MVP에서 제외할지 여부
3. Spotify ID/URL이 남은 실패 Export의 수동 복구 정책
4. 본인 소유 비공개 플레이리스트 참조 저장 허용 정책

현재 실제 내부 MVP 흐름에서 Team A Service/Mapper/SQL의 신규 P0/P1 결함은 발견되지 않았다.
