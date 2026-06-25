# 잉여 자원 정리 보고서

## 1. 정리 목적

현재 프로젝트의 실제 참조 관계를 기준으로 기능 동작과 무관한 잉여 자원을 보수적으로 정리한다. 삭제 판단은 Java, Vue, JSP, Mapper XML, Docker, 환경 변수, 문서 참조를 함께 확인했으며, 참조 가능성이 남은 항목은 삭제하지 않고 검토 필요 항목으로 분리했다.

## 2. 프로젝트 구조 요약

| 영역 | 실제 구조 | 확인 내용 |
|---|---|---|
| Backend 진입점 | `ssafit/src/main/java/com/ssafy/ssafit/SsafitApplication.java` | Spring Boot WAR 애플리케이션이다. |
| Backend 계층 | `controller`, `service`, `mapper`, `domain`, `dto` | 회원, 음악, 리뷰, 플레이리스트, 추천, 러닝 기록, 러닝 세션, Spotify 도메인별 구조가 존재한다. |
| Frontend 진입점 | `frontend/src/main.js`, `frontend/src/App.vue` | Vue 3 + Vite 구조이며 `frontend/src/router/index.js`가 화면 라우팅을 담당한다. |
| JSP 구조 | `ssafit/src/main/webapp/WEB-INF/views` | 로그인, 회원가입, 마이페이지, 러닝 프로필은 JSP 흐름이 유지된다. |
| MyBatis | `ssafit/src/main/resources/mappers/**` | Mapper 인터페이스와 XML이 도메인별로 연결되어 있다. |
| Docker | `compose.yaml`, `ssafit/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf` | MySQL, Backend, Frontend 컨테이너 실행 구조가 있다. |
| 환경 변수 | `.env.example`, `frontend/.env.example`, `compose.yaml`, `application*.yml` | DB, CORS, Spotify, AI 러닝 리포트, Vite API/JSP URL이 분리되어 있다. |
| Spotify | `ssafit/src/main/java/com/ssafy/ssafit/spotify`, `frontend/src/api/spotifyApi.js` | OAuth, 검색, 플레이리스트 내보내기 관련 코드가 존재하므로 삭제 대상에서 제외했다. |

## 3. 삭제한 항목

| 구분 | 대상 | 삭제 이유 | 검증 결과 |
|---|---|---|---|
| Placeholder | `ssafit/src/main/java/com/ssafy/ssafit/**/.gitkeep` 37개 | 각 디렉터리에 실제 Java 파일 또는 하위 패키지가 존재하여 빈 디렉터리 보존 목적이 사라졌다. Spring Bean, import, 라우트, Mapper 연결과 무관한 placeholder 파일이다. | `git ls-files "**/.gitkeep"`와 디렉터리별 tracked file 수로 확인했다. |
| Placeholder | `ssafit/src/main/resources/mappers/**/.gitkeep` 9개 | 각 Mapper 디렉터리에 실제 XML Mapper가 존재하여 빈 디렉터리 보존 목적이 사라졌다. MyBatis XML 자체는 삭제하지 않았다. | 각 디렉터리에 1개 이상의 XML Mapper가 존재함을 확인했다. |

## 4. 유지한 항목

| 대상 | 유지 이유 |
|---|---|
| `frontend/src/components/MusicSearchBar.vue` | `MusicListView.vue`에서 import 및 렌더링된다. |
| `frontend/src/components/SaveStatus.vue` | 여러 Vue 화면과 `SpotifyExportPanel.vue`, `RecommendationResult.vue`에서 공통 상태 표시 컴포넌트로 사용된다. |
| `frontend/src/api/memberApi.js` | Vue 화면 import는 없지만 기존 산출물과 인수인계 문서에서 회원 API 클라이언트 근거로 참조된다. 요구사항 문서 영향이 크므로 삭제하지 않았다. |
| `frontend/src/api/runningProfileApi.js` | Vue Router 전용 화면은 없지만 러닝 프로필 API 클라이언트 및 기존 요구사항 근거로 명시되어 있어 삭제하지 않았다. |
| `frontend/pnpm-lock.yaml`, `frontend/pnpm-workspace.yaml` | README와 인수인계 문서가 pnpm 실행을 안내하고 있어 중복 lock으로 단정할 수 없다. |
| `ssafit/src/main/webapp/WEB-INF/views/**/*.jsp`, `common/nav.jspf` | Spring View Controller에서 로그인, 회원가입, 마이페이지, 러닝 프로필 흐름으로 사용된다. |
| `ssafit/src/main/resources/static/css/member.css` | JSP 페이지에서 `<c:url value='/css/member.css'/>`로 참조된다. |
| `ssafit/src/main/resources/static/images/running-landing.webp` | `member.css`의 background image로 사용된다. |
| `ssafit/src/main/resources/static/fonts/*.ttf`, `frontend/src/assets/fonts/*.ttf` | 각각 JSP CSS와 Vue 전역 CSS에서 `@font-face`로 참조된다. |
| `ssafit/src/main/resources/db/**` | Docker 초기화, 테스트, 마이그레이션 문서와 연결 가능성이 있어 스키마/데이터 파일은 삭제하지 않았다. |
| Spotify OAuth 및 export 관련 코드 | OAuth callback, 외부 API 정책, 사용자 권한과 연결되어 있어 보수적으로 유지했다. |
| Docker 관련 파일 | `compose.yaml`, Dockerfile, nginx 설정 모두 실행 구조에 포함된다. |

