# 케이던스·운동 강도·Spotify 통합 검증 보고서

검증일: 2026-06-23

## 1. 검증 환경

- Windows, PowerShell
- Java 25.0.2
- Spring Boot 4.0.6
- Maven Wrapper
- Node.js 24.15.0, npm 11.12.1
- MySQL 8.4.10 Docker 격리 DB
- Vue 3, Vite 6.4.3
- Playwright
- 테스트 DB: `ssafit_mapper_test` 전용 계정

Spotify 관련 세 환경 변수는 검증 프로세스·사용자·시스템 환경에 모두 없었다.

- `SPOTIFY_CLIENT_ID`
- `SPOTIFY_CLIENT_SECRET`
- `SPOTIFY_REDIRECT_URI`

비밀값은 로그와 문서에 기록하지 않았다.

## 2. 자동 테스트 결과

- `mvn clean test`: 280개 통과, 실패 0, 오류 0
- `mvn clean verify`: 280개 통과 및 실행 가능한 WAR 재패키징 성공
- `mvn package -DskipTests`: 성공
- `npm run build`: 성공, Vue 프로덕션 번들 생성
- Playwright E2E: 3개 통과

추가·보완한 검증 범위:

- 케이던스 및 평균 심박수 NULL 호환
- 케이던스 60/240 허용, 59/241 거부
- 평균 심박수 30/240 허용, 29/241 거부
- 프로필 케이던스 편측 입력 및 역전 범위 거부
- 케이던스·심박수 저장/수정
- DIRECT 및 HALF_TIME 템포 매칭
- 에너지 매칭 행렬 및 NULL 처리
- 운동 강도 사용자 선택 우선, 심박수 이력 5건 미만 UNKNOWN
- 심박수 변화가 `musicBpm` 및 `cadenceScore`에 영향을 주지 않음
- 누락 점수 가중치 재정규화
- 동일 점수의 music ID 안정 정렬
- OAuth state, refresh 1회, 재연결 필요, Spotify 오류 분류
- Spotify URI 누락 skip, 100개 chunk, 부분 실패 결과 보존

## 3. MySQL 통합 테스트 결과

MySQL 8.4 전용 테스트 DB에서 `schema-reset.sql`과 실제 `schema.sql`을 적용했다.

- Mapper 통합 테스트 14개 통과
- `running_profiles.target_cadence_min/max` INSERT·UPDATE·SELECT 확인
- `running_records.average_cadence/average_heart_rate_bpm` INSERT·UPDATE·SELECT 확인
- 기존 NULL 러닝 기록 조회 확인
- `music.bpm`, `music_energy_level`, Spotify ID/URI 독립 저장 확인
- nullable `music_energy_level` 업데이트 확인
- 추천 로그 생성과 플레이리스트 복사 트랜잭션 확인
- Spotify 연결 토큰 저장·갱신 Mapper 확인

Playwright에서는 실제 백엔드와 Vue 서버를 테스트 DB에 연결해 로그인, 러닝 기록 CRUD, 러닝 모임 CRUD·권한·참여 흐름을 확인했다.

## 4. Spotify OAuth 수동 검증 결과

로컬 UI에서 다음을 확인했다.

- 프로젝트 로그인 후 추천 화면 접근
- 운동 강도 목표와 목표 케이던스(SPM) 입력 분리
- Spotify 검색 화면 접근
- Client Credentials 미설정 상태의 검색 실패 UI와 오류 코드 표시

실제 Spotify OAuth 동의, callback, 연결 상태, 취소, 정상 연결은 환경 변수 미설정으로 실행하지 못했다. Dashboard의 User Management에 발표 계정 한 개만 등록한 구성은 해당 계정으로만 시연한다면 적합하지만, 다른 Spotify 계정은 Development Mode에서 거부될 수 있다.

## 5. Spotify 검색·내보내기 검증 결과

자동 테스트에서 다음을 확인했다.

- 검색 결과 메타데이터 변환
- 빈 검색어 거부, limit 50 제한, offset 처리
- `spotifyTrackId` 및 `spotifyUri` 기반 내부 저장 중복 방지
- Spotify 미연결 내보내기 거부
- `spotifyUri` 없는 음악 skip
- 101곡을 100/1 단위로 chunk
- 일부 chunk 실패 시 성공 결과 보존 및 실패 index 반환
- `spotifyPlaylistId`, `spotifyPlaylistUrl` 저장

실제 Spotify 검색 결과, 실제 플레이리스트 생성 및 Spotify 앱에서의 결과 확인은 자격 증명 미설정으로 보류했다.

## 6. BPM / SPM / 평균 심박수 분리 검증 결과

- 러닝 케이던스: SPM
- 음악 템포: `musicBpm`, BPM
- 평균 심박수: `averageHeartRateBpm`, BPM
- 운동 강도와 음악 에너지는 Enum 정책으로 별도 계산
- 평균 심박수는 최근 이력 기반 운동 강도 참고에만 사용
- 평균 심박수 변경 전후 `musicBpm`과 `cadenceScore`가 동일함을 테스트로 확인
- BPM 없는 음악은 cadence score를 제외하고 재정규화
- 에너지 없는 음악은 intensity score를 제외하고 재정규화

## 7. 발견된 문제와 해결 내역

- 기존 테스트가 Prompt 3~7의 생성자·오류 코드·API 계약을 반영하지 못함: 최신 계약으로 보완
- Playwright가 `localhost`와 `127.0.0.1`을 혼용해 로그인 반환 URL이 어긋남: `127.0.0.1`로 통일
- 로컬 E2E 종료 후 기존 서버가 남으면 러너가 대기함: 로컬은 기존 서버 재사용, CI는 신규 서버 기동으로 변경
- Maven 재패키징 중 디스크 공간 부족: 임시 VS Code 설치 캐시를 정리한 뒤 재실행하여 성공
- Java 25에서 Mockito 동적 agent 및 Unsafe 사용 경고: 현재 테스트 성공에는 영향이 없으나 향후 Mockito agent 설정 검토 필요

## 8. 남은 제약 사항

- 실제 Spotify 환경 변수와 Dashboard Redirect URI를 설정한 수동 검증 필요
- 발표 계정으로 OAuth 동의, 검색, 내부 저장, 내보내기, Spotify 앱 확인 필요
- 실제 토큰 만료를 기다리는 검증은 수행하지 않았고 mock 기반 자동 테스트로 대체
- 100개 초과 실제 Spotify 내보내기는 mock 기반 자동 테스트로 대체
- 문서의 수동 Spotify 시나리오는 실제 실행 결과가 아니라 절차이며, 자격 증명 설정 후 체크해야 한다

## 9. 병합 가능 여부

코드·자동 테스트·MySQL·Vue 빌드 기준으로는 병합 가능하다.

다만 발표용 완전 검증 기준에서는 실제 Spotify OAuth·검색·내보내기 수동 확인이 남아 있으므로, 발표 브랜치 최종 확정 전 해당 계정으로 한 번의 정상 시나리오를 수행해야 한다.

