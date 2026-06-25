# 산출물 품질 체크리스트

| 검증 항목 | 결과 | 근거 | 조치/비고 |
|---|---|---|---|
| 요구사항 ↔ Use Case 연결 여부 | PASS | `traceability-master.md`에서 FR-001~FR-024 전부 UC 연결 | 없음 |
| 요구사항 ↔ 화면 연결 여부 | PASS | `screen-catalog.csv`, `traceability-master.md` | FR-022는 실행 환경이라 화면 없음, FR-020은 화면 미구현으로 명시 |
| 화면 ↔ API 연결 여부 | PASS | `screen-specification.md`의 API 연동 항목 | SCR-032는 API only로 별도 표시 |
| API ↔ DB 테이블 연결 여부 | PASS | `erd-data-dictionary.md`, `relationship-audit.md` | 공통 오류/실행 환경은 DB 테이블 없음 |
| WBS ↔ 요구사항 연결 여부 | PASS | `wbs.md`, `wbs.csv` | 9.4, 12.6, 12.7 상태를 최종 산출물 기준 DONE으로 갱신 |
| Gantt ↔ WBS 연결 여부 | PASS | `gantt.csv`, `gantt.mmd`, `wbs.csv` | G-803, G-605를 최종 산출물 기준 DONE으로 갱신 |
| 구현 상태 일관성 | PASS | 요구사항 상태를 기준으로 Use Case/Screen/WBS/Gantt에서 위험 항목 분리 | 외부 연동은 `UNVERIFIED`/`PARTIAL`로 유지 |
| Spotify 관련 제약 명시 여부 | PASS | `requirements-definition.md`, `use-case-specification.md`, `screen-design-gap.md`, `schedule-assumptions.md` | OAuth redirect URI, scope, 계정 권한, rate limit 명시 |
| 미구현 기능의 명확한 표시 여부 | PASS | `screen-catalog.csv`, `screen-design-gap.md`, `open-issues.md` | Spotify 가져오기 화면은 화면 미구현, Vue 러닝 프로필 화면은 후속 SPA 통합 과제로 분리 |
| 발표 시연 흐름의 실현 가능성 | NEEDS_REVIEW | `screen-flow.mmd`, `gantt.csv`, `schedule-assumptions.md` | Spotify/AI/Docker/Java 25/브라우저 수동 검증 필요 |

## 상태별 최종 판단

| 산출물 | 판단 | 이유 |
|---|---|---|
| 요구사항 정의서 | PASS | FR-001~FR-024가 근거와 상태를 포함하며 외부 검증 항목을 완료로 오인하지 않는다. |
| Use-Case Diagram | PASS | Actor와 Use Case가 요구사항과 연결되고 Spotify/AI는 partial로 구분된다. |
| ER Diagram | NEEDS_REVIEW | DDL/Mapper 기준 문서는 완성됐지만 실제 운영 DB 접속 검증은 수행하지 않았다. |
| WBS | PASS | Gantt/화면 설계 후속 작업 상태를 현재 산출물 기준으로 갱신했다. |
| Gantt Chart | PASS | 완료 산출물과 잔여 검증 작업이 구분되어 있다. |
| 화면 설계서 | PASS | Vue/JSP/API only 화면이 분리되고 FR-019/FR-020 ID 불일치를 수정했다. 러닝 프로필은 JSP 화면을 발표 기준 구현 화면으로 확정했다. |

## 발표 시연 가능 흐름 점검

| 흐름 | 가능성 | 조건 |
|---|---|---|
| 회원가입 → 로그인 → 마이페이지 | 높음 | JSP/세션 흐름 브라우저 확인 필요 |
| 음악 목록 → 상세 → 리뷰 | 높음 | 로그인 세션과 데모 음악 데이터 필요 |
| 플레이리스트 목록 → 상세 → 수록곡 관리 | 높음 | 데모 playlist/music 데이터 필요 |
| 러닝 기록/세션 CRUD | 높음 | Running E2E 근거 있음, 발표 환경 재확인 필요 |
| 자동 큐레이션 → 추천 저장 | 중간~높음 | 러닝 프로필/음악 BPM/장르 데이터 필요 |
| Spotify 검색/OAuth/export/import | 낮음~중간 | 실제 계정, redirect URI, scope, rate limit 확인 필요 |
| AI 러닝 리포트 | 중간 | Provider 환경 변수 또는 fallback 설명 필요 |
