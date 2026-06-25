# 플레이리스트 CRUD·수록곡·참조 저장 UI 연동 보고

## 1. 적용 범위

- 플레이리스트 목록, 생성, 수정, 상세, 삭제
- 플레이리스트 수록곡 추가, 삭제, 순서 변경
- `playlist_saves` 기반 참조 저장 및 내 저장 목록
- 소유자, 공식 플레이리스트, 공개 범위에 따른 UI 분기
- Spotify 검색·내보내기 다음 단계 연결 지점

Backend Controller, DTO, Service, Mapper, SQL, DDL은 변경하지 않았다.

## 2. 실제 API 계약

| 기능 | Method / URI | 요청 | 주요 응답 |
| --- | --- | --- | --- |
| 목록 | `GET /api/playlists` | 선택 query `ownerId` | `List<PlaylistResponse>` |
| 상세 | `GET /api/playlists/{playlistId}` | 없음 | `{ playlist, items }` |
| 생성 | `POST /api/playlists` | `title`, `description`, `visibility` | `PlaylistResponse` |
| 수정 | `PUT /api/playlists/{playlistId}` | `title`, `description`, `visibility` | `PlaylistResponse` |
| 삭제 | `DELETE /api/playlists/{playlistId}` | 없음 | data 없음 |
| 수록곡 목록 | `GET /api/playlists/{playlistId}/items` | 없음 | `List<ItemResponse>` |
| 수록곡 추가 | `POST /api/playlists/{playlistId}/items` | `musicId`, `orderNo` | `ItemResponse` |
| 순서 변경 | `PATCH /api/playlist-items/{itemId}/order` | `orderNo` | `ItemResponse` |
| 수록곡 삭제 | `DELETE /api/playlist-items/{itemId}` | 없음 | data 없음 |
| 내 저장 목록 | `GET /api/playlist-saves/me` | 없음 | `List<PlaylistResponse>` |
| 참조 저장 | `POST /api/playlist-saves/{playlistId}` | 없음 | `SaveResponse` |
| 저장 취소 | `DELETE /api/playlist-saves/{playlistId}` | 없음 | data 없음 |

모든 Vue 호출은 `src/api/playlistApi.js` 또는 `src/api/playlistSaveApi.js`를 거치며, 컴포넌트가 Axios를 직접 호출하지 않는다.

## 3. 계약 불일치 사항

### 3.1 목표 강도와 BPM 범위

Prompt 05에는 생성·수정 요청의 목표 강도와 BPM 범위가 언급되어 있으나, 실제 `PlaylistApiDtos.CreateRequest`, `UpdateRequest`, `Playlist` Domain, `playlists` DDL에는 해당 필드가 없다.

프론트엔드는 저장되지 않는 입력을 만들거나 임의 필드를 전송하지 않는다. 폼에는 현재 Backend 계약에서 지원하지 않는다는 안내만 표시한다.

### 3.2 상세 응답의 저장 여부

`PlaylistResponse`에는 `saved` 필드가 있지만, 상세 조회 Mapper는 현재 조회자 기준 저장 여부를 계산하지 않는다. 따라서 상세 응답의 `saved`만으로 저장 상태를 판단하면 실제 상태와 다를 수 있다.

로그인 사용자의 상세 화면에서는 `GET /api/playlist-saves/me`를 추가 조회해 동일한 playlist ID가 있는지 확인한다. 이 조회가 실패하면 저장됨으로 추측하지 않고 저장 상태 미확정 안내를 표시한다.

### 3.3 본인 비공개 플레이리스트 참조 저장

문서 설명은 공개 또는 공식 플레이리스트 참조 저장을 중심으로 하지만, 실제 `PlaylistSaveService`는 소유자 본인의 비공개 플레이리스트 저장도 허용한다. 프론트엔드는 실제 Service 동작에 맞춰 본인 비공개 플레이리스트에서 저장 버튼을 제공하되, 이 차이를 화면과 본 문서에 명시한다.

### 3.4 수록곡 순서 맞바꾸기

실제 API는 수록곡 하나의 `orderNo`만 변경하며, DB에는 `(playlist_id, order_no)` UNIQUE 제약이 있다. 두 곡의 순서를 원자적으로 맞바꾸는 API나 일괄 재정렬 계약은 없다.

프론트엔드는 지원되지 않는 위/아래 맞바꾸기를 성공한 것처럼 구현하지 않고, 비어 있는 순서 번호로 변경하는 UI를 제공한다. 이미 사용 중인 번호는 `DUPLICATE_PLAYLIST_ORDER`로 안내한다.

### 3.5 목록 응답

목록 API는 `ownerId` 필터만 지원하고 검색, 정렬, 페이지 메타데이터는 제공하지 않는다. 화면은 존재하지 않는 페이지 정보를 추측하지 않고 반환된 배열을 그대로 렌더링한다.

## 4. 화면 상태 및 권한 처리

- 공통 상태 영역으로 `loading`, `empty`, `error`, `unauthorized`, `loginRequired`를 구분한다.
- 소유자 판별은 Session 조회 응답의 `currentMember.id`와 `playlist.ownerId`를 문자열로 정규화해 비교한다.
- 수정, 삭제, 수록곡 관리는 `소유자 && 비공식 플레이리스트`일 때만 노출한다.
- 버튼을 숨겨도 서버의 `401`, `403` 검증을 신뢰하며, 실패 응답을 별도로 안내한다.
- 비공개 플레이리스트를 비소유자가 조회하면 실제 Service의 `PRIVATE_PLAYLIST(403)`을 권한 없음 상태로 표시한다.
- 존재하지 않는 플레이리스트의 `PLAYLIST_NOT_FOUND(404)`는 비공개 접근 오류와 분리한다.
- 삭제는 곡명 또는 플레이리스트 제목을 포함한 확인 절차 후 호출하며, 성공 응답을 받은 뒤에만 이동하거나 재조회한다.

