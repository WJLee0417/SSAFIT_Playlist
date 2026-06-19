# SSAFIT PlayList 팀원 A 단계별 작업 계획

## 1. 현재 상태

### 완료된 항목

- Spring Boot 4.0.6 기반 프로젝트 환경 설정
- Java 21 및 Maven Wrapper 설정
- MyBatis와 MySQL 8 연결 설정
- 공통 예외 처리 기본 구조
- Session 로그인 회원 ID 상수
- Health Check Controller
- 도메인별 기본 패키지
- Mapper XML 디렉터리
- SQL 관리 디렉터리
- 개발 환경 README
- 초기 환경 설정 커밋 및 원격 브랜치 푸시

### 아직 구현하지 않은 항목

- 전체 DDL
- Domain 클래스와 Enum
- Mapper Interface/XML
- Service 비즈니스 로직
- 도메인별 ErrorCode
- Mapper/Service 테스트
- 더미 데이터
- Spotify 실제 API 호출 및 OAuth 처리

### 선행 확인 사항

- `ErrorCode.java`와 SQL 파일 주석 일부의 깨진 한글을 UTF-8로 복구해야 한다.
- 테이블명, 컬럼명, Enum 값, 예외 응답 형식을 본 구현 전에 고정해야 한다.
- Controller와 Request/Response DTO는 팀원 B 담당 영역이므로 현재 브랜치에서 임의로 구현하지 않는다.

---

## 2. 현재 브랜치 작업 순서

현재 브랜치:

```text
feature/a-db-service
```

이 브랜치에서는 DB, Mapper, Service, Session 인증/인가, 권한 검증, 자동 큐레이션 내부 로직을 구현한다.

Spotify 실제 API 호출, OAuth, Export 내부 로직은 이후 `feature/a-spotify-service` 브랜치에서 구현한다.

---

## 3. 0단계: 구현 기준 고정

본격적인 DDL과 Java 구현 전에 다음 기준을 확정한다.

### 인코딩 정리

- 깨진 한글 문자열을 UTF-8로 복구
- Java, XML, SQL, YAML 파일을 UTF-8로 통일
- STS Workspace와 프로젝트 인코딩을 UTF-8로 설정

### DB 및 도메인 명칭 확정

- 테이블명과 컬럼명
- Java Domain 필드명
- MyBatis camel case 매핑 기준
- DTO 필드명에 영향을 주는 공통 명칭

### 공통 Enum 값 확정

- 회원 상태
- 음악 `source_type`
- 플레이리스트 `playlist_type`
- 플레이리스트 `source_type`
- 플레이리스트 공개 범위
- Spotify export 상태
- 러닝 강도

### 공통 정책 확정

- Session 로그인 키 `LOGIN_MEMBER_ID`
- 비밀번호 저장 및 검증 방식
- soft delete 대상과 삭제 정책
- 공통 성공/오류 응답 형식
- 페이징 응답 형식
- ErrorCode 명칭

팀원 B와 합의가 필요한 DTO 필드명, API URI, 공통 응답 형식은 구현 전에 공유한다.

---

## 4. 1단계: MVP DDL 구현

가장 먼저 `src/main/resources/sql/schema.sql`에 MVP 테이블을 구현한다.

### MVP 필수 테이블

의존 관계를 고려한 권장 작성 순서:

1. `members`
2. `running_profiles`
3. `music`
4. `music_reviews`
5. `playlists`
6. `playlist_items`
7. `playlist_saves`
8. `recommendation_logs`
9. `spotify_connections`

Spotify 실제 API 연동은 나중에 구현하지만, 관련 DB 구조는 MVP DDL에 포함한다.

### 핵심 제약조건

#### members

- `login_id` UNIQUE
- `email` UNIQUE
- 회원 상태 값 검증

#### running_profiles

- `member_id` UNIQUE
- 회원당 프로필 1개
- `member_id` FK

#### music

- BPM 60~220
- `source_type`: `ADMIN`, `SEED`, `EXTERNAL`, `SPOTIFY`
- `spotify_track_id` UNIQUE 권장
- `spotify_uri`
- `spotify_external_url`

#### music_reviews

- `member_id + music_id` UNIQUE
- `preference_score` 1~5
- `running_fit_score` 1~5

#### playlists

