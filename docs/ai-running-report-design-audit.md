# AI Running Report Design Audit

## 1. 조사 범위와 현재 상태

- 기준 브랜치: `feature/ai-running-report`
- 작업 트리 상태: 조사 시작 시 `git status --short --branch` 결과 변경 파일 없음
- 이번 단계 산출물: 설계 감사 문서 초안만 작성
- 구현 제외: Java, XML Mapper, SQL, Vue 구현 파일은 수정하지 않음

## 2. 현재 도메인 구조 분석

### 2.1 인증 및 현재 로그인 사용자 식별

- 인증 방식은 Spring `HttpSession` 기반이다.
- 로그인 성공 시 `AuthService.login()`이 세션에 `SessionConst.LOGIN_MEMBER_ID` 값을 저장한다.
- API 계층에서는 `SessionMemberResolver.requireMemberId(HttpSession)` 또는 기존 서비스의 `AuthService.getLoginMember()`로 현재 로그인 회원을 확인한다.
- 신규 AI 리포트 API도 회원 ID를 요청 파라미터로 받지 말고 `HttpSession`에서 `LOGIN_MEMBER_ID`를 읽어야 한다.
- 인증 실패는 기존 `ErrorCode.AUTH_REQUIRED`를 사용하며 HTTP 401과 `ErrorResponse` 형식을 따른다.

관련 파일:

- `ssafit/src/main/java/com/ssafy/ssafit/common/auth/SessionConst.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/auth/SessionMemberResolver.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/service/AuthService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/controller/api/AuthApiController.java`

### 2.2 공통 API 응답 및 예외 처리

- 성공 응답은 `ApiResponse<T>` 구조를 사용한다.
- 실패 응답은 `GlobalExceptionHandler`가 `ErrorResponse(errorCode, message)`로 반환한다.
- 도메인 오류는 `CustomException(ErrorCode)` 패턴이다.
- 신규 오류 코드는 기존 `ErrorCode`에 추가하는 방식이 가장 자연스럽다.

권장 신규 오류 코드:

- `AI_RUNNING_REPORT_DATA_NOT_ENOUGH`: HTTP 422
- `AI_RUNNING_REPORT_NOT_FOUND`: HTTP 404
- `AI_RUNNING_REPORT_LLM_FAILED`: HTTP 502

### 2.3 러닝 기록 구조

실제 운동 지표는 `running_records` 테이블에 저장된다.

주요 컬럼:

- `member_id`: 기록 소유 회원
- `record_date`: 러닝 날짜
- `distance_km`: 거리, 필수
- `duration_seconds`: 시간, 필수
- `pace_seconds_per_km`: 페이스, 필수, 서비스에서 거리/시간 기반 계산
- `average_cadence`: 평균 케이던스, nullable
- `average_heart_rate_bpm`: 평균 심박, nullable
- `memo`: 메모, nullable
- `deleted_at`: soft delete

도메인/DTO/Mapper/Service/Controller:

- `RunningRecord`
- `RunningRecordApiDtos.CreateRequest`, `UpdateRequest`, `Response`
- `RunningRecordMapper`
- `RunningRecordService`
- `RunningRecordApiController`
- `RunningRecordMapper.xml`

현재 Mapper에는 전체 내 기록 조회와 상세 조회 외에도 최근 심박/케이던스 값 조회 쿼리가 있다.

- `selectListByMemberId(memberId)`
- `selectRecentAverageHeartRates(memberId, limit)`
- `selectRecentAverageCadences(memberId, limit)`

AI 리포트에는 최근 5~10개 기록을 `record_date DESC, id DESC` 기준으로 사용하면 기존 정렬 정책과 맞다.

### 2.4 러닝 세션 구조

`running_sessions`와 `running_session_participants`는 러닝 모임 기능이다.

저장 데이터:

- 세션 제목, 설명, 장소, 예정 시각, 정원, 상태
- 참여 회원 목록

