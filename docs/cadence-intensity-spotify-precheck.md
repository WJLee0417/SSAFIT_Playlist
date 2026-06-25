# 케이던스·운동 강도·Spotify 연동 사전 점검 보고서

점검 기준: `feature/cadence-intensity-spotify-live` 브랜치, Spring Boot `4.0.6`, Java 25, MyBatis Spring Boot Starter `4.0.1`, MySQL 8 계열, Vue 3/Vite 구조.

이번 단계에서는 기능 코드, SQL, 테스트, 설정을 변경하거나 빌드·테스트·서버 실행·실제 Spotify 인증을 수행하지 않았다.

## 1. 현재 구조 요약

- Backend는 `ssafit/`의 Spring Boot WAR 프로젝트이며 JSP 회원 화면과 JSON API가 공존한다.
- Frontend는 `frontend/`의 Vue 3 SPA이며 기본 API 주소는 `http://localhost:8080/api`, 개발 서버 포트는 `5173`이다.
- DB 접근은 MyBatis XML Mapper를 사용하고 `map-underscore-to-camel-case: true`가 설정되어 있다. 주요 Mapper는 복잡한 조회와 안전한 매핑을 위해 명시적 `resultMap`도 사용한다.
- 운영 스키마는 `ssafit/src/main/resources/sql/schema.sql`을 수동 실행한다. `spring.sql.init.mode: never`이므로 애플리케이션 시작 시 자동 적용되지 않는다.
- 기존 DB 변경은 `schema.sql` 재실행이 아니라 별도 migration SQL 작성이 필요하다고 SQL README에 명시되어 있으나, Flyway/Liquibase와 같은 버전형 migration 도구와 migration 디렉터리는 현재 없다.
- 로컬 기준 데이터는 `data.sql`, 개발·확인용 데이터는 `dummy.sql`에 있다. 테스트 초기화는 `src/test/resources/sql/schema-reset.sql`, E2E seed는 `e2e-seed.sql`에서 별도 관리한다.
- 러닝 프로필은 현재 `runningIntensity`, `targetBpmMin/Max`, `preferredGenre`를 저장한다. `targetBpmMin/Max`는 추천에서 음악 `music.bpm` 범위로 직접 사용된다.
- 러닝 기록은 날짜, 거리, 시간, 계산된 페이스, 메모만 저장한다. 케이던스와 평균 심박수 필드는 없다.
- 음악은 `music.bpm` 하나를 가지며 현재 의미는 음악 템포 BPM이다. 다만 필드명이 단순 `bpm`이라 향후 심박수 BPM과 혼동될 여지가 크다.
- 추천은 프로필의 `targetBpmMin/Max` 또는 강도별 기본 음악 BPM 범위로 음악을 필터링하고, 음악 BPM 근접도 + 전체 사용자 리뷰 평균을 합산한다.
- Spotify는 곡 검색, 내부 음악 저장, OAuth 연결, 토큰 갱신, 플레이리스트 생성과 곡 추가가 코드상 구현되어 있다. 실제 Spotify 계정 기반 검증은 아직 문서 체크리스트만 있고 Mock 테스트 중심이다.

## 2. 러닝 데이터 모델 수정 대상

### `running_profiles`

현재 컬럼:

| DB 컬럼 | Java/JSON 필드 | NULL | 현재 의미 |
| --- | --- | --- | --- |
| `running_intensity` | `runningIntensity` | NOT NULL | LOW/MODERATE/HIGH/INTERVAL |
| `target_bpm_min` | `targetBpmMin` | NOT NULL | 추천 음악 최소 템포 BPM |
| `target_bpm_max` | `targetBpmMax` | NOT NULL | 추천 음악 최대 템포 BPM |
| `preferred_genre` | `preferredGenre` | NULL | 선호 장르 |

