<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>로그인 | SSAFIT Playlist</title>
    <link rel="stylesheet" href="<c:url value='/css/member.css'/>">
</head>
<body>
<%@ include file="/WEB-INF/views/common/nav.jspf" %>
<main class="page-shell auth-shell">
    <section class="auth-copy">
        <span class="eyebrow">WELCOME BACK</span>
        <h1>오늘의 러닝에도<br>좋은 리듬을.</h1>
        <p>로그인하면 저장한 플레이리스트와 러닝 프로필을 이어서 사용할 수 있습니다.</p>
    </section>

    <section class="card form-card compact-card">
        <div class="card-heading">
            <span class="eyebrow">MEMBER LOGIN</span>
            <h2>로그인</h2>
        </div>

        <c:if test="${not empty successMessage}">
            <div class="alert alert-success"><c:out value="${successMessage}"/></div>
        </c:if>

        <form:form method="post" modelAttribute="loginForm" cssClass="stack-form">
            <form:errors path="*" element="div" cssClass="alert alert-error"/>

            <label for="loginId">로그인 ID</label>
            <form:input path="loginId" id="loginId" autocomplete="username"
                        placeholder="로그인 ID"/>
            <form:errors path="loginId" cssClass="field-error"/>

            <label for="password">비밀번호</label>
            <form:password path="password" id="password" autocomplete="current-password"
                           placeholder="비밀번호"/>
            <form:errors path="password" cssClass="field-error"/>

            <button class="primary-button" type="submit">로그인</button>
        </form:form>
        <p class="form-footer">아직 회원이 아닌가요? <a href="<c:url value='/join'/>">회원가입</a></p>
    </section>
</main>
</body>
</html>
