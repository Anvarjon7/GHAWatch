package org.example.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class Step {

    private String name;
    private String status;
    private String conclusion;

    @JsonProperty("number")
    private int number;

    @JsonProperty("started_at")
    private OffsetDateTime startedAt;
    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;
}