없는 데이터:

- 실제 러닝 거리
- 실제 러닝 시간
- 실제 페이스
- 실제 케이던스
- 실제 심박

따라서 AI 러닝 리포트의 운동 성과 계산에는 러닝 세션을 직접 사용하지 않는다. 향후 "모임 참여 이력" 문맥이 필요할 때만 보조 정보로 확장한다.

### 2.5 러닝 프로필 구조

`running_profiles`는 회원별 선호/목표 설정이다.

주요 컬럼:

- `member_id`
- `running_intensity`: `LOW`, `MODERATE`, `HIGH`, `INTERVAL`
- `target_bpm_min`
- `target_bpm_max`
- `target_cadence_min`
- `target_cadence_max`
- `preferred_genre`

주의:

- 여기의 `target_bpm_min/max`는 음악 추천 BPM 범위다.
- `average_heart_rate_bpm`과 같은 생체 심박값이 아니다.
- `target_cadence_min/max`는 nullable이므로 없으면 데이터 부족 또는 최근 기록 기반 보조 산출로 처리한다.

### 2.6 음악 추천 및 추천 로그 구조

추천 조회 결과는 `recommendation_logs`에 저장된다.

주요 컬럼:

- `member_id`
- `requested_intensity`
- `requested_bpm_min`
- `requested_bpm_max`
- `requested_genre`
- `request_conditions` JSON
- `result_summary` JSON
- `recommendation_reason`
- `saved_playlist_id`
- `created_at`

현재 `result_summary`는 `{"count": n, "musicIds":[...]}` 형태로 생성된다. 추천 당시의 곡 제목, 장르, BPM, 에너지 레벨 전체 스냅샷은 저장하지 않는다.

추천 저장을 누르면 별도 트랜잭션으로 다음 구조가 생성된다.

- `playlists`: `USER + CURATION_COPY`
- `playlist_items`: 추천 곡 목록
- `recommendation_logs.saved_playlist_id`: 생성된 플레이리스트 ID 연결

AI 리포트에서 추천 음악 문맥을 쓰려면 다음 우선순위를 권장한다.

1. `recommendation_logs` 최근 로그의 요청 조건과 추천 사유 사용
2. `saved_playlist_id`가 있으면 `playlists`, `playlist_items`, `music` 조인으로 실제 플레이리스트/곡 정보 보강
3. `saved_playlist_id`가 없으면 `result_summary.musicIds`를 파싱해 `music` 조회를 추가할 수 있으나, 현재 서비스가 정규 JSON 파서를 쓰지 않고 정규식으로 파싱하므로 구현 시 구조화 파서 도입 여부를 별도 판단

### 2.7 음악, 장르, BPM, 운동 강도 데이터

`music` 테이블:

- `genre`: 장르
- `bpm`: 음악 템포 BPM, nullable
- `music_energy_level`: `CALM`, `STEADY`, `ENERGETIC`, `PEAK`, nullable
- `source_type`: `ADMIN`, `SEED`, `EXTERNAL`, `SPOTIFY`
- Spotify 메타데이터: `spotify_track_id`, `spotify_uri`, `spotify_external_url`

`running_profiles` 테이블:

- 선호 장르: `preferred_genre`
- 목표 음악 BPM: `target_bpm_min`, `target_bpm_max`
- 목표 운동 강도: `running_intensity`

`recommendation_logs` 테이블:

- 추천 요청 강도: `requested_intensity`
- 추천된 BPM 조건: `requested_bpm_min`, `requested_bpm_max`
- 추천 장르 조건: `requested_genre`

운동 강도 변화는 현재 러닝 기록 자체에 저장되지 않는다. 가능한 산출 방식은 다음 중 하나다.

- 최근 추천 로그의 `requested_intensity` 변화
- 러닝 프로필의 현재 `running_intensity`
- 평균 심박이 충분할 때 백엔드에서 내부 규칙으로 추정한 `EASY/MODERATE/HIGH` 추세