- USER 플레이리스트이면 `owner_id` 필수
- OFFICIAL 플레이리스트이면 시스템 생성 정책 적용
- Spotify 내보내기 관련 컬럼 포함
  - `spotify_playlist_id`
  - `spotify_playlist_url`
  - `spotify_export_status`
  - `spotify_exported_at`

#### playlist_items

- `playlist_id + music_id` UNIQUE
- `playlist_id + order_no` UNIQUE

#### playlist_saves

- `member_id + playlist_id` UNIQUE
- 공개 또는 공식 플레이리스트만 저장 가능

#### recommendation_logs

- 추천 요청 조건과 결과 요약 저장
- 로그 생성만으로 `playlists`, `playlist_items`를 생성하지 않음

#### spotify_connections

- `member_id` UNIQUE
- `spotify_user_id` 저장
- Access Token, Refresh Token, 만료 시각 저장
- 토큰 암호화 저장 권장

### 인덱스 구성

- 로그인 ID와 이메일 조회
- 음악 키워드, 장르, BPM 조회
- 플레이리스트 소유자 조회
- 평가 평균 계산
- 추천 조건 검색
- Spotify Track ID 검색

---

## 5. 2단계: Domain, Enum, ErrorCode 구현

DDL과 일치하도록 다음 Domain을 작성한다.

- `Member`
- `RunningProfile`
- `Music`
- `MusicReview`
- `Playlist`
- `PlaylistItem`
- `PlaylistSave`
- `RecommendationLog`
- `SpotifyConnection`

### ErrorCode 확장 대상

#### 회원 및 인증

- `DUPLICATE_LOGIN_ID`
- `DUPLICATE_EMAIL`
- `INVALID_LOGIN`
- `MEMBER_NOT_FOUND`
- `AUTH_REQUIRED`

#### 러닝 프로필

- `PROFILE_NOT_FOUND`
- `INVALID_PROFILE_VALUE`

#### 음악

- `MUSIC_NOT_FOUND`
- `INVALID_BPM_VALUE`
- `INVALID_BPM_RANGE`
- `SPOTIFY_TRACK_NOT_FOUND`

#### 음악 평가

- `DUPLICATE_MUSIC_REVIEW`
- `INVALID_SCORE`
- `ACCESS_DENIED`

#### 플레이리스트

- `PLAYLIST_NOT_FOUND`
- `INVALID_PLAYLIST_TITLE`
- `DUPLICATE_PLAYLIST_ITEM`
- `DUPLICATE_PLAYLIST_ORDER`
- `PRIVATE_PLAYLIST`
- `DUPLICATE_SAVE`

#### 자동 큐레이션

- `INVALID_INTENSITY`
- `RECOMMENDATION_DATA_NOT_ENOUGH`

---

## 6. 3단계: 회원, 인증, 러닝 프로필

역할 명세서 구현 우선순위 1~2에 해당한다.

### Mapper

#### MemberMapper

- `insert`
- `existsByLoginId`
- `existsByEmail`
- `selectByLoginId`
- `selectById`

#### RunningProfileMapper

- `selectByMemberId`
- `insert`
- `update`

### Service

#### MemberService

- 회원가입
- 로그인 ID 중복 검증
- 이메일 중복 검증
- 내 정보 조회

#### AuthService

- 로그인 ID 조회
- 비밀번호 검증
- 회원 상태 검증
- 로그인 회원 조회

#### RunningProfileService

- 회원 프로필 조회
- 프로필이 없으면 insert
- 프로필이 있으면 update
- 본인 프로필 접근 검증

### 검증 항목

- 로그인 ID 중복
- 이메일 중복
- 잘못된 로그인 정보
- 비로그인 접근
- 타인 데이터 접근
- 회원당 러닝 프로필 1개

Controller와 Request/Response DTO는 팀원 B 범위이므로 이 단계에서 구현하지 않는다.

---

## 7. 4단계: 음악 및 Spotify 선택 곡 DB 저장

역할 명세서 구현 우선순위 3~4에 해당한다.

### MusicMapper

- `selectMusicList`
- `selectMusicDetail`
- `countMusicList`
- `selectRecommendedMusic`
- `selectBySpotifyTrackId`
- `insertFromSpotify`
- `updateSpotifyMetadata`

### MusicService

