package io.jans.adapter.error;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One field-level validation failure, as clients receive it in a problem response's
 * {@code violations} array.
 *
 * <p>{@code field} names a field of the request body the client sent, never a domain type or one of
 * its fields. {@code code} is the stable contract clients branch on; {@code message} is prose for
 * humans and may be reworded or localised without notice.
 */
public record Violation(
    @JsonProperty("field") String field,
    @JsonProperty("code") String code,
    @JsonProperty("message") String message) {
}
