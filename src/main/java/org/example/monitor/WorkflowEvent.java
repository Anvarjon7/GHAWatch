package org.example.monitor;

import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Optional;

@AllArgsConstructor
public class WorkflowEvent {

    private final OffsetDateTime timeStamp;
    private final EventType eventType;
    private final String repo;
    private final Long runId;
    private final Long jobId;
    private final Integer stepNumber;
    private final String branch;
    private final String shaShort;
    private final String status;
    private final String message;


    public OffsetDateTime getTimeStamp() {
        return timeStamp;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getRepo() {
        return repo;
    }

    public long getRunId() {
        return runId;
    }

    public Optional<Long> getJobId() {
        return Optional.ofNullable(jobId);
    }

    public Optional<Integer> getStepNumber() {
        return Optional.ofNullable(stepNumber);
    }

    public Optional<String> getBranch() {
        return Optional.ofNullable(branch);
    }

    public Optional<String> getShaShort() {
        return Optional.ofNullable(shaShort);
    }

    public Optional<String> getStatus() {
        return Optional.ofNullable(status);
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }
}
