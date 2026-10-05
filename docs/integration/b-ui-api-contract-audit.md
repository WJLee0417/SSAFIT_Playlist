# B UI/API 계약 감사 보고서

## 1. 조사 범위 및 실행 환경

이 보고서는 다음 두 목표 명세와 현재 `feature/b-ui-api` 브랜치의 실제 구현을 비교한 결과다.

- `SSAFIT 플레이리스트 전체 역할 명세서.pdf` 36쪽
- `SSAFIT 플레이리스트 팀원 B 역할 명세서.pdf` 13쪽

조사 범위:

- Domain/Entity 역할의 Java 클래스와 Enum
- `schema.sql`의 테이블, 컬럼, 제약조건
- MyBatis Mapper Interface/XML
- Service 공개 메서드와 트랜잭션 경계
- `ErrorCode`, `CustomException`, `GlobalExceptionHandler`
- Session 로그인 회원 저장 방식
- Spotify 검색, OAuth, Export 구현
- 자동 큐레이션, `recommendation_logs`, `CURATION_COPY`, `playlist_saves`
- Controller, DTO, JSP, Vue, CORS, Cookie 설정
- 테스트 코드와 Maven 빌드 기준선

실행 환경:

| 항목 | 실제 값 |
| --- | --- |
| OS | Windows 11 |
| 작업 경로 | `C:\Users\dnjsw\OneDrive\바탕 화면\SSAFIT_Playlist` |
| Java | Oracle JDK 21.0.10 |
| Spring Boot | 4.0.6 |
| Maven | Maven Wrapper 3.9.16 |
| Backend | `ssafit/` |
| Frontend | 없음 |
| Node 패키지 관리자 | 해당 파일 없음 |
| DB | MySQL 8 전제 |

두 PDF는 텍스트 추출뿐 아니라 고정 API와 공통 세션/응답 규칙이 있는 페이지를 PNG로 렌더링해 원문 배치를 교차 확인했다.

## 2. 현재 브랜치 및 빌드 기준선

감사 시작 시점:

| 항목 | 결과 |
| --- | --- |
| 현재 브랜치 | `feature/b-ui-api` |
| 원격 추적 | `origin/feature/b-ui-api` |
| 시작 시 변경 파일 | 없음 |
| HEAD | `ef5b2a9 docs: add Java 25 and STS 5.1.1 environment setup guide` |
| 기준 통합 커밋 | `8193a5b Merge branch 'feature/a-running-features-service' into develop` |

최근 커밋:

```text
ef5b2a9 docs: add Java 25 and STS 5.1.1 environment setup guide
8193a5b Merge branch 'feature/a-running-features-service' into develop
1fa7987 fix: validate affected row counts for service mutations
8b9b951 fix: enforce running session capacity under concurrent participation
51c0bdb test: add MySQL integration safety checks and report concurrency failure
47ce02b docs: add full service integration audit report
dae0922 docs: add running features service integration contract
a98f144 test: add running feature mapper and service tests feat: add running feature dummy data
```

프로젝트 구조:

```text
SSAFIT_Playlist/
├─ docs/
└─ ssafit/
   ├─ pom.xml
   ├─ mvnw
   ├─ mvnw.cmd
   └─ src/
      ├─ main/java
      ├─ main/resources
      └─ test
```

- Backend는 `ssafit/pom.xml`을 사용하는 단일 Maven 모듈이다.
- Gradle 파일은 없다.
- 이 감사 시점에는 Vue/Vite 프로젝트와 `package.json`, Frontend lock 파일이 없었다. 현재 `main`은 Frontend 패키지 매니저를 npm으로 통일해 `package.json`과 `package-lock.json`을 사용한다.
- JSP 파일과 `src/main/webapp`이 없고, JSP/JSTL 실행 의존성도 `pom.xml`에 없다.

검증 결과:

| 명령 | 결과 |
| --- | --- |
| `java -version` | 성공, Java 21.0.10 |
| `.\mvnw.cmd -version` | 성공, Maven 3.9.16 |
| `.\mvnw.cmd test` | 성공, 205건 중 실패 0, 오류 0, 건너뜀 13. 이번 셸에는 전용 DB 환경변수가 없어 실제 MySQL 통합 테스트만 조건부 생략 |
| `.\mvnw.cmd package -DskipTests` | 성공, 실행 가능 JAR 생성 |
| Frontend build | Frontend 프로젝트가 없어 실행 대상 없음 |

첫 Maven 실행은 제한된 네트워크 환경에서 Maven Central 접근이 차단되어 실패했다. 네트워크 사용이 허용된 동일 명령으로 재실행했을 때 정상 성공했으므로 소스 또는 기존 빌드 결함으로 분류하지 않는다.

