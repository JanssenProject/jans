package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.shared.diagnostics.LogLevel;

import java.util.Objects;

/**
 * A single entry in a trust relationship's activation log.
 */
public class ActivationLogEntryDto {

    @JsonProperty("timestamp")
    private final String timestamp;

    @JsonProperty("level")
    private final LogLevel level;

    @JsonProperty("message")
    private final String message;

    public ActivationLogEntryDto(String timestamp, LogLevel level, String message) {

        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public String getTimestamp() {

        return timestamp;
    }

    public LogLevel getLevel() {

        return level;
    }

    public String getMessage() {

        return message;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivationLogEntryDto that = (ActivationLogEntryDto) o;
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

        return "ActivationLogEntryDto{timestamp='" + timestamp + "', level=" + level
            + ", message='" + message + "'}";
    }
}
