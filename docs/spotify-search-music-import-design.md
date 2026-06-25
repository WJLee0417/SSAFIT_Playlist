# Spotify 검색·내부 음악 저장 설계

## 1. 검색 결과에서 내부 저장까지의 흐름

1. 프론트엔드는 `GET /api/spotify/tracks/search`에 `query`, `offset`, `limit`을 전달한다.
2. 백엔드는 Client Credentials 토큰으로 Spotify Search API를 조회한다.
3. 검색 결과는 `spotifyTrackId`, `spotifyUri`, `spotifyExternalUrl`, `title`,
   `artist`, `albumName`, `imageUrl`, `durationMs`를 반환한다.
4. 검색만으로 `music` 테이블에는 아무 데이터도 저장하지 않는다.
5. 사용자가 곡을 명시적으로 선택하면
   `POST /api/music/from-spotify`가 해당 곡을 내부 음악으로 저장하거나 기존 음악을 반환한다.

검색 API의 기본 `offset`은 0, 기본 `limit`은 10이다. `limit`은 최대 50,
`offset`은 0~1000 범위만 허용한다. 공백 검색어는 거부한다.

검색은 사용자 OAuth access token이 아니라 서버의 Spotify 애플리케이션 인증을
사용한다. 따라서 Client ID/Secret 누락과 Spotify 인증·403·429·네트워크 오류는
각 Spotify 오류 코드로 전달하며, 검색 결과 조회 자체가 사용자 계정 연결을
생성하지 않는다.

## 2. Spotify 식별자 중복 정책

- `spotify_track_id`와 `spotify_uri` 모두 각각 유일해야 한다.
- 저장 전에 두 식별자로 각각 기존 `music`을 조회한다.
- 둘 중 하나가 기존 음악과 일치하면 새 레코드를 만들지 않고 기존 음악을 반환한다.
- 두 식별자가 서로 다른 내부 음악을 가리키면 자동 병합하지 않고
  `SPOTIFY_TRACK_IDENTITY_CONFLICT`로 거부한다.
- DB에도 `spotify_uri` UNIQUE 제약을 두어 동시 요청의 중복 삽입을 막는다.
- UNIQUE 충돌이 발생하면 식별자로 기존 레코드를 다시 조회한다.

기존 내부 메타데이터와 Spotify 메타데이터가 충돌할 때는 기존의 비어 있지 않은
값을 우선한다. 제목, 아티스트, 앨범, 이미지, 장르, 음악 BPM은 덮어쓰지 않고
비어 있는 값만 보강한다. Spotify 식별자 자체가 충돌하면 저장하지 않는다.

## 3. `musicBpm`의 의미

Java 및 JSON의 `musicBpm`은 곡의 음악적 템포 BPM만 의미한다. DB의 기존 `bpm`
컬럼은 호환성을 위해 유지하되 같은 의미로만 사용한다.

- `averageCadence`: 러닝 케이던스 SPM
- `averageHeartRateBpm`: 러닝 평균 심박수 BPM
- `musicBpm`: 음악 템포 BPM

세 값 사이의 자동 복사·변환은 금지한다. 특히 평균 심박수나 케이던스를
`musicBpm`에 저장하지 않는다. 이전 import 요청의 `bpm` 필드는 입력 호환을
위해 alias로만 허용하며 응답의 명시적 필드명은 `musicBpm`이다.

## 4. BPM 미확인 음악 처리

`musicBpm`은 NULL을 허용한다. BPM이 없어도 Spotify 검색, 내부 음악 저장,
내부 플레이리스트 수록 및 Spotify 내보내기는 정상 동작해야 한다.

BPM 결정 우선순위는 다음과 같다.

1. 기존 내부 DB에 저장된 `musicBpm`
2. 관리자 또는 운영자가 import 요청에 명시한 `musicBpm`
3. 선택적 `MusicTempoProvider`
4. 미확인 상태인 `null`

기존 DB 값은 import 메타데이터나 외부 Provider가 덮어쓰지 않는다.

## 5. 선택적 BPM 보강 Provider

`MusicTempoProvider.resolveMusicBpm(spotifyTrackId)`는 외부 템포 데이터 공급자를
연결하기 위한 경계다. 기본 구현은 값을 반환하지 않는다.

보강은 다음 설정이 true일 때만 시도한다.

```yaml
spotify:
  audio-feature-enrichment:
    enabled: false
```

환경 변수는 `SPOTIFY_AUDIO_FEATURE_ENRICHMENT_ENABLED`를 사용하며 기본값은
false다. Provider 오류, 미지원, 무응답, 유효하지 않은 BPM은 import 실패로
전파하지 않고 `musicBpm = null`로 처리한다.

Spotify Audio Features 등 특정 외부 API는 필수 의존성이 아니며, 실제 Provider를
추가할 때에도 Client Secret과 외부 토큰은 서버에서만 관리한다.

## 6. DB 적용 주의사항

신규 DB는 `schema.sql`을 사용한다. 기존 DB에는
`migrations/20260623_add_spotify_music_metadata.sql`을 적용한다.

마이그레이션 전에 `spotify_uri` 중복을 반드시 조회하고 정리해야 한다. 중복이
남아 있으면 UNIQUE 제약 추가가 실패한다. `duration_ms`는 NULL 허용이며 기존
데이터에 임의 값을 채우지 않는다.
