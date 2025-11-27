package org.example;

import org.example.github.GitHubClient;
import org.example.github.model.WorkflowRunsResponse;
import org.example.monitor.EventEmitter;
import org.example.monitor.EventType;
import org.example.monitor.MonitorEngine;
import org.example.state.MonitorState;
import org.example.state.StateStore;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Usage: java -jar <jarfile> <owner/repo>");
            return;
        }

        String repoArg = args[0];
        if (!repoArg.contains("/")) {
            System.err.println("Repository must be in format: owner/repo");
            return;
        }

        String[] parts = repoArg.split("/");
        String owner = parts[0];
        String repo = parts[1];

        String token = System.getenv("GITHUB_TOKEN");
        if (token == null) {
            System.err.println("ERROR: Please set GITHUB TOKEN environment variable.");
            return;
        }

        GitHubClient client = new GitHubClient(token);
        StateStore store = new StateStore(repoArg);
        EventEmitter emitter = new EventEmitter();

//        MonitorEngine engine = new MonitorEngine(
//                client,
//                store,
//                emitter,
//                owner,
//                repo,
//                5000,
//
//        );

//        engine.start();

    }
}