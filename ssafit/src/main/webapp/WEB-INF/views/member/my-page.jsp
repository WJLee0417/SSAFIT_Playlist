<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>마이페이지 | SSAFIT Playlist</title>
    <link rel="stylesheet" href="<c:url value='/css/member.css'/>">
</head>
<body class="mypage-body">
<%@ include file="/WEB-INF/views/common/nav.jspf" %>

<c:if test="${profileRegistrationRequired or spotifyOnboardingRequired or forceIdentityOnboarding}">
    <div class="onboarding-modal-backdrop" role="presentation">
        <section class="onboarding-modal"
                 role="dialog"
                 aria-modal="true"
                 aria-labelledby="onboardingTitle"
                 aria-describedby="onboardingDescription">
            <c:choose>
                <c:when test="${spotifyOnboardingRequired or forceIdentityOnboarding}">
                    <span class="eyebrow">WELCOME</span>
                    <h2 id="onboardingTitle">계정 정보를 입력해 주세요</h2>
                    <p id="onboardingDescription">
                        Spotify로 처음 로그인한 경우 ID와 닉네임을 설정하고 프로필을 등록해 주세요.
                    </p>

                    <c:url var="identityAction" value="/members/me/identity"/>
                    <form:form method="post"
                               action="${identityAction}"
                               modelAttribute="identityForm"
                               cssClass="stack-form onboarding-form">
                        <form:errors path="*" element="div" cssClass="alert alert-error"/>

                        <label for="onboardingLoginId">로그인 ID</label>
                        <form:input path="loginId"
                                    id="onboardingLoginId"
                                    autocomplete="username"
                                    maxlength="50"
                                    placeholder="앞으로 사용할 로그인 ID"/>
                        <form:errors path="loginId" cssClass="field-error"/>

                        <label for="onboardingNickname">닉네임</label>
                        <form:input path="nickname"
                                    id="onboardingNickname"
                                    autocomplete="nickname"
                                    maxlength="100"
                                    placeholder="화면에 표시할 이름"/>
                        <form:errors path="nickname" cssClass="field-error"/>

                        <div class="button-row onboarding-actions">
                            <button class="secondary-button"
                                    type="button"
                                    data-onboarding-dismiss>나중에</button>
                            <button class="primary-button inline-button" type="submit">
                                저장하고 프로필 등록
                            </button>
                        </div>
                    </form:form>
                </c:when>
                <c:otherwise>
                    <span class="eyebrow">RUNNING PROFILE</span>
                    <h2 id="onboardingTitle">러닝 프로필을 등록해 주세요</h2>
                    <p id="onboardingDescription">
                        추천 및 러닝 기록 기능을 사용하려면 프로필 등록이 필요합니다.
                    </p>
                    <div class="button-row onboarding-actions">
                        <button class="secondary-button"
                                type="button"
                                data-onboarding-dismiss>나중에</button>
                        <a class="primary-button inline-button"
                           href="<c:url value='/members/me/running-profile'/>">
                            프로필 등록하기
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</c:if>

