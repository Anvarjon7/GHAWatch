package org.example.github;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;
import org.example.github.exception.GithubApiException;
import org.example.github.exception.RateLimitException;
import org.example.github.model.JobsResponse;
import org.example.github.model.WorkflowRunsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private static final String BASE_URL = "https://api.github.com";

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final String token;

    public GitHubClient(String token) {
        this.token = token;

        this.httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();

        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public WorkflowRunsResponse listWorkflowRuns(String owner, String repo) {
        String url = BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs?per_page=50";
        return executeGet(url, WorkflowRunsResponse.class);
    }

    public JobsResponse listJobs(String owner, String repo, long runId) {
        String url = BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs?per_page=50";
        return executeGet(url, JobsResponse.class);
    }

    private <T> T executeGet(String url, Class<T> type) {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build();

        log.debug("GET {}", url);

        try (Response response = httpClient.newCall(request).execute()) {

            if (response.code() == 403 && isRateLimit(response)) {
                long wait = computeRateLimitWaitMillis(response);
                throw new RateLimitException(wait);
            }

            if (!response.isSuccessful()) {
                String body = safeBody(response);
                throw new GithubApiException(
                        "GitHub API error: " + body,
                        response.code()
                );
            }

            String body = safeBody(response);
            return mapper.readValue(body, type);

        } catch (IOException e) {
            throw new GithubApiException("Network error: " + e.getMessage(), 0);
        }
    }


    private boolean isRateLimit(Response response) throws IOException {
        String body = safeBody(response);
        return body.contains("rate limit exceeded");
    }

    private long computeRateLimitWaitMillis(Response response) {
        String resetHeader = response.header("X-RateLimit-Reset");

        if (resetHeader == null) {
            return 60_000; // fallback 60 sec
        }

        try {
            long epoch = Long.parseLong(resetHeader) * 1000;
            return Math.max(0, epoch - System.currentTimeMillis());
        } catch (NumberFormatException e) {
            return 60_000;
        }
    }

    private String safeBody(Response response) throws IOException {
        ResponseBody body = response.body();
        return body != null ? body.string() : "";
    }
}
