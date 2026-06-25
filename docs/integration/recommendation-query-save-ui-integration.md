# 자동 큐레이션 조회·저장 UI 연동 보고

## 1. 적용 범위

- 자동 큐레이션 조회
- 내 추천 로그 조회
- 추천 로그 기반 개인 플레이리스트 생성
- 조회와 저장의 상태 및 API 책임 분리
- 프로필 없음, 추천 데이터 부족, 잘못된 조건, 재저장 오류 처리

Backend Controller, DTO, Service, Mapper, SQL, DDL은 수정하지 않았다.

## 2. 실제 API 계약

| 기능 | Method / URI | 요청 | 응답 |
| --- | --- | --- | --- |
| 추천 조회 | `GET /api/recommendations` | query `intensity`, 선택 `genre`, 선택 `size` | `{ log, music }` |
| 내 추천 로그 | `GET /api/recommendations/logs/me` | 없음 | `List<LogResponse>` |
| 개인 플레이리스트 생성 | `POST /api/recommendations/{logId}/save` | body `title`, `visibility` | `{ playlist }` |

세 API 모두 Session 로그인이 필요하다. 추천 조회에는 러닝 프로필이 추가로 필요하다.

Vue 호출은 모두 `frontend/src/api/recommendationApi.js`를 거친다. 추천 화면과 결과 컴포넌트에서 Axios 또는 `playlistSaveApi`를 직접 사용하지 않는다.

## 3. 조회 API와 저장 API의 분리

### 조회

```text
RecommendationView
→ recommendationApi.recommend(condition)
→ GET /api/recommendations
→ RecommendationService.recommend
→ 추천 음악 조회
→ recommendation_logs INSERT
→ ResultResponse(log, music)
```

조회 경로는 `PlaylistMapper.insertCurationCopy`와 `PlaylistItemMapper.bulkInsertFromRecommendation`을 호출하지 않는다.

새 조회를 시작할 때 다음 상태를 먼저 초기화한다.

- 이전 추천 결과
- 이전 `logId`
- 이전 저장 성공 결과
- 생성된 플레이리스트 정보
- 저장 안내 메시지

조회 요청 순번을 기록해 이전 요청의 늦은 응답이 새 결과를 덮어쓰지 못하게 한다. 조회 중에는 버튼을 비활성화한다.

### 저장

```text
RecommendationResult
→ RecommendationView.createCurationCopy
→ recommendationApi.save(logId, { title, visibility })
→ POST /api/recommendations/{logId}/save
→ RecommendationService.saveRecommendation
→ USER + CURATION_COPY playlists INSERT
→ 추천 musicIds를 playlist_items bulk INSERT
→ recommendation_logs.saved_playlist_id 연결
```

저장 성공 응답을 받은 뒤에만 생성된 플레이리스트 정보와 상세 이동 링크를 표시한다.

## 4. 조회 직후 플레이리스트 미생성 검증

실제 Service 코드와 기존 테스트를 대조했다.

- `RecommendationService.recommend`는 음악 조회와 추천 로그 insert만 수행한다.
- `RecommendationServiceTest.recommendationQueryDoesNotCreatePlaylist`는 조회 중 `insertCurationCopy`와 `bulkInsertFromRecommendation`이 호출되지 않았음을 검증한다.
- `MapperIntegrationTest.recommendationQueryCreatesOnlyLogAndSaveCopiesItems`는 실제 DB 기준 조회 전후 `playlists` 행 수가 같고 `recommendation_logs`만 증가함을 검증한다.

Vue는 조회 응답의 `log.id`와 음악 목록만 보관하며 조회 직후 플레이리스트 목록 API를 갱신하거나 생성 상태를 표시하지 않는다.

화면에는 다음 문구를 항상 표시한다.

> 조회만으로는 플레이리스트가 생성되지 않습니다.

## 5. `logId` 보관 및 저장 전달

