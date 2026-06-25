# SSAFIT PlayList 최종 문서 정합성 검토 보고서

## 1. 검토 범위

다음 최종 제출 문서 5개와 관련 코드, 설정, DB 스키마를 대조했다.

- `docs/final/00-project-audit.md`
- `docs/final/01-feature-evidence-matrix.md`
- `docs/final/02-final-project-report.md`
- `docs/final/03-external-api-utilization.md`
- `docs/final/04-generative-ai-utilization.md`

코드 대조 범위는 다음과 같다.

- Spotify: `SpotifyApiController`, `SpotifyRestApiClient`, `SpotifyService`, `SpotifyOAuthService`, `PlaylistExportService`, `spotifyApi.js`, `SpotifyExportPanel.vue`
- 생성형 AI: `AiRunningReportApiController`, `AiRunningReportGenerationService`, `AiRunningReportAggregationService`, `OpenAiRunningReportProvider`, `AiRunningReportApiDtos`, `AiRunningReportContentDtos`, `AiRunningReportCard.vue`
- 추천: `RecommendationApiController`, `RecommendationService`, `recommendationApi.js`, `RecommendationView.vue`
- DB: `schema.sql`, `MusicMapper.xml`, `PlaylistMapper.xml`, `SpotifyConnectionMapper.xml`, `AiRunningReportMapper.xml`
- 화면/라우팅: `frontend/src/router/index.js`, `RunningRecordListView.vue`, `PlaylistDetailView.vue`, `MusicListView.vue`
- 환경 변수: `.env.example`, `compose.yaml`, `application.yml`

## 2. 문서별 핵심 주장 요약

| 문서 | 핵심 주장 | 검토 결과 |
| --- | --- | --- |
| `00-project-audit.md` | 프로젝트 전체 구현 상태, 위험 요소, 테스트/환경 현황 정리 | 대체로 정합. Spotify/AI 한계와 테스트 범위가 비교적 안전하게 표현됨 |
| `01-feature-evidence-matrix.md` | 기능별 사용자 흐름, API, Service, Mapper, 테스트/캡처 지점 정리 | 대부분 정합. AI 리포트 진입 화면과 추천 로그 관련 표현 일부 보정 필요 |
| `02-final-project-report.md` | 최종 보고서 본문, 아키텍처, 구현/검증/한계 정리 | 제출 문서로 사용 가능하나 AI 리포트가 추천 로그를 요약한다는 표현은 코드와 불일치 |
| `03-external-api-utilization.md` | Spotify API 활용 범위, OAuth, export, 정책 제한, 대응 전략 정리 | 정합. 실제 API와 Mock/수동 검증 범위를 구분하고 있음 |
| `04-generative-ai-utilization.md` | 생성형 AI 기능, 실제 호출 조건, 프롬프트, fallback, 테스트 범위 정리 | 정합. AI 활용 제출 문서로 가장 정확한 기준 문서 |

## 3. 사실 관계 불일치 점검

| 항목 | 현재 문서 표현 | 실제 코드 기준 | 판정 |
| --- | --- | --- | --- |
| AI 러닝 리포트 입력 | 일부 문서에서 "러닝 기록과 추천 로그" 기반으로 표현 | `AiRunningReportAggregationService.aggregate`는 최근 `running_records`와 `running_profiles` 존재 여부만 사용. 추천 로그/플레이리스트 저장 이력은 Aggregation에 포함되지 않음 | 수정 필요 |
| AI 리포트 화면 경로 | `01` 일부에서 `/recommendations` 또는 리포트 카드 영역으로 표현 | `AiRunningReportCard`는 `RunningRecordListView.vue`에 포함되어 `/running-records`에서 노출 | 수정 필요 |
| 음악 추천 결과와 AI 리포트 연결 | 일부 성과 문장에서 추천 로그와 AI 리포트를 함께 묶어 표현 | 목표 거리 기반 운동 계획은 `RecommendationService`에 있으나, AI 러닝 리포트 자체는 추천 로그를 입력으로 쓰지 않음 | 수정 필요 |
| Spotify Search와 BPM 검색 | Spotify가 BPM/장르/오디오 특성 기반 정밀 검색을 제공하는 것처럼 보일 위험 | 문서 대부분은 Search API는 keyword 기반이고 tempo/genre는 보강 시도 후 실패 시 null이라고 설명 | 정합 |
| 실제 LLM API 호출 | OpenAI 호환 API 호출 코드가 구현되었다고 표현 | `OpenAiRunningReportProvider`에 실제 `/responses` 호출 구현. 단 기본값은 disabled/provider none | 정합 |
| 테스트 완료 범위 | Spotify/AI 실제 외부 API 호출까지 자동 검증한 것처럼 표현할 위험 | 문서 대부분이 mock/fake provider 또는 수동 검증 필요를 명시 | 대체로 정합 |
| DB 컬럼명 | `spotify_track_id`, `spotify_uri`, `spotify_playlist_id`, `spotify_playlist_url`, `generation_source` 등 | `schema.sql` 및 Mapper에 실제 존재 | 정합 |
| API URL | `/api/spotify/tracks/search`, `/api/spotify/oauth/authorize`, `/api/ai/running-reports` 등 | Controller mapping과 일치 | 정합 |