건너뛴 13건은 `MapperIntegrationTest`이며 `SSAFIT_TEST_DB_URL`이 전용 MySQL 테스트 DB를 가리킬 때만 실행된다. 이 13건은 미검증 항목이 아니다. 기존 `docs/mysql-integration-test-report.md`와 `docs/p0-running-session-concurrency-fix-report.md`에 따라 2026-06-21 실제 MySQL 8.0.32의 `ssafit_test` DB, InnoDB, `REPEATABLE-READ` 환경에서 모두 실행 및 통과했다. 당시 전체 결과는 198건, 실패 0, 오류 0, 건너뜀 0, `BUILD SUCCESS`였고 `MapperIntegrationTest` 13건도 전부 통과했다.

따라서 기준선은 다음처럼 해석한다.

- 현재 감사 실행: 205건 중 192건 실행·통과, 이미 별도 DB 검증이 끝난 13건은 환경 조건으로 생략
- 선행 실제 MySQL 검증: Mapper 통합 테스트 13건 전부 실행·통과
- 종합 판정: Java/Service/Spring Context와 실제 MySQL Mapper 기준선 모두 검증 완료

## 3. 팀원 A 구현 현황 요약

| 영역 | 상태 | 감사 결과 |
| --- | --- | --- |
| MVP 및 추가 기능 DDL | 구현 | 12개 테이블과 주요 제약조건 존재 |
| Spotify 음악 컬럼 | 구현 | `spotify_track_id`, `spotify_uri`, `spotify_external_url` 존재 |
| Spotify 연결 | 구현 | `spotify_connections`와 Mapper/Service 존재 |
| Spotify Export 컬럼 | 구현 | ID, URL, 상태, 오류 코드, 완료 시각 존재 |
| Mapper Interface/XML | 구현 | 조사 대상 Interface와 XML ID/namespace 존재 |
| 회원/인증/프로필 Service | 구현 | Session 로그인 포함 |
| 음악/평가 Service | 구현 | 검색, 상세, 평가 CRUD, 평균 조회 포함 |
| 플레이리스트 Service | 구현 | CRUD, 수록곡, 참조 저장 포함 |
| 자동 큐레이션 Service | 구현 | 조회와 저장 분리, 로그 연결 포함 |
| Spotify Service | 구현 | 검색, DB 저장, OAuth, 토큰 갱신, Export 포함 |
| 추가 러닝 기능 Service | 구현 | 기록, 모임, 참여 기능 포함 |
| 공통 예외 처리 | 부분 구현 | `CustomException`과 validation만 처리 |
| REST/JSP Controller | 미구현 | `/health`만 존재 |
| Request/Response DTO | 미구현 | `ErrorResponse`만 존재 |
| 공통 성공/페이징 응답 | 미구현 | `ApiResponse`, `PageResponse` 없음 |
| CORS/Cookie 명시 설정 | 미구현 | Session timeout만 설정 |
| JSP | 미구현 | 파일과 런타임 의존성 없음 |
| Vue3/Vite | 미구현 | Frontend 모듈 없음 |
| Swagger/OpenAPI | 미구현 | 의존성과 설정 없음 |

실제 Session 계약:

```text
Session key: LOGIN_MEMBER_ID
Session value type: java.lang.Long
로그인 저장: AuthService.login(HttpSession, loginId, rawPassword)
로그아웃: HttpSession.invalidate()
조회: AuthService.getLoginMemberId(HttpSession)
```

## 4. 전체 역할 명세 대비 일치 항목

1. `music`에 Spotify Track ID, URI, 외부 URL 컬럼과 Domain/Mapper 매핑이 모두 존재한다.
2. `spotify_track_id` UNIQUE 제약과 Service 선조회, 동시 insert 충돌 재조회가 함께 구현되어 있다.
3. `spotify_connections`에 회원별 UNIQUE 연결, 사용자 ID, Access Token, Refresh Token, 만료 시각이 구현되어 있다.
4. `playlists`에 Spotify Playlist ID, URL, Export 상태, 완료 시각이 구현되어 있다.
5. 실제 구현에는 명세보다 추가된 `spotify_export_error_code`가 있어 실패 원인을 보존한다.
6. Spotify 검색 → 선택 메타데이터 저장은 `SpotifyService`와 `MusicService`로 연결되어 있다.
7. 저장된 `musicId`를 이용한 수록곡 추가는 `PlaylistItemService.add`로 가능하다.
8. OAuth URL 생성, state Session 저장/검증, callback, 연결 저장, 토큰 갱신이 구현되어 있다.
9. Export는 소유권 확인 → 연결/토큰 확인 → 전체 URI 검증 → Spotify Playlist 생성 → 100곡 단위 추가 → URL/상태 저장 순서로 구현되어 있다.
10. 음악 목록/상세/추천 후보, 평가 CRUD/평균, 플레이리스트 CRUD, 수록곡 관리, 참조 저장/취소/목록 Service가 존재한다.
11. 자동 큐레이션 조회는 `recommendation_logs`만 생성하고 플레이리스트와 수록곡을 생성하지 않는다.
12. 자동 큐레이션 저장은 `USER + CURATION_COPY` 플레이리스트와 수록곡을 생성하고 로그의 `saved_playlist_id`를 연결한다.
13. 자동 큐레이션 저장 경로는 `PlaylistSaveMapper` 또는 `PlaylistSaveService`를 사용하지 않는다.
14. Session 기반 인증을 유지하며 Spring Security/JWT 의존성이 없다.
15. Spotify 관련 목표 ErrorCode 8종이 모두 존재하고 구현상 필요한 추가 코드도 존재한다.

