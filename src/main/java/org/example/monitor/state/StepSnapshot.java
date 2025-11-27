package org.example.monitor.state;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StepSnapshot {

    private final int number;
    private final String status;
    private final String conclusion;
}