## 4. 구현 상태 표현 점검

| 기능 | 문서상 상태 | 코드상 상태 | 검토 의견 |
| --- | --- | --- | --- |
| 회원가입/로그인/로그아웃 | 구현 완료 | JSP/REST Controller, Service, Session 기반 구현 존재 | 정합 |
| 음악 목록/상세/검색/정렬 | 구현 완료 | `MusicApiController`, `MusicService`, `MusicMapper.xml` 존재 | 정합 |
| 음악 등록/수정 | 구현 완료 | `/api/music/internal`, `/api/music/internal/{musicId}` 존재 | 정합 |
| 음악 삭제 | 미구현 또는 범위 밖 | `MusicApiController`에 `DeleteMapping` 없음 | 정합 |
| 리뷰 CRUD | 구현 완료 | `MusicReviewApiController` 및 Mapper 존재 | 정합 |
| 플레이리스트 CRUD | 구현 완료 | `PlaylistApiController`에 CRUD mapping 존재 | 정합 |
| 수록곡 추가/삭제/순서 변경 | 구현 완료 | `PlaylistItemApiController` 존재 | 정합 |
| 러닝 조건 기반 추천 | 구현 완료 | `RecommendationService.recommend`, `RecommendationLogMapper` 존재 | 정합 |
| 목표 거리 기반 AI 운동 계획 | 부분 구현 | Provider 있으면 LLM, 없으면 `fallbackWorkoutPlan` | 정합 |
| AI 러닝 리포트 | 구현 완료 | API, Service, Provider, DB, Vue card 존재 | 정합 |
| AI 러닝 리포트의 추천 로그 분석 | 일부 문서에서 구현처럼 읽힘 | 현재 미구현 | 수정 필요 |
| Spotify 검색/OAuth/export | 구현 완료 또는 부분 구현 | 실제 호출 코드 존재. 단 정책/계정/URI 제약 존재 | 정합 |
| 실제 외부 API 자동 테스트 | 테스트 필요/수동 검증 | mock/fake 중심 자동 테스트 | 정합 |

## 5. 외부 API 활용 표현 점검

Spotify 관련 문서는 전반적으로 안전하다. 특히 `03-external-api-utilization.md`는 다음 사실을 잘 구분한다.

- Vue는 Spotify API를 직접 호출하지 않고 Spring Boot 내부 API를 호출한다.
- Client Secret과 사용자 토큰은 프론트엔드 코드에 노출되지 않는다.
- Spotify Search API는 BPM/오디오 특성 기반 정밀 검색 API가 아니다.
- tempo 조회는 `/audio-features/{trackId}` 호출 코드가 있으나 실패 시 null 처리된다.
- 장르는 track 고유 장르가 아니라 artist genres의 첫 번째 값을 선택적으로 사용한다.
- Rate Limit은 오류 코드 분류는 있으나 `Retry-After` 기반 자동 backoff는 구현되어 있지 않다.
- OAuth/export는 실제 계정, redirect URI, Spotify Developer Dashboard User Management 설정에 좌우된다.

확인된 실제 코드 근거:

- `SpotifyApiController`: `/api/spotify/tracks/search`, `/api/spotify/oauth/authorize`, `/api/spotify/oauth/callback`, `/api/spotify/me/connection`, `/api/spotify/me/playlists`, `/api/spotify/playlists/import`
- `PlaylistExportApiController`: `/api/playlists/{playlistId}/export/spotify`
- `SpotifyRestApiClient`: token, search, audio-features, artists, current user, playlists, create playlist, add tracks 호출
- `PlaylistExportService`: Spotify URI 없는 곡 skip 및 `SUCCESS`, `PARTIAL`, `FAILED` 상태 처리
- `schema.sql`: `music.spotify_track_id`, `music.spotify_uri`, `playlists.spotify_playlist_id`, `spotify_playlist_url`, `spotify_export_status`, `spotify_connections.access_token`, `refresh_token`

