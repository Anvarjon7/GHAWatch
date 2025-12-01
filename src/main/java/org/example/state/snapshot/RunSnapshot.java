package org.example.state.snapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class RunSnapshot {

    private final long runId;
    private final String status;
    private final String conclusion;

    private final Map<Long, JobSnapshot> jobs;
}
