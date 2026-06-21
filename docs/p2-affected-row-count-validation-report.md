# P2 영향 행 수 검증 보완 보고서

## 1. 작업 목적

수정·삭제·취소·상태 변경 Service가 Mapper 호출 전 조회 결과만 신뢰하고 실제 DB 변경 결과를 확인하지 않는 문제를 보완했다.

사전 조회 이후 다른 트랜잭션이 대상을 삭제하거나 상태를 변경하면 Mapper 영향 행 수가 0이 될 수 있다. 이 경우 Service가 성공을 반환하지 않고 기존 ErrorCode 체계에 맞는 예외를 발생시키도록 변경했다.

이번 작업에서는 Controller, DTO, Vue, DDL 및 P0 러닝 모임 동시 참여 잠금 순서를 변경하지 않았다.

## 2. 점검 대상 Service 및 Mapper

점검한 변경 계열 Service는 12개다.

- `MemberService`
- `RunningProfileService`
- `RunningRecordService`
- `RunningSessionService`
- `RunningSessionParticipantService`
- `MusicService`
- `MusicReviewService`
- `PlaylistService`
- `PlaylistItemService`
- `PlaylistSaveService`
- `RecommendationService`
- `SpotifyOAuthService`

추가 확인 대상:

- `PlaylistExportService`: Mapper를 직접 호출하지 않고 검증이 보완된 `PlaylistService`, `PlaylistItemService`, `SpotifyOAuthService`에 상태 변경을 위임한다.
- `SpotifyService`: DB 저장을 `MusicService`에 위임한다.
- `AuthService`: DB 변경 Mapper를 호출하지 않고 Session 변경과 회원 조회만 수행한다.
- 회원 정보 수정·탈퇴·상태 변경: 현재 `MemberService`에는 회원가입과 내 정보 조회만 있으며 수정·탈퇴 Mapper 메서드는 구현되어 있지 않다.

## 3. 영향 행 수 분류 결과

