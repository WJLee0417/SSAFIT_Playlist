# Codex 작업 지침: 러닝 케이던스 및 심박수 데이터 모델 확장

## 작업 목적

회원 러닝 프로필과 러닝 기록에 케이던스 데이터를 추가하고, 러닝 기록에 평균 심박수를 저장할 수 있도록 확장한다. 향후 음악 BPM과 러닝 케이던스의 관계를 이용한 음악 추천, 플레이리스트 생성 및 러닝 강도 분석에 활용할 수 있는 구조로 구현한다.

케이던스 단위는 반드시 `SPM(steps per minute, 분당 걸음 수)`으로 통일한다.

평균 심박수 단위는 `BPM(beats per minute, 분당 심박 수)`으로 통일한다. 음악의 템포 BPM과 의미가 다르므로 Java 및 API에서는 반드시 `averageHeartRateBpm`, DB에서는 `average_heart_rate_bpm`이라는 명확한 이름을 사용한다.

## 1. DB 스키마

### `running_profiles`

목표 케이던스 범위를 나타내는 다음 컬럼을 추가한다.

```sql
target_cadence_min SMALLINT UNSIGNED NULL,
target_cadence_max SMALLINT UNSIGNED NULL
```

제약 조건:

```sql
CONSTRAINT ck_running_profiles_cadence_min
    CHECK (
        target_cadence_min IS NULL
        OR target_cadence_min BETWEEN 60 AND 240
    ),

CONSTRAINT ck_running_profiles_cadence_max
    CHECK (
        target_cadence_max IS NULL
        OR target_cadence_max BETWEEN 60 AND 240
    ),

CONSTRAINT ck_running_profiles_cadence_range
    CHECK (
        (target_cadence_min IS NULL AND target_cadence_max IS NULL)
        OR (
            target_cadence_min IS NOT NULL
            AND target_cadence_max IS NOT NULL
            AND target_cadence_min <= target_cadence_max
        )
    )
```

최솟값과 최댓값은 둘 다 입력하거나 둘 다 비워야 한다.

### `running_records`

해당 러닝 활동의 평균 케이던스와 평균 심박수를 저장한다.

```sql
average_cadence SMALLINT UNSIGNED NULL,
average_heart_rate_bpm SMALLINT UNSIGNED NULL
```

제약 조건:

```sql
CONSTRAINT ck_running_records_average_cadence
    CHECK (
        average_cadence IS NULL
        OR average_cadence BETWEEN 60 AND 240
    ),

CONSTRAINT ck_running_records_average_heart_rate
    CHECK (
        average_heart_rate_bpm IS NULL
        OR average_heart_rate_bpm BETWEEN 30 AND 240
    )
```

기존 데이터와의 호환성을 위해 케이던스 및 평균 심박수 컬럼은 우선 `NULL`을 허용한다. 임의의 기본값은 사용하지 않는다.

평균 심박수의 허용 범위는 데이터 오류를 방지하기 위한 기술적 범위이며 의학적 안전 기준이나 운동 권장 범위를 의미하지 않는다.

## 2. 도메인 모델

### `RunningProfile`

```java
private Integer targetCadenceMin;
private Integer targetCadenceMax;
```

### `RunningRecord`

```java
private Integer averageCadence;
private Integer averageHeartRateBpm;
```

`cadence`처럼 의미가 불분명한 이름보다 목표 범위와 평균값을 명확히 표현하는 이름을 사용한다.

심박수 필드 역시 `bpm`이나 `averageBpm`처럼 음악 BPM과 혼동될 수 있는 이름을 사용하지 않는다.

## 3. DTO 및 API

### 러닝 프로필

`SaveRequest`, `Response`, `RunningProfileViewForm`에 추가한다.

```java
@Min(60)
@Max(240)
Integer targetCadenceMin,

@Min(60)
@Max(240)
Integer targetCadenceMax
```

서비스에서 다음을 검증한다.

- 둘 중 하나만 입력된 경우 실패
- 최솟값이 최댓값보다 큰 경우 실패
- 허용 범위 밖이면 실패

### 러닝 기록

`CreateRequest`, `UpdateRequest`, `Response`에 추가한다.

