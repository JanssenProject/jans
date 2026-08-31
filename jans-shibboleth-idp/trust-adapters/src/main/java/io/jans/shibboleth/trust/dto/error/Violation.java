package io.jans.shibboleth.trust.dto.error;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * One field-level validation failure, as clients receive it in a problem response's
 * {@code violations} array.
 *
 * <p>{@code field} names a field of the request body the client sent, never a domain type or one of
 * its fields. {@code code} is the stable contract clients branch on; {@code message} is prose for
 * humans and may be reworded or localised without notice.
 */
public class Violation {

    @JsonProperty("field")
    private final String field;

    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    public Violation(String field, String code, String message) {

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

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

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
