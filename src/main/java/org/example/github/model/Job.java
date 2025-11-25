package org.example.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class Job {

    private Long id;
    private String name;
    private String status;
    private String conclusion;

    @JsonProperty("started_at")
    private OffsetDateTime startedAt;
    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    private List<Step> steps;
}
