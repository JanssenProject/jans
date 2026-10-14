// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.jans.trace.uniffi.Jans_trace_uniffiKt;

/** Bulk request bodies: a JSON array of signed records (Lock MVP section 11.2). */
public final class Batch {

    private Batch() {
    }

    /**
     * Splits a bulk body and parses each record once, keeping each record's exact text. Pass the
     * body as received, read as UTF-8; do not parse it with another JSON library first. A body
     * error (not an array, broken JSON, too many records, too large) rejects the whole batch.
     */
    public static List<BatchRecord> parse(JsonBody body, Limits limits) throws ParseBatchException {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(limits, "limits");
        List<io.jans.trace.uniffi.BatchRecord> records;
        try {
            records = Jans_trace_uniffiKt.parseBatch(body.inner(), limits.inner());
        } catch (io.jans.trace.uniffi.ParseBatchException e) {
            throw new ParseBatchException(e.code(), e.toString());
        }
        List<BatchRecord> result = new ArrayList<>(records.size());
        for (io.jans.trace.uniffi.BatchRecord record : records) {
            result.add(BatchRecord.of(record));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * The bulk body for {@code records}: their own text joined into an array, never serialized
     * again. Refused above the limits Lock applies; split the batch first. On retry, resend the
     * same body and never re-sign.
     */
    public static String body(List<SignedAssertion> records, Limits limits) throws SizeLimitException {
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(limits, "limits");
        List<io.jans.trace.uniffi.SignedAssertion> inner = new ArrayList<>(records.size());
        for (SignedAssertion record : records) {
            inner.add(Objects.requireNonNull(record, "record").inner());
        }
        try {
            return Jans_trace_uniffiKt.batchBody(inner, limits.inner());
        } catch (io.jans.trace.uniffi.SizeLimitException e) {
            throw new SizeLimitException(e.code(), e.toString());
        }
    }
}
