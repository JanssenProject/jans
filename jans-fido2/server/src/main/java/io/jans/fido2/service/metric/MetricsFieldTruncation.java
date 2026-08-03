/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.service.metric;

import java.util.ArrayList;
import java.util.List;

/**
 * Shortens metrics values to the width of the column they are headed for, and
 * remembers which fields had to be cut.
 *
 * <p>A metrics entry is persisted as a single row, so one oversized field kills
 * the whole row - that is exactly how FIDO2 telemetry silently collected nothing
 * until the columns were widened. Callers shorten every free-form value through
 * this class and then log one warning per entry, rather than one per field, so a
 * client sending an oversized header on every request cannot flood the log.
 *
 * <p>Instances are single-threaded and short-lived: create one per conversion.
 *
 * @author Janssen Project
 * @version 1.0
 */
final class MetricsFieldTruncation {

    private final List<String> truncatedFields = new ArrayList<>(2);

    /**
     * Shorten {@code value} to at most {@code maxLength} characters, recording the
     * fact under {@code fieldName} if anything was removed. Null and short values
     * are returned untouched, so this is safe to wrap around a nullable getter.
     *
     * <p>The cut never splits a UTF-16 surrogate pair. A lone surrogate is rejected
     * by MySQL with error 1366 and encodes to invalid UTF-8 for the PostgreSQL
     * driver, which would trade one persistence failure for another.
     *
     * <p>{@link String#length()} counts UTF-16 code units, so a string containing
     * astral-plane characters is measured slightly long. That makes the check
     * conservative - it can cut marginally earlier than strictly necessary, never
     * later - which is the safe direction against a hard column limit.
     */
    String apply(String fieldName, String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        int cut = maxLength;
        if (Character.isHighSurrogate(value.charAt(cut - 1))) {
            cut--;
        }

        truncatedFields.add(fieldName + " (" + value.length() + " > " + maxLength + ")");

        return value.substring(0, cut);
    }

    boolean hasTruncations() {
        return !truncatedFields.isEmpty();
    }

    /**
     * Field names and lengths only. The values themselves are never included:
     * user agents, usernames, session ids and IP addresses are PII, and a warning
     * carrying them would leak into logs that are not protected the way the
     * metrics API is.
     */
    String describe() {
        return String.join(", ", truncatedFields);
    }
}