| Service | 메서드 | Mapper | SQL 유형 | 기대 건수 | 0건 허용 여부 | 조치 |
| --- | --- | --- | --- | ---: | --- | --- |
| MemberService | register | MemberMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| RunningProfileService | insertNew | RunningProfileMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| RunningProfileService | updateExisting | RunningProfileMapper.update | UPDATE | 1 | 불가 | `PROFILE_NOT_FOUND` |
| RunningRecordService | createRecord | RunningRecordMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| RunningRecordService | updateMyRecord | RunningRecordMapper.update | UPDATE | 1 | 불가 | `RUNNING_RECORD_NOT_FOUND` |
| RunningRecordService | deleteMyRecord | RunningRecordMapper.delete | soft delete | 1 | 불가 | `RUNNING_RECORD_NOT_FOUND` |
| RunningSessionService | createSession | RunningSessionMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| RunningSessionService | updateMySession | RunningSessionMapper.update | UPDATE | 1 | 불가 | 기존 검증을 `!= 1`로 강화 |
| RunningSessionService | changeMySessionStatus | RunningSessionMapper.updateStatus | 상태 UPDATE | 1 | 불가 | 기존 검증을 `!= 1`로 강화 |
| RunningSessionService | cancelMySession | RunningSessionMapper.cancel | 취소 UPDATE | 1 | 불가 | 기존 검증을 `!= 1`로 강화 |
| RunningSessionParticipantService | joinSession | ParticipantMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| RunningSessionParticipantService | cancelMyParticipation | ParticipantMapper.delete | DELETE | 1 | 불가 | 기존 검증을 `!= 1`로 강화 |
| MusicService | saveSelectedSpotifyTrack | MusicMapper.insertFromSpotify | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| MusicService | enrichExistingMusic | MusicMapper.updateSpotifyMetadata | UPDATE | 1 | 불가 | `MUSIC_NOT_FOUND` |
| MusicReviewService | create | MusicReviewMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| MusicReviewService | update | MusicReviewMapper.update | UPDATE | 1 | 불가 | `MUSIC_REVIEW_NOT_FOUND` |
| MusicReviewService | delete | MusicReviewMapper.delete | DELETE | 1 | 불가 | `MUSIC_REVIEW_NOT_FOUND` |
| PlaylistService | create | PlaylistMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| PlaylistService | update | PlaylistMapper.update | UPDATE | 1 | 불가 | `PLAYLIST_NOT_FOUND` |
| PlaylistService | delete | PlaylistMapper.softDelete | soft delete | 1 | 불가 | `PLAYLIST_NOT_FOUND` |
| PlaylistService | updateSpotifyExportStatus | PlaylistMapper.updateSpotifyExportStatus | 상태 UPDATE | 1 | 불가 | 기존 검증을 `!= 1`로 강화 |
| PlaylistService | saveSpotifyExportPending | PlaylistMapper.updateSpotifyExportPending | UPDATE | 1 | 불가 | `SPOTIFY_EXPORT_FAILED` |
| PlaylistService | saveSpotifyExportFailure | PlaylistMapper.updateSpotifyExportFailure | UPDATE | 1 | 불가 | `SPOTIFY_EXPORT_FAILED` |
| PlaylistService | saveSpotifyExportResult | PlaylistMapper.updateSpotifyExportResult | UPDATE | 1 | 불가 | `SPOTIFY_EXPORT_FAILED` |
| PlaylistItemService | add | PlaylistItemMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| PlaylistItemService | delete | PlaylistItemMapper.delete | DELETE | 1 | 불가 | `PLAYLIST_ITEM_NOT_FOUND` |
| PlaylistItemService | updateOrder | PlaylistItemMapper.updateOrder | UPDATE | 1 | 불가 | `PLAYLIST_ITEM_NOT_FOUND` |
| PlaylistSaveService | save | PlaylistSaveMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| PlaylistSaveService | cancel | PlaylistSaveMapper.delete | DELETE | 1 | 불가 | `ACCESS_DENIED` |
| RecommendationService | recommend | RecommendationLogMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| RecommendationService | saveRecommendation | PlaylistMapper.insertCurationCopy | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| RecommendationService | saveRecommendation | PlaylistItemMapper.bulkInsertFromRecommendation | 다건 INSERT | 요청 곡 수 | 불가 | 실제 건수와 `items.size()` 비교 |
| RecommendationService | saveRecommendation | RecommendationLogMapper.updateSavedPlaylistId | UPDATE | 1 | 불가 | 기존 `!= 1` 검증 유지 |
| SpotifyOAuthService | disconnect | SpotifyConnectionMapper.deleteByMemberId | DELETE | 1 | 불가 | 기존 검증을 `!= 1`로 강화 |
| SpotifyOAuthService | saveConnection 신규 | SpotifyConnectionMapper.insert | INSERT | 1 | 불가 | `INTERNAL_SERVER_ERROR` |
| SpotifyOAuthService | saveConnection 갱신 | SpotifyConnectionMapper.updateConnection | UPDATE | 1 | 불가 | `SPOTIFY_AUTH_REQUIRED` |
| SpotifyOAuthService | refreshConnection | SpotifyConnectionMapper.updateToken | UPDATE | 1 | 불가 | `SPOTIFY_AUTH_REQUIRED` |

점검 결과:

- 신규 단건 영향 행 수 검증: 30건
- 기존 `0` 검증을 정확히 `1` 검증으로 강화: 6건
- 다건 기대 건수 검증: 1건
- Mapper write가 실제 호출된 뒤 0건을 정상으로 허용한 항목: 없음

## 4. 수정 내용

### 변경 파일

Service:

- `MemberService.java`
- `RunningProfileService.java`
- `RunningRecordService.java`
- `RunningSessionService.java`
- `RunningSessionParticipantService.java`
- `MusicService.java`
- `MusicReviewService.java`
- `PlaylistService.java`
- `PlaylistItemService.java`
- `PlaylistSaveService.java`
- `RecommendationService.java`
- `SpotifyOAuthService.java`

테스트:

- 각 변경 Service의 기존 테스트 성공 stub을 실제 성공 영향 행 수 `1`로 명시
- `RunningRecordServiceTest`
- `RunningSessionServiceTest`
- `PlaylistItemServiceTest`
- `PlaylistSaveServiceTest`
- `RecommendationServiceTest`

### 수정 전 문제

Service가 사전 조회로 존재·소유권을 확인한 후 Mapper UPDATE/DELETE 결과를 무시했다. 조회와 변경 사이에 다른 요청이 대상을 삭제하거나 상태를 변경하면 실제 변경 건수는 0이지만 성공 객체 또는 정상 응답을 반환할 수 있었다.

### 수정 후 검증 방식

