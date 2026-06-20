# Spotify 수동 통합 테스트 체크리스트

이 문서는 실제 Spotify 계정과 Spotify Web API를 사용하는 수동 검증용이다.
JUnit 기본 테스트에서는 실제 Spotify API를 호출하지 않는다.

실제 Client ID, Client Secret, Access Token, Refresh Token, Authorization Code는
문서·커밋·로그·화면 캡처에 남기지 않는다.

## 1. 사전 준비

- [ ] 테스트용 Spotify 계정과 Spotify Developer 애플리케이션을 준비했다.
- [ ] 로컬 MySQL 스키마가 최신 `schema.sql`과 일치한다.
- [ ] 기존 DB라면 필요한 Spotify 관련 컬럼과 제약조건을 적용했다.
- [ ] 테스트 회원으로 로그인할 수 있다.
- [ ] 테스트 회원이 소유한 사용자 플레이리스트를 준비했다.
- [ ] 애플리케이션 로그 레벨에서 HTTP 헤더와 요청 본문을 출력하지 않는다.

## 2. 환경 변수 등록

IDE Run Configuration 또는 운영 환경의 비밀 저장소에 다음 변수를 설정한다.

```text
SPOTIFY_CLIENT_ID=<Spotify Developer Dashboard에서 발급>
SPOTIFY_CLIENT_SECRET=<Spotify Developer Dashboard에서 발급>
SPOTIFY_REDIRECT_URI=http://localhost:8080/api/spotify/oauth/callback
```

- [ ] 세 변수가 모두 설정되어 있다.
- [ ] 실제 Secret이나 Token이 `.env.example`, `application.yml`, Git 변경사항에 없다.
- [ ] `SPOTIFY_REDIRECT_URI` 앞뒤에 공백이 없다.

## 3. Redirect URI 확인

- [ ] Spotify Developer Dashboard에 `SPOTIFY_REDIRECT_URI`와 완전히 동일한 URI를 등록했다.
- [ ] 프로토콜, 호스트, 포트, 경로, 마지막 슬래시가 모두 일치한다.
- [ ] 로컬 애플리케이션의 callback 경로와 Controller 매핑이 일치한다.

## 4. OAuth 인증 성공 흐름

- [ ] Spotify 연결 시작 요청을 보낸다.
- [ ] Spotify authorize URL에 `client_id`, `redirect_uri`, `scope`, `state`가 있다.
- [ ] `state`가 요청마다 새로 생성된다.
- [ ] Spotify 로그인 후 권한 동의를 선택한다.
- [ ] callback이 동일한 브라우저 세션으로 돌아온다.
- [ ] callback의 `state` 검증이 성공한다.
- [ ] callback 성공 후 동일한 `state`를 재사용할 수 없다.
- [ ] callback 응답과 로그에 Authorization Code나 Token이 노출되지 않는다.

## 5. spotify_connections 저장 확인

민감한 토큰 원문을 복사하거나 화면 캡처하지 않는다.

- [ ] 로그인 회원의 `member_id`로 행이 1개만 존재한다.
- [ ] `spotify_user_id`가 저장됐다.
- [ ] Access Token과 Refresh Token이 빈 값이 아니다.
- [ ] `token_expires_at`이 현재보다 미래다.
- [ ] 재인증 후 같은 회원 행이 중복 insert되지 않고 갱신된다.
- [ ] 재인증 응답에 Refresh Token이 없으면 기존 Refresh Token이 유지된다.

## 6. Spotify 곡 검색

- [ ] 정상 키워드 검색 결과가 반환된다.
- [ ] Track ID와 Spotify URI가 있는 결과만 반환된다.
- [ ] 제목, 아티스트, 앨범, 이미지 또는 외부 URL이 없는 결과도 전체 검색을 실패시키지 않는다.
- [ ] 검색 결과가 없으면 빈 목록이 반환된다.
- [ ] 공백 검색어는 Spotify API 호출 전에 거부된다.
- [ ] 과도한 limit은 프로젝트 최대값으로 제한된다.
- [ ] Spotify 장애 시 `SPOTIFY_SEARCH_FAILED`가 반환된다.

## 7. 선택 곡 music 저장

- [ ] 검색 결과에서 곡 하나를 선택해 저장한다.
- [ ] `source_type`이 `SPOTIFY`다.
- [ ] `spotify_track_id`와 `spotify_uri`가 저장됐다.
- [ ] 제목과 아티스트가 저장됐다.
- [ ] 선택 정보가 있으면 앨범, 이미지, 외부 URL도 저장됐다.
- [ ] 같은 `spotify_track_id`를 다시 저장해도 music 행이 추가되지 않는다.
- [ ] 기존 music의 비어 있는 메타데이터만 보강된다.

## 8. Spotify 기반 수록곡 추가

