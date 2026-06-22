# 음악 목록·상세·리뷰 UI 연동 기록

## 1. 적용 계약

다음 구현을 함께 대조했다.

- `docs/integration/b-ui-api-contract-audit.md`
- `docs/integration/b-ui-api-contract.md`
- `MusicApiController`, `MusicReviewApiController`
- `MusicApiDtos`, `MusicReviewApiDtos`
- `MusicService`, `MusicReviewService`
- `MusicMapper`, `MusicReviewMapper`와 XML
- 음악·리뷰 Service/Controller 테스트

Backend Controller, Service, Mapper, Domain, DDL은 수정하지 않았다.

## 2. 연결 API

| 기능 | Method | URI | Request | Response |
| --- | --- | --- | --- | --- |
| 음악 목록 | GET | `/api/music` | `keyword`, `genre`, `minBpm`, `maxBpm`, `runningIntensity`, `sortType`, `page`, `size` | `PageResponse<MusicResponse>` |
| 음악 상세 | GET | `/api/music/{musicId}` | path `musicId` | `DetailResponse` |
| 리뷰 목록 | GET | `/api/music/{musicId}/reviews` | path `musicId` | `List<ReviewResponse>` |
| 리뷰 작성 | POST | `/api/music/{musicId}/reviews` | `CreateRequest` | `ReviewResponse` |
| 리뷰 수정 | PUT | `/api/music-reviews/{reviewId}` | `UpdateRequest` | `ReviewResponse` |
| 리뷰 삭제 | DELETE | `/api/music-reviews/{reviewId}` | 없음 | `Void` |

상세 응답에 평균과 리뷰 목록이 포함되므로 화면은 별도 리뷰 목록 API를 중복 호출하지 않는다.

## 3. 목록 조건 유지

- 하나의 조회 객체에서 검색어, 장르, BPM 범위, 강도, 정렬, 페이지, 크기를 관리한다.
- 조회 조건은 Router query에 저장한다.
- 새로고침과 브라우저 뒤로 가기 시 Router query를 다시 정규화해 복원한다.
- 검색·필터·정렬 변경은 `page=1`로 초기화한다.
- 페이지 이동은 나머지 검색 조건을 그대로 유지한다.
- 상세 링크에 현재 목록 경로를 `returnTo`로 전달해 목록 복귀 시 페이지와 조건을 복원한다.
- 숫자가 아니거나 비어 있는 BPM은 API query에서 제외한다.
- BPM은 60~220 정수인지 확인하고 최소값이 최대값보다 크면 API 호출 전에 차단한다.
- 페이지 응답 필드가 없거나 `items`가 배열이 아니어도 기본값으로 정규화한다.

## 4. 리뷰 상태와 권한

- 현재 로그인 사용자는 `GET /api/members/me` 응답의 `id`로 식별한다.
- `String(member.id) === String(review.memberId)` 비교 결과가 같은 리뷰에만 수정·삭제 버튼을 표시한다.
- 비로그인 401은 상세 자체의 404와 분리해 JSP 로그인 링크를 표시한다.
- 로그인 확인 실패 시 로그인 상태를 추정하지 않고 작성·수정 UI를 숨긴다.
- 작성·수정·삭제 성공 후 `GET /api/music/{musicId}`를 한 번 재조회한다.
- 이 재조회로 평균 점수, 리뷰 개수, 리뷰 목록을 같은 시점 데이터로 갱신한다.
- 화면의 버튼 노출과 무관하게 서버 403, 404, 409를 ErrorCode 우선 메시지로 처리한다.

## 5. 계약 불일치 사항