- 조회 응답의 `result.log.id`를 결과 상태에 보관한다.
- 유효한 1 이상의 정수 `logId`가 있을 때만 저장 버튼을 활성화한다.
- 저장 시 path parameter로 `POST /api/recommendations/{logId}/save`에 전달한다.
- 더미 `logId`, 임시 성공 상태, localStorage 저장을 사용하지 않는다.

## 6. CURATION_COPY 결과 표시

실제 `SaveResponse`는 `PlaylistResponse playlist`만 제공한다.

저장 성공 후 다음 응답 필드만 표시한다.

- `playlist.id`
- `playlist.ownerId`
- `playlist.title`
- `playlist.playlistType`
- `playlist.sourceType`
- `playlist.visibility`

수록곡 수나 상세 URL 문자열은 저장 응답에 없으므로 임의 계산하거나 응답 필드인 것처럼 표시하지 않는다. Router의 playlist ID 경로를 사용해 상세 화면으로 이동하며, 개인 목록 링크는 `ownerId` query를 사용하는 플레이리스트 목록으로 연결한다.

## 7. `playlist_saves`와의 분리

- 자동 큐레이션 결과 생성은 `recommendationApi.save`만 사용한다.
- `playlistSaveApi`, `/api/playlist-saves/**`, `SavedPlaylistView`를 호출하거나 이동 대상으로 사용하지 않는다.
- UI는 “참조 저장” 대신 “개인 플레이리스트 생성”, “CURATION_COPY 생성”으로 표현한다.
- 최근 추천 로그의 `savedPlaylistId`는 참조 저장 상태가 아니라 생성된 개인 플레이리스트 연결 ID로 표시한다.

## 8. 실제 계약과 문서·Prompt 차이

### BPM 요청 필드

Prompt 06은 목표 강도와 BPM 조건 입력을 요구하지만 실제 `RecommendRequest`와 Controller는 `intensity`, `genre`, `size`만 받는다. `RecommendationService`는 러닝 프로필의 유효한 BPM 범위를 우선 사용하고, 없으면 강도별 기본 범위를 사용한다.

프론트엔드는 미지원 BPM query parameter를 보내지 않는다. 선택 강도의 기본 범위를 안내하고, 조회 후 `LogResponse.requestedBpmMin`, `requestedBpmMax`로 실제 적용 범위를 표시한다.

### 저장 Request Body

실제 저장 Controller는 Body가 필요하다.

```json
{
  "title": "선택 입력",
  "visibility": "PRIVATE 또는 PUBLIC"
}
```

제목이 비어 있으면 Service가 `자동 큐레이션 - {강도}` 기본 제목을 사용하고, 공개 범위가 없으면 `PRIVATE`를 사용한다. 프론트엔드는 공개 범위를 명시적으로 전송한다.

### 저장 응답 정보

저장 응답에는 생성된 `PlaylistResponse`가 있지만 수록곡 개수와 별도 상세 URL 필드는 없다. 필요한 수록곡 정보는 생성된 플레이리스트 상세 API에서 확인해야 한다.

### 로그 응답의 JSON 문자열

`requestConditions`와 `resultSummary`는 구조화 객체가 아니라 JSON 문자열로 노출된다. 화면은 이 문자열을 업무 상태 판단에 사용하지 않고 명시 필드와 `savedPlaylistId`를 사용한다.

## 9. 중복 저장 정책

동일 `logId`는 한 번만 저장할 수 있다.

- 저장 전 `savedPlaylistId`가 있으면 `RECOMMENDATION_ALREADY_SAVED`
- 동시 요청으로 로그 연결 update가 0건이어도 `RECOMMENDATION_ALREADY_SAVED`
- HTTP status는 `409 Conflict`
- 새 CURATION_COPY를 반복 생성하는 정책이 아니다.

저장 성공 후 현재 결과의 저장 버튼을 비활성화한다. 저장 실패는 성공 상태로 바꾸지 않으며, `409` 발생 시 로그 목록을 다시 조회해 Backend 상태를 확인한다.

## 10. 오류 처리