- [ ] 저장한 Spotify music을 사용자 플레이리스트에 추가한다.
- [ ] 같은 music을 같은 플레이리스트에 중복 추가할 수 없다.
- [ ] 수록 순서가 올바르게 저장된다.
- [ ] Spotify music인데 URI가 없으면 `SPOTIFY_URI_NOT_FOUND`가 발생한다.

## 9. Spotify Export 성공 흐름

- [ ] 플레이리스트 소유자 계정으로 Export를 실행한다.
- [ ] Export 시작 시 상태가 `PENDING`으로 변경된다.
- [ ] Spotify에 플레이리스트가 생성된다.
- [ ] 수록곡이 원래 순서대로 추가된다.
- [ ] 100곡 초과 시 여러 요청으로 나뉘어 모두 추가된다.
- [ ] 완료 후 상태가 `SUCCESS`다.
- [ ] `spotify_playlist_id`가 저장됐다.
- [ ] `spotify_playlist_url`이 저장됐다.
- [ ] `spotify_exported_at`이 저장됐다.
- [ ] `spotify_export_error_code`가 비어 있다.
- [ ] 저장된 URL을 열면 실제 Spotify 플레이리스트가 표시된다.
- [ ] Spotify에서 곡 수와 순서를 확인한다.
- [ ] 성공한 플레이리스트를 다시 Export하면 `SPOTIFY_ALREADY_EXPORTED`가 발생한다.

## 10. 실패 시나리오

### OAuth

- [ ] 권한 동의를 거부하면 `SPOTIFY_AUTH_DENIED`가 발생한다.
- [ ] callback state를 변조하면 `SPOTIFY_OAUTH_STATE_MISMATCH`가 발생한다.
- [ ] 필수 환경 변수를 제거하면 설정 오류가 발생하며 실제 값은 노출되지 않는다.

### 검색

- [ ] 네트워크 또는 Spotify 오류 시 `SPOTIFY_SEARCH_FAILED`가 발생한다.
- [ ] 오류 응답과 로그에 Client Secret이나 Token이 없다.

### Export

- [ ] 소유자가 아닌 회원의 Export는 `ACCESS_DENIED`다.
- [ ] Spotify 연결이 없으면 `SPOTIFY_AUTH_REQUIRED`다.
- [ ] URI가 없는 수록곡이 하나라도 있으면 Export 전에 중단된다.
- [ ] Spotify Playlist 생성 실패 시 상태가 `FAILED`다.
- [ ] 생성 실패에서는 Spotify Playlist ID와 URL이 비어 있다.
- [ ] Playlist 생성 후 곡 추가 실패 시 상태가 `FAILED`다.
- [ ] 부분 실패에서는 생성된 Spotify Playlist ID와 URL이 보존된다.
- [ ] `spotify_export_error_code`에는 ErrorCode 이름만 저장되고 민감정보는 없다.
- [ ] 외부 ID가 없는 `FAILED` 건은 재시도할 수 있다.
- [ ] 외부 ID가 남은 부분 실패는 자동 재시도가 차단된다.

## 11. Access Token 갱신

- [ ] 만료가 임박한 연결로 Export를 실행한다.
- [ ] Refresh Token으로 Access Token이 갱신된다.
- [ ] 갱신된 Access Token과 만료 시각이 DB에 즉시 저장된다.
- [ ] 응답에 새 Refresh Token이 있으면 DB 값도 변경된다.
- [ ] 새 Refresh Token이 없으면 기존 값이 유지된다.
- [ ] Refresh 실패 시 `SPOTIFY_TOKEN_EXPIRED`가 발생한다.
- [ ] Refresh 실패 전의 DB 토큰 값이 훼손되지 않는다.

## 12. 연결 해제와 재인증

- [ ] 현재 회원의 Spotify 연결을 해제한다.
- [ ] 해당 회원의 `spotify_connections` 행이 삭제된다.
- [ ] 다른 회원의 연결 정보에는 영향이 없다.
- [ ] 연결 해제 후 Export하면 `SPOTIFY_AUTH_REQUIRED`가 발생한다.
- [ ] OAuth 인증을 다시 진행하면 새 연결 정보가 저장된다.
- [ ] 재인증 후 검색과 Export가 다시 성공한다.

## 13. 자동 테스트 확인

Spotify 자동 테스트는 API Client를 Mock으로 사용한다.

- `SpotifyServiceTest`: 검색 성공·빈 결과·오류·응답 변환
- `MusicServiceTest`: 최초 저장·동일 Track ID 재사용·URI 저장·메타데이터 보강
- `SpotifyOAuthServiceTest`: URL·callback·토큰·refresh·조회·해제
- `PlaylistExportServiceTest`: 권한·연결·성공·부분 실패·배치·재시도

Windows에서 다음 두 명령을 실행한다.

```bash
mvnw.cmd clean test
mvnw.cmd -o clean test
```

- [ ] 일반 테스트가 성공한다.
- [ ] Maven 캐시가 준비된 환경에서 오프라인 테스트도 성공한다.
- [ ] 테스트 중 실제 Spotify 도메인으로 HTTP 요청이 발생하지 않는다.