- 케이던스 필드는 없다. 추가 후보는 `target_cadence_min`, `target_cadence_max` / `targetCadenceMin`, `targetCadenceMax`이며 단위는 SPM으로 고정해야 한다.
- 평균 심박수는 개인별 기록 값이므로 프로필보다는 `running_records`가 주 저장 위치다. 운동 강도 추천용 심박 기준을 프로필에 추가할지는 별도 정책 결정이 필요하다.
- 현재 BPM 범위는 DTO, Service, JSP에서 모두 필수다. 새 케이던스 필드를 nullable로 점진 도입할지 필수로 전환할지에 따라 기존 회원 프로필 호환성이 달라진다.
- 기존 `target_bpm_min/max`를 음악 BPM으로 유지한다면 이름을 `targetMusicBpmMin/Max`로 명확히 바꾸는 방안을 검토해야 한다. DB/Java/JSON 이름 변경은 기존 API와 데이터 migration을 동반한다.

연결 코드:

- `runningprofile/domain/RunningProfile.java`
- `runningprofile/dto/RunningProfileApiDtos.java`
- `runningprofile/dto/RunningProfileViewForm.java`
- `mappers/runningprofile/RunningProfileMapper.xml`
- `runningprofile/service/RunningProfileService.java`
- `runningprofile/controller/api/RunningProfileApiController.java`
- `member/controller/view/RunningProfileViewController.java`
- `WEB-INF/views/member/running-profile.jsp`
- `frontend/src/api/runningProfileApi.js`

Mapper 누락 위험:

- 새 컬럼을 추가하면 `runningProfileResultMap`, `runningProfileColumns`, `INSERT`, `UPDATE` 네 곳을 모두 수정해야 한다.
- 현재 `UPDATE`는 전 필드를 덮어쓴다. 새 nullable 필드가 DTO 변환에서 누락되면 기존 값을 `NULL`로 지울 수 있다.
- Service는 `MIN_BPM/MAX_BPM`만 검증한다. 케이던스 범위와 음악 BPM 범위의 검증 상수·오류 코드를 분리해야 한다.

### `running_records`

현재 컬럼/필드:

- `record_date` / `recordDate`
- `distance_km` / `distanceKm`
- `duration_seconds` / `durationSeconds`
- `pace_seconds_per_km` / `paceSecondsPerKm`
- `memo` / `memo`

누락 항목:

- `average_cadence` / `averageCadence` / JSON `averageCadence`
- `average_heart_rate_bpm` / `averageHeartRateBpm` / JSON `averageHeartRateBpm`

수정 연결 지점:

- `runningrecord/domain/RunningRecord.java`
- `runningrecord/dto/RunningRecordApiDtos.java`의 `CreateRequest`, `UpdateRequest`, `Response`, `toDomain`
- `mappers/runningrecord/RunningRecordMapper.xml`의 `resultMap`, 공통 컬럼, `INSERT`, `UPDATE`
- `runningrecord/service/RunningRecordService.java`의 입력 검증과 update 값 보존
- `runningrecord/controller/api/RunningRecordApiController.java`
- `frontend/src/views/RunningRecordFormView.vue`
- `frontend/src/views/RunningRecordDetailView.vue`
- `frontend/src/views/RunningRecordListView.vue`
- `frontend/src/utils/runningRecord.js`
- `frontend/src/api/runningRecordApi.js`

NULL 호환성:

- 기존 기록을 보존하려면 두 신규 DB 컬럼은 우선 `NULL` 허용이 안전하다.
- Domain은 primitive `int`가 아니라 `Integer`, Vue 폼은 빈 문자열을 API에서 `null`로 변환하는 방식이 적합하다.
- 기존 레코드를 임의 BPM/SPM으로 backfill하면 실제 측정값처럼 오인될 수 있으므로 금지해야 한다.
- MySQL CHECK 후보는 케이던스 60~240 SPM, 평균 심박수 30~240 BPM이지만 실제 제품 정책과 센서 허용 범위를 먼저 확정해야 한다.

SQL 관리 대상:

- 운영 기준: `ssafit/src/main/resources/sql/schema.sql`
- 개발 데이터: `ssafit/src/main/resources/sql/dummy.sql`
- 기본 데이터: `ssafit/src/main/resources/sql/data.sql`
- 테스트 초기화: `ssafit/src/test/resources/sql/schema-reset.sql`
- E2E seed: `ssafit/src/test/resources/sql/e2e-seed.sql`
- 별도 migration 파일을 새로 둘 위치와 실행 순서가 현재 표준화되어 있지 않다.

## 3. 음악·추천 구조 수정 대상

