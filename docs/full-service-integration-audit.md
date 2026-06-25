# SSAFIT PlayList 전체 서비스 통합 점검 보고서

## 1. 점검 범위 및 기준

`feature/a-running-features-service` 브랜치의 Java 코드, Mapper Interface/XML, DDL, 테스트를 다음 기준으로 점검했다.

- Domain, Mapper, Service 연결 여부
- DDL, Domain 필드, Mapper `resultMap` 정합성
- Mapper Interface 메서드와 XML `namespace`, `id` 일치 여부
- 인증, 인가, 소유자, 작성자, 본인 검증
- 트랜잭션과 실패 시 데이터 정합성
- 역할 명세 대비 미구현 기능
- 팀원 B의 Controller, DTO, Vue 연동 가능성
- 기존 MVP, Spotify, 러닝 추가 기능 간 충돌 여부

점검 과정에서는 코드 및 DB 스키마를 수정하지 않았다.

## 2. 전체 서비스 연결 구조

```text
Session 로그인
 ├─ 기존 MVP Service
 │   └─ Controller가 Session에서 확인한 로그인 회원 ID를 전달해야 함
 ├─ Spotify Service
 │   └─ OAuth 연결 → 토큰 갱신 → Playlist Export
 └─ 러닝 추가 기능
     └─ Service가 HttpSession → AuthService로 로그인 회원을 직접 조회
```

목표 흐름에 필요한 Service는 대부분 존재한다. 다만 Controller, Request/Response DTO, Vue가 아직 구현되지 않아 HTTP 요청부터 화면까지 이어지는 End-to-End 흐름은 구성되지 않았다.

자동 큐레이션은 다음과 같이 역할이 분리되어 있다.

- 추천 조회 시 추천 로그만 생성한다.
- 추천 저장 시 `USER + CURATION_COPY` 플레이리스트를 생성한다.
- 추천 곡을 `playlist_items`에 복사한다.
- 생성된 플레이리스트를 `recommendation_logs.saved_playlist_id`에 연결한다.
- CURATION_COPY 생성에 `playlist_saves`를 잘못 사용하지 않는다.

## 3. 기능별 구현 상태

| 기능 | Domain | Mapper | Service | 권한 검증 | 테스트 | 연동 상태 |
| --- | --- | --- | --- | --- | --- | --- |
| 회원가입 | 완료 | 완료 | 완료 | 공개 기능 | 있음 | Service 완료 |
| 로그인/로그아웃/Session | 완료 | 완료 | 완료 | AuthService | 있음 | 완료 |
| 내 정보 조회 | 완료 | 완료 | 완료 | 호출자 ID 의존 | 있음 | 주의 필요 |
| 러닝 프로필 | 완료 | 완료 | 완료 | 본인 검증 | 있음 | 주의 필요 |
| 음악 목록/검색/상세 | 완료 | 완료 | 완료 | 공개 조회 | 있음 | 완료 |
| Spotify 검색/DB 저장 | 완료 | 완료 | 완료 | 인증 없음 | 있음 | 일부 구현 |
| 음악 평가 CRUD/평균 점수 | 완료 | 완료 | 완료 | 작성자 검증 | 있음 | 완료 |
| 플레이리스트 CRUD | 완료 | 완료 | 완료 | 소유자/공개 범위 검증 | 있음 | 완료 |
| 수록곡 추가/삭제/순서 변경 | 완료 | 완료 | 완료 | 소유자 검증 | 있음 | 완료 |
| Spotify OAuth/갱신/연결 상태 | 완료 | 완료 | 완료 | 호출자 ID 의존 | 있음 | 완료 |
| Spotify Export | 완료 | 완료 | 완료 | 소유자 검증 | 있음 | 조건부 완료 |
| 플레이리스트 저장/취소 | 완료 | 완료 | 완료 | 접근 범위 검증 | 있음 | 정책 확인 필요 |
| 자동 큐레이션 조회/저장 | 완료 | 완료 | 완료 | 본인 로그 검증 | 있음 | Export 연계 문제 |
| 러닝 기록 CRUD | 완료 | 완료 | 완료 | Session 기반 본인 검증 | 있음 | 완료 |
| 러닝 모임 CRUD/상태 | 완료 | 완료 | 완료 | Session 기반 생성자 검증 | 있음 | 일부 보완 필요 |
| 모임 참여/취소 | 완료 | 완료 | 완료 | Session 기반 본인 검증 | 있음 | 완료 |
| Controller/DTO/Vue | 없음 | 해당 없음 | 해당 없음 | 미연동 | 없음 | 연결 불가 |

