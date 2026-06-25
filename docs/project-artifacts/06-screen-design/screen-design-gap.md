# 화면 설계 Gap 분석

## 1. 요구사항은 있으나 화면이 없는 기능

| 요구사항 | 항목 | 현재 근거 | Gap | 권장 조치 |
|---|---|---|---|---|
| FR-005 | Vue 전용 러닝 프로필 화면 | `runningProfileApi.js`, `RunningProfileApiController`, JSP `running-profile.jsp` | JSP 화면은 실제 구현되어 있으나 Vue Router 화면은 없다. | 발표 기준 화면은 JSP로 확정한다. Vue 전용 화면은 발표 전 신규 구현 대상이 아니라 후속 SPA 통합 과제로 분리한다. |
| FR-020 | Spotify 플레이리스트 가져오기 | `spotifyApi.getPlaylists`, `spotifyApi.importPlaylist`, `SpotifyApiController` | 가져오기 API는 있으나 Vue route/page/component가 확인되지 않는다. | 별도 화면을 만들기 전까지 “API 구현, 화면 미연결”로 표기한다. |

## 2. API는 있으나 화면 연결이 없는 기능

| API | 코드 근거 | 화면 연결 상태 | 위험 |
|---|---|---|---|
| `GET/PUT /api/members/me/running-profile` | `RunningProfileApiController`, `runningProfileApi.js` | JSP 폼은 `POST /members/me/running-profile`을 사용하고 Vue API client 사용처는 확인되지 않음 | Vue SPA 내 프로필 편집 흐름을 기대하면 누락으로 보일 수 있다. |
| `GET /api/spotify/me/playlists`, `POST /api/spotify/playlists/import` | `SpotifyApiController`, `spotifyApi.js` | 전용 화면 미확인 | Spotify 가져오기 요구사항을 시연하기 어렵다. |
| `POST /api/auth/login`, `POST /api/members` | `AuthApiController`, `MemberApiController`, `authApi.js`, `memberApi.js` | 실제 회원가입/로그인은 JSP POST 화면 사용 | Vue 전용 인증 화면으로 오해하지 않도록 문서에서 JSP 기준으로 분리해야 한다. |

## 3. 화면은 있으나 서버 기능 또는 외부 검증이 불완전한 기능

| 화면 | 현재 상태 | 불완전 사유 | 발표 주의 |
|---|---|---|---|
| SCR-012 내부 음악 등록/수정 | UNVERIFIED | 관리자/권한 모델이 문서와 코드상 확정되지 않음 | 일반 회원 기능처럼 설명하지 않는다. |
| SCR-026 AI 러닝 리포트 | UNVERIFIED | OpenAI 호환 Provider 실호출은 환경 변수와 외부 API에 의존 | 실패 시 “최신 리포트 없음/생성 실패” 대체 설명 필요 |
| SCR-028~SCR-030 Spotify OAuth/export | PARTIAL | Spotify Dashboard redirect URI, scope, 사용자 계정 권한, 외부 API 성공 여부가 미확인 | 발표 전 실제 계정으로 재검증하거나 녹화/대체 시나리오 준비 |

## 4. JSP/Vue 전환 중 경로 오류 가능성

| 흐름 | 근거 | 위험 | 확인 포인트 |
|---|---|---|---|
| Vue 보호 라우트 → `/login-required` → JSP `/login` | `router/index.js`, `LoginRequiredView.vue`, `buildJspLoginUrl` | `returnTo`가 잘못 유지되면 로그인 후 원래 Vue 화면 복귀 실패 | `/running-records`, `/recommendations`에서 로그인 유도 후 복귀 확인 |
| JSP nav → Vue 화면 | `nav.jspf`, `${frontendOrigin}` | Docker/개발 환경에서 `frontendOrigin` 설정이 다르면 다른 host/port로 이동 | `.env`, application 설정, Docker 환경 변수 확인 |
| Spotify callback → Vue 완료 화면 | `SpotifyApiController`, `SpotifyOAuthCompleteView.vue` | redirect URI가 실제 배포 URL과 불일치하면 완료 화면까지 오지 못함 | Spotify Dashboard callback URL과 `returnTo` 확인 |

## 5. 로그인·로그아웃·회원가입 이후 redirect 문제 가능성

| 화면 | 현재 흐름 | 위험 | 권장 검증 |
|---|---|---|---|
| 회원가입 | 성공 후 로그인 화면으로 이동하는 JSP 흐름 | 성공 메시지/중복 ID 오류 표시가 브라우저에서 미확인 | 새 계정 생성 후 `/login` 메시지 확인 |
| 로그인 | `returnTo` hidden input 기반 복귀 | 외부 origin 또는 잘못된 returnTo 방어 정책 확인 필요 | 보호 Vue route에서 로그인 후 복귀 확인 |
| 로그아웃 | `nav.jspf` POST `/logout` | SPA 세션 캐시와 JSP 세션 만료 시점 차이 가능 | 로그아웃 후 `/recommendations` 접근 시 로그인 필요 화면 확인 |

## 6. 화면 설계서 반영 원칙

- 실제 화면: Vue route 또는 JSP 파일이 확인된 화면만 `IMPLEMENTED`로 표기한다.
- 부분 구현: 화면은 있으나 외부 API 또는 권한 검증이 필요한 기능은 `PARTIAL` 또는 `UNVERIFIED`로 둔다.
- 화면 미구현: API와 요구사항은 있으나 route/component가 없으면 `NOT_IMPLEMENTED_SCREEN`으로 별도 분리한다.
