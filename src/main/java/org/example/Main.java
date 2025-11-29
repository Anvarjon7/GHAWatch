package org.example;

import org.example.github.GitHubClient;
import org.example.monitor.EventEmitter;
import org.example.monitor.MonitorEngine;
import org.example.state.MonitorState;
import org.example.state.StateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "gha-watch",
        version = "GHAWatch 1.0.0",
        description = "Monitor GithubActions workflow runs and print one-line events to stdout."
)
public class Main implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private String repoArg;

    @CommandLine.Option(names = {"--token"}, description = "Github personal access token (overrides GITHUB_TOKEN env)")
    private String tokenOpt;
    @CommandLine.Option(names = {"--interval"}, description = "Polling interval in seconds (default: ${DEFAULT-VALUE})", defaultValue = "10")
    private String stateFileOpt;
    @CommandLine.Option(names = {"--state"}, description = "Path to the state file (default: ~/.gha-watch/<owner>/<repo>/state.json)")
    private int intervalSeconds;
    @CommandLine.Option(names = {"--verbose"}, description = "Enable verbose (DEBUG) logging")
    private boolean verbose;
    @CommandLine.Option(names = {"--since-seconds"}, description = "When first run: look back this many seconds to emit recent completion events (default: 0)", defaultValue = "0")
    private long sinceSeconds;

    public static void main(String[] args) {

        int exit = new CommandLine(new Main()).execute(args);
        System.exit(exit);
    }

    @Override
    public Integer call() throws Exception {

        configureLogging(verbose);

        System.out.println("GHAWatch v1.0.0 - Github Actions Monitor");
        System.out.println("JETBRAINS");
        System.out.println("-----------------------------------------");

        if (repoArg == null || !repoArg.contains("/")) {
            System.err.println("Repository must be specified in the form: owner/repo");
            return 2;
        }

        String[] parts = repoArg.split("/", 2);
        String owner = parts[0];
        String repo = parts[1];

        if (owner.isEmpty() || repo.isEmpty()) {
            System.err.println("Repository must be specified in the form: owner/repo");
            return 2;
        }

        String token = resolveToken(tokenOpt);
        if (token == null || token.isBlank()) {
            System.err.println("ERROR: GitHub token not provided. Use --token or set GITHUB_TOKEN environment variable.");
            return 3;
        }

        Path statePath = resolveStateFilePath(stateFileOpt, owner, repo);
        log.info("Using state file: {}", statePath.toAbsolutePath());

        GitHubClient client = new GitHubClient(token);
        StateStore store = new StateStore(statePath.toString());
        MonitorState state = store.load();

        if (sinceSeconds > 0 && state.getLastProcessedRunId() == 0) {
            // We won't fetch by timestamp; this flag will be used in Phase 6 to decide lookback;
            log.info("First run lookback requested: {} seconds — (will be applied if implemented)", sinceSeconds);
        }

        EventEmitter emitter = new EventEmitter();
        MonitorEngine engine = new MonitorEngine(
                client, store, emitter, owner, repo, intervalSeconds * 1000L
        );

        try {
            engine.start();
            return 0;
        } catch (Exception e) {
            log.error("Fatal error: {}", e.getMessage(), e);
            System.err.println("Fatal error: " + e.getMessage());
            return 1;
        }
    }

    private void configureLogging(boolean verbose) {
        if (verbose) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.out.println("[verbose] debug logging enabled");
        } else {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        }

        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd'T'HH:mm:ss");
    }

    private String resolveToken(String tokenOpt) {
        if (tokenOpt != null && !tokenOpt.isBlank()) return tokenOpt;
        String env = System.getenv("GITHUB TOKEN");
        if (env != null && !env.isBlank()) return env;
        String env2 = System.getenv("GH_TOKEN");
        if (env2 != null && !env2.isBlank()) return env2;
        return null;
    }

    private Path resolveStateFilePath(String explicit, String owner, String repo) {
        if (explicit != null && !explicit.isBlank()) {
            return Paths.get(explicit);
        }

        String home = System.getProperty("user.home");
        return Paths.get(home, ".gha-watch", owner, repo, "state.json");
    }
}