<main class="mypage-main">
    <section class="mypage-landing" id="mypageLanding" aria-labelledby="beatRateTitle">
        <div class="mypage-landing-image" aria-hidden="true"></div>
        <div class="mypage-landing-overlay" aria-hidden="true"></div>
        <div class="mypage-landing-content" id="mypageLandingContent">
            <h1 id="beatRateTitle">오늘의<br>BEAT RATE</h1>
            <a class="primary-button mypage-playlist-button"
               href="${frontendOrigin}/playlists">플레이리스트</a>
        </div>
        <span class="mypage-scroll-cue" aria-hidden="true"></span>
    </section>

    <section class="page-shell dashboard-shell mypage-dashboard" id="mypageDashboard">
        <section class="mypage-tile-grid" aria-label="나의 러닝 정보">
            <a class="card mypage-tile mypage-profile-tile mypage-clickable-tile"
               href="<c:url value='/members/me/running-profile'/>"
               aria-labelledby="runningProfileTileTitle">
                <div class="section-heading row-heading">
                    <div>
                        <span class="eyebrow">MY PROFILE</span>
                        <h2 id="runningProfileTileTitle">러닝 프로필</h2>
                    </div>
                </div>
                <dl class="info-list mypage-profile-list">
                    <div><dt>ID</dt><dd><c:out value="${member.loginId}"/></dd></div>
                    <div><dt>닉네임</dt><dd><c:out value="${member.nickname}"/></dd></div>
                </dl>
                <c:choose>
                    <c:when test="${empty runningProfile}">
                        <div class="empty-state">
                            <strong>아직 러닝 프로필이 없습니다.</strong>
                            <p>기본 프로필을 저장하고 러닝 기록을 등록시 스코어가 계산됩니다.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <dl class="info-list mypage-profile-list">
                            <div>
                                <dt>최근 심박 범위</dt>
                                <dd>
                                    <c:choose>
                                        <c:when test="${not empty runningProfile.targetBpmMin and not empty runningProfile.targetBpmMax}">
                                            <c:out value="${runningProfile.targetBpmMin}"/> - <c:out value="${runningProfile.targetBpmMax}"/> BPM
                                        </c:when>
                                        <c:otherwise>기록을 등록해 주세요</c:otherwise>
                                    </c:choose>
                                </dd>
                            </div>
                            <div>
                                <dt>최근 케이던스 범위</dt>
                                <dd>
                                    <c:choose>
                                        <c:when test="${not empty runningProfile.targetCadenceMin and not empty runningProfile.targetCadenceMax}">
                                            <c:out value="${runningProfile.targetCadenceMin}"/> - <c:out value="${runningProfile.targetCadenceMax}"/> SPM
                                        </c:when>
                                        <c:otherwise>기록을 등록해 주세요</c:otherwise>
                                    </c:choose>
                                </dd>
                            </div>
                            <div><dt>선호 장르</dt><dd><c:out value="${empty runningProfile.preferredGenre ? '설정 안 함' : runningProfile.preferredGenre}"/></dd></div>
                        </dl>
                    </c:otherwise>
                </c:choose>
                <span class="mypage-tile-arrow" aria-hidden="true">↗</span>
            </a>

            <a class="card mypage-tile mypage-link-tile"
               href="${frontendOrigin}/running-records">
                <span class="eyebrow">RECORDS</span>
                <div>
                    <strong>나의 러닝 기록</strong>
                    <p>거리와 페이스를 기록하고<br>지난 러닝의 흐름을 확인하세요.</p>
                </div>
                <span class="mypage-tile-arrow" aria-hidden="true">↗</span>
            </a>

            <a class="card mypage-tile mypage-link-tile"
               href="${frontendOrigin}/running-sessions">
                <span class="eyebrow">SESSIONS</span>
                <div>
                    <strong>나의 러닝 세션</strong>
                    <p>함께 달릴 세션을 찾고<br>참여 중인 일정을 관리하세요.</p>
                </div>
                <span class="mypage-tile-arrow" aria-hidden="true">↗</span>
            </a>
        </section>

        <section class="card running-chart-card" aria-labelledby="runningChartTitle">
            <div class="running-chart-heading">
                <div>
                    <h2 id="runningChartTitle">최근 러닝 흐름</h2>
                    <p id="runningChartDescription">최근 7일 동안 기록한 러닝 거리를 보여줍니다.</p>
                </div>
                <div class="running-chart-switch" role="group" aria-label="차트 지표 선택">
                    <button class="running-chart-option is-active"
                            type="button"
                            data-chart-metric="distance"
                            aria-pressed="true">러닝 거리</button>
                    <button class="running-chart-option"
                            type="button"
                            data-chart-metric="pace"
                            aria-pressed="false">평균 페이스</button>
                </div>
            </div>
            <div class="running-chart-canvas-wrap">
                <canvas id="runningTrendChart"
                        hidden
                        role="img"
                        aria-label="최근 7일 러닝 거리 꺾은선 그래프"></canvas>
                <p class="running-chart-state" id="runningChartState">러닝 기록을 불러오는 중입니다.</p>
            </div>
            <div class="running-chart-summary" id="runningChartSummary" aria-live="polite"></div>
        </section>
    </section>
</main>

