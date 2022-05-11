package io.jans.ca.server.utils;

public enum ErrorResponse {

    DEFAULT_SAMPLE_ERROR("Default sample error."),
    DEFAULT_SAMPLE_ERROR_2("Default sample error 2.");

    private final String description;

    ErrorResponse(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "description='" + description + '\'' +
                '}';
    }
}
