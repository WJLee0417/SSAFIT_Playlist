<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>마이페이지 | SSAFIT Playlist</title>
    <link rel="stylesheet" href="<c:url value='/css/member.css'/>">
</head>
<body>
<%@ include file="/WEB-INF/views/common/nav.jspf" %>
<main class="page-shell dashboard-shell">
    <section class="hero-panel">
        <div>
            <span class="eyebrow">MY RUNNING SPACE</span>
            <h1><c:out value="${member.nickname}"/>님의 러닝 공간</h1>
            <p>프로필과 음악 취향을 관리하고, Vue 화면에서 플레이리스트를 만들어 보세요.</p>
        </div>
        <a class="primary-button inline-button" href="http://localhost:5173/music">음악 둘러보기</a>
    </section>

    <div class="dashboard-grid">
        <section class="card">
            <div class="section-heading">
                <span class="eyebrow">ACCOUNT</span>
                <h2>회원 정보</h2>
            </div>
            <dl class="info-list">
                <div><dt>로그인 ID</dt><dd><c:out value="${member.loginId}"/></dd></div>
                <div><dt>이메일</dt><dd><c:out value="${member.email}"/></dd></div>
                <div><dt>닉네임</dt><dd><c:out value="${member.nickname}"/></dd></div>
                <div><dt>회원 상태</dt><dd><c:out value="${member.status}"/></dd></div>
            </dl>
        </section>

        <section class="card">
            <div class="section-heading row-heading">
                <div>
                    <span class="eyebrow">RUNNING PROFILE</span>
                    <h2>러닝 프로필</h2>
                </div>
                <a class="text-link" href="<c:url value='/members/me/running-profile'/>">
                    ${empty runningProfile ? '등록하기' : '수정하기'}
                </a>
            </div>
            <c:choose>
                <c:when test="${empty runningProfile}">
                    <div class="empty-state">
                        <strong>아직 러닝 프로필이 없습니다.</strong>
                        <p>러닝 강도와 목표 BPM을 설정하면 자동 큐레이션에 활용됩니다.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <dl class="info-list">
                        <div><dt>러닝 강도</dt><dd><c:out value="${runningProfile.runningIntensity}"/></dd></div>
                        <div><dt>목표 BPM</dt><dd><c:out value="${runningProfile.targetBpmMin}"/> - <c:out value="${runningProfile.targetBpmMax}"/></dd></div>
                        <div><dt>선호 장르</dt><dd><c:out value="${empty runningProfile.preferredGenre ? '설정 안 함' : runningProfile.preferredGenre}"/></dd></div>
                    </dl>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <section class="link-grid" aria-label="Vue 화면 바로가기">
        <a class="feature-card" href="http://localhost:5173/music">
            <span>01</span><strong>음악 탐색</strong><small>검색·상세·평가</small>
        </a>
        <a class="feature-card" href="http://localhost:5173/playlists">
            <span>02</span><strong>플레이리스트</strong><small>생성·수록곡·Spotify 내보내기</small>
        </a>
        <a class="feature-card" href="http://localhost:5173/recommendations">
            <span>03</span><strong>자동 큐레이션</strong><small>러닝 프로필 기반 추천</small>
        </a>
    </section>
</main>
</body>
</html>