## 5. 검토 필요 항목

| 대상 | 검토가 필요한 이유 | 권장 조치 |
|---|---|---|
| `frontend/src/api/memberApi.js` | 현재 Vue 화면에서는 직접 import되지 않지만 `authApi.getSession()`과 기능 영역이 일부 중복된다. | Vue 마이페이지를 JSP로 유지할지, Vue 화면으로 전환할지 결정한 뒤 API 클라이언트 통합 여부를 판단한다. |
| `frontend/src/api/runningProfileApi.js` | API 클라이언트는 있으나 Vue Router 전용 러닝 프로필 화면이 없다. | 화면 설계 단계에서 JSP 유지 또는 Vue 전환을 결정한 뒤 정리한다. |
| PowerShell 기본 출력 인코딩 | `Get-Content` 기본 출력에서는 한글이 깨져 보일 수 있으나 `Get-Content -Encoding UTF8`로 README와 본 보고서가 정상 표시됨을 확인했다. | 문서 확인 시 UTF-8 명시 읽기 또는 에디터 표시를 기준으로 검토한다. |
| `docs/integration/b-ui-api-contract-audit.md`의 lock 파일 설명 | 현재 `package-lock.json`, `pnpm-lock.yaml`이 존재하지만 문서에는 lock 파일이 없다고 적힌 오래된 내용이 있다. | 문서 현행화 단계에서 수정한다. |
| `.metadata/.plugins/org.eclipse.swt/**/LOG.old` | 프로젝트 코드가 아니라 로컬 IDE/WebView 캐시 파일이다. | 저장소 추적 대상이 아니면 `.gitignore` 정책만 확인하고, IDE 캐시는 별도 로컬 정리 대상으로 분리한다. |

## 6. 의존성 정리 결과

### Frontend

- 제거한 npm package: 없음
- 유지한 package: `axios`, `vue`, `vue-router`, `@vitejs/plugin-vue`, `vite`, `@playwright/test`
- 제거하지 않은 이유: 모든 의존성이 API 통신, Vue 런타임, 라우터, Vite 빌드, E2E 테스트 설정과 직접 연결되어 있다. pnpm 관련 파일도 README와 문서 실행 경로에 남아 있어 제거하지 않았다.

### Backend

- 제거한 Maven dependency: 없음
- 유지한 dependency: Spring Web, JSP/JSTL, Validation, MyBatis, MySQL Connector/J, Lombok, DevTools, Spring Boot Test
- 제거하지 않은 이유: JSP 혼합 구조, Validation annotation, MyBatis Mapper, MySQL 런타임, Lombok annotation processing, 테스트 코드와 연결되어 있다. DevTools는 런타임 optional 개발 편의 의존성이므로 자동 설정 영향 가능성을 고려해 유지했다.

## 7. 검증 결과

| 항목 | 결과 |
|---|---|
| Backend test | 실패: `.\mvnw.cmd clean test`가 Maven enforcer에서 중단되었다. 현재 Maven Java는 `1.8.0_192`이며 프로젝트는 Java 25를 요구한다. 삭제 항목과 무관한 환경 조건 실패다. |
| Frontend install/build | 성공: `npm.cmd install`은 변경 없이 완료되었고, `npm.cmd run build`가 정상 통과했다. |
| Docker build/config | 성공: placeholder 환경 변수로 `docker compose config --quiet`를 실행해 Compose 설정 해석을 확인했다. 실제 `docker compose up --build`는 장기 실행 서비스 기동을 피하기 위해 수행하지 않았다. |
| 주요 기능 영향 여부 | 삭제 대상이 `.gitkeep` placeholder뿐이므로 기능 코드, 화면, DB 스키마, Mapper XML, Docker 설정에는 영향이 없다. Frontend build와 Docker config로 영향 없음도 확인했다. |

## 8. 주의 사항

- 이번 정리는 기능 코드, 화면 디자인, DB 스키마, Mapper XML, Spotify OAuth, 인증 경로를 변경하지 않았다.
- Vue 화면에서 직접 사용하지 않는 API 클라이언트라도 기존 요구사항 산출물의 근거로 쓰이는 항목은 삭제하지 않았다.
- 요구사항 정의서와 Use Case Diagram은 삭제 대상이 placeholder 파일뿐이므로 기능 상태 변화가 없어 수정하지 않았다.
- 향후 별도 리팩터링이 필요한 영역은 JSP/Vue 회원 흐름 통합 여부, 러닝 프로필 Vue 화면 연결 여부, 문서 인코딩/현행화이다.