수정 권고는 크지 않다. 다만 발표에서는 "Spotify BPM 검색"이라는 말 대신 "Spotify 검색 결과를 가져온 뒤 가능한 경우 tempo/genre를 보강하고, 추천 점수에는 내부 DB 값을 함께 사용한다"라고 표현해야 한다.

## 6. 생성형 AI 활용 표현 점검

생성형 AI 관련 정합성 기준은 `04-generative-ai-utilization.md`가 가장 정확하다. 이 문서를 기준으로 다른 문서의 표현을 맞추는 것이 좋다.

확인된 실제 구현은 다음과 같다.

- `OpenAiRunningReportProvider`는 OpenAI 호환 Responses API `/responses` 호출 코드를 가진다.
- Provider Bean은 `ai.running-report.enabled=true`이고 `provider=openai`일 때만 활성화된다.
- 기본 환경값은 `AI_RUNNING_REPORT_ENABLED=false`, `AI_RUNNING_REPORT_PROVIDER=none`이다.
- `AiRunningReportGenerationService`는 LLM 호출 실패, Provider 없음, 데이터 부족, JSON 파싱 실패 시 `FALLBACK` 리포트를 저장한다.
- `AiRunningReportAggregationService`는 최근 러닝 기록, 케이던스, 심박, 페이스, 추세, 데이터 품질을 집계한다.
- `AiRunningReportAggregationDtos`에는 `RecommendationSummary`, `SavedPlaylistSummary` 타입이 있지만 현재 `Aggregation` record에 포함되지 않는다.
- `ReportContent` 필드는 `runningPatternSummary`, `paceAndIntensityInsight`, `cadenceAndHeartRateInsight`, `nextRunningGoal`, `dataLimitations` 5개다.
- 테스트는 Fake Provider와 fallback 중심이며, 실제 API Key를 사용하는 LLM 호출 자동 테스트는 기본 범위가 아니다.

문서 간 불일치:

- `04`는 "추천 로그/플레이리스트 이력은 현재 AI 리포트 입력에 포함되지 않는다"고 정확히 명시한다.
- `02`와 `01` 일부 문장은 "러닝 기록과 추천 로그 기반 AI 리포트"로 읽혀 실제 코드와 맞지 않는다.

권장 통일 표현:

> AI 러닝 리포트는 최근 러닝 기록과 러닝 프로필 존재 여부를 서버에서 집계한 스냅샷을 기반으로 자연어 요약을 생성한다. 추천 로그와 저장 플레이리스트 이력은 현재 AI 리포트 입력에는 포함되지 않으며, 향후 개선 항목이다.

## 7. 민감 정보 노출 점검

검토 결과 실제 API Key, Client Secret, Access Token, Refresh Token 값은 최종 문서에 노출되지 않았다.

| 항목 | 상태 | 의견 |
| --- | --- | --- |
| `SPOTIFY_CLIENT_SECRET` | 환경 변수명/placeholder만 노출 | 허용 가능 |
| `AI_RUNNING_REPORT_API_KEY` | 환경 변수명만 노출 | 허용 가능 |
| `access_token`, `refresh_token` | DB 컬럼명과 Spotify 응답 필드명만 노출 | 값이 없으므로 허용 가능 |
| `Bearer` | API 호출 방식 설명으로만 등장 | 허용 가능 |
| 실제 secret 패턴 | `sk-` 등 실제 키 패턴 노출 없음 | 문제 없음 |
| 캡처 가이드 | 토큰/secret 값을 가리라고 명시 | 적절함 |

주의 사항:

- 발표 캡처에서 DB의 `spotify_connections.access_token`, `refresh_token` 값은 절대 보여주면 안 된다.
- `OpenAiRunningReportProvider` 코드 캡처 시 Authorization 헤더 라인은 보여도 되지만 실제 환경 변수 값 또는 로그는 보여주면 안 된다.
- `.env` 파일이 있다면 제출/캡처 대상에서 제외해야 한다.

## 8. 수정 권고 사항

기존 문서에서 수정이 필요한 부분은 아래와 같다. 코드는 수정할 필요가 없고, 문장 보정만 권장한다.

### 권고 1. `docs/final/02-final-project-report.md` 1.3 프로젝트 목표

현재 문장:

> 생성형 AI를 활용해 러닝 기록과 추천 로그를 자연어 리포트로 요약한다.