<script>
    (() => {
        const dismissButton = document.querySelector("[data-onboarding-dismiss]");
        const backdrop = document.querySelector(".onboarding-modal-backdrop");
        if (!dismissButton || !backdrop) return;

        dismissButton.addEventListener("click", () => {
            backdrop.remove();
            const dashboard = document.getElementById("mypageDashboard");
            if (dashboard) dashboard.scrollIntoView({ behavior: "smooth", block: "start" });
        });
    })();

    (() => {
        const landing = document.getElementById("mypageLanding");
        if (!landing || window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            return;
        }

        let ticking = false;

        const updateLanding = () => {
            const landingHeight = Math.max(landing.offsetHeight, 1);
            const progress = Math.min(Math.max(window.scrollY / (landingHeight * 0.72), 0), 1);
            landing.style.setProperty("--landing-progress", progress.toFixed(4));
            ticking = false;
        };

        const requestUpdate = () => {
            if (ticking) return;
            ticking = true;
            window.requestAnimationFrame(updateLanding);
        };

        updateLanding();
        window.addEventListener("scroll", requestUpdate, { passive: true });
        window.addEventListener("resize", requestUpdate);
    })();

    (() => {
        const canvas = document.getElementById("runningTrendChart");
        const state = document.getElementById("runningChartState");
        const summary = document.getElementById("runningChartSummary");
        const description = document.getElementById("runningChartDescription");
        const buttons = Array.from(document.querySelectorAll("[data-chart-metric]"));
        if (!canvas || !state || !summary || !description || buttons.length === 0) return;

        const context = canvas.getContext("2d");
        const chart = {
            metric: "distance",
            days: [],
        };

        const localDateKey = (date) => {
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, "0");
            const day = String(date.getDate()).padStart(2, "0");
            return year + "-" + month + "-" + day;
        };

        const formatPace = (seconds) => {
            if (!Number.isFinite(seconds) || seconds <= 0) return "-";
            const rounded = Math.round(seconds);
            return Math.floor(rounded / 60)
                + "'"
                + String(rounded % 60).padStart(2, "0")
                + "\"";
        };

        const buildDays = (records) => {
            const today = new Date();
            const days = [];
            for (let offset = 6; offset >= 0; offset -= 1) {
                const date = new Date(today);
                date.setDate(today.getDate() - offset);
                days.push({
                    key: localDateKey(date),
                    label: (date.getMonth() + 1) + "." + date.getDate(),
                    distance: 0,
                    duration: 0,
                });
            }

            const dayMap = new Map(days.map((day) => [day.key, day]));
            records.forEach((record) => {
                const day = dayMap.get(record.recordDate);
                if (!day) return;
                const distance = Number(record.distanceKm);
                const duration = Number(record.durationSeconds);
                if (Number.isFinite(distance) && distance > 0) day.distance += distance;
                if (Number.isFinite(duration) && duration > 0) day.duration += duration;
            });

            return days.map((day) => ({
                ...day,
                pace: day.distance > 0 ? day.duration / day.distance : 0,
            }));
        };

        const drawChart = () => {
            if (chart.days.length === 0) return;
            const wrapper = canvas.parentElement;
            const ratio = Math.min(window.devicePixelRatio || 1, 2);
            const width = Math.max(wrapper.clientWidth, 280);
            const height = width < 560 ? 280 : 340;
            canvas.width = Math.round(width * ratio);
            canvas.height = Math.round(height * ratio);
            canvas.style.width = width + "px";
            canvas.style.height = height + "px";
            context.setTransform(ratio, 0, 0, ratio, 0, 0);
            context.clearRect(0, 0, width, height);

            const padding = {
                top: 28,
                right: width < 560 ? 18 : 34,
                bottom: 48,
                left: width < 560 ? 42 : 58,
            };
            const graphWidth = width - padding.left - padding.right;
            const graphHeight = height - padding.top - padding.bottom;
            const values = chart.days.map((day) => day[chart.metric]);
            const rawMax = Math.max(...values, chart.metric === "distance" ? 5 : 360);
            const maxValue = chart.metric === "distance"
                ? Math.max(5, Math.ceil(rawMax / 5) * 5)
                : Math.max(360, Math.ceil(rawMax / 60) * 60);

            context.lineWidth = 1;
            context.strokeStyle = "rgba(255, 255, 255, .1)";
            context.fillStyle = "rgba(235, 243, 238, .52)";
            context.font = "12px Pretendard, sans-serif";
            context.textAlign = "right";
            context.textBaseline = "middle";

            for (let index = 0; index <= 4; index += 1) {
                const y = padding.top + (graphHeight * index / 4);
                const value = maxValue * (1 - index / 4);
                context.beginPath();
                context.moveTo(padding.left, y);
                context.lineTo(width - padding.right, y);
                context.stroke();
                const label = chart.metric === "distance"
                    ? value.toFixed(value % 1 === 0 ? 0 : 1)
                    : formatPace(value);
                context.fillText(label, padding.left - 9, y);
            }

            const points = chart.days.map((day, index) => ({
                x: padding.left + graphWidth * (index / 6),
                y: padding.top + graphHeight * (1 - day[chart.metric] / maxValue),
                value: day[chart.metric],
                label: day.label,
            }));

            const gradient = context.createLinearGradient(0, padding.top, 0, height - padding.bottom);
            gradient.addColorStop(0, "rgba(104, 166, 145, .34)");
            gradient.addColorStop(1, "rgba(104, 166, 145, 0)");
            context.beginPath();
            context.moveTo(points[0].x, height - padding.bottom);
            points.forEach((point) => context.lineTo(point.x, point.y));
            context.lineTo(points[points.length - 1].x, height - padding.bottom);
            context.closePath();
            context.fillStyle = gradient;
            context.fill();

            context.beginPath();
            points.forEach((point, index) => {
                if (index === 0) context.moveTo(point.x, point.y);
                else context.lineTo(point.x, point.y);
            });
            context.strokeStyle = "#8bc8b3";
            context.lineWidth = 3;
            context.lineJoin = "round";
            context.lineCap = "round";
            context.stroke();

            points.forEach((point) => {
                context.beginPath();
                context.arc(point.x, point.y, 4.5, 0, Math.PI * 2);
                context.fillStyle = "#101412";
                context.fill();
                context.strokeStyle = "#8bc8b3";
                context.lineWidth = 3;
                context.stroke();
            });

            context.fillStyle = "rgba(235, 243, 238, .64)";
            context.font = "12px Pretendard, sans-serif";
            context.textAlign = "center";
            context.textBaseline = "top";
            points.forEach((point) => {
                context.fillText(point.label, point.x, height - padding.bottom + 15);
            });

            const totalDistance = chart.days.reduce((total, day) => total + day.distance, 0);
            const totalDuration = chart.days.reduce((total, day) => total + day.duration, 0);
            const averagePace = totalDistance > 0 ? totalDuration / totalDistance : 0;
            summary.innerHTML = chart.metric === "distance"
                ? "<span>7일 누적 거리</span><strong>"
                    + totalDistance.toFixed(1)
                    + " km</strong>"
                : "<span>7일 평균 페이스</span><strong>"
                    + formatPace(averagePace)
                    + " /km</strong>";
            canvas.setAttribute(
                "aria-label",
                chart.metric === "distance"
                    ? "최근 7일 러닝 거리 꺾은선 그래프"
                    : "최근 7일 평균 페이스 꺾은선 그래프"
            );
        };

        const selectMetric = (metric) => {
            chart.metric = metric;
            buttons.forEach((button) => {
                const active = button.dataset.chartMetric === metric;
                button.classList.toggle("is-active", active);
                button.setAttribute("aria-pressed", String(active));
            });
            description.textContent = metric === "distance"
                ? "최근 7일 동안 기록한 러닝 거리를 보여줍니다."
                : "최근 7일 동안 기록한 날짜별 평균 페이스를 보여줍니다.";
            drawChart();
        };

        buttons.forEach((button) => {
            button.addEventListener("click", () => selectMetric(button.dataset.chartMetric));
        });

        fetch("/api/running-records/me", {
            headers: { Accept: "application/json" },
            credentials: "same-origin",
        })
            .then((response) => {
                if (!response.ok) throw new Error("러닝 기록을 불러오지 못했습니다.");
                return response.json();
            })
            .then((payload) => {
                const records = Array.isArray(payload.data) ? payload.data : [];
                chart.days = buildDays(records);
                state.hidden = true;
                canvas.hidden = false;
                selectMetric("distance");
            })
            .catch(() => {
                canvas.hidden = true;
                state.hidden = false;
                state.textContent = "최근 러닝 기록을 불러오지 못했습니다.";
            });

        let resizeTimer;
        window.addEventListener("resize", () => {
            window.clearTimeout(resizeTimer);
            resizeTimer = window.setTimeout(drawChart, 120);
        });
    })();
</script>
</body>
</html>