## 5. 명세 대비 차이점

### 5.1 BLOCKER

BLOCKER는 팀원 B가 명세를 그대로 구현하면 거짓 응답, 잘못된 UI 의미 또는 계약 충돌이 발생하는 항목이다.

| ID | 차이 | 영향 | 필요한 결정 |
| --- | --- | --- | --- |
| B-01 | 공통 오류 응답 실제 형식은 `{ "code", "message" }`이고 명세는 `{ "success", "message", "errorCode" }`다. | Vue 공통 오류 처리와 Swagger 예시를 한 형식으로 확정할 수 없다. | 기존 형식을 유지할지 명세 형식으로 변경할지 공동 확정 |
| B-02 | 자동 큐레이션 실제 조회 메서드는 강도, 장르, 개수만 받고 요청 BPM 범위를 받지 않는다. | 목표 BPM 직접 입력 UI와 API를 명세대로 연결할 수 없다. 실제로는 회원 프로필 BPM을 우선 사용한다. | UI에서 BPM 입력을 제거할지 Service 계약을 확장할지 팀원 A 확인 |
| B-03 | Spotify 연결이 없을 때 `getConnection`은 연결 안 됨 객체가 아니라 `SPOTIFY_AUTH_REQUIRED` 예외를 발생시킨다. | `GET /api/spotify/me/connection`의 정상적인 `connected=false` 응답 의미가 확정되지 않는다. | 미연결을 200 상태 DTO로 변환할지 401 오류로 유지할지 확정 |
| B-04 | Export 실제 반환값은 `Playlist`이며 성공/실패 곡 수를 제공하지 않는다. 실패 중 일부 곡이 외부에 추가됐는지도 계산할 수 없다. | 명세의 성공/실패 곡 수를 신뢰 가능한 값으로 반환할 수 없다. | MVP 응답에서 곡 수를 제외/nullable 처리할지 별도 추적을 추가할지 확정 |

고정 API 6개는 모두 Controller가 없어 현재 HTTP 레벨에서 존재하지 않는다. 이는 팀원 B의 구현 범위이지만, B-01~B-04가 확정되기 전에는 응답 계약을 최종 고정할 수 없다.

### 5.2 ADAPTABLE

ADAPTABLE은 팀원 A 소스를 수정하지 않고 Controller/DTO 조립으로 흡수 가능한 차이다.

| ID | 차이 | 팀원 B 대응 |
| --- | --- | --- |
| A-01 | 음악 목록과 전체 개수가 별도 Service 메서드다. | 동일 검색 조건으로 목록과 count를 호출해 `PageResponse` 조립 |
| A-02 | 음악 상세, 평가 목록, 평균 점수가 각각 별도 메서드다. | Controller에서 3개 결과를 `MusicDetailResponse`로 조립 |
| A-03 | 플레이리스트 상세와 수록곡 목록이 별도이며 수록곡은 `musicId`만 가진다. | 수록곡별 음악 상세를 조립하되 N+1 조회 비용을 기록 |
| A-04 | Spotify 검색 최대 개수는 10으로 고정되어 있다. | UI와 Request DTO의 최대값을 10으로 제한 |
| A-05 | Spotify 검색 결과의 BPM과 장르는 항상 `null`이다. | 저장/추가/Export는 가능하나 큐레이션 후보가 되지 않을 수 있음을 UI/테스트에 반영 |
| A-06 | Spotify 연결 Domain에는 토큰이 포함된다. | Response DTO에는 `connected`, `spotifyUserId`, `tokenExpiresAt`만 노출 |
| A-07 | Export 성공 응답은 최신 `Playlist`다. | ID, URL, 상태, 완료 시각만 안전하게 변환 |
| A-08 | OAuth URL Service는 로그인 여부를 직접 확인하지 않는다. | Controller에서 먼저 Session 회원을 확정한 뒤 호출 |
| A-09 | 대부분의 MVP Service는 `loginMemberId`를 받고 추가 러닝 Service는 `HttpSession`을 직접 받는다. | Controller별 호출 형태를 구분하고 요청 DTO의 회원 ID는 무시 |
| A-10 | Spotify 연결 만료 컬럼명이 명세의 일반적 `expires_at` 표현과 달리 `token_expires_at`이다. | DTO는 `tokenExpiresAt`으로 고정 |
| A-11 | 플레이리스트 참조 저장 Service는 소유자의 비공개 플레이리스트 저장을 허용한다. | UI에서 이 동작을 노출하지 말고 팀 정책 확정 전 공개/공식 대상만 호출 |
| A-12 | Playlist Export는 모든 수록곡에 Spotify URI가 있어야 한다. | Export 버튼 활성화 전 검증 안내를 표시하고 `SPOTIFY_URI_NOT_FOUND` 처리 |
| A-13 | 예외 처리기는 예상하지 못한 예외의 공통 JSON 형식을 보장하지 않는다. | API 테스트에서 500 응답을 확인하고 공통 처리 추가 여부를 공동 결정 |

