package io.jans.shibboleth.trust.dto.activation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.shared.diagnostics.LogLevel;

import java.util.Objects;

/**
 * One entry of a worker's activation report log. {@code timestamp} is an ISO-8601 date-time.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ActivationLogEntryRequest {

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("level")
    private LogLevel level;

    @JsonProperty("message")
    private String message;

    public ActivationLogEntryRequest() {
    }

    public ActivationLogEntryRequest(String timestamp, LogLevel level, String message) {

        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public String getTimestamp() {

        return timestamp;
    }

    public void setTimestamp(String timestamp) {

        this.timestamp = timestamp;
    }

    public LogLevel getLevel() {

        return level;
    }

    public void setLevel(LogLevel level) {

        this.level = level;
    }

    public String getMessage() {

        return message;
    }

    public void setMessage(String message) {

        this.message = message;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivationLogEntryRequest that = (ActivationLogEntryRequest) o;
        return Objects.equals(timestamp, that.timestamp)
            && level == that.level
            && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {

        return Objects.hash(timestamp, level, message);
    }

    @Override
    public String toString() {

        return "ActivationLogEntryRequest{timestamp='" + timestamp + "', level=" + level
            + ", message='" + message + "'}";
    }
}
