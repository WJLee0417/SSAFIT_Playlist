# Recommendation Design

## Goal

추천 로직은 러닝 조건과 음악 특성을 하나의 점수로 결합하되, 서로 다른 의미의 데이터를 억지로 동일시하지 않는 것을 목표로 합니다.

## Input Signals

- 목표 케이던스 SPM
- 음악 BPM
- 요청 또는 프로필의 운동 강도
- 음악 에너지 수준
- 사용자 음악 선호 점수
- 최근 러닝 기록

## Target Cadence Resolution

목표 케이던스는 다음 순서로 결정합니다.

1. 요청의 `targetCadenceSpm`
2. 러닝 프로필 목표 범위의 중앙값
3. 최근 유효 러닝 기록의 평균 케이던스 중앙값
4. 사용할 정보가 없으면 케이던스 매칭을 제외

## Cadence Matching

음악 템포는 목표 케이던스와 직접 매칭하거나 half-time 관계를 고려합니다.

```text
directDifference = abs(targetCadenceSpm - musicBpm)
halfTimeDifference = abs(targetCadenceSpm - musicBpm * 2)
cadenceDifference = min(directDifference, halfTimeDifference)
```

`cadenceScore`는 차이가 작을수록 높아집니다.

```text
max(0, 100 - cadenceDifference * 5)
```

예를 들어 목표 케이던스가 172 SPM이라면 172 BPM과 86 BPM 모두 높은 매칭 점수를 받을 수 있습니다.

## Intensity Matching

요청의 운동 강도를 우선하고, 없으면 프로필과 최근 러닝 데이터를 이용해 강도를 해석합니다. 음악의 내부 에너지 레벨과 비교해 `intensityScore`를 계산합니다.

심박 데이터는 음악 BPM이나 에너지 레벨로 변환하지 않습니다.

## Preference Score

선호 점수는 다음 순서로 사용합니다.

1. 해당 사용자가 남긴 음악 선호 점수
2. 개인 점수가 없으면 전체 평균
3. 리뷰가 없으면 중립값

## Weighted Score

기본 가중치는 다음과 같습니다.

| Signal | Weight |
| --- | ---: |
| Cadence | 0.60 |
| Intensity | 0.30 |
| Preference | 0.10 |

최종 점수는 사용 가능한 점수만으로 재정규화합니다.

```text
finalScore =
    sum(availableScore * originalWeight)
    / sum(availableWeight)
```

음악 BPM이나 에너지 정보가 없다고 해서 해당 항목을 0점으로 넣지 않습니다. 외부 메타데이터 누락이 추천 전체를 과도하게 낮추는 것을 방지하기 위한 처리입니다.

## Sorting

추천 결과는 다음 순서로 정렬합니다.

1. 최종 점수
2. 케이던스 점수
3. 강도 점수
4. 생성 시각
5. 음악 ID

마지막에 PK 기준을 두어 동일한 입력에서 동점 정렬이 흔들리지 않도록 합니다.
