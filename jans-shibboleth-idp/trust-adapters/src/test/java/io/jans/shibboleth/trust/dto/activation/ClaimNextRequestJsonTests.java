package io.jans.shibboleth.trust.dto.activation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.activation.model.WorkItemType;

import org.junit.jupiter.api.Test;

class ClaimNextRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesOriginAndType() throws Exception {

        ClaimNextRequest request = mapper.readValue(
            "{\"origin\":\"worker-1@host\",\"type\":\"PROCESS_AGGREGATE_METADATA\"}", ClaimNextRequest.class);

        assertThat(request.getOrigin()).isEqualTo("worker-1@host");
        assertThat(request.getType()).isEqualTo(WorkItemType.PROCESS_AGGREGATE_METADATA);
    }

    @Test
    void rejectsUnknownField() {

        assertThatThrownBy(() -> mapper.readValue(
            "{\"origin\":\"w@h\",\"type\":\"PROCESS_AGGREGATE_METADATA\",\"bogus\":1}", ClaimNextRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }

    @Test
    void rejectsUnknownType() {

        assertThatThrownBy(() -> mapper.readValue(
            "{\"origin\":\"w@h\",\"type\":\"NONSENSE\"}", ClaimNextRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
    }
}