정확한 생리학적 강도 변화로 표현하지 말고, "기록 기반 추정" 또는 "추천 요청 기준 변화"로 라벨링해야 한다.

## 3. 실제 재사용 가능한 데이터 목록

### 3.1 바로 사용 가능

러닝 기록:

- 최근 러닝 기록 5~10개: `running_records`
- 최근 평균 거리: `distance_km`
- 최근 평균 시간: `duration_seconds`
- 최근 평균 페이스: `pace_seconds_per_km` 또는 거리/시간 재계산
- 케이던스 평균/범위: `average_cadence` 값이 있는 기록만 사용
- 심박 평균/범위: `average_heart_rate_bpm` 값이 있는 기록만 사용
- 기록 날짜 범위: `record_date`

러닝 프로필:

- 현재 운동 강도 목표: `running_intensity`
- 목표 음악 BPM 범위: `target_bpm_min`, `target_bpm_max`
- 목표 케이던스 범위: `target_cadence_min`, `target_cadence_max`
- 선호 장르: `preferred_genre`

추천 로그:

- 최근 추천 로그: `recommendation_logs`
- 추천 BPM 범위: `requested_bpm_min`, `requested_bpm_max`
- 추천 장르: `requested_genre`
- 추천 강도: `requested_intensity`
- 추천 조건 JSON: `request_conditions`
- 추천 결과 요약 JSON: `result_summary`
- 추천 사유: `recommendation_reason`
- 저장 플레이리스트 연결: `saved_playlist_id`

플레이리스트/음악:

- 저장된 추천 플레이리스트: `playlists`
- 플레이리스트 수록곡: `playlist_items`
- 곡 제목/아티스트/장르/BPM/에너지/Spotify URL: `music`

### 3.2 제한적으로 사용 가능

- `music.bpm`: 음악 템포이며 러너 심박 BPM이 아니다.
- `running_profiles.target_bpm_min/max`: 추천 음악 BPM 범위이며 평균 심박이 아니다.
- `recommendation_logs.result_summary`: 곡 ID 목록만 안정적으로 확인 가능하다.
- `music_energy_level`: 곡 에너지 레벨이지 실제 운동 강도가 아니다.

## 4. 누락 데이터 및 대응 정책

### 4.1 현재 없는 데이터

- 러닝 중 구간별 페이스
- 러닝 중 구간별 심박
- 러닝 중 케이던스 범위 원본
- 최고/최저 심박
- 운동 강도 이력의 명시적 저장 컬럼
- 러닝 기록과 특정 추천 로그/플레이리스트의 직접 연결
- AI 리포트 저장 테이블
- LLM 호출 이력/상태 저장 구조
- LLM API Key 환경 변수와 설정 구조

### 4.2 데이터 부족 정책

누락된 숫자 값을 0으로 보정하지 않는다.

권장 기준:

- 최근 러닝 기록이 3개 미만이면 리포트 생성은 422 `AI_RUNNING_REPORT_DATA_NOT_ENOUGH`
- 최근 기록이 3개 이상 5개 미만이면 생성은 허용하되 `dataQuality.status = "LIMITED"`로 표시
- 케이던스 값이 있는 기록이 2개 미만이면 케이던스 해석은 `insufficient`로 표시
- 심박 값이 있는 기록이 2개 미만이면 심박 해석은 `insufficient`로 표시
- 최근 추천 로그가 없으면 음악 추천 문맥은 `notAvailable`로 표시하고 리포트 자체는 생성 가능

LLM 입력에는 각 지표마다 `sampleCount`, `missingCount`, `status`를 포함한다.

## 5. API 설계안

### 5.1 최종 URL 제안

생성 API:

```http
POST /api/members/me/ai-running-reports
```

조회 API:

```http
GET /api/members/me/ai-running-reports/latest
GET /api/members/me/ai-running-reports/{reportId}
```