### 음악과 플레이리스트 관계

- `music`: 음악 메타데이터와 템포 `bpm`, Spotify 식별자/URI를 저장한다.
- `playlists`: 내부 플레이리스트, Spotify export 상태와 결과를 저장한다.
- `playlist_items`: `playlist_id + music_id`, `playlist_id + order_no` UNIQUE 관계 테이블이다.
- `playlist_saves`: 다른 공개/공식 플레이리스트를 회원 저장 목록에 참조하는 관계다. 자동 추천 저장에는 사용하지 않는다.
- 자동 추천 저장은 `RecommendationService.saveRecommendation`이 `USER + CURATION_COPY` 플레이리스트를 생성하고 `playlist_items`를 bulk insert한 뒤 `recommendation_logs.saved_playlist_id`를 연결한다.

### 음악 BPM과 등록·수정

- `music.bpm` / `Music.bpm` / JSON `bpm`은 현재 음악 템포 BPM이며 60~220 또는 NULL이다.
- 관리자용 일반 음악 등록·수정 API/화면은 현재 확인되지 않았다. Vue 음악 화면은 조회·상세·리뷰 중심이고, 신규 음악 저장은 Spotify 검색 결과 import 경로만 있다.
- Spotify 검색 결과의 `bpm`, `genre`는 `SpotifyService.toMetadata`에서 항상 `null`로 설정된다.
- `MusicApiDtos.SpotifyImportRequest`는 클라이언트가 BPM/장르를 선택적으로 보낼 수 있게 되어 있지만, 현재 Vue는 검색 응답의 null 값을 그대로 재전송한다.
- 결과적으로 Spotify에서 저장한 음악은 대개 `music.bpm IS NULL`, `genre IS NULL`이며 BPM 범위가 설정된 자동 추천 SQL에서 제외된다.
- 향후 Spotify 오디오 특성 기반 BPM/energy를 가져오려면 API 제공 가능 여부와 권한/정책을 실제 Spotify 문서 및 계정으로 재확인해야 한다. 현재 코드에는 audio features 또는 energy 호출이 없다.

### 현재 추천 점수

`MusicMapper.xml#selectRecommendedMusic`의 현재 점수는 다음 합이다.

- 음악 BPM과 요청 BPM 범위 중앙값의 근접도: 최대 약 5점
- 전체 사용자 `avg_preference_score`: 최대 5점
- 전체 사용자 `avg_running_fit_score * 1.5`: 최대 7.5점
- 요청 장르 일치: 2점

위험:

- 이름이 `preferenceScore`지만 현재 로그인 회원 개인 취향이 아니라 모든 리뷰의 전역 평균이다.
- 찜에 해당하는 `playlist_saves`는 플레이리스트 단위이고, 음악 단위 좋아요/청취 이력은 없다.
- 사용자 개인 리뷰는 `music_reviews.member_id`로 존재하지만 추천 SQL은 회원별 점수로 좁히지 않는다.
- BPM 필터가 먼저 적용되므로 `bpm IS NULL`인 Spotify import 곡은 리뷰 점수가 높아도 추천 대상이 되지 않는다.
- 운동 강도는 현재 별도 생체 강도 점수가 아니라 음악 BPM 기본 범위를 선택하는 스위치 역할이다.

수정 대상:

- `music/domain/Music.java`: `bpm`을 음악 템포임이 드러나는 이름으로 명확화 검토, energy 등 원본 음악 특성 추가 여부 결정
- `music/domain/MusicSearchCondition.java`
- `music/dto/MusicApiDtos.java`
- `mappers/music/MusicMapper.xml`
- `music/service/MusicService.java`
- `recommendation/domain/RecommendationLog.java`
- `recommendation/dto/RecommendationApiDtos.java`
- `mappers/recommendation/RecommendationLogMapper.xml`
- `recommendation/service/RecommendationService.java`
- `frontend/src/components/MusicSearchBar.vue`
- `frontend/src/utils/musicQuery.js`
- `frontend/src/views/MusicDetailView.vue`
- `frontend/src/views/RecommendationView.vue`
- `frontend/src/components/RecommendationResult.vue`

권장 메트릭 분리:

- `tempoMatchScore`: 목표 케이던스 SPM과 음악 템포 BPM의 1:1 및 2:1 하프타임 차이
- `intensityMatchScore`: 평균 심박수 BPM 또는 명시적 운동 강도와 음악 energy의 적합도
- `preferenceScore`: 개인 리뷰, 전체 리뷰, 저장/찜 등 사용 가능한 취향 신호
- `finalScore`: 위 점수의 가중 합. 각 원점수·가중치·설명을 응답과 로그에서 분리해 추적 가능하게 해야 한다.

## 4. Spotify 연동 수정 대상

### 현재 구현 상태

| 항목 | 현재 구현 |
| --- | --- |
| OAuth 시작 | `GET /api/spotify/oauth/authorize`, 로그인 필수, authorize URL JSON 반환 |
| OAuth callback | `GET /api/spotify/oauth/callback`, 로그인 세션의 회원 ID 사용 |
| state | `SecureRandom` 32 bytes, Base64 URL-safe, HttpSession 저장, 일회 검증·삭제 |
| 토큰 저장 | `spotify_connections`에 access/refresh token 평문 저장 |
| 연결 상태 | `GET /api/spotify/me/connection`, 미연결도 HTTP 200 `connected=false` |
| 곡 검색 | Backend가 Client Credentials token 발급 후 `/v1/search?type=track` 호출 |
| 내부 저장 | `POST /api/music/from-spotify` → `SpotifyService` → `MusicService` |
| 플레이리스트 생성 | `POST /v1/me/playlists` |
| 곡 추가 | `POST /v1/playlists/{playlistId}/items` |
| 토큰 갱신 | 만료 30초 전 refresh, 새 refresh token이 없으면 기존 값 보존 |
| Vue OAuth 이동 | Backend URL 수신 후 origin/path 검증, `window.location.assign` |
| 회원 연결 | 프로젝트 HttpSession 회원 ID ↔ `spotify_connections.member_id`, Spotify `/me` ID 저장 |

`/tracks`와 `/items`:

- 내부 API 검색 경로는 `/api/spotify/tracks/search`이며 Spotify Web API 검색은 `/v1/search`다.
- Spotify 플레이리스트 곡 추가는 현재 `/v1/playlists/{id}/items`를 사용한다.
- 플레이리스트 추가용 구형 `/v1/playlists/{id}/tracks` 호출은 현재 main 코드에서 발견되지 않았다.

### 보안과 설정

- `application.yml`은 `${SPOTIFY_CLIENT_ID:}`, `${SPOTIFY_CLIENT_SECRET:}`, `${SPOTIFY_REDIRECT_URI:}`로 환경 변수만 참조하고 실제 Spotify 값은 포함하지 않는다.
- Vue 코드에는 Client Secret이 없으며 Spotify API를 직접 호출하지 않는다.
- 연결 응답 DTO는 `connected`, `spotifyUserId`, `tokenExpiresAt`만 노출한다.
- `.env`는 `.gitignore` 대상이다. 현재 `.env.example`에는 빈 값만 있으나 `.env` 패턴에 함께 매치되어 Git 추적되지 않는 상태다.
- 실제 Secret을 `application.yml`, README, 문서, 테스트 fixture, 화면 캡처, 로그에 붙여 넣으면 Git 노출 위험이 생긴다.
- 별도 위험으로 현재 추적되는 `application.yml`에는 로컬 MySQL 비밀번호가 평문으로 들어 있다. Spotify Secret과는 별개지만 설정 보안 정책을 함께 정리해야 한다.
- DB access/refresh token도 현재 평문이다. 애플리케이션 계층 암호화 또는 KMS/Secret Manager 연계를 검토해야 한다.

### Redirect URI와 scope

- 환경 변수에 기본값이 없으므로 로컬에서도 반드시 `SPOTIFY_REDIRECT_URI`를 설정해야 한다.
- 문서와 Controller 기준 로컬 callback 주소는 `http://localhost:8080/api/spotify/oauth/callback`이다.
- callback 성공/실패 후 Vue 고정 주소 `http://localhost:5173/oauth/spotify/complete`로 이동한다. 이 origin은 `FrontendReturnUrl`에 하드코딩되어 있어 발표/배포 환경 분리가 필요하다.
- 현재 scope는 `user-read-private`, `playlist-modify-private`, `playlist-modify-public`이다.
- `/me`로 사용자 ID만 확인하려면 `user-read-private`의 필요성을 실제 API 응답과 최소 권한 원칙으로 재검토할 수 있다.
- 비공개 플레이리스트만 만들 정책이면 `playlist-modify-public`은 제거 후보이며, 공개 export를 지원하려면 유지해야 한다.

