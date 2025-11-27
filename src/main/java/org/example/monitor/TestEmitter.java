package org.example.monitor;

import java.time.OffsetDateTime;

public class TestEmitter {

    public static void main(String[] args) {

        EventEmitter eventEmitter = new EventEmitter();

        WorkflowEvent event = new WorkflowEvent(
                OffsetDateTime.now(),
                EventType.JOB_COMPLETED,
                "testorg/testrepo",
                24L,
                343L,
                null,
                "main",
                "abc123",
                "success",
                "Job 'build' completed"
        );

        eventEmitter.emit(event);
    }
}
