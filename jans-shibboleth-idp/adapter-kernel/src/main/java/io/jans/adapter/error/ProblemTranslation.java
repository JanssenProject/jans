package io.jans.adapter.error;

import java.util.Objects;

/**
 * How one kind of failure appears to a client: the stable {@code code}, the human {@code title}, the
 * HTTP {@code status}, and the template its detail message is built from.
 *
 * <p>These four travel together on purpose. Registered separately, an error could acquire a code but
 * no title, or a title inconsistent with its status — the same drift the {@link Problem#type()}
 * derivation exists to prevent. One value per error type makes a partial registration impossible.
 */
public record ProblemTranslation(String code, String title, int status, String messageTemplate) {

    public ProblemTranslation {

        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(messageTemplate, "messageTemplate");
    }

    /**
     * Detail prose for this failure as it applies to {@code field}.
     *
     * <p>A template with no placeholder describes the request as a whole, so the field is irrelevant
     * and the template stands alone. A template with a placeholder needs a field; without one there
     * is nothing to name, so the message stays deliberately general.
     */
    public String message(String field) {

        if (!messageTemplate.contains("%s")) {

            return messageTemplate;
        }

        return field == null || field.isEmpty()
            ? "The request could not be processed"
            : String.format(messageTemplate, field);
    }

    /**
     * This failure as a problem response, before any request-specific detail is attached.
     */
    public Problem toProblem() {

        return Problem.of(status, code, title);
    }
}
