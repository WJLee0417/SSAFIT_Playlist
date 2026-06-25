# AI 러닝 리포트 기능 발표 자료

## 1. 기능 목적

AI 러닝 리포트는 사용자의 최근 러닝 기록과 음악 추천 로그를 사람이 읽기 쉬운 한국어 리포트로 요약하는 기능이다. 기존 화면은 거리, 시간, 페이스, 케이던스, 심박, 추천 BPM, 추천 장르처럼 숫자와 로그가 중심이기 때문에 사용자가 직접 패턴을 해석해야 했다.

이 기능은 서버가 계산한 통계와 추천 로그만 LLM에 전달하고, LLM은 계산이 아니라 해석과 문장화를 담당한다. 결과적으로 사용자는 최근 러닝 패턴, 페이스와 강도 변화, 음악 선택 방향, 다음 러닝 제안을 한 카드에서 확인할 수 있다.

## 2. LLM 활용 전후 비교

### 활용 전

- 거리
- 시간
- 페이스
- 케이던스
- 심박
- 추천 BPM
- 추천 장르
- 추천 로그 목록

### 활용 후

- 러닝 패턴 요약
- 페이스·강도 해석
- 케이던스·심박 참고
- 다음 음악 방향
- 다음 러닝 제안

## 3. 주요 프롬프트 구조

실제 프롬프트에는 API Key, 회원 ID, 이메일, 닉네임, 실제 회원 데이터가 포함되지 않는다. 입력은 `AiRunningReportAggregationService`가 만든 집계 DTO의 JSON 스냅샷이다.

### 시스템 프롬프트 핵심 지시

- 응답은 한국어 JSON 객체만 반환한다.
- 제공된 JSON 외의 사실을 만들지 않는다.
- 거리, 페이스, 심박, 케이던스 수치를 새로 계산하거나 추측하지 않는다.
- 심박 데이터는 건강 상태, 질병, 부상, 치료 조언으로 해석하지 않는다.
- 데이터가 부족하면 부족하다고 명시한다.
- 음악 방향은 BPM 범위, 강도, 장르 선호가 존재할 때만 제안한다.
- 목표는 무리한 수치 목표가 아니라 현실적인 다음 러닝 방향으로 작성한다.
- JSON 필드는 `runningPatternSummary`, `paceAndIntensityInsight`, `cadenceAndHeartRateInsight`, `musicRecommendationDirection`, `nextRunningGoal`, `dataLimitations`만 사용한다.
- 각 필드는 화면 표시를 고려해 짧은 1~2문장으로 작성한다.

### 사용자 프롬프트 핵심 구조

```text
아래 집계 JSON은 서버가 계산한 확정 통계입니다.
수치를 새로 계산하지 말고, 값이 null이거나 status가 NOT_AVAILABLE,
INSUFFICIENT, LIMITED인 항목은 데이터 한계로 설명하세요.

집계 JSON:
{ ...서버 집계 DTO 스냅샷... }
```

### 출력 JSON 형식

```json
{
  "runningPatternSummary": "최근 러닝 패턴 요약",
  "paceAndIntensityInsight": "페이스와 강도 변화 해석",
  "cadenceAndHeartRateInsight": "케이던스와 심박 참고 설명",
  "musicRecommendationDirection": "BPM, 장르, 플레이리스트 기반 음악 방향",
  "nextRunningGoal": "다음 러닝 제안",
  "dataLimitations": "부족하거나 제한적인 데이터 설명"
}
```

## 4. 구현 코드 캡처 대상

### 데이터 집계 서비스

- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportAggregationService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/dto/AiRunningReportAggregationDtos.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningrecord/mapper/RunningRecordMapper.java`
- `ssafit/src/main/resources/mappers/runningrecord/RunningRecordMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/recommendation/mapper/RecommendationLogMapper.java`
- `ssafit/src/main/resources/mappers/recommendation/RecommendationLogMapper.xml`

### LLM Provider Interface 및 구현체

- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/provider/AiRunningReportLlmProvider.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/provider/OpenAiRunningReportProvider.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/provider/AiRunningReportProviderResponse.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/provider/AiRunningReportProviderException.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/provider/AiRunningReportProviderFailureReason.java`

### AI 리포트 생성 서비스

- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportGenerationService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportStorageService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportResponseAssembler.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/dto/AiRunningReportContentDtos.java`

### Controller

- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/controller/api/AiRunningReportApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/dto/AiRunningReportApiDtos.java`

### MyBatis Mapper

- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/mapper/AiRunningReportMapper.java`
- `ssafit/src/main/resources/mappers/airunningreport/AiRunningReportMapper.xml`
- `ssafit/src/main/resources/sql/schema.sql`
- `ssafit/src/main/resources/sql/migrations/20260624_add_ai_running_reports.sql`

### Vue 리포트 카드 컴포넌트

- `frontend/src/components/AiRunningReportCard.vue`
- `frontend/src/api/aiRunningReportApi.js`
- `frontend/src/views/RunningRecordListView.vue`
- `frontend/src/styles.css`

### 환경 변수 설정

- `.env.example`
- `compose.yaml`
- `ssafit/src/main/resources/application.yml`
- `ssafit/src/main/resources/application-docker.yml`
- `ssafit/src/test/resources/application-mapper-test.yml`
- `ssafit/src/test/resources/application-e2e.yml`

### 테스트 코드

- `ssafit/src/test/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportAggregationServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportStorageServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportGenerationServiceTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/airunningreport/controller/api/AiRunningReportApiControllerTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/mapper/AiRunningReportMapperIntegrationTest.java`

## 5. 발표용 시연 흐름

### 2~3분 시연 시나리오

1. 기존 러닝 기록과 추천 로그 화면을 보여준다.
   - 러닝 기록 목록에서 거리, 시간, 페이스, 케이던스, 심박처럼 숫자 중심 정보가 쌓여 있음을 보여준다.
   - 추천 화면 또는 추천 로그에서 BPM, 장르, 강도, 생성된 플레이리스트 정보를 확인한다.

2. 러닝 기록 화면의 AI 리포트 카드에서 생성 버튼을 클릭한다.
   - 최초 사용자는 최신 리포트가 없으므로 생성 버튼이 노출된다.
   - 생성 중에는 버튼이 잠기고 로딩 상태가 표시된다.
   - 최근 데이터가 동일하면 이후 요청은 저장된 리포트를 재사용한다.

3. 생성된 리포트의 핵심 섹션을 확인한다.
   - 러닝 패턴: 최근 기록의 흐름을 문장으로 확인한다.
   - 페이스·강도: 평균 페이스와 강도 분포를 바탕으로 해석한다.
   - 케이던스·심박 참고: 건강 진단이 아니라 운동 기록 참고 정보로만 표시한다.
   - 음악 추천 방향: BPM 범위와 장르 데이터가 있을 때만 다음 음악 방향을 제안한다.
   - 다음 러닝 제안: 무리한 목표 대신 최근 흐름을 이어가는 방향을 제안한다.

4. 데이터 부족 또는 fallback 상태를 시연한다.
   - 러닝 기록이 없거나 주요 항목이 부족하면 데이터 부족 안내가 표시된다.
   - AI 기능이 꺼져 있거나 LLM 호출이 실패하면 `FALLBACK` 출처의 안전한 리포트가 표시된다.

5. 구조를 설명한다.
   - 백엔드가 최근 기록과 추천 로그를 조회하고 통계를 계산한다.
   - LLM에는 개인정보가 제거된 집계 DTO만 전달된다.
   - LLM은 계산하지 않고 해석 문장 JSON만 반환한다.
   - 응답은 DB에 저장되어 최신 리포트 조회와 캐시 재사용에 활용된다.

## 6. 캡처 대상 체크리스트

### 화면 캡처

- [ ] 러닝 기록 목록 화면
- [ ] 음악 추천 결과 또는 추천 로그 화면
- [ ] AI 리포트 없음 상태와 생성 버튼
- [ ] AI 리포트 생성 중 로딩 상태
- [ ] 생성 완료 리포트 카드
- [ ] fallback 리포트 표시 상태
- [ ] 데이터 부족 안내 상태
- [ ] 모바일 너비의 리포트 카드

### 코드 캡처

- [ ] 집계 서비스가 최근 러닝 기록과 추천 로그를 모으는 부분
- [ ] 집계 DTO에 개인정보가 없는 구조
- [ ] 시스템 프롬프트와 사용자 프롬프트 핵심 지시
- [ ] LLM Provider Interface와 OpenAI 구현 분리
- [ ] fallback 저장 처리
- [ ] 최신 리포트 조회/생성 API Controller
- [ ] `ai_running_reports` 저장 Mapper와 XML
- [ ] Vue 카드의 상태 분기
- [ ] Docker/application 환경 변수 설정
- [ ] Fake Provider 기반 테스트

## 7. 한계와 안전장치

- 데이터 부족 시 숫자를 0으로 보정하거나 과도하게 해석하지 않는다.
- 심박과 케이던스는 운동 기록 참고 정보로만 사용하고 건강 진단을 제공하지 않는다.
- API Key는 서버 환경 변수 또는 비밀 관리 영역에서만 관리하며 브라우저와 Git에 노출하지 않는다.
- LLM 호출 실패, timeout, JSON 파싱 실패, provider 비활성 상태에서는 fallback 리포트를 저장하고 반환한다.
- LLM 입력에는 회원 ID, 이메일, 닉네임 같은 개인정보를 포함하지 않는다.
- 리포트 생성 결과는 상태, 출처, 프롬프트 버전, 모델명, 입력 스냅샷 해시와 함께 저장되어 캐시 재사용과 문제 추적이 가능하다.

## 8. 발표 시 강조 문장

- "AI가 숫자를 계산하는 것이 아니라, 서버가 검증한 통계를 사람이 읽기 쉬운 문장으로 바꿉니다."
- "러닝 기록과 음악 추천 로그를 연결해서 다음 운동과 음악 선택 방향을 한 번에 보여줍니다."
- "데이터가 부족하거나 LLM이 실패해도 추측하지 않고 fallback 리포트로 안전하게 응답합니다."
- "API Key와 사용자 개인정보는 LLM 입력과 브라우저에 노출하지 않습니다."