확인 결과 Mapper Interface에 대응하는 XML이 존재하며, Service에서 호출하는 Mapper 메서드도 실제로 선언되어 있다. 주요 Enum 값도 DDL의 `CHECK` 제약과 일치한다.

## 4. 정상 연결된 핵심 흐름

### 4.1 인증

`AuthService`는 비밀번호 확인 후 `LOGIN_MEMBER_ID`를 Session에 저장한다. Session 회원 조회 시 회원 존재 여부와 활성 상태를 확인한다.

### 4.2 플레이리스트와 Spotify Export

다음 흐름이 연결되어 있다.

```text
플레이리스트 소유권 확인
→ Spotify 연결 조회 및 토큰 갱신
→ 수록곡 Spotify URI 검증
→ Export 상태 PENDING 변경
→ Spotify 플레이리스트 생성
→ Spotify 수록곡 추가
→ URL 및 SUCCESS 상태 저장
```

외부 API 실패 시 `FAILED` 상태와 오류 정보를 저장하는 흐름도 존재한다.

### 4.3 자동 큐레이션

추천 조회 단계에서는 플레이리스트나 수록곡을 생성하지 않는다. 추천 저장 시에만 플레이리스트 생성, 수록곡 복사, 추천 로그 연결을 하나의 트랜잭션으로 처리한다.

### 4.4 러닝 모임 참여

모임 참여는 다음 순서로 처리된다.

1. 로그인 회원 조회
2. `selectByIdForUpdate`로 모임 행 잠금
3. 모임 존재 여부 확인
4. `OPEN` 상태 확인
5. 중복 참여 확인
6. 현재 참여자 수와 정원 비교
7. 참여 관계 INSERT
8. 실패 시 트랜잭션 롤백

Service 검증과 `(session_id, member_id)` UNIQUE 제약을 함께 사용한다. 설계상 중복 참여와 동시 요청에 의한 정원 초과를 방지한다.

### 4.5 Soft delete

- `playlists`: 일반 조회에서 `deleted_at IS NULL` 조건 사용
- `running_records`: 일반 조회에서 `deleted_at IS NULL` 조건 사용
- `members`: 회원 상태와 `deleted_at`을 조합한 soft delete 사용

삭제된 플레이리스트와 러닝 기록이 일반 Service 조회에 직접 노출되는 SQL은 발견되지 않았다.

## 5. 발견된 문제 및 미구현 항목

### P0

발견되지 않았다.

현재 코드에서 빌드 실패, 즉각적인 데이터 손실 또는 확정적인 주요 인증 우회는 발견되지 않았다.

### P1

#### 인증 주체 전달 방식 불일치

- **현재 상태:** 일부 구현
- **근거 파일:** `RunningProfileService`, `MusicReviewService`, `PlaylistService`, `PlaylistItemService`, `PlaylistSaveService`, `RecommendationService`, `SpotifyOAuthService`, `PlaylistExportService`
- **누락 내용:** 기존 MVP와 Spotify Service 다수가 외부에서 전달된 `loginMemberId`를 인증 주체로 사용한다. 러닝 기록·모임 Service는 `HttpSession`과 `AuthService`로 회원을 직접 확인한다.
- **영향 범위:** Controller가 요청 파라미터나 Request DTO의 회원 ID를 전달하면 소유권 검증을 우회할 수 있다.
- **수정 권장 방향:** 팀원 B Controller는 반드시 `AuthService.getLoginMember(session)` 결과에서 얻은 ID만 전달해야 한다. 장기적으로 보호 Service의 인증 주체 처리 방식을 통일한다.

#### Controller/DTO/Vue 미구현

