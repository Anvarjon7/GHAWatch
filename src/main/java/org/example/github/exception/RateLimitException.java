package org.example.github.exception;

public class RateLimitException extends RuntimeException {

    private final long retryAfterMillis;

    public RateLimitException(long retryAfterMillis) {
        super("Github API rete limit exceeded. Retry after: " + retryAfterMillis);
        this.retryAfterMillis = retryAfterMillis;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
