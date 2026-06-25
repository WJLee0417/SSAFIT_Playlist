# 와이어프레임 전환용 화면 레이아웃 명세

## 1. 공통 기준

- Vue 화면은 `AppLayout.vue`를 공통 레이아웃으로 사용한다.
- JSP 화면은 `nav.jspf`와 `member.css`를 공유한다.
- 실제 CSS와 화면 코드는 데스크톱 기반 레이아웃에 반응형 보정이 들어간 형태다. 모바일 전용 우선 구조로 단정할 근거는 없다.
- 아래 명세는 Figma 또는 draw.io에서 저해상도 와이어프레임으로 옮기기 위한 블록 단위 설명이다.

| 화면 ID | Header | Navigation | Main Content | Form | Action Button | Error Area | 레이아웃 근거 |
|---|---|---|---|---|---|---|---|
| SCR-001 | 없음 | 라우터 가드 | 인증 성공 시 `/music`, 실패 시 JSP 로그인 | 없음 | 없음 | `/access-check-failed` 분기 | Vue router 기반, 화면 없음 |
| SCR-002 | JSP site-header | `nav.jspf` | 소개 copy + 회원가입 카드 | ID/password/email/nickname | 계정 만들기, Spotify 로그인 | `form:errors`, field-error | 데스크톱 2단 auth layout |
| SCR-003 | JSP site-header | `nav.jspf` | 소개 copy + 로그인 카드 | loginId/password/returnTo | 로그인, Spotify 로그인 | `form:errors`, success/error alert | 데스크톱 2단 auth layout |
| SCR-004 | JSP site-header | `nav.jspf` | 전용 본문 없음 | POST form hidden only | 로그아웃 | 없음 | nav 내 처리 버튼 |
| SCR-005 | JSP site-header | `nav.jspf` | 히어로, 프로필 타일, 기록/세션 타일, 차트 | 온보딩 identity form | 프로필 등록, 플레이리스트, 차트 토글 | 온보딩 form errors, chart state | 데스크톱 대시보드 + 반응형 canvas |
| SCR-006 | JSP site-header | `nav.jspf` | Fitness Score, 자동 목표 요약 | gender/age/preferredGenre | 프로필 저장, 마이페이지로 | `form:errors`, success alert | 데스크톱 카드 form |
| SCR-007 | Vue AppLayout | AppLayout nav | 로그인 필요 안내 | 없음 | 로그인 이동 | 안내 메시지 | Vue 상태 화면 |
| SCR-008 | Vue AppLayout | AppLayout nav | 접근 확인 실패 안내 | 없음 | 복귀/로그인 | status/errorCode 표시 | Vue 상태 화면 |
| SCR-009 | Vue AppLayout | AppLayout nav | Spotify 검색, 필터, 음악 카드 grid, pagination | Spotify query, 필터 form | 검색, 저장, 필터 적용, 이전/다음 | PageStatePanel, SaveStatus | 데스크톱 grid + responsive cards |
| SCR-010 | Vue AppLayout | AppLayout nav | 음악 상세, 리뷰 목록 | 리뷰 form | 리뷰 저장/수정/삭제, 좋아요 | PageStatePanel, review errors | 상세 카드 layout |
| SCR-011 | 상위 화면 | 상위 화면 | 리뷰 편집 블록 | score/liked/runningFit/comment | 저장, 취소, 삭제 | field-error/save status | 내장 component |
| SCR-012 | Vue AppLayout | AppLayout nav | 음악 생성/수정 폼 | title/artist/album/genre/bpm 등 | 저장, 취소 | SaveStatus/PageStatePanel | editor form |
| SCR-013 | Vue AppLayout | AppLayout nav | 플레이리스트 그룹, Spotify 검색, 카드 목록 | 검색/필터, Spotify query | 생성, 상세, Spotify 추가 | PageStatePanel, SaveStatus | 카드/목록 혼합 |
| SCR-014 | Vue AppLayout | AppLayout nav | 저장한 플레이리스트 카드 목록 | 없음 | 상세 이동 | PageStatePanel | 카드 grid |
| SCR-015 | Vue AppLayout | AppLayout nav | 상세 header, 설명, 저장 panel, Spotify 검색, 수록곡, export | Spotify query | 수정, 삭제, 저장, 추가, export | PageStatePanel, SaveStatus | 상세 + 내장 도구 |
| SCR-016 | Vue AppLayout | AppLayout nav | 플레이리스트 form | title/description/visibility | 저장, 취소 | SaveStatus/PageStatePanel | editor form |
| SCR-017 | 상위 화면 | 상위 화면 | 수록곡 리스트, drag 영역, 리뷰 영역 | 순서, 리뷰 입력 | 삭제, 리뷰 저장 | 상위 SaveStatus | 내장 list component |
| SCR-018 | Vue AppLayout | AppLayout nav | 기록 summary, scope toggle, 기록 cards, AI card | 연도/월 선택 | 새 기록, 첫 기록 등록, AI 생성 | PageStatePanel, AI error | 대시보드 + 카드 grid |
| SCR-019 | Vue AppLayout | AppLayout nav | 기록 상세 지표, 메모 | 없음 | 수정, 삭제, 목록 | PageStatePanel, notice | 상세 카드 |
| SCR-020 | Vue AppLayout | AppLayout nav | 러닝 기록 입력 form, pace preview | date/distance/duration/cadence/heartRate/memo | 저장, 취소 | field-error, SaveStatus | editor form |
| SCR-021 | Vue AppLayout | AppLayout nav | 세션 filter, row list | 필터 선택 | 세션 생성, 상세 이동 | PageStatePanel | 리스트 row layout |
| SCR-022 | Vue AppLayout | AppLayout nav | 세션 상세, 참여자 목록, 상태 | 없음 | 참여/취소, 상태변경, 수정 | PageStatePanel, action message | 상세 + action bar |
| SCR-023 | Vue AppLayout | AppLayout nav | 세션 form | title/capacity/place/datetime/distance/pace/description | 저장, 취소 | field-error, SaveStatus | editor form |
| SCR-024 | Vue AppLayout | AppLayout nav | 추천 조건 form, 프로필 안내, 히스토리 | distance/intensity/genre | 운동 계획 생성, 로그 새로고침, 결과 보기/삭제 | PageStatePanel, field-error | form + history list |
| SCR-025 | 상위 화면 | 상위 화면 | 운동 계획, 추천 음악 목록, 저장 영역 | 저장 payload | 저장, 닫기 | save message | 내장 result component |
| SCR-026 | 상위 화면 | 상위 화면 | 최신 AI 리포트 카드, 누락 데이터 | force refresh | 조회/생성/새로고침 | provider/data error | 내장 card component |
| SCR-027 | 상위 화면 | 상위 화면 | Spotify 검색 dropdown, album thumbnail | query | 검색, 저장/추가 | spotifyErrorMessage, field-error | 내장 search panel |
| SCR-028 | 없음 | 없음 | redirect 처리 | returnTo | Spotify 로그인/연결 버튼은 상위 화면에 있음 | 상위 오류 영역 | redirect flow |
| SCR-029 | Vue AppLayout | AppLayout nav | OAuth 완료/연결 확인 메시지 | 없음 | 원래 화면 이동, 재시도 | 연결 실패 메시지 | 상태 화면 |
| SCR-030 | 상위 화면 | 상위 화면 | Spotify 연결 상태, export hint/url | 없음 | 연결, 내보내기, Spotify에서 열기 | export error/partial message | 내장 export panel |
| SCR-031 | Vue AppLayout | AppLayout nav | Not Found 안내 | 없음 | 홈/목록 이동 | Not Found 문구 | 상태 화면 |
| SCR-032 | 없음 | 없음 | 화면 미구현 | 미정 | 미정 | 미정 | API only |

## 2. 발표용 핵심 화면 묶음

| 묶음 | 화면 | 와이어프레임 포인트 |
|---|---|---|
| 인증 시작 | SCR-002, SCR-003, SCR-005 | JSP와 Vue 진입이 분리되어 있음을 표시 |
| 음악 탐색 | SCR-009, SCR-010, SCR-011, SCR-027 | 검색/상세/리뷰/Spotify 저장을 하나의 흐름으로 표현 |
| 플레이리스트 | SCR-013, SCR-015, SCR-016, SCR-017, SCR-030 | 목록 → 상세 → 수록곡 관리 → export |
| 러닝 데이터 | SCR-018, SCR-019, SCR-020, SCR-021, SCR-022, SCR-023 | 기록과 세션은 Vue route로 완성된 흐름 |
| 추천 | SCR-024, SCR-025, SCR-026 | 조건 입력 → 추천 결과 → 저장, AI 리포트는 별도 카드 |
| Spotify | SCR-027, SCR-028, SCR-029, SCR-030, SCR-032 | 검색은 구현, OAuth/export는 부분 검증, import 화면은 없음 |