### 5.3 MISSING

MISSING은 명세상 팀원 B 또는 공통 영역에서 새로 구현해야 하는 항목이다.

- 고정 API를 포함한 모든 업무 REST Controller
- JSP 회원가입, 로그인, 마이페이지, 러닝 프로필 Controller와 화면
- JSP/JSTL 실행 의존성과 View Resolver
- Vue3/Vite 프로젝트, Router, API Module, 화면/컴포넌트
- Request/Response DTO
- `ApiResponse<T>`, `PageResponse<T>`, 페이지 요청 DTO
- 명세 기준 성공 응답 포맷
- `http://localhost:5173` CORS 허용과 credential 허용 설정
- Cookie 전달을 검증하는 Controller 통합 테스트
- Swagger/OpenAPI 또는 API Tester 문서
- 고정 API 6개와 Session 인증의 MockMvc/브라우저 통합 테스트
- 현재 셸에서 선행 MySQL 통합 테스트를 자동 재현하기 위한 환경변수 주입 절차
- 실제 Spotify 계정 기반 OAuth/Export 통합 검증

고정 API 현재 상태:

| API | 현재 존재 | 실제 연결 가능한 Service |
| --- | --- | --- |
| `POST /api/music/from-spotify` | 없음 | `SpotifyService.saveSelectedTrack` |
| `POST /api/playlists/{playlistId}/items` | 없음 | `PlaylistItemService.add` |
| `GET /api/spotify/oauth/authorize` | 없음 | `SpotifyOAuthService.buildAuthorizationUrl` |
| `GET /api/spotify/me/connection` | 없음 | `SpotifyOAuthService.getConnection` |
| `POST /api/playlists/{playlistId}/export/spotify` | 없음 | `PlaylistExportService.export` |
| `POST /api/recommendations/{logId}/save` | 없음 | `RecommendationService.saveRecommendation` |

## 6. 팀원 B가 사용할 실제 Service 계약

아래 표의 안전 여부는 팀원 A 소스 변경 없이 Controller/DTO에서 호출할 수 있는지를 기준으로 한다.