### 오류 처리 부족

`SpotifyRestApiClient`는 `RestClientException`을 모두 `SPOTIFY_API_ERROR`로 변환한다.

- 401: access token 만료/무효를 감지해 한 번 refresh 후 재시도하지 않는다.
- 403: scope 부족, 사용자/앱 권한 문제를 구분하지 않는다.
- 429: `Retry-After`를 읽거나 사용자에게 대기 시간을 전달하지 않는다.
- 네트워크 오류/5xx: timeout, 제한된 재시도, circuit breaker 정책이 없다.
- 검색 Service와 export Service가 하위 오류를 다시 각각 `SPOTIFY_SEARCH_FAILED`, `SPOTIFY_PLAYLIST_*_FAILED`로 넓게 감싸 원인이 더 소실된다.
- `getConnection`은 DB에 만료 토큰이 있어도 `connected=true`를 반환하며 실제 유효성/refresh는 export 직전에만 확인한다.
- export는 외부 Spotify 상태와 내부 DB 트랜잭션을 원자적으로 롤백할 수 없다. 곡 추가 일부 성공 후 실패한 경우 정확한 성공 곡 수를 알 수 없고 자동 재시도를 차단하는 방식이다.

### 실제 연동 수동 확인 체크리스트

- Spotify Developer Dashboard의 Client ID가 발표용 애플리케이션과 일치하는지
- Client Secret이 현재 실행 환경에만 주입되고 Git/화면/로그에 없는지
- Redirect URI가 `http://localhost:8080/api/spotify/oauth/callback`과 문자 단위로 일치하는지
- 발표 환경 host/port가 다르면 Dashboard와 Backend 환경 변수를 함께 변경했는지
- Development Mode라면 발표용 Spotify 계정 이메일이 허용 사용자 목록에 등록되어 있는지
- 앱 상태와 Spotify 계정 유형에서 검색, `/me`, 플레이리스트 생성/추가 API가 허용되는지
- 프로젝트 로그인 회원과 연결된 `spotify_user_id`가 발표 계정의 실제 ID인지
- OAuth 동의 화면에 기대한 scope만 표시되는지
- 권한 거절, state 불일치, callback 세션 쿠키 유지 여부를 확인했는지
- 만료 직전/만료 후 refresh token 갱신이 실제로 동작하는지
- 비공개/공개 플레이리스트가 의도한 공개 범위로 생성되는지
- 100곡 초과 export가 100개 단위 batch로 순서대로 추가되는지
- URI 없는 내부 곡이 포함된 경우 외부 플레이리스트 생성 전에 차단되는지
- 실제 Spotify 앱에서 곡 수·순서·공개 범위와 저장 URL을 확인했는지

## 5. BPM / SPM / 심박수 BPM 혼용 위험 요소

확인 결과 요청에 제시된 직접 오연결은 현재 main 코드에 존재하지 않는다.

- `averageHeartRateBpm -> musicBpm`: 해당 필드 자체가 아직 없어 연결 없음
- `averageCadence -> musicBpm` 자동 저장: 해당 필드 자체가 아직 없어 연결 없음
- `musicBpm -> averageHeartRateBpm` 자동 변환: 없음
- `averageHeartRateBpm`으로 음악 템포 매칭 점수 계산: 없음

다만 다음 구조적 혼용 위험이 이미 존재한다.

1. `running_profiles.target_bpm_min/max`
   - 위치: `schema.sql`, `RunningProfile`, DTO, Mapper, Service, JSP
   - 문제: 이름만 보면 목표 심박수 BPM으로 오해할 수 있지만 실제 `RecommendationService.resolveBpmRange`에서 음악 템포 필터에 사용된다.