- 단건 INSERT, UPDATE, DELETE: 영향 행 수가 정확히 1인지 확인
- 추천 곡 다건 INSERT: 요청한 곡 수와 실제 반영 건수 비교
- 기존 상태 조건 SQL: 영향 행 수가 1이 아니면 상태 충돌 예외
- 모든 검증 예외는 `CustomException`이며 RuntimeException 계열이므로 `@Transactional` 범위에서 롤백된다.

### 사용한 예외 및 ErrorCode

- 대상 엔티티 소실: 각 `*_NOT_FOUND`
- 러닝 모임 상태 충돌: `SESSION_CLOSED`
- 저장 관계 동시 취소: `ACCESS_DENIED`
- Spotify 연결 소실: `SPOTIFY_AUTH_REQUIRED`
- Spotify Export 상태 저장 실패: `SPOTIFY_EXPORT_FAILED`
- 생성 또는 내부 복사에서 비정상적인 0건: `INTERNAL_SERVER_ERROR`

## 5. 0건을 정상으로 유지한 항목

실제 Mapper write 호출 결과가 0인 경우를 정상으로 유지한 항목은 없다. 이번 범위의 변경 작업은 모두 단건 필수 또는 전체 성공이 필요한 트랜잭션이었다.

다만 다음 조건부 무변경 흐름은 Mapper write 자체를 호출하지 않으며 기존 정책을 유지했다.

| 메서드 | 정상인 이유 | 정책 근거 |
| --- | --- | --- |
| MusicService.saveSelectedSpotifyTrack | 기존 음악의 보강할 필드가 없으면 UPDATE 불필요 | 저장 데이터가 이미 완전함 |
| SpotifyOAuthService.getValidConnection | 토큰이 유효하면 token UPDATE 불필요 | 불필요한 갱신 방지 |
| PlaylistExportService.addTracksInBatches | 외부 Spotify API가 반환 건수를 제공하지 않는 void 계약 | DB Mapper 영향 행 수 대상 아님 |

중복 INSERT는 `DuplicateKeyException`을 기존 도메인 예외로 변환하는 정책을 그대로 유지했다.

## 6. 테스트 결과

### 추가된 테스트

- 러닝 기록 수정 직전 soft delete로 UPDATE 0건
- 러닝 기록 삭제 직전 다른 요청의 soft delete로 DELETE 0건
- 러닝 모임 상태가 동시 변경되어 상태 UPDATE 0건
- 플레이리스트 수록곡 동시 삭제로 DELETE 0건
- 삭제된 수록곡의 순서 UPDATE 0건
- 저장 관계의 동시 취소로 DELETE 0건
- 추천 곡 벌크 INSERT가 요청 2건 중 1건만 반환한 경우

검증 내용:

- 엔티티별 기존 ErrorCode 발생
- 추천 로그 연결 Mapper 미호출
- RuntimeException 기반 트랜잭션 롤백 가능
- P0 참여 트랜잭션의 `FOR UPDATE` 우선 순서 유지

### 실제 MySQL 테스트 결과

- MySQL: 8.0.32
- DB: `ssafit_test`
- Engine: InnoDB
- Isolation: `REPEATABLE-READ`
- Mapper Integration Test: 13건 실제 실행
- 테스트 종료 후 대상 테이블 잔존 데이터: 0건

```text
Tests run: 205
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

실제 MySQL 테스트의 추천 저장 실패 시 플레이리스트 및 추천 로그 연결이 롤백되는 기존 시나리오도 계속 통과했다.

## 7. develop 병합 영향

- 기존 병합 가능 판정: 유지

근거:

- 변경 계열 Service 전체 조사 완료
- 단건 필수 변경의 실제 영향 행 수 검증 완료
- 다건 추천 복사의 기대 건수 검증 완료
- 정상 무변경 흐름과 0건 실패 흐름 분리
- P0 러닝 모임 참여 잠금 흐름 유지
- 실제 MySQL `REPEATABLE-READ` 전체 테스트 205건 통과
- Failures, Errors, Skipped 모두 0
- 테스트 데이터 정리 완료

남은 후속 개선 항목:

- 새로운 회원 정보 수정·탈퇴 기능이 추가될 경우 동일한 영향 행 수 검증 정책 적용
- 외부 Spotify API는 영향 행 수를 제공하지 않으므로 별도 응답 검증 및 재시도 정책 범위에서 관리

