package org.example.monitor;

import java.time.OffsetDateTime;

public class TestEmitter {

    public static void main(String[] args) {

        EventEmitter eventEmitter = new EventEmitter();

        WorkflowEvent event = new WorkflowEvent(
                OffsetDateTime.now(),
                EventType.JOB_STARTED,
                "myorg/repo",
                12324L,
                34343L,
                2,
                "main",
                "abc1234",
                "in_progress",
                "Job 'build' started"
        );

        eventEmitter.emit(event);
    }
}
