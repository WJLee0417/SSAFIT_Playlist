# 음악 에너지·운동 강도 매칭 정책

## 1. MusicEnergyLevel 의미

`MusicEnergyLevel`은 음악의 템포가 아니라 운영자가 판단하는 체감 에너지 또는
내부 큐레이션 태그다.

- `CALM`: 회복주·가벼운 러닝에 어울리는 낮은 에너지
- `STEADY`: 안정적인 지속주에 어울리는 중간 에너지
- `ENERGETIC`: 템포런·강한 러닝에 어울리는 높은 에너지
- `PEAK`: 고강도·인터벌 성격의 매우 높은 에너지

`music.music_energy_level`은 NULL을 허용한다. 기존 음악 전체에 값을 일괄
입력하지 않으며 Spotify import도 에너지 값을 요구하거나 자동 생성하지 않는다.
관리 화면에서 운영자가 명시적으로 선택한 경우에만 저장한다.

## 2. RunningIntensityGoal 매칭 규칙

`IntensityMatchingPolicy`는 높은 매칭 3점, 중간 매칭 2점, 낮은 매칭 1점을
반환한다.

| 목표 | CALM | STEADY | ENERGETIC | PEAK |
|---|---:|---:|---:|---:|
| EASY | 3 | 2 | 1 | 1 |
| MODERATE | 2 | 3 | 2 | 1 |
| HIGH | 1 | 2 | 3 | 3 |

`RunningIntensityGoal` 또는 `MusicEnergyLevel`이 NULL이면 `intensityScore`도
NULL이다. 이는 낮은 점수가 아니라 계산 불가 상태다.

정책은 Controller와 Vue에서 계산하지 않는다. 향후 추천 조합 단계는
`IntensityMatchingPolicy.calculateIntensityScore()`를 호출해 별도 점수 축으로
사용해야 한다.

## 3. musicBpm과 musicEnergyLevel의 차이

- `musicBpm`: 곡의 객관적인 템포 BPM
- `musicEnergyLevel`: 곡의 체감 에너지 또는 큐레이션 태그

빠른 BPM 곡이 항상 `PEAK`인 것은 아니며 느린 BPM 곡이 항상 `CALM`인 것도
아니다. 두 값은 독립적으로 저장하고 평가한다. 이번 단계에서는 케이던스 기반
템포 매칭을 구현하거나 두 값을 하나의 점수로 합산하지 않는다.

## 4. 평균 심박수로 에너지를 자동 결정하지 않는 이유

`averageHeartRateBpm`은 사용자의 러닝 반응 데이터이고 `musicEnergyLevel`은
곡의 특성 태그다. 개인별 체력, 환경, 운동 유형에 따라 같은 곡에서도 심박수는
달라질 수 있으므로 평균 심박수로 음악 에너지를 생성하거나 수정하지 않는다.

Prompt 2의 심박수 이력은 충분한 기록이 있을 때 운동 강도 목표를 참고하는
용도로만 사용한다. 의료적 진단이나 곡 메타데이터 생성 근거로 사용하지 않는다.

## 5. NULL 에너지 태그 처리

- 음악 조회·Spotify import·플레이리스트 저장·내보내기는 정상 동작한다.
- 강도 매칭 점수는 계산하지 않고 NULL로 유지한다.
- 임의의 중간 점수나 BPM 기반 추정값을 부여하지 않는다.
- 추천 단계는 NULL 점수를 별도의 미확인 상태로 처리해야 한다.

## 6. 관리 API와 권한 주의사항

내부 음악 등록은 `POST /api/music/internal`, 수정은
`PUT /api/music/internal/{musicId}`를 사용한다. 현재 회원 모델에는 관리자 역할
필드가 없어 로그인 세션만 검사한다. 실제 운영 전에는 관리자 역할 또는 별도
운영자 allowlist를 도입해 이 두 Endpoint와 Vue 관리 경로에 권한 검사를
추가해야 한다.

기존 DB에는 `migrations/20260623_add_music_energy_level.sql`을 적용한다.
컬럼은 nullable이며 기존 데이터 backfill은 하지 않는다.
