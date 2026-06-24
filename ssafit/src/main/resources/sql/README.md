# SQL 관리 기준

## 파일 역할

- `schema.sql`: MVP 테이블, 제약조건, FK, 인덱스
- `demo-snapshot.sql`: 시연용 통합 스냅샷 데이터
  - 공식 데모 플레이리스트
  - Zone 2 데모
  - WORKOUT K-POP 데모
  - 데모 회원, 러닝 프로필, 러닝 기록, 러닝 세션, 참여 관계
  - 플레이리스트 저장 관계와 음악 리뷰

## 실행 정책

`application.yml`의 `spring.sql.init.mode`는 `never`입니다. SQL 파일은 애플리케이션
시작 시 자동 실행하지 않고 MySQL Workbench 또는 MySQL CLI에서 직접 실행합니다.
Docker Compose의 MySQL 신규 볼륨 초기화는 `schema.sql` 실행 후
`demo-snapshot.sql`을 자동 실행합니다.

```bash
mysql -u ssafit -p ssafit_playlist < src/main/resources/sql/schema.sql
mysql -u ssafit -p ssafit_playlist < src/main/resources/sql/demo-snapshot.sql
```

기존 테이블이 있는 DB에 `schema.sql`을 반복 실행하지 않습니다. 스키마 변경이 필요한
경우 기존 데이터를 확인한 후 별도 마이그레이션 SQL을 작성합니다.
케이던스·평균 심박수 확장은 기존 DB에서
`migrations/20260623_add_cadence_heart_rate.sql`을 한 번만 실행합니다.
Spotify 음악 메타데이터 확장은 `spotify_uri` 중복을 먼저 정리한 뒤
`migrations/20260623_add_spotify_music_metadata.sql`을 한 번만 실행합니다.
음악 에너지 태그 확장은
`migrations/20260623_add_music_energy_level.sql`을 한 번만 실행합니다.
Spotify 부분 내보내기 상태 확장은
`migrations/20260623_add_spotify_partial_export_status.sql`을 한 번만 실행합니다.
`demo-snapshot.sql`은 중복 방지 구문을 사용해 반복 실행할 수 있습니다.
실제 개인 계정, Spotify OAuth 토큰, refresh token, Spotify user id, 운영 DB 덤프는
저장소에 커밋하지 않습니다.

## 명명과 매핑

- 테이블명과 컬럼명: `snake_case`
- Java Domain 필드명: `camelCase`
- MyBatis 기본 매핑: `map-underscore-to-camel-case: true`
- 복잡한 조인 결과: 명시적인 `resultMap` 사용

## 도메인 정책

- 회원 삭제는 `members.status`와 `members.deleted_at`을 이용한 soft delete를 우선합니다.
- USER 플레이리스트는 소유자가 반드시 필요합니다.
- OFFICIAL 플레이리스트는 시스템 생성 데이터로 소유자가 없고 공개 상태로 관리합니다.
- `playlist_saves`는 공개/공식 플레이리스트 참조 저장 전용입니다.
- 자동 큐레이션 저장은 USER + CURATION_COPY 플레이리스트를 별도로 생성합니다.
- 추천 조회 로그 생성만으로 플레이리스트와 수록곡을 생성하지 않습니다.
- Spotify 토큰은 애플리케이션 계층에서 암호화한 값으로 저장하는 방식을 권장합니다.
