package io.jans.shibboleth.trust.api.problem;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A single field-level validation failure carried inside a {@link Problem}. {@code field} names the
 * offending input and {@code code} is the stable machine-readable reason; {@code message} is an
 * optional human-readable explanation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Violation {

    @JsonProperty("field")
    private final String field;

    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    @JsonCreator
    public Violation(
            @JsonProperty("field") String field,
            @JsonProperty("code") String code,
            @JsonProperty("message") String message) {

        this.field = field;
        this.code = code;
        this.message = message;
    }

    public String getField() {

        return field;
    }

    public String getCode() {

        return code;
    }

    public String getMessage() {

        return message;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (o == null || getClass() != o.getClass()) {

            return false;
        }
        Violation that = (Violation) o;
        return Objects.equals(field, that.field)
            && Objects.equals(code, that.code)
            && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {

        return Objects.hash(field, code, message);
    }

    @Override
    public String toString() {

        return "Violation{field='" + field + "', code='" + code + "', message='" + message + "'}";
    }
}
