package org.example.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class WorkflowRun {

    private Long id;

    @JsonProperty("head_branch")
    private String headBranch;

    @JsonProperty("head_sha")
    private String headSha;

    private String status;
    private String conclusion;

    @JsonProperty("run_number")
    private int runNumber;
}
