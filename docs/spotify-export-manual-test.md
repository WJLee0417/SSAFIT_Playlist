# Spotify 플레이리스트 내보내기 수동 테스트 절차

이 문서는 실제 연동 검증 단계에서 수행할 절차다. Prompt 7 구현 중에는 OAuth,
Spotify API 호출, 서버 실행을 수행하지 않는다.

## 1. 사전 조건

- Spotify Developer Dashboard의 Redirect URI와 로컬 환경 변수가 일치한다.
- 테스트 계정이 개발 모드 허용 사용자에 등록되어 있다.
- `playlist-modify-private`, `playlist-modify-public` scope가 설정되어 있다.
- 기존 DB에는 `20260623_add_spotify_partial_export_status.sql`을 적용한다.
- 내보낼 내부 플레이리스트의 소유자와 현재 로그인 회원이 같다.

내보내기 필수 음악 데이터는 `spotifyTrackId`와 `spotifyUri`뿐이다.
`musicBpm`, `averageCadence`, `averageHeartRateBpm`, `musicEnergyLevel`은 없어도 된다.
곡 추가 Endpoint는 `/playlists/{spotifyPlaylistId}/items`를 사용한다.

현재 정책은 내부 플레이리스트당 최초 내보내기 한 번만 허용한다. 성공 또는
부분 성공으로 Spotify 플레이리스트가 생성되면 기존 원격 플레이리스트를
덮어쓰지 않고 ID, URL, 시각과 상태를 내부 플레이리스트에 유지한다.

## 2. Spotify 연결 전 시나리오

1. 프로젝트에는 로그인하고 Spotify 연결은 해제한다.
2. 본인 내부 플레이리스트 상세 화면을 연다.
3. Spotify 내보내기를 누른다.
4. 연결되지 않음 상태와 연결 버튼이 표시되는지 확인한다.
5. Spotify 플레이리스트가 생성되지 않고 내부 export 상태가 변경되지 않는지 확인한다.

예상 오류는 `SPOTIFY_NOT_CONNECTED`다.

## 3. Spotify 연결 후 성공 시나리오

1. Spotify OAuth를 완료한다.
2. `spotifyTrackId`와 `spotifyUri`가 모두 있는 곡으로 내부 플레이리스트를 구성한다.
3. 내보내기 버튼을 한 번 누른다.
4. 요청 수와 성공 수가 일치하고 skipped/failed chunk가 0인지 확인한다.
5. 내부 DB에서 다음 값을 확인한다.
   - `spotify_playlist_id`
   - `spotify_playlist_url`
   - `spotify_export_status = SUCCESS`
   - `spotify_exported_at`
6. 동일 내부 플레이리스트를 다시 내보낼 때
   `SPOTIFY_ALREADY_EXPORTED`로 거부되는지 확인한다.

기존 Spotify 플레이리스트를 덮어쓰거나 새 Spotify 플레이리스트를 다시 만들지 않는다.

## 4. spotifyUri 없는 음악 skip 시나리오

1. Spotify 식별자가 완전한 곡과 `spotifyUri` 또는 `spotifyTrackId`가 없는 곡을 섞는다.
2. 내보내기를 실행한다.
3. 식별자가 완전한 곡만 Spotify에 추가되는지 확인한다.
4. 응답의 다음 값을 확인한다.
   - `requestedCount`: 내부 전체 곡 수
   - `exportedCount`: 실제 추가된 곡 수
   - `skippedCount`: 식별자 누락 곡 수
   - `skippedMusicIds`: 누락 곡의 내부 ID
5. BPM이나 에너지 태그가 NULL인 곡도 식별자만 있으면 성공하는지 확인한다.

skip은 전체 내보내기 실패가 아니다.

## 5. 100개 초과 chunk 시나리오

1. Spotify 식별자가 유효한 음악 101개 이상을 내부 플레이리스트에 넣는다.
2. 내보내기를 실행한다.
3. Spotify item 추가 요청이 최대 100개씩 나뉘는지 네트워크 로그로 확인한다.
4. 101곡이면 chunk index 0에 100곡, index 1에 1곡이 처리되는지 확인한다.
5. 모든 chunk 성공 시 `exportedCount = requestedCount - skippedCount`인지 확인한다.

chunk index는 응답에서 0부터 시작한다.

## 6. 토큰 만료 시나리오

1. 저장된 access token을 만료 상태로 준비하되 refresh token은 유효하게 둔다.
2. 내보내기를 실행한다.
3. refresh 후 플레이리스트 생성과 item 추가가 진행되는지 확인한다.
4. Spotify API가 401을 반환하는 경우 refresh 후 동일 작업이 한 번만 재시도되는지 확인한다.
5. refresh 실패 시 `SPOTIFY_TOKEN_REFRESH_FAILED`와 재연결 필요 상태가 표시되는지 확인한다.

무한 재시도가 없어야 한다.

## 7. 권한·rate limit·생성 실패 시나리오

- 권한 부족 응답은 `SPOTIFY_FORBIDDEN`으로 구분한다.
- 429 응답은 `SPOTIFY_RATE_LIMITED`로 구분한다.
- Spotify 플레이리스트 생성 자체가 실패하면
  `SPOTIFY_PLAYLIST_CREATE_FAILED` 또는 원래 인증 오류를 반환한다.
- 생성 실패 시 item 추가를 시도하지 않는다.
- 내부 상태는 `FAILED`이며 생성된 Spotify ID가 없다면 ID/URL도 NULL이어야 한다.

## 8. 부분 실패 시나리오

1. 2개 이상의 chunk가 생기도록 101곡 이상을 준비한다.
2. 특정 item 추가 chunk만 실패하도록 Spotify 응답을 제어한다.
3. 다른 chunk의 성공분이 Spotify 플레이리스트에 남는지 확인한다.
4. 응답에서 다음을 확인한다.
   - 성공한 곡 수 `exportedCount`
   - 실패한 0-based `failedChunkIndexes`
   - 생성된 `spotifyPlaylistId`, `spotifyPlaylistUrl`
5. 내부 상태가 `PARTIAL`이고 `spotify_exported_at`과 오류 코드가 저장되는지 확인한다.
6. 부분 성공 플레이리스트의 URL 열기 버튼이 제공되는지 확인한다.
7. 같은 내부 플레이리스트의 자동 재내보내기가 차단되는지 확인한다.

성공 chunk는 롤백하거나 제거하지 않는다. 실패 chunk를 자동 반복 호출하지 않는다.

## 9. 전체 item 추가 실패 시나리오

1. Spotify 플레이리스트 생성은 성공시키고 모든 item chunk를 실패시킨다.
2. `exportedCount = 0`이고 모든 chunk index가 응답에 포함되는지 확인한다.
3. 내부 상태가 `FAILED`인지 확인한다.
4. 생성된 빈 Spotify 플레이리스트의 ID와 URL이 보존되는지 확인한다.

## 10. 성공 결과와 URL 확인

1. 응답의 `spotifyPlaylistUrl` 또는 화면의 Spotify에서 열기 버튼을 누른다.
2. Spotify 웹/앱에서 플레이리스트 제목, 공개 범위, 곡 순서를 확인한다.
3. `requestedCount`, `exportedCount`, `skippedCount`가 실제 목록과 일치하는지 확인한다.
4. 음악 템포는 “음악 템포 BPM”, 러닝 케이던스는 “SPM”, 평균 심박수는
   “평균 심박수 BPM”, 에너지는 `MusicEnergyLevel`로 분리 표시되는지 확인한다.
