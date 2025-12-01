package org.example.monitor;

import org.example.event.EventEmitter;
import org.example.event.EventType;
import org.example.event.WorkflowEvent;
import org.example.github.GitHubClient;
import org.example.github.exception.GithubApiException;
import org.example.github.exception.RateLimitException;
import org.example.github.model.*;
import org.example.monitor.backoff.BackOffStrategy;
import org.example.state.MonitorState;
import org.example.state.StateStore;
import org.example.state.snapshot.JobSnapshot;
import org.example.state.snapshot.RunSnapshot;
import org.example.state.snapshot.StepSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MonitorEngine {

    private static final Logger log = LoggerFactory.getLogger(MonitorEngine.class);

    private final GitHubClient client;
    private final StateStore stateStore;
    private final EventEmitter emitter;
    private final String owner;
    private final String repo;
    private final long pollIntervalMillis;
    private final BackOffStrategy backOff;

    public MonitorEngine(GitHubClient client,
                         StateStore stateStore,
                         EventEmitter emitter,
                         String owner,
                         String repo,
                         long pollIntervalMillis) {
        this.client = client;
        this.stateStore = stateStore;
        this.emitter = emitter;
        this.owner = owner;
        this.repo = repo;
        this.pollIntervalMillis = pollIntervalMillis;
        this.backOff = new BackOffStrategy(60);
    }

    private final AtomicBoolean running = new AtomicBoolean(true);

    public void start() {

        System.out.println("Starting monitoring for " + owner + "/" + repo + "......");

        MonitorState state = stateStore.load();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown requested..");
            running.set(false);
        }));

        while (running.get()) {
            try {
                log.info("[poll] starting poll cycle");
                pollOnce(state);

                backOff.reset();
                sleepInterruptibly(pollIntervalMillis);

            } catch (RateLimitException rle) {
                long waitMs = rle.getRetryAfterMillis();
                log.warn("Rate limited by Github. Sleeping {} ms." + waitMs);
                sleepInterruptibly(waitMs);
            } catch (GithubApiException ghe) {
                int delaySec = backOff.nextDelay();
                log.warn("Github APi error: {}. Backing off {}s and retrying.", ghe.getMessage(), delaySec);
                sleepInterruptibly(delaySec + 1000L);
            } catch (Exception e) {
                int delaySec = backOff.nextDelay();
                log.error("Unexpected error during polling: {}. Backing off {}s.", e.getMessage(), delaySec);
            }
        }

        try {
            stateStore.save(state);
            log.info("Final state saved.");
        } catch (Exception e) {
            log.error("Failed to save final state: {}", e.getMessage());
        }

        log.info("Monitor stopped");
    }

    private void pollOnce(MonitorState state) throws IOException {
        WorkflowRunsResponse runsResponse =
                client.listWorkflowRuns(owner, repo);

        List<WorkflowRun> runs = runsResponse == null ? null : runsResponse.getWorkflowRuns();
        if (runs == null || runs.isEmpty()) {
            log.debug("[poll] no runs returned");
            return;
        }

        runs.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        long maxSeenRunId = state.getLastProcessedRunId();

        for (WorkflowRun run : runs) {
            long runId = run.getId();

            if (runId <= state.getLastProcessedRunId()) {
                continue;
            }

            try {
                processRunWithSnapshot(run, state);
            } catch (RateLimitException | GithubApiException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error processing run {}: {}", runId, e.getMessage());
            }

            if (runId > maxSeenRunId) {
                maxSeenRunId = runId;
            }

            state.setLastProcessedRunId(maxSeenRunId);
            stateStore.save(state);
        }
    }

    private void emitRunEvents(WorkflowRun run) {

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

    private void processRunWithSnapshot(WorkflowRun run, MonitorState state) throws IOException {
        long runId = run.getId();
        String fullREpo = owner + "/" + repo;

        Map<Long, RunSnapshot> runSnapshots = state.getRunSnapshots();

        RunSnapshot previousRunSnapshot = runSnapshots == null ? null : runSnapshots.get(runId);

        if (previousRunSnapshot == null) {
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

        if (previousRunSnapshot != null && previousRunSnapshot.getConclusion() == null && run.getConclusion() != null) {
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

        JobsResponse jobsResponse = client.listJobs(owner, repo, runId);
        List<Job> jobs = jobsResponse == null ? null : jobsResponse.getJobs();
        if (jobs == null) jobs = List.of();

        Map<Long, JobSnapshot> currentJobSnapshots = new HashMap<>();

        for (Job job : jobs) {
            long jobId = job.getId();
            JobSnapshot prevJobSnapshot = previousRunSnapshot != null
                    && previousRunSnapshot.getJobs() != null ? previousRunSnapshot.getJobs().get(jobId) : null;

            boolean jobStartedNow = (prevJobSnapshot == null) && (job.getStartedAt() != null || "in_progress".equalsIgnoreCase(job.getStatus()));
            if (jobStartedNow) {
                emitter.emit(new WorkflowEvent(
                        OffsetDateTime.now(),
                        EventType.JOB_STARTED,
                        fullREpo,
                        runId,
                        jobId,
                        null,
                        run.getHeadBranch(),
                        shorten(run.getHeadSha()),
                        job.getStatus(),
                        "Job started: " + safeString(job.getName())
                ));
            }

            boolean jobCompletedNow = (prevJobSnapshot == null && job.getConclusion() != null)
                    || (prevJobSnapshot != null && prevJobSnapshot.getConclusion() == null && job.getConclusion() != null);
            if (jobCompletedNow) {
                emitter.emit(new WorkflowEvent(
                        OffsetDateTime.now(),
                        EventType.JOB_COMPLETED,
                        fullREpo,
                        runId,
                        jobId,
                        null,
                        run.getHeadBranch(),
                        shorten(run.getHeadSha()),
                        job.getConclusion(),
                        "Job completed: " + safeString(job.getName())
                ));
            }

            List<Step> steps = job.getSteps();
            Map<Integer, StepSnapshot> currentStepSnapshots = new HashMap<>();

            if (steps != null && !steps.isEmpty()) {
                for (Step step : steps) {
                    int stepNumber = step.getNumber();
                    StepSnapshot prevStep = prevJobSnapshot != null && prevJobSnapshot.getSteps() != null
                            ? prevJobSnapshot.getSteps().get(stepNumber)
                            : null;

                    boolean stepStartedNow = (prevStep == null) && (step.getStartedAt() != null || "in_progress".equalsIgnoreCase(step.getStatus()));

                    if (stepStartedNow) {
                        emitter.emit(new WorkflowEvent(
                                OffsetDateTime.now(),
                                EventType.STEP_STARTED,
                                fullREpo,
                                runId,
                                jobId,
                                stepNumber,
                                run.getHeadBranch(),
                                shorten(run.getHeadSha()),
                                step.getStatus(),
                                "Step started: " + safeString(step.getName())
                        ));

                        boolean stepCompletedNow = (prevStep == null && step.getConclusion() != null)
                                || (prevStep != null && prevStep.getConclusion() == null && step.getConclusion() != null);

                        if (stepCompletedNow) {
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
                                    "Step completed: " + safeString(step.getName())
                            ));
                        }
                        currentStepSnapshots.put(stepNumber, new StepSnapshot(stepNumber, step.getStatus(), step.getConclusion()));
                    }
                }

                currentJobSnapshots.put(jobId, new JobSnapshot(jobId, job.getStatus(), job.getConclusion(), currentStepSnapshots));
            }

            RunSnapshot newRunSnapshot = new RunSnapshot(runId, run.getStatus(), run.getConclusion(), currentJobSnapshots);

            boolean allJobsFinished = currentJobSnapshots.values().stream()
                    .allMatch(j -> j.getConclusion() != null);

            if (run.getConclusion() != null && allJobsFinished) {
                if (state.getRunSnapshots() != null) {
                    state.getRunSnapshots().remove(runId);
                }
            } else {
                if (state.getRunSnapshots() == null) {
                    state.setRunSnapshots(new HashMap<>());
                }
                state.getRunSnapshots().put(runId, newRunSnapshot);
            }
            stateStore.save(state);
        }


    }

    private String shorten(String sha) {
        return sha != null && sha.length() > 7 ? sha.substring(0, 7) : sha;
    }

    private String safeString(String s) {
        return s == null ? "-" : s;
    }

    private void sleepInterruptibly(long millis) {
        try {
            long slept = 0;
            final long chunk = 1000L;
            while (running.get() && slept < millis) {
                long toSleep = Math.min(chunk, millis - slept);
                Thread.sleep(toSleep);
                slept += toSleep;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }
}