```java
@Min(60)
@Max(240)
Integer averageCadence,

@Min(30)
@Max(240)
Integer averageHeartRateBpm
```

요청·응답 JSON 필드:

```json
{
  "averageCadence": 172,
  "averageHeartRateBpm": 154
}
```

프로필 예시:

```json
{
  "targetCadenceMin": 165,
  "targetCadenceMax": 180
}
```

## 4. MyBatis Mapper

다음 항목을 모두 수정한다.

- `resultMap`
- 공통 조회 컬럼
- `INSERT`
- `UPDATE`
- 프로필 upsert
- 러닝 기록 목록 및 상세 조회

DB 컬럼과 Java 필드 매핑:

```xml
<result property="targetCadenceMin" column="target_cadence_min"/>
<result property="targetCadenceMax" column="target_cadence_max"/>
<result property="averageCadence" column="average_cadence"/>
<result property="averageHeartRateBpm" column="average_heart_rate_bpm"/>
```

## 5. 추천 기능 확장 대비

이번 작업에서는 추천 알고리즘을 변경하지 않아도 된다. 다만 향후 다음 방식으로 사용할 수 있도록 원본 케이던스 값을 보존한다.

- 회원 목표 케이던스와 음악 BPM의 직접 매칭
- `케이던스 ≈ BPM`인 1:1 매칭
- `케이던스 ≈ BPM × 2`인 하프타임 매칭
- 최근 러닝 기록의 평균 케이던스 계산
- 목표 케이던스와 실제 케이던스의 편차 분석
- 케이던스 안정성을 고려한 플레이리스트 정렬

추천 계산 결과를 DB에 미리 저장하지 말고, 우선 원본 SPM 값만 저장한다.

향후 추천 점수 예시:

```text
cadenceDifference =
    min(
        abs(targetCadence - musicBpm),
        abs(targetCadence - musicBpm * 2)
    )
```

## 6. 더미 데이터

`dummy.sql`의 기존 러닝 프로필과 러닝 기록에 현실적인 케이던스 값을 추가한다.

```sql
target_cadence_min = 165
target_cadence_max = 180
average_cadence = 172
average_heart_rate_bpm = 154
```

기존 데이터는 `NULL`이어도 정상 조회되어야 한다.

## 7. 오류 처리

가능하면 케이던스 전용 오류 코드를 추가한다.

- `INVALID_CADENCE_VALUE`
- `INVALID_CADENCE_RANGE`
- `INVALID_HEART_RATE_VALUE`

케이던스 오류 메시지에는 단위가 SPM이라는 점을 명시한다. 심박수 오류 메시지에는 해당 값이 평균 심박수이며 단위가 BPM이라는 점을 명시한다.

음악 BPM 관련 오류 코드와 평균 심박수 오류 코드를 공유하지 않는다.

## 8. 테스트 항목

- 케이던스 없이 기존 API 사용 가능
- 평균 케이던스 저장·수정·조회
- 평균 심박수 저장·수정·조회
- 목표 케이던스 범위 저장·조회
- 60과 240 경계값 허용
- 59와 241 거부
- 평균 심박수 30과 240 경계값 허용
- 평균 심박수 29와 241 거부
- 평균 심박수를 생략하거나 `null`로 전달해도 기존 API 사용 가능
- 최소값만 입력한 프로필 거부
- 최대값만 입력한 프로필 거부
- 최소값이 최대값보다 큰 경우 거부
- 기존 러닝 기록과 프로필 조회 시 오류 없음
- Mapper의 `INSERT`·`UPDATE`·`SELECT` 필드 누락 여부 확인

## 작업 제한

- 음악 BPM 필드와 케이던스 필드를 혼용하지 않는다.
- 음악 BPM 필드와 평균 심박수 BPM 필드를 혼용하지 않는다.
- 평균 심박수를 단순히 `bpm`이라는 이름으로 저장하거나 응답하지 않는다.
- 케이던스에서 BPM을 자동 생성하거나 반대로 변환해 저장하지 않는다.
- 평균 심박수로 음악 BPM이나 케이던스를 자동 생성해 저장하지 않는다.
- 기존 API 필드의 이름이나 의미는 변경하지 않는다.
- 기존 데이터에 임의의 케이던스 또는 평균 심박수 값을 일괄 입력하지 않는다.