| Service 클래스명 | 실제 메서드 시그니처 | 입력 타입 | 반환 타입 | 필요한 Controller/DTO 변환 | 명세와 다른 점 | 안전 여부 |
| --- | --- | --- | --- | --- | --- | --- |
| `MemberService` | `register(Member, String)` | 회원 Domain, 원문 비밀번호 | `Member` | Request에서 Domain 생성, `passwordHash` 제외 응답 | 전용 Request/Response 없음 | 안전 |
| `MemberService` | `getMyInfo(Long)` | Session 회원 ID | `Member` | 민감 필드 제외 | 활성 상태는 `AuthService` 경유 시 더 안전 | 조건부 안전 |
| `AuthService` | `login(HttpSession, String, String)` | Session, ID, 비밀번호 | `Member` | Login DTO 변환, 민감 필드 제외 | Session ID 재발급은 하지 않음 | 안전 |
| `AuthService` | `logout(HttpSession)` | Session | `void` | 성공 응답 또는 redirect | 없음 | 안전 |
| `AuthService` | `getLoginMemberId(HttpSession)` | Session | `Long` | 보호 API의 인증 주체로 사용 | 고정 Session key 사용 | 안전 |
| `AuthService` | `getLoginMember(HttpSession)` | Session | `Member` | ID만 하위 Service에 전달 | 없음 | 안전 |
| `RunningProfileService` | `getProfile(Long, Long)` | 로그인 ID, 대상 ID | `RunningProfile` | 본인 조회 DTO | 대상은 반드시 본인 | 안전 |
| `RunningProfileService` | `saveOrUpdate(Long, RunningProfile)` | 로그인 ID, 프로필 Domain | `RunningProfile` | Request에서 Domain 생성 | insert/update 통합 | 안전 |
| `MusicService` | `getMusicList(MusicSearchCondition)` | 검색 조건 Domain | `List<Music>` | 목록 DTO 변환 | 페이징 메타데이터 별도 | 안전 |
| `MusicService` | `countMusicList(MusicSearchCondition)` | 검색 조건 Domain | `long` | `PageResponse` 조립 | 목록과 별도 호출 | 안전 |
| `MusicService` | `getMusicDetail(Long)` | 음악 ID | `Music` | 상세 DTO 변환 | 평가/평균 별도 | 안전 |
| `MusicService` | `getRecommendedMusic(MusicSearchCondition)` | 검색 조건 Domain | `List<Music>` | 필요 시 내부 후보 DTO | 자동 큐레이션 API는 `RecommendationService` 사용 | 안전 |
| `MusicService` | `saveSelectedSpotifyTrack(SpotifyTrackMetadata)` | Spotify 메타데이터 | `Music` | Import Request 변환 | 중복 시 기존 음악 반환 | 안전 |
| `SpotifyService` | `searchTracks(String, Integer)` | 검색어, 최대 10 | `List<SpotifyTrackMetadata>` | Track Response 변환 | BPM/장르 없음 | 안전 |
| `SpotifyService` | `saveSelectedTrack(SpotifyTrackMetadata)` | Import Request 변환값 | `Music` | `MusicResponse` 변환 | 내부적으로 `MusicService` 위임 | 안전 |
| `MusicReviewService` | `create(Long, MusicReview)` | 로그인 ID, 평가 Domain | `MusicReview` | 회원 ID는 Session 값으로 강제 | 생성 DTO 없음 | 안전 |
| `MusicReviewService` | `update(Long, MusicReview)` | 로그인 ID, 평가 Domain | `MusicReview` | ID와 변경 필드만 설정 | 작성자/음악 ID는 기존 값 유지 | 안전 |
| `MusicReviewService` | `delete(Long, Long)` | 로그인 ID, 평가 ID | `void` | 성공 응답 | 없음 | 안전 |
| `MusicReviewService` | `getReviewsByMusicId(Long)` | 음악 ID | `List<MusicReview>` | 평가 응답 목록 | 없음 | 안전 |
| `MusicReviewService` | `getAverageScores(Long)` | 음악 ID | `MusicReviewAverage` | 상세 DTO에 병합 | 없음 | 안전 |
| `PlaylistService` | `create(Long, Playlist)` | 로그인 ID, Playlist Domain | `Playlist` | 허용 필드만 Request에서 복사 | 타입/소스/소유자/Export 상태는 Service 강제 | 안전 |
| `PlaylistService` | `getList(Long, Long)` | 조회자 ID nullable, 소유자 필터 | `List<Playlist>` | 목록 DTO | 검색/페이징 없음 | 안전 |
| `PlaylistService` | `getDetail(Long, Long)` | 조회자 ID nullable, 플레이리스트 ID | `Playlist` | 수록곡과 별도 조립 | 상세 집계가 아님 | 안전 |
| `PlaylistService` | `update(Long, Playlist)` | 로그인 ID, 변경 Domain | `Playlist` | 허용 필드만 설정 | 기존 Export 필드 유지 | 안전 |
| `PlaylistService` | `delete(Long, Long)` | 로그인 ID, 플레이리스트 ID | `void` | 성공 응답 | soft delete | 안전 |
| `PlaylistItemService` | `add(Long, PlaylistItem)` | 로그인 ID, 수록곡 Domain | `PlaylistItem` | path ID 강제, Request의 회원/플레이리스트 ID 불신 | Spotify 음악만 URI 필수 | 안전 |
| `PlaylistItemService` | `delete(Long, Long)` | 로그인 ID, 수록곡 행 ID | `void` | 성공 응답 | 삭제 기준은 `musicId`가 아니라 수록곡 행 ID | 안전 |
| `PlaylistItemService` | `updateOrder(Long, Long, Integer)` | 로그인 ID, 수록곡 행 ID, 순서 | `PlaylistItem` | 순서 변경 응답 | 없음 | 안전 |
| `PlaylistItemService` | `getItems(Long, Long)` | 조회자 ID nullable, 플레이리스트 ID | `List<PlaylistItem>` | 음악 메타데이터 별도 조립 | `musicId` 중심 | 조건부 안전 |
| `PlaylistItemService` | `getSpotifyUris(Long, Long)` | 로그인 ID, 플레이리스트 ID | `List<String>` | 외부 응답에 URI 목록 노출 불필요 | 전체 수록곡 URI 필수 | 내부 전용 |
| `PlaylistSaveService` | `save(Long, Long)` | 로그인 ID, 플레이리스트 ID | `PlaylistSave` | 저장 상태 DTO | 소유자 비공개 저장 허용 | 정책 확인 필요 |
| `PlaylistSaveService` | `cancel(Long, Long)` | 로그인 ID, 플레이리스트 ID | `void` | 성공 응답 | 저장 관계 없을 때 `ACCESS_DENIED` | 안전 |
| `PlaylistSaveService` | `getMySavedList(Long)` | 로그인 ID | `List<Playlist>` | 목록 DTO | `saved=true` 포함 | 안전 |
| `RecommendationService` | `recommend(Long, RunningIntensity, String, Integer)` | 로그인 ID, 강도, 장르, 개수 | `RecommendationResult` | 로그 ID, 사유, 음악 목록 DTO | 요청 BPM 범위 없음 | 계약 확정 필요 |
| `RecommendationService` | `saveRecommendation(Long, Long, String, PlaylistVisibility)` | 로그인 ID, 로그 ID, 제목, 공개 범위 | `Playlist` | Curation 저장 응답 | 저장은 1회만 가능 | 안전 |
| `RecommendationService` | `getMyRecommendationLogs(Long)` | 로그인 ID | `List<RecommendationLog>` | JSON 문자열 필드는 구조화하거나 그대로 문서화 | 상세 단건 조회 공개 메서드 없음 | 안전 |
| `SpotifyOAuthService` | `buildAuthorizationUrl(HttpSession)` | 동일 브라우저 Session | `String` | URL 응답 또는 redirect | 로그인 검증은 Controller 책임 | 조건부 안전 |
| `SpotifyOAuthService` | `handleCallback(Long, String, String, String, HttpSession)` | 로그인 ID, code/error/state, Session | `SpotifyConnection` | 토큰 제외 연결 DTO 또는 redirect | Domain 직접 반환 금지 | 안전 |
| `SpotifyOAuthService` | `getConnection(Long)` | 로그인 ID | `SpotifyConnection` | 토큰 제외 연결 DTO | 미연결 시 예외 | 계약 확정 필요 |
| `SpotifyOAuthService` | `getValidConnection(Long)` | 로그인 ID | `SpotifyConnection` | Controller 직접 사용 불필요 | 만료 시 자동 갱신 | 내부 전용 |
| `SpotifyOAuthService` | `disconnect(Long)` | 로그인 ID | `void` | 성공 응답 | 외부 권한 철회는 하지 않음 | 안전 |
| `PlaylistExportService` | `export(Long, Long)` | 로그인 ID, 플레이리스트 ID | `Playlist` | Export 응답 DTO | 곡별 성공/실패 수 없음 | 조건부 안전 |

