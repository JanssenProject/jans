package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;

import java.util.List;
import java.util.Objects;

/**
 * Outcome and log of the most recent activation attempt on a trust relationship. Timestamps are
 * ISO-8601 date-times.
 */
public class ActivationDiagnosticsDto {

    @JsonProperty("status")
    private final ActivationStatus status;

    @JsonProperty("origin")
    private final String origin;

    @JsonProperty("started_at")
    private final String startedAt;

    @JsonProperty("completed_at")
    private final String completedAt;

    @JsonProperty("log_entries")
    private final List<ActivationLogEntryDto> logEntries;

    public ActivationDiagnosticsDto(ActivationStatus status, String origin, String startedAt,
        String completedAt, List<ActivationLogEntryDto> logEntries) {

        this.status = status;
        this.origin = origin;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.logEntries = logEntries;
    }

    public ActivationStatus getStatus() {

        return status;
    }

    public String getOrigin() {

        return origin;
    }

    public String getStartedAt() {

        return startedAt;
    }

    public String getCompletedAt() {

        return completedAt;
    }

    public List<ActivationLogEntryDto> getLogEntries() {

        return logEntries;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivationDiagnosticsDto that = (ActivationDiagnosticsDto) o;
        return status == that.status
            && Objects.equals(origin, that.origin)
            && Objects.equals(startedAt, that.startedAt)
            && Objects.equals(completedAt, that.completedAt)
            && Objects.equals(logEntries, that.logEntries);
    }

    @Override
    public int hashCode() {

        return Objects.hash(status, origin, startedAt, completedAt, logEntries);
    }

    @Override
    public String toString() {

        return "ActivationDiagnosticsDto{status=" + status + ", origin='" + origin
            + "', startedAt='" + startedAt + "', completedAt='" + completedAt
            + "', logEntries=" + logEntries + '}';
    }
}