1. 계약 감사 보고서의 Controller/JSP/Vue 미구현 표는 감사 당시 기록이며 현재 실제 브랜치 상태와 다르다.
2. 목표 오류 표현 중 `errorCode`가 있으나 실제 Backend 오류 DTO는 `{code, message}`다. 공통 Client가 두 필드를 모두 수용한다.
3. `ReviewResponse`에는 작성자 ID만 있고 닉네임 또는 표시명이 없다. 현재 UI는 회원 번호만 표시한다.
4. `runningIntensity`는 Mapper 조건으로 직접 전달되지 않는다. 실제 Service는 명시적 BPM 조건이 없을 때 강도를 BPM 범위로 변환한다.
5. 음악 상세 Controller는 음악 조회 후 리뷰 목록과 평균 Service를 호출하며, 두 리뷰 Service 메서드도 각각 음악 존재를 재확인한다. 한 상세 요청에서 음악 존재 조회가 중복될 수 있다.
6. `GET /api/music/{musicId}`에 리뷰 목록이 이미 포함되지만 별도 리뷰 목록 API도 존재한다. 화면은 상세 API를 우선 사용한다.
7. `GlobalExceptionHandler`에는 예상하지 못한 예외를 공통 500 JSON으로 변환하는 catch-all 처리가 없다.
8. JSP 로그인 Controller는 Vue가 전달하는 `returnTo`를 아직 복원하지 않는다.

## 6. 처리 ErrorCode

- `INVALID_BPM_VALUE`, `INVALID_BPM_RANGE`, `INVALID_REQUEST`
- `AUTH_REQUIRED`
- `ACCESS_DENIED`
- `MUSIC_NOT_FOUND`, `MUSIC_REVIEW_NOT_FOUND`
- `DUPLICATE_MUSIC_REVIEW`
- `INVALID_SCORE`
- 알 수 없는 500 이상 오류

## 7. 검증 결과

완료:

- 검색 후 정렬 변경과 `page=1` 초기화
- 검색·장르·BPM·정렬 조건을 유지한 페이지 이동
- 목록 → 상세 → 기존 목록 query 복귀
- 존재하지 않는 음악의 404 `MUSIC_NOT_FOUND`
- 비로그인 상세의 리뷰 작성 차단과 JSP 로그인 링크
- 본인 리뷰 작성·수정·삭제 후 상세 재조회 및 평균 변경
- 409 `DUPLICATE_MUSIC_REVIEW` 메시지
- 403 `ACCESS_DENIED` 메시지
- BPM 최소·최대 역전의 API 호출 전 검증
- Spotify 기반 표시와 외부 URL이 있는 곡에만 `target="_blank"`, `rel="noopener noreferrer"` 링크 제공
- 페이지 응답 정규화 유틸의 결측 필드 방어
- Frontend production build

실데이터 기준 미검증:

- MySQL의 실제 음악 데이터와 FULLTEXT 검색 결과
- JSP 로그인 Session Cookie를 이용한 실제 리뷰 CRUD
- 실제 동시 리뷰 작성의 DB UNIQUE 충돌
- 실제 Spotify 외부 URL의 유효성

Backend build:

- 프로젝트 설정은 Java 25다.
- 현재 기본 Java 17에서는 `release version 25 not supported`로 실패한다.
- 설치된 Java 26으로도 Lombok annotation processing 중 `Cannot close compiler resources`가 발생해 컴파일되지 않았다.
- Java 25 런타임이 현재 환경에 없어 Backend 전체 테스트와 package 성공을 재현하지 못했다.

## 8. 팀원 A 확인 요청 사항

1. 리뷰 화면에 작성자 닉네임이 필요하다면 `ReviewResponse`에 안전한 표시명을 추가할지 확정이 필요하다.
2. 음악 상세 요청에서 반복되는 음악 존재 확인 조회를 유지할지, Service 조합을 최적화할지 확인이 필요하다.
3. 별도 리뷰 목록 API와 상세 응답 내 리뷰 목록을 모두 유지할지 계약을 확정해야 한다.
4. 예상하지 못한 500 오류도 `{code, message}` JSON으로 반환할지 결정이 필요하다.
5. `runningIntensity`가 명시적 BPM 조건보다 후순위인 현재 Service 의미를 UI 계약으로 확정해야 한다.
6. `MusicReviewApiController`의 401, 403, 404, 409 응답을 직접 검증하는 Controller 테스트 보완이 필요하다.