추가 러닝 Service는 모두 구현되어 있으나 본 P0의 고정 API보다 후순위다. 이 중 `RunningRecordService`, `RunningSessionService`, `RunningSessionParticipantService`는 `HttpSession`을 직접 받으므로 기존 MVP Service와 인증 인자 형태가 다르다.

## 7. 팀원 B가 작성해야 하는 Controller/API 계약

공통 규칙:

- 보호 API는 요청의 회원 ID를 신뢰하지 않는다.
- `AuthService.getLoginMemberId(session)`으로 로그인 회원 ID를 확정한다.
- Domain을 그대로 JSON 직렬화하지 않는다.
- path variable과 Request Body의 중복 ID가 있으면 path 값을 우선하고 Body ID는 받지 않는 것을 권장한다.
- 성공 응답과 오류 응답 형식은 B-01 결정 후 전 API에 동일 적용한다.

고정 API 권장 계약:

| API | 인증 | Request | Service 호출 | Response 핵심 필드 | 현재 주의점 |
| --- | --- | --- | --- | --- | --- |
| `POST /api/music/from-spotify` | Session 필수 권장 | Spotify Track ID, URI, 외부 URL, 제목, 아티스트, 앨범, 이미지, 선택 BPM/장르 | `spotifyService.saveSelectedTrack(metadata)` | `musicId`, 메타데이터, Spotify 식별 필드 | 중복이면 기존 `musicId` 반환 |
| `POST /api/playlists/{playlistId}/items` | Session 필수 | `musicId`, `orderNo` | `playlistItemService.add(loginMemberId, item)` | 수록곡 행 ID, playlist ID, music ID, 순서 | 추가 전 음악 DB 저장이 선행되어야 함 |
| `GET /api/spotify/oauth/authorize` | Session 필수 | 없음 | `spotifyOAuthService.buildAuthorizationUrl(session)` | `authorizationUrl` 또는 302 redirect | Vue에서는 URL JSON 응답이 테스트하기 쉬움 |
| `GET /api/spotify/me/connection` | Session 필수 | 없음 | `spotifyOAuthService.getConnection(loginMemberId)` | `connected`, `spotifyUserId`, `tokenExpiresAt` | 미연결 200/401 정책 결정 필요 |
| `POST /api/playlists/{playlistId}/export/spotify` | Session 필수 | MVP에서는 Body 없음 가능 | `playlistExportService.export(loginMemberId, playlistId)` | ID, URL, 상태, 오류 코드, 완료 시각 | 정확한 곡별 성공/실패 수 없음 |
| `POST /api/recommendations/{logId}/save` | Session 필수 | `title`, `visibility` | `recommendationService.saveRecommendation(...)` | 생성된 playlist ID, `USER`, `CURATION_COPY`, 제목, 공개 범위 | `playlist_saves` 호출 금지 |

연결 흐름:

