package io.jans.shibboleth.trust.dto.activation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class RenewLeaseRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesOrigin() throws Exception {

        RenewLeaseRequest request =
            mapper.readValue("{\"origin\":\"worker-1@host\"}", RenewLeaseRequest.class);

        assertThat(request.getOrigin()).isEqualTo("worker-1@host");
    }

    @Test
    void rejectsUnknownField() {

        assertThatThrownBy(() -> mapper.readValue("{\"origin\":\"w@h\",\"bogus\":1}", RenewLeaseRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
