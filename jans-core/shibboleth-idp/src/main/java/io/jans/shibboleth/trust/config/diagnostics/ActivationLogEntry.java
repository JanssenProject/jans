package io.jans.shibboleth.trust.config.diagnostics;

import java.time.Instant;
import java.util.Objects;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

public class ActivationLogEntry {

    private final Instant timestamp;
    private final LogLevel level;
    private final String message;

    private ActivationLogEntry(Instant timestamp, LogLevel level, String message) {

        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public Instant getTimestamp() {

        return timestamp;
    }

    public LogLevel getLevel() {

        return level;
    }

    public String getMessage() {

        return message;
    }

    public static Result<ActivationLogEntry> of(Instant timestamp, LogLevel level, String message) {

        if (timestamp == null) {

            return Result.failure(RequiredValueMissing.forField("timestamp"));
        }

        if (level == null ) {

            return Result.failure(RequiredValueMissing.forField("level"));
        }

        if (message == null) {

            return Result.failure(RequiredValueMissing.forField("message"));
        }

        return Result.success(new ActivationLogEntry(timestamp, level, message));
    }

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivationLogEntry entry = (ActivationLogEntry) o;
        return Objects.equals(timestamp,entry.timestamp)
            && Objects.equals(level,entry.level) 
            && Objects.equals(message,entry.message);
    }

    @Override
    public int hashCode() {

        return Objects.hash(timestamp,level,message);
    }

}