```text
Spotify 검색
→ 선택 결과를 POST /api/music/from-spotify
→ 반환된 musicId를 POST /api/playlists/{playlistId}/items
→ GET /api/spotify/me/connection
→ 미연결이면 GET /api/spotify/oauth/authorize
→ callback 완료
→ POST /api/playlists/{playlistId}/export/spotify
→ spotifyPlaylistUrl 표시
```

자동 큐레이션 흐름:

```text
추천 조회 API
→ RecommendationResult 반환
→ recommendationLog.id 보관
→ 이 시점에는 플레이리스트 미생성
→ POST /api/recommendations/{logId}/save
→ USER + CURATION_COPY 플레이리스트 ID 반환
```

## 8. DTO 중복·충돌 위험

1. `MusicSearchCondition`은 이미 Domain 패키지에 있으므로 같은 이름의 Request DTO를 만들면 import 충돌이 난다. `MusicListRequest` 또는 `MusicSearchRequest`를 권장한다.
2. `ErrorResponse`가 이미 `common.response`에 존재한다. 같은 이름을 새 패키지에 중복 생성하지 않는다.
3. `PageRequest`는 Spring Data의 동명 타입과 충돌 가능성이 있다. 현재 Spring Data 의존성은 없지만 `PageQuery` 또는 `PageRequestDto`가 안전하다.
4. `SpotifyTrackMetadata`는 Service 입력 Domain이다. 외부 Request DTO는 `SpotifyTrackImportRequest`로 분리하고 허용 필드만 변환한다.
5. `SpotifyConnection`은 토큰을 가진 민감 Domain이다. `SpotifyConnectionResponse`와 절대 혼용하지 않는다.
6. `Member`는 `passwordHash`를 가진다. 회원 응답은 반드시 별도 DTO를 사용한다.
7. `Playlist`에는 내부 실패 코드와 삭제 시각이 있다. 화면별 DTO에서 필요한 필드만 노출한다.
8. `RecommendationLog.resultSummary`와 `requestConditions`는 JSON 문자열이다. 문자열 그대로 응답할지 구조화 DTO로 변환할지 한 방식으로 고정해야 한다.
9. `PlaylistItem`에는 음악 표시 정보가 없다. `PlaylistItemResponse`는 `MusicResponse`를 포함하도록 Controller 조립 계약을 명확히 해야 한다.
10. Lombok Domain은 입력 제한이 없으므로 Request Body를 Domain에 직접 바인딩하지 않는다.

## 9. Session/CORS/Cookie 계약

확정된 실제 계약:

- 로그인 성공 시 `LOGIN_MEMBER_ID`에 `Long` 회원 ID를 저장한다.
- Session timeout은 30분이다.
- 로그아웃은 Session 전체를 무효화한다.
- JWT와 Spring Security는 사용하지 않는다.

미구현 공통 설정:

- `http://localhost:5173` 허용 CORS
- `allowCredentials(true)`
- `/api/**` 대상 Methods/Headers 정책
- Cookie의 SameSite/Secure/HttpOnly 명시 정책

Vue 권장 호출:

```javascript
axios.create({
  baseURL: "http://localhost:8080/api",
  withCredentials: true,
});
```

개발 서버의 두 포트는 origin이 다르므로 `withCredentials`만으로 충분하지 않다. Spring이 정확한 origin을 허용하고 credential 응답 헤더를 보내야 한다. wildcard origin과 credential 허용을 함께 사용하면 안 된다.

Controller 인증 규칙:

```text
HttpSession
→ AuthService.getLoginMemberId(session)
→ 반환된 Long만 Service의 loginMemberId로 전달
```

Request Body, query, path에서 받은 회원 ID를 인증 주체로 전달하면 소유권 검증이 우회될 수 있다.

## 10. Spotify OAuth 및 Export 계약

OAuth 실제 계약:

- 환경변수: `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REDIRECT_URI`
- Scope: `user-read-private`, `playlist-modify-private`, `playlist-modify-public`
- authorize URL 생성 시 state를 Session에 저장한다.
- callback에서 같은 Session의 state를 검증하고 즉시 제거한다.
- 연결 정보는 회원별 insert/update한다.
- Access Token이 만료됐거나 30초 이내 만료 예정이면 Refresh Token으로 갱신한다.
- 연결 응답에 Access Token과 Refresh Token을 포함하면 안 된다.

권장 callback 경로:

```text
GET /api/spotify/oauth/callback
```

이 경로는 `SPOTIFY_REDIRECT_URI`와 Spotify 개발자 설정에 문자 단위로 일치해야 한다.

Export 실제 계약:

1. USER 플레이리스트인지 확인한다.
2. 로그인 회원이 소유자인지 확인한다.
3. 이미 성공했거나 불완전한 기존 외부 Playlist 정보가 있으면 차단한다.
4. Spotify 연결과 유효 Token을 확인한다.
5. 수록곡이 1개 이상이고 모든 곡에 Spotify URI가 있는지 확인한다.
6. 상태를 `PENDING`으로 변경한다.
7. Spotify Playlist를 생성하고 ID/URL을 DB에 임시 저장한다.
8. 수록곡을 100개 단위로 추가한다.
9. 성공 시 `SUCCESS`, URL, 완료 시각을 저장한다.
10. 실패 시 `FAILED`와 오류 코드를 저장한다.

