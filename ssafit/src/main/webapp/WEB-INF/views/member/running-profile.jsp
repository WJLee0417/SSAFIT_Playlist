<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>러닝 프로필 | SSAFIT Playlist</title>
    <link rel="stylesheet" href="<c:url value='/css/member.css'/>">
</head>
<body>
<%@ include file="/WEB-INF/views/common/nav.jspf" %>
<main class="page-shell profile-shell">
    <section class="page-heading">
        <h1>나의 프로필</h1>
        <p>최근 러닝 기록을 바탕으로 운동 목표를 추천합니다</p>
    </section>

    <section class="card profile-card">
        <c:if test="${not empty successMessage}">
            <div class="alert alert-success"><c:out value="${successMessage}"/></div>
        </c:if>

        <section class="profile-score-panel" aria-label="Running Fitness Score">
            <div>
                <span class="eyebrow">FITNESS SCORE</span>
                <h2>
                    <c:choose>
                        <c:when test="${runningFitnessScore ne null}">
                            <c:out value="${runningFitnessScore}"/><small>/100</small>
                        </c:when>
                        <c:otherwise>--<small>/100</small></c:otherwise>
                    </c:choose>
                </h2>
                <p>*최근 러닝의 거리, 심박 효율, 케이던스 안정성, 기록 변동성을 100점 만점으로 환산</p>
            </div>
            <dl class="profile-score-breakdown">
                <div>
                    <dt>평균 거리</dt>
                    <dd><c:out value="${averageDistanceScore == null ? '-' : averageDistanceScore}"/></dd>
                </div>
                <div>
                    <dt>심박 효율</dt>
                    <dd><c:out value="${paceEfficiencyScore == null ? '-' : paceEfficiencyScore}"/></dd>
                </div>
                <div>
                    <dt>케이던스 안정성</dt>
                    <dd><c:out value="${cadenceStabilityScore == null ? '-' : cadenceStabilityScore}"/></dd>
                </div>
                <div>
                    <dt>기록 변동성</dt>
                    <dd><c:out value="${variationScore == null ? '-' : variationScore}"/></dd>
                </div>
            </dl>
        </section>

        <form:form method="post" modelAttribute="profileForm" cssClass="stack-form">
            <c:if test="${not empty returnTo}">
                <input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>">
            </c:if>
            <form:errors path="*" element="div" cssClass="alert alert-error"/>

            <div class="two-column">
                <div>
                    <label for="gender">성별</label>
                    <form:select path="gender" id="gender">
                        <form:option value="">선택 안 함</form:option>
                        <form:option value="FEMALE">여성</form:option>
                        <form:option value="MALE">남성</form:option>
                        <form:option value="OTHER">기타</form:option>
                        <form:option value="UNSPECIFIED">응답하지 않음</form:option>
                    </form:select>
                    <form:errors path="gender" cssClass="field-error"/>
                </div>
                <div>
                    <label for="age">나이 (만)</label>
                    <form:input path="age" id="age" type="number"
                                min="10" max="100" placeholder="예: 32"/>
                    <form:errors path="age" cssClass="field-error"/>
                </div>
            </div>
            <small class="muted">
                추정 최대 심박수는 = 220 - 나이.
                <c:if test="${profileForm.age ne null}">
                    현재 입력 기준 <strong><c:out value="${220 - profileForm.age}"/> BPM</strong>
                </c:if>
            </small>

            <section class="auto-target-summary" aria-label="자동 산출 기준값">
                <span class="eyebrow">SUMMARY</span>
                <div class="summary-highlight-grid">
                    <div class="summary-highlight">
                        <span>누적 거리</span>
                        <strong><c:out value="${totalDistanceKmText}"/></strong>
                        <small>km</small>
                    </div>
                    <div class="summary-highlight">
                        <span>평균 페이스</span>
                        <strong><c:out value="${averagePaceText}"/></strong>
                        <small>/km</small>
                    </div>
                </div>
                <dl>
                    <div>
                        <dt>최근 심박 범위</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${profileForm.targetBpmMin ne null and profileForm.targetBpmMax ne null}">
                                    <c:out value="${profileForm.targetBpmMin}"/> - <c:out value="${profileForm.targetBpmMax}"/> BPM
                                </c:when>
                                <c:otherwise>기록 등록 후 자동 산출</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                    <div>
                        <dt>최근 케이던스 범위</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${profileForm.targetCadenceMin ne null and profileForm.targetCadenceMax ne null}">
                                    <c:out value="${profileForm.targetCadenceMin}"/> - <c:out value="${profileForm.targetCadenceMax}"/> SPM
                                </c:when>
                                <c:otherwise>기록 등록 후 자동 산출</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                </dl>
            </section>

            <label for="preferredGenre">선호 장르</label>
            <form:input path="preferredGenre" id="preferredGenre" maxlength="100"
                        placeholder="예: POP, ROCK, EDM"/>
            <form:errors path="preferredGenre" cssClass="field-error"/>

            <div class="button-row">
                <a class="secondary-button" href="<c:url value='/members/me'/>">마이페이지로</a>
                <button class="primary-button inline-button" type="submit">프로필 저장</button>
            </div>
        </form:form>
    </section>
</main>
</body>
</html>
