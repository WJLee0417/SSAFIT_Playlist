# 케이던스·운동 강도 통합 추천 설계

## 1. 세 가지 단위의 경계

- `musicBpm`: 곡의 음악 템포 BPM
- `targetCadenceSpm` / `averageCadence`: 러닝 발걸음 빈도 SPM
- `averageHeartRateBpm`: 러닝 중 평균 심박수 BPM

음악 BPM과 케이던스 SPM은 템포 매칭에서 비교할 수 있지만 같은 데이터는 아니다.
평균 심박수 BPM은 사용자의 생리 반응이며 음악 BPM으로 변환하거나 케이던스
차이 계산에 사용하지 않는다.

## 2. 목표 케이던스 결정

우선순위는 다음과 같다.

1. 요청의 `targetCadenceSpm`
2. 프로필 `targetCadenceMin`과 `targetCadenceMax`의 중앙값
3. 최근 유효 러닝 기록 최대 5건의 `averageCadence` 중앙값
4. 정보가 없으면 케이던스 매칭 미적용

NULL 케이던스 기록은 제외한다. 프로필 중앙값과 짝수 개 기록의 중앙값은 모두
정수 산술평균을 버림하는 동일 규칙을 사용한다.

## 3. 케이던스 템포 매칭

```text
directDifference = abs(targetCadenceSpm - musicBpm)
halfTimeDifference = abs(targetCadenceSpm - musicBpm * 2)
cadenceDifference = min(directDifference, halfTimeDifference)
```

- `DIRECT`: directDifference가 더 작거나 두 차이가 같은 경우
- `HALF_TIME`: halfTimeDifference가 더 작은 경우
- `NONE`: 목표 케이던스 또는 음악 BPM이 없는 경우

`cadenceScore`는 0~100이며 다음 규칙을 사용한다.

```text
max(0, 100 - cadenceDifference * 5)
```

예를 들어 172 SPM과 172 BPM은 DIRECT 100점이고, 172 SPM과 86 BPM은
HALF_TIME 100점이다. 음악 BPM이 NULL이면 점수를 계산하지 않는다.

## 4. 운동 강도와 음악 에너지

운동 강도는 요청의 `runningIntensityGoal`을 우선하고, 없으면
`RunningIntensityResolutionService` 결과를 사용한다. 둘 다 결정되지 않으면
강도 매칭을 적용하지 않는다.

`IntensityMatchingPolicy`의 높음/중간/낮음 3/2/1 점수를 각각
100/66.67/33.33으로 환산한다. `MusicEnergyLevel`이 NULL이면
`intensityScore`도 NULL이다.

평균 심박수 이력은 운동 강도 목표를 보조적으로 해석하는 데만 사용한다.
`musicEnergyLevel`을 생성하거나 변경하지 않는다.

## 5. 선호 점수

현재 `music_reviews.preference_score`를 사용한다.

1. 해당 회원이 남긴 곡 선호 점수
2. 개인 점수가 없으면 해당 곡의 전체 평균
3. 리뷰가 전혀 없으면 중립 기본값 3/5

1~5 값을 0~100 비율로 환산한다. 기본값을 두기 때문에 템포와 에너지 정보가
모두 없는 음악도 추천 계산 전체를 실패시키지 않는다.

## 6. 가중치와 재정규화

기본 가중치는 다음과 같다.

- cadenceScore: 0.60
- intensityScore: 0.30
- preferenceScore: 0.10

최종 점수는 사용 가능한 점수의 가중치 합만 분모로 사용한다.

```text
finalScore =
    sum(availableScore * originalWeight)
    / sum(availableWeight)
```

따라서 음악 BPM이 없으면 케이던스 0.60을 분자와 분모에서 모두 제외하고,
에너지 태그가 없으면 강도 0.30을 제외한다. 둘 다 없으면 선호 점수만으로
계산한다. 누락 항목을 0점으로 간주하지 않는다.

## 7. 요청 시점 계산과 정렬

점수는 추천 요청 시점에 후보 음악과 리뷰 데이터를 읽어 계산한다. 음악 테이블에
추천 점수를 미리 저장하지 않는다. 기존 추천 로그에는 저장 가능한 곡 ID와 요청
조건을 남기지만, 곡별 계산 점수는 응답용 임시 결과다.

정렬 순서는 다음과 같다.

1. `finalScore` 내림차순
2. `cadenceScore` 내림차순
3. `intensityScore` 내림차순
4. 기존 생성 시각 내림차순
5. `musicId` 오름차순

NULL 세부 점수는 해당 비교 단계의 뒤에 배치한다. 마지막 PK 기준으로 동일 요청의
동점 정렬이 흔들리지 않게 한다.

## 8. 심박수와 음악 BPM을 1:1 대응하지 않는 이유

평균 심박수는 체력, 기온, 경사, 피로, 측정 장비 등에 영향을 받는다. 같은 음악을
들어도 사람과 상황에 따라 달라지므로 음악 템포 BPM과 동일시할 수 없다.
평균 심박수는 `cadenceDifference`, `musicBpm`, `MusicEnergyLevel` 계산에
사용하지 않으며 사용자 이력 설명과 운동 강도 보조 해석에만 사용한다.
