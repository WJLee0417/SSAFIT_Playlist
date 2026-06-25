package com.ssafy.ssafit.runningsession.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.ssafit.runningsession.domain.RunningSession;
import com.ssafy.ssafit.runningsession.domain.RunningSessionParticipant;
import com.ssafy.ssafit.runningsession.domain.RunningSessionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class RunningSessionApiDtos {

    private RunningSessionApiDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 255) String meetingPlace,
            @NotNull LocalDateTime scheduledAt,
            @NotNull @Positive Integer capacity,
            @NotNull @Positive BigDecimal targetDistanceKm,
            @NotNull @Positive Integer targetPaceSecondsPerKm) {

        public RunningSession toDomain() {
            return RunningSessionApiDtos.toDomain(
                    null,
                    title,
                    description,
                    meetingPlace,
                    scheduledAt,
                    capacity,
                    targetDistanceKm,
                    targetPaceSecondsPerKm);
        }
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 255) String meetingPlace,
            @NotNull LocalDateTime scheduledAt,
            @NotNull @Positive Integer capacity,
            @NotNull @Positive BigDecimal targetDistanceKm,
            @NotNull @Positive Integer targetPaceSecondsPerKm) {

        public RunningSession toDomain(Long runningSessionId) {
            return RunningSessionApiDtos.toDomain(
                    runningSessionId,
                    title,
                    description,
                    meetingPlace,
                    scheduledAt,
                    capacity,
                    targetDistanceKm,
                    targetPaceSecondsPerKm);
        }
    }

    public record StatusUpdateRequest(
            @NotNull RunningSessionStatus status) {
    }

    public record SessionResponse(
            Long id,
            Long hostMemberId,
            String hostNickname,
            String title,
            String description,
            String meetingPlace,
            LocalDateTime scheduledAt,
            Integer capacity,
            BigDecimal targetDistanceKm,
            Integer targetPaceSecondsPerKm,
            RunningSessionStatus status,
            LocalDateTime cancelledAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int participantCount,
            boolean currentMemberParticipating,
            boolean currentMemberHosting,
            boolean isMine,
            boolean expired,
            boolean recruiting) {

        public static SessionResponse from(RunningSession runningSession) {
            int participantCount = runningSession.getParticipantCount() == null
                    ? 0
                    : runningSession.getParticipantCount();
            boolean currentMemberParticipating =
                    Boolean.TRUE.equals(runningSession.getCurrentMemberParticipating());
            boolean currentMemberHosting =
                    Boolean.TRUE.equals(runningSession.getCurrentMemberHosting());
            boolean expired = runningSession.getScheduledAt() != null
                    && runningSession.getScheduledAt().isBefore(LocalDateTime.now());
            boolean recruiting = runningSession.getStatus() == RunningSessionStatus.OPEN
                    && !expired
                    && runningSession.getCapacity() != null
                    && participantCount < runningSession.getCapacity();
            return new SessionResponse(
                    runningSession.getId(),
                    runningSession.getHostMemberId(),
                    runningSession.getHostNickname(),
                    runningSession.getTitle(),
                    runningSession.getDescription(),
                    runningSession.getMeetingPlace(),
                    runningSession.getScheduledAt(),
                    runningSession.getCapacity(),
                    runningSession.getTargetDistanceKm(),
                    runningSession.getTargetPaceSecondsPerKm(),
                    runningSession.getStatus(),
                    runningSession.getCancelledAt(),
                    runningSession.getCreatedAt(),
                    runningSession.getUpdatedAt(),
                    participantCount,
                    currentMemberParticipating,
                    currentMemberHosting,
                    currentMemberHosting || currentMemberParticipating,
                    expired,
                    recruiting);
        }
    }

    public record ParticipantResponse(
            Long id,
            Long sessionId,
            Long memberId,
            String memberNickname,
            LocalDateTime joinedAt) {

        public static ParticipantResponse from(
                RunningSessionParticipant participant) {
            return new ParticipantResponse(
                    participant.getId(),
                    participant.getSessionId(),
                    participant.getMemberId(),
                    participant.getMemberNickname(),
                    participant.getJoinedAt());
        }
    }

    public record DetailResponse(
            SessionResponse session,
            int participantCount,
            List<ParticipantResponse> participants) {

        public DetailResponse {
            participants = participants == null
                    ? List.of()
                    : List.copyOf(participants);
        }
    }

    private static RunningSession toDomain(
            Long id,
            String title,
            String description,
            String meetingPlace,
            LocalDateTime scheduledAt,
            Integer capacity,
            BigDecimal targetDistanceKm,
            Integer targetPaceSecondsPerKm) {
        RunningSession runningSession = new RunningSession();
        runningSession.setId(id);
        runningSession.setTitle(title);
        runningSession.setDescription(description);
        runningSession.setMeetingPlace(meetingPlace);
        runningSession.setScheduledAt(scheduledAt);
        runningSession.setCapacity(capacity);
        runningSession.setTargetDistanceKm(targetDistanceKm);
        runningSession.setTargetPaceSecondsPerKm(targetPaceSecondsPerKm);
        return runningSession;
    }
}
