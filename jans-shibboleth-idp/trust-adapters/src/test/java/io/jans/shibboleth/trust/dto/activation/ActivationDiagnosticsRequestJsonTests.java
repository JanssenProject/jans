package io.jans.shibboleth.trust.dto.activation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.shared.diagnostics.LogLevel;

import org.junit.jupiter.api.Test;

class ActivationDiagnosticsRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesFromSnakeCaseWithLogEntries() throws Exception {

        String body = "{\"origin\":\"worker-1@host\",\"status\":\"SUCCEEDED\","
            + "\"started_at\":\"2027-01-01T00:00:00Z\",\"completed_at\":\"2027-01-01T00:05:00Z\","
            + "\"log_entries\":[{\"timestamp\":\"2027-01-01T00:02:00Z\",\"level\":\"INFO\",\"message\":\"ok\"}]}";

        ActivationDiagnosticsRequest request = mapper.readValue(body, ActivationDiagnosticsRequest.class);

        assertThat(request.getOrigin()).isEqualTo("worker-1@host");
        assertThat(request.getStatus()).isEqualTo(ActivationStatus.SUCCEEDED);
        assertThat(request.getStartedAt()).isEqualTo("2027-01-01T00:00:00Z");
        assertThat(request.getLogEntries()).hasSize(1);
        assertThat(request.getLogEntries().get(0).getLevel()).isEqualTo(LogLevel.INFO);
    }

    @Test
    void leavesLogEntriesNullWhenOmitted() throws Exception {

        String body = "{\"origin\":\"w@h\",\"status\":\"NO_DATA\","
            + "\"started_at\":\"2027-01-01T00:00:00Z\",\"completed_at\":\"2027-01-01T00:00:00Z\"}";

        ActivationDiagnosticsRequest request = mapper.readValue(body, ActivationDiagnosticsRequest.class);

        assertThat(request.getLogEntries()).isNull();
    }

    @Test
    void rejectsUnknownField() {

        String body = "{\"origin\":\"w@h\",\"status\":\"NO_DATA\",\"started_at\":\"2027-01-01T00:00:00Z\","
            + "\"completed_at\":\"2027-01-01T00:00:00Z\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, ActivationDiagnosticsRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
