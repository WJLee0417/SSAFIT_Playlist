<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>회원가입 | SSAFIT Playlist</title>
    <link rel="stylesheet" href="<c:url value='/css/member.css'/>">
</head>
<body>
<%@ include file="/WEB-INF/views/common/nav.jspf" %>
<main class="page-shell auth-shell">
    <section class="auth-copy">
        <span class="eyebrow">RUN WITH YOUR RHYTHM</span>
        <h1>나만의 러닝 플레이리스트를 시작하세요.</h1>
        <p>러닝 강도와 목표 BPM을 설정하면 운동 흐름에 어울리는 음악을 더 쉽게 찾을 수 있습니다.</p>
        <ul class="feature-list">
            <li>Spotify 음악 검색과 플레이리스트 구성</li>
            <li>프로필을 활용한 자동 큐레이션</li>
        </ul>
    </section>

    <section class="card form-card">
        <div class="card-heading">
            <span class="eyebrow">CREATE ACCOUNT</span>
            <h2>회원가입</h2>
            <p><span class="required">*</span> 표시는 필수 입력값입니다.</p>
        </div>

        <form:form method="post" modelAttribute="joinForm" cssClass="stack-form">
            <form:errors path="*" element="div" cssClass="alert alert-error"/>

            <label for="loginId">로그인 ID <span class="required">*</span></label>
            <form:input path="loginId" id="loginId" autocomplete="username"
                        maxlength="50" placeholder="사용할 로그인 ID"/>
            <form:errors path="loginId" cssClass="field-error"/>

            <label for="password">비밀번호 <span class="required">*</span></label>
            <form:password path="password" id="password" autocomplete="new-password"
                           maxlength="100" placeholder="8자 이상 입력"/>
            <form:errors path="password" cssClass="field-error"/>

            <label for="email">이메일 <span class="required">*</span></label>
            <form:input path="email" id="email" type="email" autocomplete="email"
                        maxlength="255" placeholder="runner@example.com"/>
            <form:errors path="email" cssClass="field-error"/>

            <label for="nickname">닉네임 <span class="required">*</span></label>
            <form:input path="nickname" id="nickname" autocomplete="nickname"
                        maxlength="100" placeholder="화면에 표시할 이름"/>
            <form:errors path="nickname" cssClass="field-error"/>

            <button class="primary-button" type="submit">계정 만들기</button>
        </form:form>
        <c:url var="spotifyLoginUrl" value="/login/spotify"/>
        <a class="spotify-login-button" href="${spotifyLoginUrl}">Spotify로 로그인</a>
        <p class="form-footer">이미 계정이 있나요? <a href="<c:url value='/login'/>">로그인</a></p>
    </section>
</main>
</body>
</html>