- **현재 상태:** 연결 불가
- **근거 파일:** 각 기능의 controller/dto 디렉터리에 `.gitkeep`만 존재
- **누락 내용:** Service를 실제 HTTP 요청으로 호출할 API 및 화면 계층이 없다.
- **영향 범위:** 명시된 전체 사용자 흐름을 End-to-End로 실행할 수 없다.
- **수정 권장 방향:** 팀원 B 범위에서 Controller, Request/Response DTO, Vue API Module과 화면을 구현한 후 HTTP 통합 테스트를 수행한다.

#### Spotify 저장 음악의 BPM·장르 누락

- **현재 상태:** 일부 구현
- **근거 파일:** `SpotifyService.toMetadata`, `MusicService.saveSelectedSpotifyTrack`, `RecommendationService`
- **누락 내용:** Spotify 검색 결과를 저장할 때 BPM과 장르가 채워지지 않는다. 자동 추천은 BPM 범위를 주요 검색 조건으로 사용한다.
- **영향 범위:** `Spotify 검색 → 곡 저장 → 자동 큐레이션` 흐름에서 저장한 곡이 추천 대상에서 제외될 수 있다.
- **수정 권장 방향:** BPM·장르 보강 정책 또는 별도 메타데이터 입력 흐름을 정의한다.

#### CURATION_COPY와 Spotify Export 불완전 연계

- **현재 상태:** 일부 구현
- **근거 파일:** `RecommendationService.saveRecommendation`, `PlaylistItemService.getSpotifyUris`
- **누락 내용:** 큐레이션은 `spotify_uri`가 없는 곡도 복사하지만 Export는 모든 수록곡에 URI가 있어야 한다.
- **영향 범위:** 저장된 CURATION_COPY 플레이리스트가 Spotify로 Export되지 않을 수 있다.
- **수정 권장 방향:** 추천 저장 시 Export 가능 여부를 표시하거나, Export 대상 곡 필터링 및 URI 보강 정책을 마련한다.

#### 모임 정원을 현재 참여자보다 작게 변경 가능

- **현재 상태:** 일부 구현
- **근거 파일:** `RunningSessionService.updateMySession`
- **누락 내용:** 수정 시 `capacity > 0`만 검증하고 현재 참여자 수와 비교하지 않는다.
- **영향 범위:** 참여자가 이미 5명인 모임의 정원을 2명으로 변경하는 모순 상태가 가능하다.
- **수정 권장 방향:** 정원 변경 시 현재 참여자 수 이상인지 검증한다.

#### 민감 Domain 직접 반환 위험

- **현재 상태:** 일부 구현
- **근거 파일:** `Member.passwordHash`, `SpotifyConnection.accessToken`, `SpotifyConnection.refreshToken`
- **누락 내용:** 민감 필드를 가진 Domain을 Service가 반환한다.
- **영향 범위:** Controller가 Domain을 그대로 JSON으로 반환하면 비밀번호 해시 또는 Spotify 토큰이 노출될 수 있다.
- **수정 권장 방향:** 팀원 B는 허용 필드만 포함한 Response DTO를 사용하고 Domain 직접 직렬화를 금지한다.

### P2

#### Mapper 통합 테스트 12건 미실행

- **현재 상태:** 테스트 실행 누락
- **근거 파일:** `MapperIntegrationTest`의 `@EnabledIfEnvironmentVariable`
- **누락 내용:** `SSAFIT_TEST_DB_URL`이 없으면 실제 MySQL 기반 Mapper 테스트를 건너뛴다.
- **영향 범위:** FK, UNIQUE, 실제 MyBatis SQL, `FOR UPDATE`, 참여 동시성은 이번 실행에서 실제 DB로 검증되지 않았다.
- **수정 권장 방향:** develop 병합 전 MySQL 테스트 환경변수를 설정하고 재실행한다.

#### Session fixation 방어 없음

- **현재 상태:** 일부 구현
- **근거 파일:** `AuthService.login`
- **누락 내용:** 로그인 성공 시 Session ID를 교체하지 않고 로그인 회원 속성만 저장한다.
- **영향 범위:** Session fixation 공격에 대한 방어가 약하다.
- **수정 권장 방향:** 로그인 성공 시 `session.changeSessionId()` 적용을 검토한다.

#### Spotify 토큰 평문 저장