## 5. 상태 갱신 방식

- 플레이리스트 생성 성공: 생성 응답 ID의 상세 화면으로 이동
- 플레이리스트 수정 성공: 수정 응답 ID의 상세 화면으로 이동
- 수록곡 추가·삭제·순서 변경 성공: 상세 API와 저장 목록을 재조회
- 참조 저장·취소 성공: 상세 API와 내 저장 목록을 재조회
- 삭제 성공: 기존 목록 복귀 경로 또는 플레이리스트 목록으로 이동

수록곡 수, 순서, 저장 상태를 클라이언트 배열에서 임의 추정하지 않고 서버 응답으로 다시 동기화한다.

## 6. `playlist_saves`와 자동 큐레이션 저장 구분

- `playlist_saves`: 기존 플레이리스트를 내 저장 목록에 참조 연결
- 자동 큐레이션 저장: 추천 로그를 `USER + CURATION_COPY` 플레이리스트로 새로 생성
- Spotify 내보내기: 소유한 플레이리스트를 Spotify 계정으로 전송

세 기능은 API, 상태, 안내 문구를 분리했다. 참조 저장 성공을 개인 복사본 생성이나 Spotify 내보내기 성공으로 표시하지 않는다.

## 7. Spotify 다음 단계 연결 지점

- 상세 화면에 실제 `spotifyExportStatus`, `spotifyExportErrorCode`, `spotifyPlaylistUrl` 표시 영역을 둔다.
- 실제 URL이 있을 때만 새 창 링크를 노출하며 `noopener noreferrer`를 적용한다.
- 소유자이며 비공식 플레이리스트인 경우에만 `spotify-search` Router로 이동하는 연결점을 제공한다.
- query에 `playlistId`와 `returnTo`를 보존해 곡 선택 또는 OAuth 완료 후 상세 화면으로 복귀할 수 있다.
- 임시 Spotify URL, 더미 연결 상태, 성공한 것처럼 보이는 Mock 데이터는 사용하지 않는다.

## 8. 팀원 A 확인 요청 사항

1. 생성·수정 DTO에 목표 강도와 BPM 범위를 추가할지, Prompt 요구에서 제외할지 확정이 필요하다.
2. 상세 조회의 `PlaylistResponse.saved`를 조회자 기준으로 계산할지 확정이 필요하다.
3. 본인 비공개 플레이리스트의 `playlist_saves` 허용 정책을 문서 계약에 반영할지 확인이 필요하다.
4. 위/아래 이동 UI를 안전하게 지원하려면 두 항목 교환 또는 전체 순서 일괄 변경 API가 필요하다.
5. 저장 취소 대상이 없을 때 현재 `ACCESS_DENIED`를 사용하는 정책이 `PLAYLIST_SAVE_NOT_FOUND` 같은 별도 ErrorCode보다 적절한지 확인이 필요하다.
6. 공식 플레이리스트와 사용자 플레이리스트의 저장 정책 및 관리자 편집 정책을 최종 확정해야 한다.

## 9. 검증 한계

- 실제 Session Cookie, 사용자 두 명, 공개·비공개 데이터, Spotify URI가 있는 음악 데이터가 필요한 통합 흐름은 실행 환경이 준비되어야 완전 검증할 수 있다.
- Backend 빌드는 프로젝트가 요구하는 Java 25가 필요하다. 현재 확인 가능한 Java 17은 버전 미달이고 Java 26에서는 Lombok 컴파일러 호환 오류가 발생하므로 Java 25 환경에서 재검증해야 한다.
- 브라우저 UI 검증에서 사용하는 임시 응답 서버는 화면 분기와 요청 형태 확인용이며 제품 코드나 기본 동작에 포함하지 않는다.

## 10. 이번 작업 검증 결과

완료:

- Frontend `npm run build`
- 플레이리스트 생성 후 생성 응답 ID 상세 진입
- 소유자 수정
- 비소유자 상세에서 수정·삭제·수록곡 관리 UI 비노출
- 공개 플레이리스트 상세와 비공개 플레이리스트 `403` 분리
- 존재하지 않는 플레이리스트 `404` 분리
- 수록곡 추가, 중복 추가 `409`, 순서 변경, 삭제 후 재조회
- 참조 저장, 내 저장 목록 반영, 저장 취소
- 중복 저장 `409`, 비공개 저장 `403`, 비소유자 수정 `403` 응답 구조
- 공식 플레이리스트 관리 UI 비노출 및 실제 Spotify URL 조건부 노출
- 비로그인 보호 경로의 Router Guard와 JSP 로그인 `returnTo` 보존

미완료 또는 제한 검증:

- Backend `mvnw test`: 현재 기본 Java가 `release 25`를 지원하지 않아 컴파일 전 단계에서 실패
- 실제 JSP 로그인 Session Cookie를 이용한 Backend 통합 호출
- 실제 사용자 두 명 및 MySQL 데이터 기반 권한·공개 범위 통합 테스트
- 플레이리스트 삭제의 브라우저 확인창 이후 화면 이동: 자동화 확인창 연결이 시간 초과되어 코드와 요청 형태까지만 확인
- 위/아래 인접 순서 교환: 이를 안전하게 수행할 Backend 일괄 재정렬 계약이 없음
- Spotify 실제 검색·OAuth·내보내기: 이번 단계 구현 범위에서 제외
