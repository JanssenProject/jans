package io.jans.shibboleth.trust.dto.activation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;

import java.util.List;
import java.util.Objects;

/**
 * A worker's report of an activation outcome ({@code POST .../work-items/{id}/report}). {@code origin}
 * is the reporting worker's identity ("instance@host") and is the fence the server checks against the
 * work item's lease holder. Timestamps are ISO-8601 date-times. A dumb data holder — unknown
 * properties are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ActivationDiagnosticsRequest {

    @JsonProperty("origin")
    private String origin;

    @JsonProperty("status")
    private ActivationStatus status;

    @JsonProperty("started_at")
    private String startedAt;

    @JsonProperty("completed_at")
    private String completedAt;

    @JsonProperty("log_entries")
    private List<ActivationLogEntryRequest> logEntries;

    public ActivationDiagnosticsRequest() {
    }

    public String getOrigin() {

        return origin;
    }

    public void setOrigin(String origin) {

        this.origin = origin;
    }

    public ActivationStatus getStatus() {

        return status;
    }

    public void setStatus(ActivationStatus status) {

        this.status = status;
    }

    public String getStartedAt() {

        return startedAt;
    }

    public void setStartedAt(String startedAt) {

        this.startedAt = startedAt;
    }

    public String getCompletedAt() {

        return completedAt;
    }

    public void setCompletedAt(String completedAt) {

        this.completedAt = completedAt;
    }

    public List<ActivationLogEntryRequest> getLogEntries() {

        return logEntries;
    }

    public void setLogEntries(List<ActivationLogEntryRequest> logEntries) {

        this.logEntries = logEntries;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivationDiagnosticsRequest that = (ActivationDiagnosticsRequest) o;
        return Objects.equals(origin, that.origin)
            && status == that.status
            && Objects.equals(startedAt, that.startedAt)
            && Objects.equals(completedAt, that.completedAt)
            && Objects.equals(logEntries, that.logEntries);
    }

    @Override
    public int hashCode() {

        return Objects.hash(origin, status, startedAt, completedAt, logEntries);
    }

    @Override
    public String toString() {

        return "ActivationDiagnosticsRequest{origin='" + origin + "', status=" + status
            + ", startedAt='" + startedAt + "', completedAt='" + completedAt
            + "', logEntries=" + logEntries + '}';
    }
}
