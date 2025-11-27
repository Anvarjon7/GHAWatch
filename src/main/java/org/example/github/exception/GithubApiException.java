package org.example.github.exception;

public class GithubApiException extends RuntimeException{

    private final int statusCode;

    public GithubApiException(String message, int statusCode){
        super(message + "(HTTP " + statusCode + ")");
        this.statusCode=statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