2. `RecommendationService.resolveBpmRange`
   - 문제: 프로필의 모호한 BPM 값을 `MusicSearchCondition.minBpm/maxBpm`에 직접 넣는다.
   - 후속 조치: 케이던스 기반 tempo match와 심박/운동 강도 기반 energy match를 별도 입력·점수로 분리해야 한다.

3. `MusicService.bpmRangeFor`와 `RecommendationService.resolveBpmRange`
   - 문제: LOW/MODERATE/HIGH/INTERVAL을 음악 BPM 범위로 바로 변환한다. 이는 현재 강도 추천의 유일한 정량 모델이며 심박수 기반 강도와 음악 energy가 없다.

4. `Music.bpm`, `MusicApiDtos.*.bpm`, Vue의 `BPM`
   - 문제: 음악 템포라는 접두어가 없어 평균 심박수 BPM 도입 시 DTO 조립·화면 라벨·추천 공식에서 잘못 연결하기 쉽다.

5. `recommendation_logs.requested_bpm_min/max`
   - 문제: 현재는 음악 템포 BPM이지만 이름과 JSON `bpmMin/bpmMax`만으로 의미가 고정되지 않는다. 향후 `requested_music_bpm_*`, `target_cadence_*`, `average_heart_rate_bpm`, energy 조건을 분리해야 한다.

6. `backend-cadence-extension-guidelines.md`
   - 문서에는 `targetCadenceMin/Max`, `averageCadence`, `averageHeartRateBpm` 분리 원칙이 이미 제시되어 있으나 실제 코드와 SQL에는 아직 반영되지 않았다. 구현 시 이 문서와 실제 API 계약의 범위·NULL 정책을 먼저 맞춰야 한다.

## 6. 단계별 수정 파일 목록

아래 Prompt 구분은 최종 범위를 안전한 의존 순서로 배치한 구현 대상 목록이다.

### Prompt 1 대상 파일

러닝 DB 스키마와 seed/migration:

- `ssafit/src/main/resources/sql/schema.sql`
- 신규 migration SQL 파일 및 실행 문서
- `ssafit/src/main/resources/sql/dummy.sql`
- 필요 시 `ssafit/src/main/resources/sql/data.sql`
- `ssafit/src/test/resources/sql/schema-reset.sql`
- `ssafit/src/test/resources/sql/e2e-seed.sql`
- `ssafit/src/main/resources/sql/README.md`

### Prompt 2 대상 파일

러닝 프로필·기록 Backend:

- `ssafit/src/main/java/com/ssafy/ssafit/runningprofile/domain/RunningProfile.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningprofile/dto/RunningProfileApiDtos.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningprofile/dto/RunningProfileViewForm.java`
- `ssafit/src/main/resources/mappers/runningprofile/RunningProfileMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/runningprofile/service/RunningProfileService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningprofile/controller/api/RunningProfileApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningrecord/domain/RunningRecord.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningrecord/dto/RunningRecordApiDtos.java`
- `ssafit/src/main/resources/mappers/runningrecord/RunningRecordMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/runningrecord/service/RunningRecordService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningrecord/controller/api/RunningRecordApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/exception/ErrorCode.java`

### Prompt 3 대상 파일

러닝 입력·조회 UI:

- `ssafit/src/main/webapp/WEB-INF/views/member/running-profile.jsp`
- `ssafit/src/main/java/com/ssafy/ssafit/member/controller/view/RunningProfileViewController.java`
- `frontend/src/api/runningProfileApi.js`
- `frontend/src/api/runningRecordApi.js`
- `frontend/src/views/RunningRecordFormView.vue`
- `frontend/src/views/RunningRecordDetailView.vue`
- `frontend/src/views/RunningRecordListView.vue`
- `frontend/src/utils/runningRecord.js`
- `frontend/src/router/index.js`

### Prompt 4 대상 파일

음악 템포·운동 강도·추천 메트릭:

