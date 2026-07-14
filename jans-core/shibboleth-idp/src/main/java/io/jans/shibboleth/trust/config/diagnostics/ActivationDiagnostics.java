package io.jans.shibboleth.trust.config.diagnostics;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.util.TrustResult;
import io.jans.shibboleth.shared.Origin;

public class ActivationDiagnostics {

    private final ActivationStatus status;
    private final Origin origin;
    private final List<ActivationLogEntry> logEntries;
    private final Instant startedAt;
    private final Instant completedAt;

    private ActivationDiagnostics(
        ActivationStatus status,
        Origin origin,
        List<ActivationLogEntry> logEntries,
        Instant startedAt, 
        Instant completedAt ) {
        
        this.status = status;
        this.origin = origin;
        this.logEntries = List.copyOf(logEntries);
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public ActivationStatus getStatus() {

        return status;
    }

    public Origin getOrigin() {

        return origin;
    }

    public List<ActivationLogEntry> getLogEntries() {

        return logEntries;
    }

    public Instant getStartedAt() {

        return startedAt;
    }

    public Instant getCompletedAt() {

        return completedAt;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivationDiagnostics diagnostics = (ActivationDiagnostics) o;
        return Objects.equals(status,diagnostics.status)
            && Objects.equals(origin,diagnostics.origin)
            && Objects.equals(logEntries,diagnostics.logEntries)
            && Objects.equals(startedAt,diagnostics.startedAt)
            && Objects.equals(completedAt,diagnostics.completedAt);
    }

    @Override
    public int hashCode() {

        return Objects.hash(status,origin,logEntries,startedAt,completedAt);
    }

    public static ActivationDiagnostics none() {

        return builder()
            .status(ActivationStatus.NO_DATA)
            .origin(Origin.of(""))
            .logEntries(List.of())
            .startedAt(Instant.EPOCH)
            .completedAt(Instant.EPOCH)
            .build()
            .getValue();
    }

    public static TrustResult<ActivationDiagnostics> of(ActivationStatus status, 
        Origin origin,List<ActivationLogEntry> logEntries, Instant startedAt, Instant completedAt) {

        return builder()
            .status(status)
            .origin(origin)
            .logEntries(logEntries)
            .startedAt(startedAt)
            .completedAt(completedAt)
            .build();
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(ActivationDiagnostics existing) {

        return new Builder(existing);
    }

    public static class Builder {

        private ActivationStatus status;
        private Origin origin;
        private List<ActivationLogEntry> logEntries;
        private Instant startedAt;
        private Instant completedAt;

        private Builder(ActivationDiagnostics existing) {

            if (existing != null) {

                status = existing.status;
                origin = existing.origin;
                logEntries = existing.logEntries;
                startedAt = existing.startedAt;
                completedAt = existing.completedAt;
            }else {
                status = null;
                origin = null;
                logEntries = null;
                startedAt = null;
                completedAt = null;
            }
        }

        public Builder status(ActivationStatus status) {

            this.status = status;
            return this;
        }

        public Builder origin(Origin origin) {

            this.origin = origin;
            return this;
        }

        public Builder logEntries(List<ActivationLogEntry> logEntries) {

            this.logEntries = logEntries;
            return this;
        }

        public Builder startedAt(Instant startedAt) {

            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {

            this.completedAt = completedAt;
            return this;
        }

        public TrustResult<ActivationDiagnostics> build() {

            

            if (status == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("status"));
            }

            if (origin == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("origin"));
            }

            if (logEntries == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("logEntries"));
            }

            if (startedAt == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("startedAt"));
            }

            if (completedAt == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("completedAt"));
            }

            return TrustResult.success(new ActivationDiagnostics(status, origin, logEntries, startedAt, completedAt));
        }
    }
}
