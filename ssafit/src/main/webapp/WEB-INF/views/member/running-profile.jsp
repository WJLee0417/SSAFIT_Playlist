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
        <span class="eyebrow">RUNNING PROFILE</span>
        <h1>나의 러닝 기준 설정</h1>
        <p>저장한 강도와 BPM 범위는 자동 큐레이션의 실제 Service 계약에 사용됩니다.</p>
    </section>

    <section class="card profile-card">
        <c:if test="${not empty successMessage}">
            <div class="alert alert-success"><c:out value="${successMessage}"/></div>
        </c:if>

        <form:form method="post" modelAttribute="profileForm" cssClass="stack-form">
            <c:if test="${not empty returnTo}">
                <input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>">
            </c:if>
            <form:errors path="*" element="div" cssClass="alert alert-error"/>

            <fieldset>
                <legend>러닝 강도 <span class="required">*</span></legend>
                <div class="choice-grid">
                    <label><form:radiobutton path="runningIntensity" value="LOW"/> 가볍게</label>
                    <label><form:radiobutton path="runningIntensity" value="MODERATE"/> 보통</label>
                    <label><form:radiobutton path="runningIntensity" value="HIGH"/> 강하게</label>
                    <label><form:radiobutton path="runningIntensity" value="INTERVAL"/> 인터벌</label>
                </div>
                <form:errors path="runningIntensity" cssClass="field-error"/>
            </fieldset>

            <div class="two-column">
                <div>
                    <label for="targetBpmMin">최소 BPM <span class="required">*</span></label>
                    <form:input path="targetBpmMin" id="targetBpmMin" type="number"
                                min="60" max="220" placeholder="예: 120"/>
                    <form:errors path="targetBpmMin" cssClass="field-error"/>
                </div>
                <div>
                    <label for="targetBpmMax">최대 BPM <span class="required">*</span></label>
                    <form:input path="targetBpmMax" id="targetBpmMax" type="number"
                                min="60" max="220" placeholder="예: 160"/>
                    <form:errors path="targetBpmMax" cssClass="field-error"/>
                </div>
            </div>

            <div class="two-column">
                <div>
                    <label for="targetCadenceMin">목표 케이던스 최소값 (SPM)</label>
                    <form:input path="targetCadenceMin" id="targetCadenceMin"
                                type="number" min="60" max="240"
                                placeholder="예: 165"/>
                    <form:errors path="targetCadenceMin" cssClass="field-error"/>
                </div>
                <div>
                    <label for="targetCadenceMax">목표 케이던스 최대값 (SPM)</label>
                    <form:input path="targetCadenceMax" id="targetCadenceMax"
                                type="number" min="60" max="240"
                                placeholder="예: 180"/>
                    <form:errors path="targetCadenceMax" cssClass="field-error"/>
                </div>
            </div>
            <small class="muted">목표 케이던스는 두 값을 함께 입력하거나 모두 비워 두세요.</small>

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