- 음악 목록 조회
- 키워드, 장르, BPM, 강도 검색
- 정렬과 페이징
- 음악 상세 조회
- BPM과 검색 범위 검증
- Spotify 검색 결과에서 선택된 곡 저장
- `spotify_track_id` 중복 검증
- 기존 곡이 존재하면 기존 `music` 재사용
- 일반 회원의 임의 음악 생성/수정/삭제 차단

Spotify Web API를 실제로 호출하는 로직은 `feature/a-spotify-service`에서 구현한다.

---

## 8. 5단계: 음악 평가

역할 명세서 구현 우선순위 5에 해당한다.

### MusicReviewMapper

- `insert`
- `update`
- `delete`
- `existsByMemberAndMusic`
- `selectByMusicId`
- `selectAverageScores`

### MusicReviewService

- 평가 작성
- 평가 수정
- 평가 삭제
- 회원과 음악 기준 중복 평가 검증
- 작성자 본인 검증
- `preferenceScore` 1~5 검증
- `runningFitScore` 1~5 검증
- 음악별 평균 점수 조회

---

## 9. 6단계: 플레이리스트와 수록곡

역할 명세서 구현 우선순위 6~7에 해당한다.

### PlaylistMapper

- `insert`
- `selectList`
- `selectDetail`
- `update`
- `softDelete`
- `selectOwnerId`
- `insertCurationCopy`
- `updateSpotifyExportResult`
- `updateSpotifyExportStatus`
- `selectExportTargetPlaylist`

### PlaylistService

- USER + MANUAL 플레이리스트 생성
- 플레이리스트 목록과 상세 조회
- 소유자만 수정/삭제
- 공개/비공개 접근 정책
- 공식/사용자 플레이리스트 구분
- Spotify 내보내기 결과 저장 지원

### PlaylistItemMapper

- `insert`
- `delete`
- `updateOrder`
- `existsByPlaylistAndMusic`
- `selectByPlaylistId`
- `bulkInsertFromRecommendation`
- `selectSpotifyUrisByPlaylistId`

### PlaylistItemService

- 수록곡 추가
- 수록곡 삭제
- 순서 변경
- 플레이리스트 소유자 검증
- 음악 존재 검증
- 같은 음악 중복 방지
- 같은 순서 중복 방지
- Spotify 기반 곡의 `spotify_uri` 존재 검증
- Spotify 내보내기 대상 URI 목록 구성

---

## 10. 7단계: 플레이리스트 참조 저장

역할 명세서 구현 우선순위 10에 해당한다.

### PlaylistSaveMapper

- `insert`
- `delete`
- `existsByMemberAndPlaylist`
- `selectSavedList`

### PlaylistSaveService

- 공개 또는 공식 플레이리스트 저장
- 중복 저장 검증
- 저장 취소
- 본인 저장 관계 검증
- 내 저장 목록 조회

### 중요 정책

`playlist_saves`는 기존 공개/공식 플레이리스트를 내 저장 목록에 추가하는 참조 관계다.

자동 큐레이션 결과 저장에는 `playlist_saves`를 사용하지 않는다.

자동 큐레이션 저장본은 USER + CURATION_COPY 개인 플레이리스트로 생성한다.

---

## 11. 8단계: 자동 큐레이션

역할 명세서 구현 우선순위 11에 해당한다.

### RecommendationLogMapper

- `insert`
- `selectById`
- `selectByMemberId`
- `updateSavedPlaylistId`

### RecommendationService - 조회

- 러닝 프로필 존재 여부 검증
- 러닝 강도와 BPM 조건 계산
- `music`, `music_reviews`, `running_profiles` 기반 추천
- 임시 추천 음악 목록 반환
- 추천 요청 조건, 결과 요약, 추천 사유 로그 저장
- 조회만으로 `playlists`, `playlist_items`를 생성하지 않음

### RecommendationService - 저장

- 저장 버튼 클릭 시 USER + CURATION_COPY 플레이리스트 생성
- 추천 음악을 `playlist_items`에 bulk insert
- 추천 로그의 저장 플레이리스트 ID 갱신
- 다음 작업을 하나의 트랜잭션으로 처리
  1. 플레이리스트 생성
  2. 수록곡 복사
  3. 추천 로그 연결

---

## 12. 9단계: 테스트와 더미 데이터

