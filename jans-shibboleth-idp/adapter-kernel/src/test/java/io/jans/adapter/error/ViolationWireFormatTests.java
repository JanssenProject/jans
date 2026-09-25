package io.jans.adapter.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pins the JSON clients actually receive. The names are published in the shared {@code Violation}
 * schema, so a rename here is a breaking API change and should fail rather than ship.
 */
@DisplayName("Violation — the published wire shape")
public class ViolationWireFormatTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @DisplayName("GIVEN a violation WHEN serialised THEN it carries exactly field, code and message")
    public void serialisesToThePublishedNames() throws Exception {

        String json = JSON.writeValueAsString(
            new Violation("display_name", "required_value_missing", "'display_name' is required"));

        assertThat(json).isEqualTo(
            "{\"field\":\"display_name\","
                + "\"code\":\"required_value_missing\","
                + "\"message\":\"'display_name' is required\"}");
    }

    @Test
    @DisplayName("GIVEN a validation failure WHEN its violations are serialised THEN order is preserved")
    public void violationsSerialiseAsAnOrderedArray() throws Exception {

        List<Violation> violations = Arrays.asList(
            new Violation("entity_id", "required_value_missing", "'entity_id' is required"),
            new Violation("valid_until", "invalid_timestamp_syntax", "'valid_until' is bad"));

        String json = JSON.writeValueAsString(RequestValidationFailed.with(violations).getViolations());

        assertThat(json).startsWith("[{\"field\":\"entity_id\"");
        assertThat(json).contains("{\"field\":\"valid_until\"");
    }

    @Test
    @DisplayName("GIVEN two violations with the same parts WHEN compared THEN they are equal")
    public void violationsAreValues() {

        Violation one = new Violation("uri", "invalid_uri_syntax", "bad");
        Violation other = new Violation("uri", "invalid_uri_syntax", "bad");

        assertThat(one).isEqualTo(other);
        assertThat(one.hashCode()).isEqualTo(other.hashCode());
        assertThat(one).isNotEqualTo(new Violation("base_url", "invalid_uri_syntax", "bad"));
    }

    @Test
    @DisplayName("GIVEN a validation failure WHEN built THEN it holds an unmodifiable list and counts fields")
    public void carriesAnUnmodifiableList() {

        RequestValidationFailed failure = RequestValidationFailed.with(
            Collections.singletonList(new Violation("uri", "invalid_uri_syntax", "bad")));

        assertThat(failure.getMessage()).isEqualTo("Request validation failed: 1 invalid field");
        assertThatThrownBy(() -> failure.getViolations().add(new Violation("x", "y", "z")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("GIVEN no violations WHEN a validation failure is built THEN the misuse is rejected")
    public void refusesToDescribeNothing() {

        assertThatThrownBy(() -> RequestValidationFailed.with(Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RequestValidationFailed.with(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
