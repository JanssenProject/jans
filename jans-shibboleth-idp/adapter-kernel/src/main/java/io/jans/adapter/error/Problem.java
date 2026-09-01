package io.jans.adapter.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * An error response in the RFC 7807 problem format — the shape both API specs share via
 * {@code openapi/components/common.yaml}.
 *
 * <p>{@code code} is the stable contract clients branch on; {@code type}, {@code title},
 * {@code status} and {@code detail} describe the failure for humans and tooling. {@code violations}
 * carries the field-level detail when the failure was a request-body validation failure, and is
 * omitted otherwise.
 *
 * <p>{@code type} is derived from {@code code} rather than passed separately: the spec defines it as
 * the code under {@link #TYPE_BASE}, so accepting both independently would only let them disagree.
 * A caller may still supply an explicit {@code type} for a problem that lives outside that
 * namespace.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(
    @JsonProperty("type") String type,
    @JsonProperty("title") String title,
    @JsonProperty("status") int status,
    @JsonProperty("detail") String detail,
    @JsonProperty("instance") String instance,
    @JsonProperty("code") String code,
    @JsonProperty("violations") @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Violation> violations) {

    /**
     * Namespace every problem type URI sits under, per the shared schema.
     */
    public static final String TYPE_BASE = "https://jans.io/shibboleth-idp/problems/";

    public Problem {

        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(title, "title");

        type = type == null ? TYPE_BASE + code : type;
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    /**
     * A problem with no field-level detail — the shape for a conflict, a not-found, or anything else
     * that describes the request as a whole.
     */
    public static Problem of(int status, String code, String title) {

        return new Problem(null, title, status, null, null, code, List.of());
    }

    public Problem withDetail(String detail) {

        return new Problem(type, title, status, detail, instance, code, violations);
    }

    /**
     * Attaches the request URI the failure happened at. The transport layer knows this; the boundary
     * does not.
     */
    public Problem withInstance(String instance) {

        return new Problem(type, title, status, detail, instance, code, violations);
    }

    /**
     * Attaches field-level failures, as collected by {@link Violations}.
     */
    public Problem withViolations(List<Violation> violations) {

        return new Problem(type, title, status, detail, instance, code, violations);
    }

    public boolean hasViolations() {

        return !violations.isEmpty();
    }
}
