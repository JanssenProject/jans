package io.jans.kernel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainError — shared root error type")
public class DomainErrorTests {

    private static final class TestError extends DomainError {

        private TestError(String message) {

            super(message);
        }
    }

    @Test
    @DisplayName("GIVEN a DomainError subclass WHEN inspected THEN message and toString agree")
    public void exposesMessageContract() {

        DomainError error = new TestError("something went wrong");

        assertThat(error.getMessage()).isEqualTo("something went wrong");
        assertThat(error.toString()).isEqualTo(error.getMessage());
    }

    @Test
    @DisplayName("GIVEN RequiredValueMissing WHEN created for a field THEN it is a DomainError naming that field")
    public void requiredValueMissingNamesField() {

        RequiredValueMissing error = RequiredValueMissing.forField("displayName");

        assertThat(error).isInstanceOf(DomainError.class);
        assertThat(error.getFieldName()).isEqualTo("displayName");
        assertThat(error.getMessage()).contains("displayName");
    }
}
