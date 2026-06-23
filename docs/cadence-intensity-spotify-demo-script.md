# 케이던스·운동 강도·Spotify 발표 시연 스크립트

## 시연 전 준비

1. Spotify Dashboard의 Redirect URI가 `SPOTIFY_REDIRECT_URI`와 문자 단위로 같은지 확인한다.
2. 발표 Spotify 계정이 User Management에 등록되어 있는지 확인한다.
3. `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REDIRECT_URI`를 백엔드 실행 환경에 설정한다.
4. Client Secret이 Vue `.env`, 브라우저 저장소, Git 추적 파일에 없는지 확인한다.
5. MySQL에 최신 `schema.sql` 또는 Prompt 1~7 migration을 반영한다.

## 수동 빌드·테스트 방법

PowerShell에서 Java 25를 사용한다.

```powershell
cd C:\Users\SSAFY\Desktop\SSAFIT_Playlist\ssafit

$env:JAVA_HOME = '<JAVA_25_HOME>'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:33306/ssafit_mapper_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = '<TEST_DB_USER>'
$env:SSAFIT_TEST_DB_PASSWORD = '<TEST_DB_PASSWORD>'

.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd package -DskipTests
```

프런트엔드:

```powershell
cd C:\Users\SSAFY\Desktop\SSAFIT_Playlist\frontend
npm.cmd install
npm.cmd run build

$env:SSAFIT_TEST_DB_URL = '<DEDICATED_TEST_DB_URL>'
$env:SSAFIT_TEST_DB_USERNAME = '<TEST_DB_USER>'
$env:SSAFIT_TEST_DB_PASSWORD = '<TEST_DB_PASSWORD>'
npm.cmd run e2e
```

테스트 DB 이름에는 반드시 `test`가 포함되어야 하며 운영·개발 DB를 사용하면 안 된다.

## 발표 시연 순서

1. 프로젝트 회원으로 로그인한다.
2. 러닝 프로필에서 목표 케이던스 최소 165 SPM, 최대 180 SPM을 저장한다.
3. 러닝 기록에 평균 케이던스 172 SPM, 평균 심박수 154 BPM을 입력한다.
4. 추천 화면에서 운동 강도 목표 `HIGH`를 선택한다.
5. 목표 케이던스 172 SPM으로 추천을 요청한다.
6. 172 BPM은 DIRECT, 86 BPM은 HALF_TIME으로 계산됨을 설명한다.
7. 운동 강도 `HIGH`와 음악 에너지 `ENERGETIC` 또는 `PEAK`의 매칭을 설명한다.
8. 평균 심박수 154 BPM은 운동 이력 참고값이며 음악 템포 BPM과 다른 데이터임을 설명한다.
9. Spotify 연결 버튼을 눌러 브라우저 전체 이동으로 OAuth를 시작한다.
10. Spotify 동의 화면에서 발표 계정으로 승인한다.
11. callback 후 Vue 화면으로 복귀하고 연결됨 상태를 확인한다.
12. Spotify 곡을 검색하고 한 곡을 선택해 내부 music으로 저장한다.
13. 저장된 곡의 Spotify ID/URI와 nullable 음악 BPM·에너지 값을 확인한다.
14. 추천 결과 또는 내부 플레이리스트에서 Spotify 내보내기를 실행한다.
15. 요청·성공·건너뜀 곡 수와 Spotify 플레이리스트 URL을 확인한다.
16. Spotify 앱 또는 웹에서 생성된 플레이리스트와 수록곡을 확인한다.
17. DB에서 `spotify_playlist_id`, `spotify_playlist_url`, `exported_at` 저장을 확인한다.

## 실패 시 확인 순서

- OAuth 시작 실패: 세 Spotify 환경 변수와 Redirect URI 확인
- OAuth 취소: 연결 실패 상태와 취소 오류 코드 확인
- state 불일치: 세션/호스트(`127.0.0.1` 대 `localhost`) 혼용 여부 확인
- 검색 502: Client ID/Secret과 Spotify API 응답 확인
- 내보내기 권한 부족: `playlist-modify-public`, `playlist-modify-private` scope 확인
- 429: 재시도 대기 시간을 확인하고 즉시 반복 호출하지 않음
- 곡 skip: 해당 내부 음악의 `spotifyUri` 존재 여부 확인

