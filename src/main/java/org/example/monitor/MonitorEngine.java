package org.example.monitor;

import lombok.AllArgsConstructor;
import org.example.github.GitHubClient;
import org.example.github.model.WorkflowRun;
import org.example.github.model.WorkflowRunsResponse;
import org.example.state.MonitorState;
import org.example.state.StateStore;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class MonitorEngine {

    private final GitHubClient client;
    private final StateStore stateStore;
    private final EventEmitter emitter;
    private final String owner;
    private final String repo;
    private final long pollIntervalMillis;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public void start(){

        System.out.println("Starting monitoring for " + owner + "/" + repo + "......");

        MonitorState state = stateStore.load();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown requested..");
            running.set(false);
        }));

        while (running.get()){
            try {
                pollOnce(state);
                Thread.sleep(pollIntervalMillis);
            }catch (InterruptedException e){
                System.out.println("interrupted. Shutting down...");
                Thread.currentThread().interrupt();
                break;
            }catch (Exception e){
                System.err.println("Error during polling: " + e.getMessage());
            }
        }
    }

    private void pollOnce(MonitorState state) throws IOException {
        WorkflowRunsResponse runsResponse =
                client.listWorkflowRuns(owner,repo);

        List<WorkflowRun> runs = runsResponse.getWorkflowRuns();
        if (runs==null || runs.isEmpty()){
            return;
        }

        runs.sort(Comparator.comparing(WorkflowRun::getId));

        for (WorkflowRun run : runs){
            long runId = run.getId();

            if (runId <= state.getLastProcessedRunId()){
                continue;
            }

            emitRunEvents(run);

            state.setLastProcessedRunId(runId);
            stateStore.save(state);
        }
    }

    private void emitRunEvents(WorkflowRun run){

        String fullRepo = owner + "/" + repo;
        OffsetDateTime now = OffsetDateTime.now();

        emitter.emit(new WorkflowEvent(
                now,
                EventType.WORKFLOW_QUEUED,
                fullRepo,
                run.getId(),
                null,
                null,
                run.getHeadBranch(),
                shorten(run.getHeadSha()),
                run.getStatus(),
                "Workflow run detected"
        ));
    }

    private String shorten(String sha){
        return sha!= null && sha.length() > 7 ? sha.substring(0,7) : sha;
    }
}
