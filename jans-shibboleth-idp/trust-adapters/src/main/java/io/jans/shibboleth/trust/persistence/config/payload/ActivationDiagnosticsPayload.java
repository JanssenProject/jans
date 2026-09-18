package io.jans.shibboleth.trust.persistence.config.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Stored JSON representation of a trust relationship's activation diagnostics (the {@code jansActivationDiag}
 * {@code @JsonObject} column). A dedicated persistence carrier (TP3): enums as {@code name()} strings,
 * instants as ISO-8601 strings, log entries as a nested list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivationDiagnosticsPayload {

    public String status;
    public String origin;
    public String startedAt;
    public String completedAt;
    public List<LogEntry> logEntries;

    public static class LogEntry {

        public String timestamp;
        public String level;
        public String message;
    }
}