권장 문장:

> 생성형 AI를 활용해 최근 러닝 기록 집계 데이터를 자연어 리포트로 요약한다. 추천 로그와 저장 플레이리스트 이력은 현재 AI 리포트 입력에는 포함되지 않으며 향후 확장 항목이다.

### 권고 2. `docs/final/02-final-project-report.md` 2.2 핵심 사용자 시나리오

현재 문장:

> 러닝 기록과 추천 로그를 기반으로 AI 러닝 리포트를 생성한다.

권장 문장:

> 최근 러닝 기록 집계 데이터를 기반으로 AI 러닝 리포트를 생성한다. 추천 로그는 추천 기능의 저장/조회 근거이며, 현재 AI 리포트 입력에는 포함되지 않는다.

### 권고 3. `docs/final/02-final-project-report.md` 4.7 생성형 AI 기반 기능

현재 문장:

> 러닝 기록과 추천 로그를 집계한다.

권장 문장:

> 최근 러닝 기록, 케이던스, 심박, 페이스, 추세, 데이터 품질을 집계한다. 추천 로그와 플레이리스트 저장 이력은 현재 Aggregation 결과에 포함되지 않는다.

### 권고 4. `docs/final/02-final-project-report.md` 발표용 핵심 요약

현재 문장:

> 생성형 AI 기능은 러닝 기록과 추천 로그를 요약하는 AI 러닝 리포트로 구현했습니다.

권장 문장:

> 생성형 AI 기능은 최근 러닝 기록 집계 데이터를 요약하는 AI 러닝 리포트로 구현했습니다. 실제 OpenAI-compatible API 호출 코드는 있으며, 기본 환경이나 실패 상황에서는 fallback 리포트를 생성합니다.

### 권고 5. `docs/final/01-feature-evidence-matrix.md` 기능별 증빙 매트릭스의 생성형 AI 러닝 리포트 행

현재 표현:

> `/recommendations` 또는 리포트 카드 영역

권장 표현:

> `/running-records`의 `AiRunningReportCard`

사유:

`RunningRecordListView.vue`가 `AiRunningReportCard`를 import하고 렌더링한다. `/recommendations`는 목표 거리 기반 운동 계획 및 추천 기능 화면이다.

### 권고 6. `docs/final/01-feature-evidence-matrix.md` AI 데모 시나리오

현재 문장:

> 러닝 기록과 추천 로그가 있는 계정으로 로그인

권장 문장:

> 러닝 기록이 있는 계정으로 로그인한다. 추천 로그는 현재 AI 러닝 리포트 입력에는 필요하지 않으며, 추천 기능 시연 근거로 별도 활용한다.

### 권고 7. `docs/final/01-feature-evidence-matrix.md` 프로젝트 핵심 성과 5개

현재 문장:

> 러닝 데이터와 추천 로그를 바탕으로 AI 러닝 리포트와 목표 거리 기반 운동 계획을 생성하는 구조를 만들고...

권장 문장:

> 러닝 기록 집계 데이터를 바탕으로 AI 러닝 리포트를 생성하고, 목표 거리 입력 시 운동 계획을 생성하는 구조를 만들고...

### 권고 8. `docs/final/00-project-audit.md` 최종 성과 또는 AI 설명 문장

현재 문서 대부분은 정합하지만, AI 리포트와 추천 로그를 함께 묶는 표현이 있다면 다음 문장으로 통일하는 것을 권장한다.

> AI 러닝 리포트는 최근 러닝 기록 집계 데이터를 기반으로 하며, 추천 로그/플레이리스트 저장 이력 분석은 향후 개선 항목이다.

### 권고 9. `docs/final/03-external-api-utilization.md`

현재 문서는 제출 가능하다. 수정이 필요하다면 표현을 더 안전하게 하기 위해 다음 문장을 유지하거나 강조한다.

> Spotify Search API는 BPM/오디오 특성 기반 정밀 검색 API가 아니며, tempo/genre 보강은 실패 가능성이 있어 null 처리된다.

### 권고 10. `docs/final/04-generative-ai-utilization.md`

현재 문서는 제출 가능하다. 이 문서를 생성형 AI 관련 최종 기준 문서로 삼고, `01`, `02`의 AI 관련 표현을 `04` 기준으로 맞추는 것이 좋다.

## 9. 제출 가능 판정

판정: 조건부 제출 가능

판정 근거:

- 핵심 기능, API URL, DB 컬럼, Controller/Service/Mapper 경로는 전반적으로 실제 코드와 일치한다.
- Spotify 정책 제한, Mock/fake 테스트 범위, 실제 API 수동 검증 필요성, 토큰 보안 한계는 문서에 대체로 정확히 반영되어 있다.
- 실제 민감 정보 값은 문서에 노출되어 있지 않다.
- Maven 및 환경 테스트는 사용자 확인에 따라 통과한 것으로 반영되어 있으며, 문서도 현재 blocker처럼 표현하지 않는다.
- 단, `01`, `02` 일부 문장에서 AI 러닝 리포트가 추천 로그까지 입력으로 사용하는 것처럼 읽히는 표현이 있어 제출 전 문장 보정이 필요하다.
- `01` 일부 AI 리포트 화면 경로가 실제 렌더링 위치인 `/running-records`와 다르게 `/recommendations`로 읽힐 수 있어 보정이 필요하다.

따라서 코드 수정 없이 문서 문장만 보정하면 제출 가능 수준으로 판단한다.

## 10. 최종 발표 체크리스트

| 체크 항목 | 발표 전 확인 내용 | 상태 |
| --- | --- | --- |
| 프로젝트 소개 슬라이드 | 프로젝트명, 기간 `2026.6.22~6.26`, 러닝 음악 큐레이션 목표 | 준비 필요 |
| 문제 정의 슬라이드 | 러닝 기록과 음악 선택 경험의 분리, Spotify 정책 제약, AI 해석 필요성 | 준비 필요 |
| 시스템 아키텍처 슬라이드 | Vue/JSP/Spring Boot/MyBatis/MySQL/Docker/Spotify/LLM 구조 | 준비 필요 |
| 핵심 기능 시연 | 회원, 음악, 리뷰, 플레이리스트, 러닝 기록, 추천, Spotify export, AI 리포트 | 준비 필요 |
| Spotify 외부 API 연동 설명 | Search, OAuth, playlist import/export, create playlist, add tracks | 준비 필요 |
| Spotify 정책 제한 및 대응 전략 | BPM/Audio Features/genre 제약, null 처리, 내부 DB 기반 추천 점수화 | 필수 |
| 생성형 AI 활용 설명 | 실제 Provider 호출 조건, 기본 fallback, 러닝 기록 집계 기반 리포트 | 필수 |
| 주요 프롬프트 캡처 | `SYSTEM_PROMPT`, `WORKOUT_PLAN_SYSTEM_PROMPT`, 권장 프롬프트와 실제 프롬프트 분리 | 필수 |
| LLM 활용 전후 비교 | 숫자 기록 조회에서 자연어 요약/다음 행동 제안으로 바뀐 가치 | 필수 |
| AI 기능 구현 코드 캡처 | `AiRunningReportGenerationService`, `OpenAiRunningReportProvider`, `AiRunningReportCard` | 필수 |
| 테스트 및 검증 결과 | STS Maven 통과, Mapper 통합 테스트, Playwright E2E, AI fake/fallback 테스트 | 필수 |
| 한계 및 개선 방향 | 음악 삭제 미구현, 실제 API 수동 검증, token 암호화, AI 추천 로그 미포함 | 필수 |
| 팀 역할 및 협업 과정 | 역할 단위 설명. 개인별 상세 역할이 불명확하면 기능/문서/테스트 단위로 표현 | 준비 필요 |

발표에서 반드시 피해야 할 표현:

- "Spotify가 BPM 기반으로 곡을 정밀 검색했다."
- "AI 리포트가 추천 로그와 저장 플레이리스트까지 종합 분석했다."
- "자동 테스트가 실제 Spotify/LLM API 호출까지 모두 검증했다."
- "기본 실행 시 항상 OpenAI API를 호출한다."
- "심박 데이터를 기반으로 건강 상태를 진단한다."
- "토큰 보안이 운영 수준으로 완비되었다."

발표에서 권장하는 안전한 표현:

- "Spotify API는 검색, OAuth, 플레이리스트 생성/트랙 추가에 사용했고, BPM/장르/오디오 특성은 정책 및 응답 제약으로 가능한 범위에서만 보강했습니다."
- "AI 러닝 리포트는 서버가 계산한 러닝 기록 집계 스냅샷을 자연어로 설명하며, Provider 미설정이나 실패 시 fallback 리포트를 저장합니다."
- "테스트는 핵심 로직과 fallback 안정성을 자동화했고, 실제 외부 API 시연은 환경 변수와 계정 설정을 갖춘 상태에서 수동 검증 대상으로 분리했습니다."