- **현재 상태:** 일부 구현
- **근거 파일:** `spotify_connections.access_token`, `spotify_connections.refresh_token`
- **누락 내용:** 토큰 저장 암호화 계층이 없다.
- **영향 범위:** DB 유출 시 Spotify 자격 증명이 노출된다.
- **수정 권장 방향:** 운영 배포 전 애플리케이션 암호화 또는 비밀 저장소를 적용한다.

#### 플레이리스트 저장 정책 불일치

- **현재 상태:** 일부 구현
- **근거 파일:** `PlaylistSaveService.validateSaveAccess`, `PlaylistSaveServiceTest.privatePlaylistOwnerCanSaveReference`
- **누락 내용:** 비공개 플레이리스트도 소유자라면 저장할 수 있도록 구현되어 있다.
- **영향 범위:** 공개 또는 공식 플레이리스트를 저장한다는 문서 정책과 동작이 다를 수 있다.
- **수정 권장 방향:** 자기 비공개 플레이리스트 저장 허용 여부를 명세로 확정한다.

#### 팀원 B 표시용 조회 데이터 부족

- **현재 상태:** 일부 구현
- **누락 내용:** 러닝 모임 목록에 현재 참여자 수가 포함되지 않고 생성자와 참여자는 회원 ID 중심이다. 플레이리스트 수록곡도 음악 ID 중심이다.
- **영향 범위:** 화면 구성 시 N+1 조회 또는 Mapper 직접 접근이 발생할 수 있다.
- **수정 권장 방향:** Service 집계 결과나 화면용 Response 조립 기능을 추가한다.

#### Export PENDING 복구 정책 없음

- **현재 상태:** 일부 구현
- **근거 파일:** `PlaylistExportService`, `PlaylistService`
- **누락 내용:** Export 중 프로세스가 종료되면 `PENDING` 상태가 장기간 유지될 수 있다.
- **영향 범위:** 해당 플레이리스트의 재Export가 차단될 수 있다.
- **수정 권장 방향:** timeout 기반 재시도 또는 운영 복구 정책을 마련한다.

#### Spotify 실제 API 통합 테스트 없음

- **현재 상태:** 테스트 없음
- **누락 내용:** 현재 Spotify 테스트는 Mock 기반이다.
- **영향 범위:** redirect URI, 운영 자격 증명, Premium 계정, 개발 모드 사용자 등록 및 실제 API 응답 차이를 자동 검증하지 못한다.
- **수정 권장 방향:** 배포 전 수동 통합 체크리스트 또는 별도 운영 환경 통합 테스트를 실행한다.

### P3

- `INTERNAL_SERVER_ERROR` 등 일부 ErrorCode는 실제 예외 흐름에서 사용되지 않는다.
- 일부 준비용 Mapper 메서드는 Service에서 사용되지 않는다.
- 추천 요청 조건 JSON을 문자열 결합으로 생성해 확장성이 낮다.
- 기존 MVP Service에 인증 및 소유권 검증 코드가 반복된다.
- 생성·수정 후 DB 값을 재조회하지 않아 반환 객체의 `createdAt`, `updatedAt`, `joinedAt`, `cancelledAt` 값이 비어 있거나 오래된 상태일 수 있다.

## 6. 기능 간 충돌 또는 데이터 정합성 위험

- 기존 MVP/Spotify와 러닝 기능 사이의 테이블 및 클래스 이름 충돌은 발견되지 않았다.
- Enum 값은 DDL의 상태 `CHECK` 값과 일치한다.
- CURATION_COPY는 `playlist_saves`를 사용하지 않아 저장 의미가 섞이지 않는다.
- 러닝 참여는 행 잠금과 UNIQUE 제약으로 동시성 방지 구조가 마련되어 있다.
- 모임 취소 시 기존 참여 관계는 남는다. 이력 보존인지 삭제 대상인지 정책 확정이 필요하다.
- Spotify Export는 외부 API 호출을 포함하므로 DB 트랜잭션만으로 Spotify에 생성된 플레이리스트까지 롤백할 수 없다.
- 회원 탈퇴 데이터는 `AuthService` 경유 시 차단되지만, `MemberService.getMyInfo(memberId)` 직접 호출은 회원 활성 상태를 별도로 확인하지 않는다.
- 다수의 수정 및 삭제 Service가 Mapper의 영향 행 수를 확인하지 않아 동시 삭제 상황에서 실제 반영 없이 성공처럼 종료될 가능성이 있다.

