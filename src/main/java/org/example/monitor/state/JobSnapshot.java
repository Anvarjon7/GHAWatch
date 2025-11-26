package org.example.monitor.state;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class JobSnapshot {

    private final long jobId;
    private final String status;
    private final String conclusion;

    private final Map<Integer,StepSnapshot> steps;
}
