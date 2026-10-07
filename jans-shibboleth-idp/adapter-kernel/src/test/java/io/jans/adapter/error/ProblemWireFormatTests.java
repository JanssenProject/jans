package io.jans.adapter.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Pins the JSON a client receives against the shared {@code Problem} schema in
 * {@code openapi/components/common.yaml}. The required set there is
 * {@code [type, title, status, code]}, and everything else is omitted when absent — so a change that
 * starts emitting {@code null}s, or drops a required field, fails here rather than reaching clients.
 */
@DisplayName("Problem — the published wire shape")
public class ProblemWireFormatTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @DisplayName("GIVEN a problem with no optional parts WHEN serialised THEN only the required fields appear")
    public void omitsEveryAbsentOptionalField() throws Exception {

        String json = JSON.writeValueAsString(
            Problem.of(404, "trust_relationship_not_found", "Trust relationship not found"));

        assertThat(JSON.readTree(json).fieldNames()).toIterable()
            .containsExactlyInAnyOrder("type", "title", "status", "code");
        assertThat(json).doesNotContain("null");
    }

    @Test
    @DisplayName("GIVEN a code WHEN the problem is built THEN type is that code under the shared namespace")
    public void derivesTheTypeUriFromTheCode() {

        Problem problem = Problem.of(400, "required_value_missing", "Required value missing");

        assertThat(problem.type())
            .isEqualTo("https://jans.io/shibboleth-idp/problems/required_value_missing");
        assertThat(problem.type()).isEqualTo(Problem.TYPE_BASE + problem.code());
    }

    @Test
    @DisplayName("GIVEN an explicit type WHEN supplied THEN it is kept rather than derived")
    public void keepsAnExplicitTypeOutsideTheNamespace() {

        Problem problem = new Problem(
            "https://example.org/problems/custom", "Custom", 418, null, null, "custom", List.of());

        assertThat(problem.type()).isEqualTo("https://example.org/problems/custom");
    }

    @Test
    @DisplayName("GIVEN violations WHEN attached THEN they serialise under the shared Violation shape")
    public void carriesViolationsInTheSharedShape() throws Exception {

        Problem problem = Problem.of(400, "required_value_missing", "Required value missing")
            .withDetail("display_name is required")
            .withInstance("/jans-config-api/shibboleth/trust-relationships")
            .withViolations(Arrays.asList(
                new Violation("display_name", "required_value_missing", "'display_name' is required"),
                new Violation("nature", "required_value_missing", "'nature' is required")));

        JsonNode node = JSON.readTree(JSON.writeValueAsString(problem));

        assertThat(node.get("status").asInt()).isEqualTo(400);
        assertThat(node.get("detail").asText()).isEqualTo("display_name is required");
        assertThat(node.get("instance").asText())
            .isEqualTo("/jans-config-api/shibboleth/trust-relationships");
        assertThat(node.get("violations")).hasSize(2);
        assertThat(node.get("violations").get(0).get("field").asText()).isEqualTo("display_name");
        assertThat(node.get("violations").get(0).get("code").asText()).isEqualTo("required_value_missing");
    }

    @Test
    @DisplayName("GIVEN a validation failure WHEN rendered THEN its violations reach the problem unchanged")
    public void rendersAValidationFailure() {

        RequestValidationFailed failure = RequestValidationFailed.with(
            List.of(new Violation("uri", "invalid_uri_syntax", "'uri' is not a valid URI")));

        Problem problem = Problem.of(400, "invalid_uri_syntax", "Invalid URI syntax")
            .withViolations(failure.getViolations());

        assertThat(problem.hasViolations()).isTrue();
        assertThat(problem.violations()).isEqualTo(failure.getViolations());
    }

    @Test
    @DisplayName("GIVEN no violations WHEN inspected THEN the list is empty rather than null")
    public void violationsAreNeverNull() {

        Problem problem = new Problem(null, "Title", 500, null, null, "code", null);

        assertThat(problem.violations()).isEmpty();
        assertThat(problem.hasViolations()).isFalse();
    }

    @Test
    @DisplayName("GIVEN attached violations WHEN read THEN the list cannot be modified")
    public void violationsAreNotModifiable() {

        Problem problem = Problem.of(400, "c", "T")
            .withViolations(List.of(new Violation("f", "c", "m")));

        assertThatThrownBy(() -> problem.violations().add(new Violation("x", "y", "z")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("GIVEN a missing required field WHEN built THEN the misuse is rejected")
    public void refusesToBuildWithoutTheRequiredFields() {

        assertThatThrownBy(() -> Problem.of(400, null, "Title")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Problem.of(400, "code", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN a serialised problem WHEN read back THEN it round-trips")
    public void roundTrips() throws Exception {

        Problem problem = Problem.of(409, "operation_forbidden_from_status", "Operation forbidden")
            .withDetail("not allowed while ACTIVE")
            .withViolations(List.of(new Violation("status", "invalid_status_for_operation", "bad")));

        assertThat(JSON.readValue(JSON.writeValueAsString(problem), Problem.class)).isEqualTo(problem);
    }
}