## 7. 팀원 B 연동 전 확인 필요 사항

1. 요청에서 받은 `memberId`를 인증 주체로 사용하지 않는다.
2. 보호 API는 Session과 `AuthService.getLoginMember()`로 로그인 회원을 확정한다.
3. `Member`, `SpotifyConnection` Domain을 Response로 직접 반환하지 않는다.
4. 러닝 모임 목록 DTO에 현재 참여자 수와 생성자 표시 정보를 포함할 방법을 합의한다.
5. 참여자 목록에 닉네임 등 공개 회원 정보가 필요하면 별도 조회 계약을 마련한다.
6. 플레이리스트 상세 DTO에서 항목과 음악 메타데이터를 조립한다.
7. 생성·수정 응답에 DB 생성 시간과 갱신 시간이 즉시 필요한지 확정한다.
8. Spotify URI가 없는 음악의 Export UI 정책을 마련한다.
9. Controller 구현 후 비로그인, 타인 ID 전달, 비공개 접근에 대한 HTTP 통합 테스트를 추가한다.
10. Spotify OAuth 시작 API도 애플리케이션 로그인 상태에서만 호출하도록 제한한다.

## 8. 테스트 누락 항목

- 실제 MySQL 기반 Mapper 테스트 12건
- 실제 DB를 사용한 동시 참여 요청 테스트
- Spotify OAuth callback 실제 연동
- Spotify token refresh 실제 연동
- Spotify Playlist Export 실제 생성
- Controller Session 위변조 및 타인 ID 전달 테스트
- 민감 필드 Response 제외 테스트
- 참여자 수보다 작은 정원 수정 테스트
- URI 없는 곡이 포함된 CURATION_COPY Export 테스트
- Export 도중 프로세스 중단 후 복구 테스트
- 참여 중인 모임 취소 시 참여 관계 처리 테스트

실행 명령과 결과:

```text
mvnw.cmd clean test

Tests run: 196
Failures: 0
Errors: 0
Skipped: 12
BUILD SUCCESS
```

12건은 `SSAFIT_TEST_DB_URL` 미설정으로 건너뛴 Mapper/MySQL 통합 테스트다.

## 9. 권장 수정 순서

1. 팀원 B Controller의 Session 인증 주체 획득 방식을 고정한다.
2. 민감 Domain 직접 반환을 방지하는 Response DTO를 구현한다.
3. 실제 MySQL 환경에서 Mapper 및 동시성 테스트를 실행한다.
4. 러닝 모임 정원 축소 검증을 추가한다.
5. Spotify 저장 음악의 BPM·장르 보강 정책을 결정한다.
6. CURATION_COPY와 Spotify Export 가능 곡 정책을 정리한다.
7. 참여자 수, 생성자, 참여자 표시 정보 등 조회 계약을 보강한다.
8. Spotify 토큰 암호화와 `PENDING` 상태 복구 정책을 마련한다.
9. Controller와 Vue 연결 후 전체 End-to-End 테스트를 수행한다.

## 10. 최종 결론

Domain, Mapper, Service, DDL 사이에서 빌드를 막는 구조적 불일치는 발견되지 않았다. 러닝 기록, 러닝 모임, 참여 기능도 기존 MVP 및 Spotify 구현을 직접 침범하지 않으며, 러닝 모임 참여의 잠금·중복·정원·상태 검증 구조도 적절하다.

현재 핵심 위험은 다음과 같다.

- 기존 Service가 외부에서 전달된 `loginMemberId`를 인증 주체로 신뢰하는 구조
- Spotify 저장 음악과 자동 큐레이션/Export 사이의 메타데이터 단절
- Controller/DTO/Vue 부재로 실제 전체 사용자 흐름을 검증할 수 없는 상태
- 실제 MySQL Mapper 및 동시성 테스트가 기본 테스트 실행에서 제외된 상태

따라서 현재 브랜치는 Service 기반 구현으로는 정상적인 상태지만, develop 병합 전 실제 MySQL 통합 테스트와 팀원 B의 Session 인증 및 DTO 보안 계약 확인이 필요하다.

