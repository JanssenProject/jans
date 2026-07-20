package io.jans.shibboleth.trust.dto.mapper.activation;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.error.InvalidTimestampSyntax;
import io.jans.shibboleth.trust.dto.activation.ActivationDiagnosticsRequest;
import io.jans.shibboleth.trust.dto.activation.ActivationLogEntryRequest;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.shared.diagnostics.LogLevel;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class ActivationDiagnosticsMapperTests {

    @Test
    void shouldBuildDiagnosticsFromValidRequest() {

        ActivationDiagnosticsRequest request = request(ActivationStatus.SUCCEEDED);
        request.setLogEntries(List.of(
            new ActivationLogEntryRequest("2027-01-01T00:02:00Z", LogLevel.INFO, "metadata processed")));

        Result<ActivationDiagnostics> result = ActivationDiagnosticsMapper.toDomain(request);

        assertThat(result.isSuccess()).isTrue();
        ActivationDiagnostics diagnostics = result.getValue();
        assertThat(diagnostics.getStatus()).isEqualTo(ActivationStatus.SUCCEEDED);
        assertThat(diagnostics.getOrigin().getValue()).isEqualTo("worker-1@host");
        assertThat(diagnostics.getStartedAt()).isEqualTo(Instant.parse("2027-01-01T00:00:00Z"));
        assertThat(diagnostics.getCompletedAt()).isEqualTo(Instant.parse("2027-01-01T00:05:00Z"));
        assertThat(diagnostics.getLogEntries()).hasSize(1);
        assertThat(diagnostics.getLogEntries().get(0).getLevel()).isEqualTo(LogLevel.INFO);
        assertThat(diagnostics.getLogEntries().get(0).getMessage()).isEqualTo("metadata processed");
    }

    @Test
    void shouldSucceedWithNoLogEntries() {

        Result<ActivationDiagnostics> result = ActivationDiagnosticsMapper.toDomain(request(ActivationStatus.NO_DATA));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getLogEntries()).isEmpty();
    }

    @Test
    void shouldFailWhenOriginIsMissing() {

        ActivationDiagnosticsRequest request = request(ActivationStatus.SUCCEEDED);
        request.setOrigin("   ");

        Result<ActivationDiagnostics> result = ActivationDiagnosticsMapper.toDomain(request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldFailWhenStatusIsMissing() {

        Result<ActivationDiagnostics> result = ActivationDiagnosticsMapper.toDomain(request(null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldFailWhenStartedAtIsMalformed() {

        ActivationDiagnosticsRequest request = request(ActivationStatus.SUCCEEDED);
        request.setStartedAt("not-a-timestamp");

        Result<ActivationDiagnostics> result = ActivationDiagnosticsMapper.toDomain(request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidTimestampSyntax.class);
    }

    @Test
    void shouldFailWhenALogEntryTimestampIsMalformed() {

        ActivationDiagnosticsRequest request = request(ActivationStatus.SUCCEEDED);
        request.setLogEntries(List.of(new ActivationLogEntryRequest("nope", LogLevel.ERROR, "boom")));

        Result<ActivationDiagnostics> result = ActivationDiagnosticsMapper.toDomain(request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidTimestampSyntax.class);
    }

    private static ActivationDiagnosticsRequest request(ActivationStatus status) {

        ActivationDiagnosticsRequest request = new ActivationDiagnosticsRequest();
        request.setOrigin("worker-1@host");
        request.setStatus(status);
        request.setStartedAt("2027-01-01T00:00:00Z");
        request.setCompletedAt("2027-01-01T00:05:00Z");
        return request;
    }
}
