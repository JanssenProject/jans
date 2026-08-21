package io.jans.shibboleth.trust.dto.activation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class WorkerDtoJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void registerRequestDeserialisesOrigin() throws Exception {

        RegisterWorkerRequest request =
            mapper.readValue("{\"origin\":\"worker-1@host\"}", RegisterWorkerRequest.class);

        assertThat(request.getOrigin()).isEqualTo("worker-1@host");
    }

    @Test
    void registerRequestRejectsUnknownField() {

        assertThatThrownBy(() -> mapper.readValue("{\"origin\":\"w@h\",\"bogus\":\"x\"}", RegisterWorkerRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }

    @Test
    void workerViewSerialisesWithSnakeCaseKeys() throws Exception {

        WorkerView view = new WorkerView("worker-1@host", "2027-01-01T00:00:00Z", "2027-01-01T00:01:00Z");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder("origin", "registered_at", "last_heartbeat_at");
        assertThat(json.get("origin").asText()).isEqualTo("worker-1@host");
        assertThat(json.get("last_heartbeat_at").asText()).isEqualTo("2027-01-01T00:01:00Z");
    }

    private static Iterable<String> fieldNames(JsonNode node) {

        return () -> node.fieldNames();
    }
}
