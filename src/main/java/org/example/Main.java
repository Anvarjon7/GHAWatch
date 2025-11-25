package org.example;

import org.example.github.GitHubClient;
import org.example.github.model.WorkflowRunsResponse;
import org.example.state.MonitorState;
import org.example.state.StateStore;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

//        String token = System.getenv("GITHUB_TOKEN");
//
//        if (token == null || token.isBlank()) {
//            System.err.println("ERROR: Please set GITHUB_TOKEN environment variable.");
//            return;
//        }
//
//        try {
//            GitHubClient client = new GitHubClient(token);
//
//            WorkflowRunsResponse runs = client.listWorkflowRuns("Anvarjon7", "learning-platfrom");
//
//            System.out.println("Number of workflow runs: " + runs.getWorkflowRuns().size());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        StateStore stateStore = new StateStore("state/test.json");

        MonitorState monitorState = stateStore.load();
        System.out.println("Loaded: " + monitorState.getLastProcessedRunId());

        monitorState.setLastProcessedRunId(12345L);

        stateStore.save(monitorState);

        MonitorState monitorState1 = stateStore.load();

        System.out.println("After save: " + monitorState1.getLastProcessedRunId());

        System.out.println("Loaded + " + monitorState1.getLastProcessedRunId());

    }
}