Export 응답으로 신뢰 가능한 값:

- `playlistId`
- `spotifyPlaylistId`
- `spotifyPlaylistUrl`
- `spotifyExportStatus`
- `spotifyExportErrorCode`
- `spotifyExportedAt`

현재 신뢰할 수 없는 값:

- 실패 중 정확히 추가된 곡 수
- 실패 중 정확히 추가되지 않은 곡 수

실패 후에도 Spotify Playlist ID/URL이 남을 수 있다. UI는 이 경우 URL과 실패 상태를 함께 표시하고 자동 재시도 버튼을 제공하지 않는 것이 안전하다.

## 11. 자동 큐레이션 및 playlist_saves 구분 계약

자동 큐레이션 조회:

- 회원의 러닝 프로필이 필수다.
- 요청 강도는 필수다.
- 요청 장르가 없으면 프로필 선호 장르를 사용한다.
- BPM은 유효한 프로필 범위를 우선 사용하고 없으면 강도 기본 범위를 사용한다.
- 추천 음악이 없으면 `RECOMMENDATION_DATA_NOT_ENOUGH`다.
- 추천 결과와 조건을 `recommendation_logs`에 저장한다.
- 플레이리스트와 수록곡은 생성하지 않는다.

자동 큐레이션 저장:

- 로그인 회원 소유 로그만 저장할 수 있다.
- 이미 저장된 로그는 다시 저장할 수 없다.
- `playlists`에 `playlistType=USER`, `sourceType=CURATION_COPY`를 생성한다.
- 추천 음악 ID를 `playlist_items`에 순서대로 bulk insert한다.
- `recommendation_logs.saved_playlist_id`를 연결한다.
- 위 작업은 하나의 Spring Transaction이다.

`playlist_saves` 계약:

- 기존 플레이리스트를 내 저장 목록에 참조 저장하는 관계다.
- 자동 큐레이션 저장에 사용하지 않는다.
- 큐레이션 저장본은 내 개인 플레이리스트 목록에서 조회해야 한다.
- 현재 Service는 소유자의 비공개 플레이리스트 참조 저장도 허용하므로 명세의 공개/공식 한정 정책과 차이가 있다.

## 12. 후속 작업 우선순위

### P0 - 계약 확정

1. 오류 응답 형식 B-01 확정
2. 자동 큐레이션 BPM 입력 정책 B-02 확정
3. Spotify 미연결 응답 B-03 확정
4. Export 곡 수 응답 B-04 확정

### P1 - 즉시 구현 가능

1. 공통 Session 회원 ID 추출 규칙 적용
2. 민감 Domain 비노출 Response DTO
3. 음악 목록/상세/평가 Controller와 DTO
4. 플레이리스트 CRUD/수록곡 Controller와 DTO
5. `POST /api/music/from-spotify`
6. `POST /api/playlists/{playlistId}/items`
7. `POST /api/recommendations/{logId}/save`
8. Spotify OAuth authorize/callback Controller
9. Spotify Export Controller
10. Vue3/Vite 프로젝트와 credential 포함 API Client
11. CORS 설정

### P2 - 통합 검증

1. MockMvc Session 인증 테스트
2. 고정 API 6개 Request/Response 테스트
3. 필요 시 선행 검증 절차대로 실제 MySQL Mapper 통합 테스트 13건 회귀 실행
4. 실제 Spotify OAuth/Export 수동 통합 테스트
5. JSP → Session → Vue API Cookie 전달 브라우저 테스트
6. Swagger/OpenAPI 또는 API Tester 예시 고정

## 13. 팀원 A 확인 요청 사항

1. 공통 오류 응답은 현재 `{code, message}`를 유지하는가, 목표 `{success, message, errorCode}`로 변경하는가?
2. 자동 큐레이션 조회 UI의 BPM 입력은 제거하고 러닝 프로필 BPM만 사용하는가?
3. `GET /api/spotify/me/connection` 미연결 상태는 200과 `connected=false`인가, 401 `SPOTIFY_AUTH_REQUIRED`인가?
4. Export 응답의 성공/실패 곡 수는 MVP에서 제외해도 되는가?
5. 소유자 본인의 비공개 플레이리스트를 `playlist_saves`에 저장할 수 있는 현재 동작을 유지하는가?
6. Spotify 검색 결과의 BPM/장르가 `null`인 상태를 MVP에서 허용하는가?
7. CURATION_COPY에 Spotify URI가 없는 음악이 포함되면 Export 불가로 안내하는 정책이 맞는가?
8. 실패 후 Spotify ID/URL이 남은 Export는 수동 복구만 허용하는가?
9. 예상하지 못한 500 예외도 공통 JSON으로 감쌀 것인가?