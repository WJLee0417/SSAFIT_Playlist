package com.ssafy.ssafit.runningsession.dto;

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
            @NotNull @Positive Integer capacity) {

        public RunningSession toDomain() {
            return RunningSessionApiDtos.toDomain(
                    null,
                    title,
                    description,
                    meetingPlace,
                    scheduledAt,
                    capacity);
        }
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 255) String meetingPlace,
            @NotNull LocalDateTime scheduledAt,
            @NotNull @Positive Integer capacity) {

        public RunningSession toDomain(Long runningSessionId) {
            return RunningSessionApiDtos.toDomain(
                    runningSessionId,
                    title,
                    description,
                    meetingPlace,
                    scheduledAt,
                    capacity);
        }
    }

    public record StatusUpdateRequest(
            @NotNull RunningSessionStatus status) {
    }

    public record SessionResponse(
            Long id,
            Long hostMemberId,
            String title,
            String description,
            String meetingPlace,
            LocalDateTime scheduledAt,
            Integer capacity,
            RunningSessionStatus status,
            LocalDateTime cancelledAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static SessionResponse from(RunningSession runningSession) {
            return new SessionResponse(
                    runningSession.getId(),
                    runningSession.getHostMemberId(),
                    runningSession.getTitle(),
                    runningSession.getDescription(),
                    runningSession.getMeetingPlace(),
                    runningSession.getScheduledAt(),
                    runningSession.getCapacity(),
                    runningSession.getStatus(),
                    runningSession.getCancelledAt(),
                    runningSession.getCreatedAt(),
                    runningSession.getUpdatedAt());
        }
    }

    public record ParticipantResponse(
            Long id,
            Long sessionId,
            Long memberId,
            LocalDateTime joinedAt) {

        public static ParticipantResponse from(
                RunningSessionParticipant participant) {
            return new ParticipantResponse(
                    participant.getId(),
                    participant.getSessionId(),
                    participant.getMemberId(),
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
            Integer capacity) {
        RunningSession runningSession = new RunningSession();
        runningSession.setId(id);
        runningSession.setTitle(title);
        runningSession.setDescription(description);
        runningSession.setMeetingPlace(meetingPlace);
        runningSession.setScheduledAt(scheduledAt);
        runningSession.setCapacity(capacity);
        return runningSession;
    }
}
