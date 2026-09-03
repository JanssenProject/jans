package io.jans.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainError — shared root error type")
public class DomainErrorTests {

    private static final class TestError extends DomainError {

        private TestError(String message) {

            super(message);
        }
    }

    private static final class TestOwner {
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

        RequiredValueMissing error = RequiredValueMissing.forField(TestOwner.class, "displayName");

        assertThat(error).isInstanceOf(DomainError.class);
        assertThat(error.getOwner()).isEqualTo(TestOwner.class);
        assertThat(error.getFieldName()).isEqualTo("displayName");
        assertThat(error.namesField()).isTrue();
        assertThat(error.getMessage()).contains("TestOwner").contains("displayName");
    }

    @Test
    @DisplayName("GIVEN a single-field type WHEN its value is missing THEN the error names the type and no field")
    public void requiredValueMissingNamesOwningTypeAlone() {

        RequiredValueMissing error = RequiredValueMissing.of(TestOwner.class);

        assertThat(error.getOwner()).isEqualTo(TestOwner.class);
        assertThat(error.namesField()).isFalse();
        assertThat(error.getFieldName()).isEmpty();
        assertThat(error.getMessage()).isEqualTo("TestOwner requires a value");
    }

    @Test
    @DisplayName("GIVEN forField WHEN given a blank field name THEN it rejects the call in favour of of(Class)")
    public void requiredValueMissingRejectsBlankFieldName() {

        assertThatThrownBy(() -> RequiredValueMissing.forField(TestOwner.class, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("of(Class)");
    }
}
