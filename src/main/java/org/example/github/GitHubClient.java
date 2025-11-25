package org.example.github;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.github.model.JobsResponse;
import org.example.github.model.WorkflowRunsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private static final String API_URL = "https://api.github.com";

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

    public WorkflowRunsResponse listWorkflowRuns(String owner, String repo) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repo + "/actions/runs?per_page=50";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "Application/vnd.github+json")
                .header("X-Github-Api-Version", "2022-11-28")
                .build();

        log.debug("GET {}", url);

        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Github API error (runs): " + response.code() + " - " + response.message());
        }

        return mapper.readValue(response.body().string(), WorkflowRunsResponse.class);
    }

    public JobsResponse listJobsForRun(String owner, String repo, long runId) throws IOException {
        String url = API_URL + "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-Github-Api+Version", "2022-11-28")
                .build();

        log.debug("GET {}", url);

        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Github API error (jobs): " + response.code() + " - " + response.message());
        }

        return mapper.readValue(response.body().string(), JobsResponse.class);
    }


}