| ErrorCode / status | 화면 처리 |
| --- | --- |
| `AUTH_REQUIRED`, 401 | Router Guard 또는 로그인 필요 상태 |
| `PROFILE_NOT_FOUND` | JSP 러닝 프로필 작성 경로와 현재 복귀 URL 안내 |
| `INVALID_INTENSITY`, 400 | 강도 재선택 안내 |
| `INVALID_REQUEST`, 400 | 곡 수와 조건 재확인 |
| `RECOMMENDATION_DATA_NOT_ENOUGH` | 장르 완화 또는 다른 강도 안내 |
| `ACCESS_DENIED`, 403 | 다른 회원 로그 저장 권한 없음 |
| `RECOMMENDATION_LOG_NOT_FOUND`, 404 | 추천 재조회 안내 |
| `RECOMMENDATION_ALREADY_SAVED`, 409 | 이미 생성된 추천 안내 및 로그 재조회 |
| 500 이상 | 잠시 후 재시도 안내 |

## 11. 팀원 A 확인 요청 사항

1. 자동 큐레이션 조회 UI에서 직접 BPM 입력을 최종 제외하고 프로필 BPM만 사용할지 확정이 필요하다.
2. `LogResponse.requestConditions`, `resultSummary`를 JSON 문자열로 계속 노출할지 구조화 DTO로 변경할지 확인이 필요하다.
3. 저장 응답에 수록곡 개수 또는 상세 DTO가 필요한지 확인이 필요하다. 현재는 생성된 playlist 정보만 제공한다.
4. 저장 요청의 `title`, `visibility`가 nullable인 현재 계약을 유지할지 명시가 필요하다.
5. 추천 로그 목록에 전체 `memberId`를 노출할 필요가 있는지 검토가 필요하다.
6. 예상하지 못한 저장 동시성 오류가 모두 `RECOMMENDATION_ALREADY_SAVED`로 안정적으로 매핑되는지 실제 MySQL 환경에서 재검증이 필요하다.
7. JSP 러닝 프로필 Controller는 현재 `returnTo` query를 소비하거나 저장 후 Vue 경로로 복귀시키지 않는다. 프로필 저장 후 자동 큐레이션 복귀 정책을 확정해야 한다.

## 12. 검증 상태

완료:

- 정상 추천 조회 후 `logId=100`, 추천 사유, 음악 2곡, 실제 적용 BPM 표시
- 조회 직후 테스트 상태 기준 `recommendation_logs=1`, `playlists=0`, `playlist_items=0` 확인
- 저장 후 `USER + CURATION_COPY` 플레이리스트 생성 정보 표시
- 저장 후 테스트 상태 기준 `playlists=1`, `playlist_items=2` 확인
- 생성 결과 상세 화면 이동 및 수록곡 2곡 표시
- 최근 추천 로그가 `savedPlaylistId` 기반으로 미생성/생성 상태를 구분하는지 확인
- 프로필 없음 오류와 JSP 러닝 프로필 `returnTo` 링크 확인
- 추천 데이터 부족 오류 안내 확인
- 곡 수 51 입력이 API 호출 전에 차단되는지 확인
- 동일 `logId` 재저장 시 `409 RECOMMENDATION_ALREADY_SAVED` 확인
- 브라우저 console error/warning 없음
- Frontend `npm run build` 성공

미완료 또는 제한 검증:

- Backend `mvnw test`: 현재 Java 17이 프로젝트의 `release 25`를 지원하지 않아 컴파일 시작 단계에서 실패
- 실제 JSP 로그인 Session Cookie와 MySQL 데이터를 사용하는 Controller 통합 호출
- 실제 MySQL에서 조회 직후 행 수와 저장 트랜잭션을 이번 환경에서 재실행하지 못함
- 기존 `MapperIntegrationTest`에는 조회 시 playlist 행 수 불변, 저장 시 수록곡 복사, 실패 시 rollback 검증이 존재함
- 직접 BPM 최소·최대 입력 검증은 실제 조회 DTO에 해당 필드가 없어 수행 대상에서 제외