- `ssafit/src/main/java/com/ssafy/ssafit/music/domain/Music.java`
- `ssafit/src/main/java/com/ssafy/ssafit/music/domain/MusicSearchCondition.java`
- `ssafit/src/main/java/com/ssafy/ssafit/music/dto/MusicApiDtos.java`
- `ssafit/src/main/resources/mappers/music/MusicMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/music/service/MusicService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/recommendation/domain/RecommendationLog.java`
- `ssafit/src/main/java/com/ssafy/ssafit/recommendation/domain/RecommendationResult.java`
- `ssafit/src/main/java/com/ssafy/ssafit/recommendation/dto/RecommendationApiDtos.java`
- `ssafit/src/main/resources/mappers/recommendation/RecommendationLogMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/recommendation/service/RecommendationService.java`
- `ssafit/src/main/resources/mappers/musicreview/MusicReviewMapper.xml`
- `frontend/src/components/MusicSearchBar.vue`
- `frontend/src/utils/musicQuery.js`
- `frontend/src/views/MusicDetailView.vue`
- `frontend/src/views/RecommendationView.vue`
- `frontend/src/components/RecommendationResult.vue`

### Prompt 5 대상 파일

Spotify 곡 검색·음악 메타데이터 보강·내부 저장:

- `ssafit/src/main/java/com/ssafy/ssafit/spotify/client/SpotifyApiClient.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/client/SpotifyRestApiClient.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/domain/SpotifyTrack.java`
- `ssafit/src/main/java/com/ssafy/ssafit/music/domain/SpotifyTrackMetadata.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/SpotifyService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/music/service/MusicService.java`
- `ssafit/src/main/resources/mappers/music/MusicMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/dto/SpotifyApiDtos.java`
- `ssafit/src/main/java/com/ssafy/ssafit/music/dto/MusicApiDtos.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/controller/api/SpotifyApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/music/controller/api/MusicApiController.java`
- `frontend/src/api/spotifyApi.js`
- `frontend/src/views/SpotifySearchView.vue`

### Prompt 6 대상 파일

Spotify OAuth·토큰·오류 처리:

