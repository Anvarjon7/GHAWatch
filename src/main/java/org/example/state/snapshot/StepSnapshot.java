package org.example.state.snapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StepSnapshot {

    private final int number;
    private final String status;
    private final String conclusion;
}
