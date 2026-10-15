// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/**
 * JSON text exactly as received. Pass an HTTP body read as UTF-8 unchanged: do not parse and
 * serialize it again, and do not normalize Unicode. A type of its own, so JSON cannot be passed
 * where an id or a key is expected.
 */
public final class JsonBody {

    private final String value;

    public JsonBody(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public String value() {
        return value;
    }

    io.jans.trace.uniffi.JsonBody inner() {
        return new io.jans.trace.uniffi.JsonBody(value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JsonBody && value.equals(((JsonBody) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /** Only the length: the text may be a record, and records do not belong in logs. */
    @Override
    public String toString() {
        return "JsonBody[" + value.length() + " chars]";
    }
}