선정 이유:

- 기존 `GET/PUT /api/members/me/running-profile`과 같은 `/members/me` 패턴을 따른다.
- 회원 ID를 URL 파라미터로 노출하지 않는다.
- 생성과 조회를 분리해 LLM 실패 재시도와 저장 리포트 조회를 명확히 나눌 수 있다.

### 5.2 생성 API 응답 초안

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 1,
    "status": "COMPLETED",
    "periodStart": "2026-06-01",
    "periodEnd": "2026-06-24",
    "recordCount": 8,
    "dataQuality": {
      "overall": "OK",
      "cadence": "OK",
      "heartRate": "LIMITED",
      "recommendationLogs": "OK"
    },
    "summary": "최근 러닝은 거리와 페이스가 안정적으로 유지되고 있습니다.",
    "sections": [
      {
        "title": "페이스",
        "content": "최근 평균 페이스는 ...",
        "severity": "INFO"
      }
    ],
    "generatedAt": "2026-06-24T12:00:00"
  }
}
```

### 5.3 조회 API 응답 초안

생성 API와 같은 `ReportResponse`를 반환한다. 최신 리포트가 없으면 404 `AI_RUNNING_REPORT_NOT_FOUND`를 권장한다.

### 5.4 실패 응답 정책

인증 실패:

- HTTP 401
- `AUTH_REQUIRED`
- 기존 로그인 유도 UI 재사용

권한 실패:

- 본인 리포트가 아닌 ID 접근 시 HTTP 403
- `ACCESS_DENIED`

데이터 부족:

- HTTP 422
- `AI_RUNNING_REPORT_DATA_NOT_ENOUGH`
- 메시지는 "최근 러닝 기록이 부족해 AI 리포트를 생성할 수 없습니다." 계열

LLM API 실패:

- HTTP 502
- `AI_RUNNING_REPORT_LLM_FAILED`
- 백엔드 계산 결과는 저장하지 않거나, 저장한다면 `status = FAILED`와 실패 코드만 저장
- API Key, 원문 프롬프트, 회원 식별 정보는 로그에 남기지 않음

## 6. DB 설계안

### 6.1 저장 테이블 필요 여부

저장 테이블이 필요하다.

이유:

- LLM 호출 비용과 지연을 줄여야 한다.
- 같은 데이터에 대한 최신 리포트를 Vue에서 빠르게 조회해야 한다.
- LLM 실패/성공 상태를 분리해 재시도 정책을 만들 수 있다.
- 사용자가 과거 리포트를 확인할 가능성이 있다.

### 6.2 최소 테이블 제안

테이블명:

- `ai_running_reports`

최소 컬럼:

```sql
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
member_id BIGINT UNSIGNED NOT NULL
status VARCHAR(20) NOT NULL
period_start DATE NULL
period_end DATE NULL
record_count INT UNSIGNED NOT NULL
input_snapshot JSON NOT NULL
report_summary TEXT NULL
report_sections JSON NULL
model_name VARCHAR(100) NULL
error_code VARCHAR(50) NULL
created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
```

제약 조건:

- PK: `id`
- FK: `member_id -> members(id)`
- CHECK: `status IN ('PENDING', 'COMPLETED', 'FAILED')`
- INDEX: `(member_id, created_at)`

저장하지 말아야 할 값:

- 회원 이메일
- 닉네임
- 로그인 ID
- Spotify access token/refresh token
- LLM API Key
- 원문 개인정보 포함 프롬프트

### 6.3 스키마 관리 방식

현재 프로젝트는 다음 방식을 사용한다.

- `spring.sql.init.mode: never`
- `schema.sql`, `data.sql`, `dummy.sql`을 수동 실행
- `ssafit/src/main/resources/sql/migrations`에 날짜 기반 수동 SQL 파일 존재
- Flyway/Liquibase 의존성 없음
- Docker Compose MySQL 초기화는 `schema.sql`을 `/docker-entrypoint-initdb.d/01-schema.sql`로 마운트

따라서 구현 단계에서는 다음 둘을 함께 반영해야 한다.

- 신규 설치용 `schema.sql`에 `ai_running_reports` 추가
- 기존 DB용 `migrations/YYYYMMDD_add_ai_running_reports.sql` 추가

## 7. 백엔드 계산 영역과 LLM 해석 영역 분리

### 7.1 백엔드 계산 영역

백엔드는 결정적이고 검증 가능한 값을 계산한다.

- 최근 기록 조회: 5~10개
- 기간 시작/종료일
- 기록 수
- 총 거리
- 평균 거리
- 총 시간
- 평균 시간
- 평균 페이스
- 케이던스 평균, 최소, 최대, sampleCount, missingCount
- 심박 평균, 최소, 최대, sampleCount, missingCount
- 추천 로그 최근 N개
- 추천 BPM 범위 최근 분포
- 추천 장르 최근 분포
- 저장 플레이리스트/곡 정보 요약
- 데이터 품질 플래그

백엔드는 다음 판단을 직접 수행한다.

- 데이터 부족 여부
- null 지표의 제외
- 숫자 반올림
- LLM에 전달 가능한 익명화 입력 생성

### 7.2 LLM 해석 영역

LLM은 계산값을 바탕으로 자연어 해석만 수행한다.

허용:

- 페이스 안정성 해석
- 거리/시간 변화 요약
- 케이던스/심박 데이터가 충분할 때의 경향 설명
- 최근 추천 음악 조건과 러닝 지표의 관계 설명
- 다음 러닝을 위한 일반적 제안

금지:

- 회원 식별 정보 사용
- 이메일, 닉네임, 로그인 ID 전달
- API Key 전달
- 의료 진단
- 누락 데이터를 0으로 간주
- 음악 BPM을 심박 BPM처럼 해석
- 저장되지 않은 러닝 데이터를 추측

## 8. LLM 입력 데이터 계약

권장 입력 스키마:

```json
{
  "version": "2026-06-24",
  "locale": "ko-KR",
  "reportType": "AI_RUNNING_REPORT",
  "dataQuality": {
    "overall": "OK",
    "runningRecords": { "status": "OK", "sampleCount": 8, "missingCount": 0 },
    "cadence": { "status": "LIMITED", "sampleCount": 3, "missingCount": 5 },
    "heartRate": { "status": "INSUFFICIENT", "sampleCount": 1, "missingCount": 7 },
    "recommendationLogs": { "status": "OK", "sampleCount": 5 }
  },
  "period": {
    "startDate": "2026-06-01",
    "endDate": "2026-06-24"
  },
  "runningSummary": {
    "recordCount": 8,
    "averageDistanceKm": 5.24,
    "averageDurationSeconds": 1840,
    "averagePaceSecondsPerKm": 351,
    "totalDistanceKm": 41.9,
    "totalDurationSeconds": 14720
  },
  "recentRecords": [
    {
      "recordDate": "2026-06-24",
      "distanceKm": 5.2,
      "durationSeconds": 1800,
      "paceSecondsPerKm": 346,
      "averageCadence": 172,
      "averageHeartRateBpm": null
    }
  ],
  "cadence": {
    "average": 171,
    "min": 168,
    "max": 176,
    "sampleCount": 3,
    "status": "LIMITED"
  },
  "heartRate": {
    "average": null,
    "min": null,
    "max": null,
    "sampleCount": 1,
    "status": "INSUFFICIENT"
  },
  "runningProfile": {
    "runningIntensity": "MODERATE",
    "targetCadenceMin": 165,
    "targetCadenceMax": 175,
    "preferredGenre": "Pop",
    "targetMusicBpmMin": 120,
    "targetMusicBpmMax": 160
  },
  "recentRecommendationLogs": [
    {
      "createdAt": "2026-06-23T21:00:00",
      "requestedIntensity": "MODERATE",
      "requestedMusicBpmMin": 120,
      "requestedMusicBpmMax": 160,
      "requestedGenre": "Pop",
      "savedPlaylist": {
        "exists": true,
        "playlistTitle": "Evening Run",
        "spotifyExportStatus": "SUCCESS"
      }
    }
  ]
}
```

개인정보 제거 원칙:

- `memberId`도 LLM 입력에서 제외한다.
- `loginId`, `email`, `nickname`은 절대 포함하지 않는다.
- Spotify 사용자 ID와 토큰은 절대 포함하지 않는다.
- 플레이리스트 제목은 사용자가 입력한 텍스트일 수 있으므로 필요 최소한만 전달하고, 민감정보 필터링을 고려한다.

## 9. Vue 화면 배치안

### 9.1 최종 선정 위치

가장 자연스러운 위치는 Vue의 `RunningRecordListView.vue` 상단 대시보드 아래다.

근거:

- 이미 `/running-records`는 인증이 필요한 개인 러닝 기록 화면이다.
- 현재 화면에 전체/연/월 필터와 합산 거리, 평균 페이스 요약이 있어 AI 리포트의 근거 데이터와 가장 가깝다.
- 마이페이지는 JSP 기반이고 Vue API 모듈/상태 처리 패턴과 분리되어 있어 신규 AI 리포트 카드 구현 비용이 더 크다.
- 추천 화면(`/recommendations`)은 음악 추천 목적이 강해 러닝 리포트의 주요 진입점으로는 덜 직관적이다.

### 9.2 프론트 구조 제안

신규 파일:

- `frontend/src/api/aiRunningReportApi.js`
- `frontend/src/components/AiRunningReportPanel.vue`

수정 후보:

- `frontend/src/views/RunningRecordListView.vue`

화면 동작:

- 최신 리포트 조회: 진입 시 `GET /api/members/me/ai-running-reports/latest`
- 리포트 없음: "최근 기록으로 리포트 생성" 버튼 표시
- 생성 버튼: `POST /api/members/me/ai-running-reports`
- 데이터 부족: 기존 `PageStatePanel` 또는 인라인 상태로 안내
- LLM 실패: 재시도 가능 상태 표시

로그인 상태:

- 라우터의 `meta.requiresAuth`와 `authApi.getSession()` 보호를 그대로 사용한다.
- API 401은 기존 `ApiError.loginRequired` 흐름을 따른다.

## 10. 환경 변수 및 application 설정안

현재 설정 방식:

- 기본 설정: `application.yml`
- Docker 설정: `application-docker.yml`
- Docker Compose 환경 변수: `compose.yaml`
- 예시 환경 변수: `.env.example`, `frontend/.env.example`

LLM 설정 추가 시 권장:

```yaml
ai:
  running-report:
    enabled: ${AI_RUNNING_REPORT_ENABLED:false}
    model: ${AI_RUNNING_REPORT_MODEL:}
    api-key: ${AI_RUNNING_REPORT_API_KEY:}
    timeout-ms: ${AI_RUNNING_REPORT_TIMEOUT_MS:10000}
