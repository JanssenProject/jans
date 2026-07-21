package io.jans.shibboleth.trust.dto.mapper.activation;

import io.jans.shibboleth.trust.config.error.InvalidTimestampSyntax;
import io.jans.shibboleth.trust.dto.activation.ActivationDiagnosticsRequest;
import io.jans.shibboleth.trust.dto.activation.ActivationLogEntryRequest;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationLogEntry;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the domain {@link ActivationDiagnostics} a worker reports from its wire request. Surfaces the
 * domain {@link Result} rather than throwing; timestamps are parsed from ISO-8601 strings. The
 * {@code origin} is required — it is the fence the orchestrator checks against the lease holder.
 */
public final class ActivationDiagnosticsMapper {

    private ActivationDiagnosticsMapper() {
    }

    public static Result<ActivationDiagnostics> toDomain(ActivationDiagnosticsRequest request) {

        if (request.getOrigin() == null || request.getOrigin().isBlank()) {

            return Result.failure(RequiredValueMissing.forField("origin"));
        }

        Result<Instant> startedAt = parseInstant(request.getStartedAt(), "started_at");
        if (startedAt.isFailure()) {

            return Result.failure(startedAt.getError());
        }

        Result<Instant> completedAt = parseInstant(request.getCompletedAt(), "completed_at");
        if (completedAt.isFailure()) {

            return Result.failure(completedAt.getError());
        }

        List<ActivationLogEntry> logEntries = new ArrayList<>();
        List<ActivationLogEntryRequest> requestEntries =
            request.getLogEntries() == null ? List.of() : request.getLogEntries();
        for (ActivationLogEntryRequest item : requestEntries) {

            Result<Instant> timestamp = parseInstant(item.getTimestamp(), "timestamp");
            if (timestamp.isFailure()) {

                return Result.failure(timestamp.getError());
            }

            Result<ActivationLogEntry> entry =
                ActivationLogEntry.of(timestamp.getValue(), item.getLevel(), item.getMessage());
            if (entry.isFailure()) {

                return Result.failure(entry.getError());
            }
            logEntries.add(entry.getValue());
        }

        return ActivationDiagnostics.of(
            request.getStatus(),
            Origin.of(request.getOrigin()),
            logEntries,
            startedAt.getValue(),
            completedAt.getValue());
    }

    private static Result<Instant> parseInstant(String value, String field) {

        if (value == null || value.isBlank()) {

            return Result.failure(RequiredValueMissing.forField(field));
        }
        try {

            return Result.success(Instant.parse(value));
        } catch (DateTimeParseException e) {

            return Result.failure(InvalidTimestampSyntax.forValue(value));
        }
    }
}
