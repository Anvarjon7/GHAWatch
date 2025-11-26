package org.example.monitor;

import lombok.AllArgsConstructor;
import org.example.github.GitHubClient;
import org.example.github.model.WorkflowRun;
import org.example.github.model.WorkflowRunsResponse;
import org.example.monitor.state.JobSnapshot;
import org.example.monitor.state.RunSnapshot;
import org.example.monitor.state.StepSnapshot;
import org.example.state.MonitorState;
import org.example.state.StateStore;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
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

            processRunWithSnapshot(run,state);

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

    private void processRunWithSnapshot(WorkflowRun run, MonitorState state) throws IOException {
        long runId = run.getId();
        String fullREpo = owner+"/"+repo;

        RunSnapshot previous = state.getRunSnapshots().get(runId);

        if (previous == null){
            emitter.emit(new WorkflowEvent(
                    OffsetDateTime.now(),
                    EventType.WORKFLOW_STARTED,
                    fullREpo,
                    runId,
                    null,
                    null,
                    run.getHeadBranch(),
                    shorten(run.getHeadSha()),
                    run.getStatus(),
                    "Workflow run started"
            ));
        }

        if (previous != null && previous.getConclusion() == null && run.getConclusion() != null){
            emitter.emit(new WorkflowEvent(
                    OffsetDateTime.now(),
                    EventType.WORKFLOW_COMPLETED,
                    fullREpo,
                    runId,
                    null,
                    null,
                    run.getHeadBranch(),
                    shorten(run.getHeadSha()),
                    run.getConclusion(),
                    "Workflow run completed"
            ));
        }

        var jobsResponse = client.listJobsForRun(owner,repo,runId);
        var currentJobSnapshot = new HashMap<Long, JobSnapshot>();

        for (var job : jobsResponse.getJobs()){
            JobSnapshot prevJob = previous != null ?
                    previous.getJobs().get(job.getId()) : null;

            if (prevJob == null && job.getStartedAt() != null){
                emitter.emit(new WorkflowEvent(
                        OffsetDateTime.now(),
                        EventType.JOB_STARTED,
                        fullREpo,
                        runId,
                        job.getId(),
                        null,
                        run.getHeadBranch(),
                        shorten(run.getHeadSha()),
                        job.getStatus(),
                        "Job started: " + job.getName()
                ));
            }

            if (prevJob != null && prevJob.getConclusion() == null && job.getConclusion() != null){
                emitter.emit(new WorkflowEvent(
                        OffsetDateTime.now(),
                        EventType.JOB_COMPLETED,
                        fullREpo,
                        runId,
                        job.getId(),
                        null,
                        run.getHeadBranch(),
                        shorten(run.getHeadSha()),
                        job.getConclusion(),
                        "Job completed: " + job.getName()
                ));
            }

            var steps = job.getSteps();
            var currentStepSnapshots = new HashMap<Integer, StepSnapshot>();

            if (steps != null){
                for (var step : steps){
                    StepSnapshot prevStep = prevJob!=null ? prevJob.getSteps().get(step.getNumber()):null;

                    if (prevStep == null && step.getStartedAt() != null){
                        emitter.emit(new WorkflowEvent(
                                OffsetDateTime.now(),
                                EventType.STEP_STARTED,
                                fullREpo,
                                runId,
                                job.getId(),
                                step.getNumber(),
                                run.getHeadBranch(),
                                shorten(run.getHeadSha()),
                                step.getStatus(),
                                "Step started: " +step.getName()
                        ));
                    }

                    if (prevStep != null && prevStep.getConclusion() == null && step.getConclusion() != null){
                        emitter.emit(new WorkflowEvent(
                                OffsetDateTime.now(),
                                EventType.STEP_COMPLETED,
                                fullREpo,
                                runId,
                                job.getId(),
                                step.getNumber(),
                                run.getHeadBranch(),
                                shorten(run.getHeadSha()),
                                step.getConclusion(),
                                "Step completed: " + step.getName()
                        ));

                        currentStepSnapshots.put(step.getNumber(), new StepSnapshot(
                                step.getNumber(),
                                step.getStatus(),
                                step.getConclusion()
                        ));
                    }
                }

                currentJobSnapshot.put(job.getId(),new JobSnapshot(
                        job.getId(),
                        job.getStatus(),
                        job.getConclusion(),
                        currentStepSnapshots
                ));
            }

            state.updateSnapshot(runId, new RunSnapshot(
                    runId,
                    run.getStatus(),
                    run.getConclusion(),
                    currentJobSnapshot
            ));
        }
    }
}