```

주의:

- API Key는 로그에 출력하지 않는다.
- LLM 요청/응답 원문 로깅은 기본 비활성화한다.
- Docker Compose에는 `${AI_RUNNING_REPORT_API_KEY:-}` 형태로 주입하되 `.env.example`에는 실제 값 없이 키 이름만 둔다.

## 11. Mapper XML 위치 및 구현 패턴

현재 Mapper XML 위치:

- `ssafit/src/main/resources/mappers/**/*.xml`

MyBatis 설정:

- `mapper-locations: classpath:/mappers/**/*.xml`
- `map-underscore-to-camel-case: true`

AI 리포트 구현 시 권장 패키지:

- `com.ssafy.ssafit.airunningreport.domain`
- `com.ssafy.ssafit.airunningreport.dto`
- `com.ssafy.ssafit.airunningreport.mapper`
- `com.ssafy.ssafit.airunningreport.service`
- `com.ssafy.ssafit.airunningreport.controller.api`

Mapper XML:

- `ssafit/src/main/resources/mappers/airunningreport/AiRunningReportMapper.xml`

주의:

- `SsafitApplication`의 `@MapperScan`에 신규 mapper 패키지를 추가해야 한다.
- 기존 네이밍이 `runningrecord`, `runningprofile`, `runningsession`처럼 소문자 도메인 패키지를 쓰므로 `airunningreport`가 가장 일관적이다.

## 12. 위험 요소

1. `schema.sql` 일부 주석/문자열이 깨져 보인다.
   - SQL 실행 자체보다 문서 가독성 문제로 보이나, 신규 SQL 작성 시 UTF-8 저장을 확인해야 한다.

2. 추천 로그의 `result_summary`가 상세 스냅샷이 아니다.
   - 시간이 지난 뒤 곡 정보가 수정되면 과거 추천 당시의 장르/BPM과 조회 시점 정보가 달라질 수 있다.

3. 음악 BPM과 심박 BPM의 의미 혼동 위험이 크다.
   - DTO와 LLM 입력에서 `musicBpm`, `heartRateBpm` 이름을 명확히 분리해야 한다.

4. LLM 응답 비결정성
   - 숫자 계산은 반드시 백엔드에서 완료하고, LLM은 해석 문장만 생성해야 한다.

5. 개인정보 유출 위험
   - 회원 식별 정보, 이메일, 닉네임, Spotify 토큰, API Key는 LLM 입력과 로그에서 제외해야 한다.

6. 데이터 부족 처리
   - 케이던스/심박 nullable 데이터를 0으로 평균내면 리포트가 왜곡된다.

7. 동기 LLM 호출 지연
   - 초기 구현은 동기 `POST`로 가능하지만, 응답 지연이 커지면 `PENDING` 저장 후 polling 구조가 필요하다.

## 13. 구현 우선순위

1. 백엔드 조회/계산 전용 서비스 설계
   - 최근 기록 5~10개 조회
   - 계산 DTO 구성
   - 데이터 품질 판정

2. `ai_running_reports` 테이블 및 Mapper 추가
   - `schema.sql`
   - 수동 migration SQL
   - Mapper XML

3. LLM 클라이언트 추상화
   - 실제 호출 구현과 테스트용 fake 분리
   - API Key/PII 로깅 금지

4. 생성/조회 API 추가
   - `POST /api/members/me/ai-running-reports`
   - `GET /api/members/me/ai-running-reports/latest`
   - `GET /api/members/me/ai-running-reports/{reportId}`

5. Vue 패널 추가
   - `RunningRecordListView.vue`에 최신 리포트 카드 배치
   - `aiRunningReportApi.js`
   - 생성/조회/실패/데이터 부족 상태 처리

6. 테스트
   - 서비스 계산 단위 테스트
   - Mapper 통합 테스트
   - Controller 인증/데이터 부족/LLM 실패 테스트
   - Vue API 상태 처리 테스트 또는 e2e 보강

## 14. 다음 단계 전 확인 사항

- LLM 제공자와 모델명을 확정해야 한다.
- 리포트 생성 기준을 최근 5개, 10개, 또는 최대 10개 중 최소 3개로 할지 확정해야 한다.
- 리포트 저장 기간 또는 과거 리포트 목록 제공 여부를 결정해야 한다.
- LLM 실패 시 계산 스냅샷을 `FAILED`로 저장할지, 저장하지 않을지 결정해야 한다.
- 추천 로그의 `result_summary.musicIds`를 정규 JSON 파서로 읽도록 개선할지 결정해야 한다.