- `ssafit/src/main/resources/application.yml`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/config/SpotifyProperties.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/config/SpotifyClientConfig.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/SpotifyOAuthStateManager.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/SpotifyOAuthService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/client/SpotifyRestApiClient.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/domain/SpotifyConnection.java`
- `ssafit/src/main/resources/mappers/spotify/SpotifyConnectionMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/dto/SpotifyApiDtos.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/controller/api/SpotifyApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/exception/ErrorCode.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/web/FrontendReturnUrl.java`
- `frontend/src/api/spotifyApi.js`
- `frontend/src/views/SpotifyOAuthCompleteView.vue`
- `frontend/src/utils/oauthReturn.js`
- `frontend/src/utils/spotifyError.js`

### Prompt 7 대상 파일

내부 플레이리스트와 Spotify export 통합:

- `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/PlaylistExportService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/controller/api/PlaylistExportApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/playlist/service/PlaylistService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/playlist/service/PlaylistItemService.java`
- `ssafit/src/main/resources/mappers/playlist/PlaylistMapper.xml`
- `ssafit/src/main/resources/mappers/playlist/PlaylistItemMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/playlist/dto/PlaylistApiDtos.java`
- `ssafit/src/main/java/com/ssafy/ssafit/recommendation/service/RecommendationService.java`
- `frontend/src/components/SpotifyExportPanel.vue`
- `frontend/src/views/PlaylistDetailView.vue`
- `frontend/src/api/playlistApi.js`
- `frontend/src/api/spotifyApi.js`

### Prompt 8 검증 대상 파일

- `ssafit/src/test/java/com/ssafy/ssafit/runningprofile/service/RunningProfileServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/runningrecord/service/RunningRecordServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/runningrecord/controller/api/RunningRecordApiControllerTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/music/service/MusicServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/recommendation/service/RecommendationServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/spotify/service/SpotifyServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/spotify/service/SpotifyOAuthServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/spotify/service/PlaylistExportServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/spotify/controller/api/SpotifyApiControllerTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/mapper/MapperIntegrationTest.java`
- `frontend/e2e/running-features.spec.js`
- 신규 추천/Spotify E2E 시나리오
- `docs/spotify-manual-integration-checklist.md`
- `docs/integration/spotify-ui-flow.md`
- `docs/running-features-final-verification-report.md`
- `docs/verification/running-features-e2e-guide.md`

## 7. 구현 전 위험 요소 및 주의사항

1. `targetBpmMin/Max` 의미를 먼저 고정해야 한다. 기존처럼 음악 템포 범위로 유지할지, 명시적 `targetMusicBpm*`로 이름을 바꿀지 결정 없이 케이던스와 심박수를 추가하면 혼용이 발생한다.
2. 기존 DB를 보존하는 별도 migration 전략이 필요하다. 현재 `schema.sql`은 전체 생성 스크립트이며 자동 migration 도구가 없다.
3. 케이던스와 평균 심박수 신규 컬럼은 기존 데이터 때문에 우선 NULL 허용이 안전하다. 임의 backfill은 측정 데이터의 신뢰성을 훼손한다.
4. Domain·DTO·Mapper의 `resultMap`, 공통 SELECT 컬럼, INSERT, UPDATE 중 하나라도 누락되면 조회는 되지만 저장이 안 되거나 update 시 값이 사라지는 반쪽 구현이 된다.
5. 추천은 현재 개인 취향 점수가 아니라 전역 리뷰 평균이다. `preferenceScore`라는 이름과 실제 데이터 원천을 분리해야 한다.
6. Spotify 검색 저장 곡은 BPM/장르가 NULL이므로 현재 자동 추천 대상에서 빠진다. 메타데이터 보강 정책을 Spotify 실제 API 가능 범위와 함께 먼저 확정해야 한다.
7. 음악 tempo BPM, 케이던스 SPM, 심박수 BPM은 단위 숫자 범위가 겹친다. DB·Java·JSON·화면 라벨·오류 코드를 각각 명시적 이름으로 유지해야 한다.
8. 운동 강도와 음악 energy는 현재 데이터 모델에 없다. 심박수만으로 energy를 직접 생성·저장하지 말고, 원본 생체 데이터와 원본 음악 특성을 별도로 저장한 뒤 추천 시 계산해야 한다.
9. Spotify 외부 API의 401·403·429·5xx·네트워크 오류를 구분하지 않는다. 특히 429의 `Retry-After`, 401 refresh 후 단일 재시도, timeout 정책이 필요하다.
10. Spotify token은 DB 평문 저장이다. 발표용 계정 사용 전 최소한 DB 접근 통제와 로그 마스킹을 확인하고, 장기적으로 암호화를 적용해야 한다.
11. OAuth state는 안전하게 구현되어 있지만 HttpSession 쿠키가 authorize와 callback 사이에 유지되어야 한다. 프론트/백엔드 host를 `localhost`와 `127.0.0.1`로 섞으면 세션 문제가 생길 수 있다.
12. Backend callback URI와 Vue 완료 URI가 localhost에 고정되어 있다. 발표 환경에서는 Spotify Dashboard, `SPOTIFY_REDIRECT_URI`, Backend frontend origin 설정을 동시에 맞춰야 한다.
13. Development Mode의 Spotify 앱은 허용 사용자 제한이 있을 수 있다. 발표 계정을 Dashboard에 등록하지 않으면 구현이 정상이어도 OAuth가 실패할 수 있다.
14. export는 외부 Spotify 작업과 내부 DB 상태를 하나의 트랜잭션으로 원자화할 수 없다. 부분 성공, 중복 생성, 재시도 정책을 현재 상태 머신과 함께 유지·보강해야 한다.
15. `/items` 경로는 현재 구현되어 있으나 실제 Spotify API 버전과 앱 권한에서 동작하는지 수동 검증해야 한다. 구형 `/tracks`로 임의 회귀시키지 말고 공식 사양 확인 후 결정해야 한다.
16. 실제 Secret을 `.env.example`, `application.yml`, README, 테스트 코드에 넣지 않는다. Git 추적 전 `git diff --cached`와 전체 문자열 검색이 필요하다.
17. `application.yml`의 MySQL 비밀번호도 현재 평문 추적 상태이므로 Spotify 작업과 함께 환경 변수 분리를 검토한다.
18. 마지막 Prompt 8 전까지 실제 OAuth·export 성공으로 간주하지 않는다. 현재 자동 테스트는 Mock 중심이며 실제 Redirect URI, 계정 허용, scope, rate limit, API 응답 차이를 증명하지 못한다.