테스트는 각 기능 구현과 함께 작성한다.

### Mapper 테스트

- SQL 결과
- 검색 조건
- 정렬
- 페이징
- 조인 결과
- UNIQUE 및 FK 제약조건

### Service 테스트

- 중복 검증
- 범위 검증
- 권한 검증
- 예외 발생
- 트랜잭션 롤백

### 인증/인가 테스트

- 비로그인 접근
- 타인 데이터 접근
- 평가 작성자 검증
- 플레이리스트 소유자 검증
- 본인 프로필 접근 검증

### 추천 테스트

- 프로필 없음
- 추천 데이터 부족
- 정상 추천
- 추천 로그 저장
- 조회 후 플레이리스트 미생성
- 저장 후 플레이리스트와 수록곡 복사 생성

### SQL 데이터 파일

#### data.sql

- 서비스 실행에 필요한 기준 데이터

#### dummy.sql

- 일반 회원 2명 이상
- 관리자 후보 회원 1명
- 강도별 러닝 프로필
- BPM 120~200 범위 음악
- 다양한 장르와 `source_type`
- 음악 평가 평균 계산용 데이터
- 공식 플레이리스트
- 사용자 플레이리스트
- 수록곡
- 공개 플레이리스트 저장 관계
- Spotify Track ID/URI가 있는 테스트 곡

---

## 13. 이후 별도 브랜치 작업

다음 작업은 `feature/a-spotify-service` 브랜치에서 진행한다.

### SpotifyService

- Spotify 곡 검색 API 호출
- 검색 결과 메타데이터 변환
- 선택 곡 저장을 MusicService와 연결

### SpotifyOAuthService

- OAuth 인증 URL 생성
- callback 처리
- `spotify_connections` 저장/조회/갱신
- Access Token 만료 검사
- Refresh Token 기반 갱신
- 인증 필요 및 권한 거부 예외 처리

### PlaylistExportService

- 플레이리스트 소유자 검증
- Spotify 연결 여부 검증
- Spotify Playlist 생성
- `spotify_uri` 목록 추가
- Spotify Playlist ID와 URL 저장
- export 상태와 시각 저장
- 중복 내보내기 정책 처리

Spotify Client ID와 Secret은 A/B 기능 구현과 통합 준비가 끝난 후 환경 변수로 등록한다.

---

## 14. MVP 이후 추가 기능

추가 기능은 MVP 완료 후 다음 순서로 구현한다.

### 1. 러닝 기록

- `running_records` DDL
- `RunningRecordMapper`
- `RunningRecordService`
- 본인 기록만 CRUD 가능

### 2. 러닝 모임

- `running_sessions` DDL
- `RunningSessionMapper`
- `RunningSessionService`
- 생성자만 수정/취소 가능

### 3. 러닝 모임 참여

- `running_session_participants` DDL
- `RunningSessionParticipantMapper`
- `RunningSessionParticipantService`
- 중복 참여 방지
- 정원 초과 방지
- 닫힌 모임 참여 방지
- 참여 취소

---

## 15. 현재 브랜치에서 구현하지 않을 항목

- JSP
- Vue3 화면
- Vue Router
- Vue API Module
- 전체 Controller
- Request/Response DTO
- Swagger/API Tester 문서
- Spring Security
- JWT
- 실제 AI API 추천
- 관리자 기능
- 팀원 B 담당 파일의 임의 수정
- `main`, `develop` 직접 merge

---

## 16. 추천 커밋 단위

다음과 같이 기능 단위로 작게 커밋한다.

```text
fix: restore UTF-8 source encoding
feat: add MVP database schema
feat: add domain models and error codes
feat: implement member auth and profile services
feat: implement music persistence and query services
feat: implement music review service
feat: implement playlist and item services
feat: implement playlist save service
feat: implement recommendation service
test: add mapper and service tests
feat: add MVP dummy data
```

각 커밋 전 다음 명령으로 검증한다.

```bash
mvnw.cmd clean test
```

현재 브랜치에서 가장 먼저 착수할 실제 작업은 다음과 같다.

```text
0단계: UTF-8 및 구현 정책 정리
→ 1단계: MVP DDL 작성
→ 2단계: Domain/Enum/ErrorCode 작성
→ 3단계 이후: 우선순위에 따른 Mapper/Service 구현
```
