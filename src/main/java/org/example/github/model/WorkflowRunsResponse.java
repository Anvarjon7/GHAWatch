package org.example.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class WorkflowRunsResponse {

    @JsonProperty("workflow_runs")
    private List<WorkflowRun> workflowRuns;
}
