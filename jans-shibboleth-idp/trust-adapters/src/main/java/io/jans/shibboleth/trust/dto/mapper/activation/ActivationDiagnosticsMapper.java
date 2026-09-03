package io.jans.shibboleth.trust.dto.mapper.activation;

import io.jans.shibboleth.trust.config.error.InvalidTimestampSyntax;
import io.jans.shibboleth.trust.dto.activation.ActivationDiagnosticsRequest;
import io.jans.shibboleth.trust.dto.activation.ActivationLogEntryRequest;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.kernel.FieldPath;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;
import io.jans.adapter.error.Violations;
import io.jans.shibboleth.trust.dto.error.TrustErrorTranslation;
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

        Violations violations = Violations.create(TrustErrorTranslation.INSTANCE);

        if (request.getOrigin() == null || request.getOrigin().isBlank()) {

            violations.record(
                RequiredValueMissing.forField(ActivationDiagnosticsRequest.class, "origin"),
                FieldPath.of("origin"));
        }

        Instant startedAt = violations.take(
            parseInstant(ActivationDiagnosticsRequest.class, request.getStartedAt(), "started_at"));

        Instant completedAt = violations.take(
            parseInstant(ActivationDiagnosticsRequest.class, request.getCompletedAt(), "completed_at"));

        // every log entry is checked, so a worker reporting a malformed batch learns about all of it
        List<ActivationLogEntry> logEntries = new ArrayList<>();
        List<ActivationLogEntryRequest> requestEntries =
            request.getLogEntries() == null ? List.of() : request.getLogEntries();
        for (int index = 0; index < requestEntries.size(); index++) {

            ActivationLogEntryRequest item = requestEntries.get(index);

            Instant timestamp = violations.take(
                parseInstant(ActivationLogEntryRequest.class, item.getTimestamp(), "timestamp")
                    .at("log_entries", index));

            if (timestamp == null) {

                continue;
            }

            ActivationLogEntry entry = violations.take(
                ActivationLogEntry.of(timestamp, item.getLevel(), item.getMessage()).at("log_entries", index));

            if (entry != null) {

                logEntries.add(entry);
            }
        }

        if (violations.any()) {

            return violations.asFailure();
        }

        return violations.completeWith(ActivationDiagnostics.of(
            request.getStatus(),
            Origin.of(request.getOrigin()),
            logEntries,
            startedAt,
            completedAt));
    }

    private static Result<Instant> parseInstant(Class<?> owner, String value, String field) {

        if (value == null || value.isBlank()) {

            return Result.failure(RequiredValueMissing.forField(owner, field), FieldPath.of(field));
        }
        try {

            return Result.success(Instant.parse(value));
        } catch (DateTimeParseException e) {

            return Result.failure(InvalidTimestampSyntax.forValue(value), FieldPath.of(field));
        }
    }
}
