// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/**
 * Bounds for untrusted input, checked before and while parsing. Immutable: every
 * {@code withMax*} returns a new value. Keep the host's own API limits equal or lower.
 */
public final class Limits {

    private final io.jans.trace.uniffi.Limits inner;

    /** The library defaults. */
    public Limits() {
        this(new io.jans.trace.uniffi.Limits());
    }

    private Limits(io.jans.trace.uniffi.Limits inner) {
        this.inner = inner;
    }

    io.jans.trace.uniffi.Limits inner() {
        return inner;
    }

    @FunctionalInterface
    private interface Setter {
        io.jans.trace.uniffi.Limits apply() throws io.jans.trace.uniffi.InvalidLimitException;
    }

    /** A negative limit is a programming error, not a property of the input. */
    private static Limits with(Setter setter) {
        try {
            return new Limits(setter.apply());
        } catch (io.jans.trace.uniffi.InvalidLimitException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public Limits withMaxInputBytes(long value) {
        return with(() -> inner.withMaxInputBytes(value));
    }

    /** Nesting of arrays and objects; the outermost container is depth 1. */
    public Limits withMaxDepth(long value) {
        return with(() -> inner.withMaxDepth(value));
    }

    public Limits withMaxObjectMembers(long value) {
        return with(() -> inner.withMaxObjectMembers(value));
    }

    public Limits withMaxArrayElements(long value) {
        return with(() -> inner.withMaxArrayElements(value));
    }

    /** UTF-8 bytes of one string value or member name after unescaping. */
    public Limits withMaxStringBytes(long value) {
        return with(() -> inner.withMaxStringBytes(value));
    }

    public Limits withMaxCanonicalBytes(long value) {
        return with(() -> inner.withMaxCanonicalBytes(value));
    }

    /** Bound on a whole bulk request body, checked before it is parsed. */
    public Limits withMaxBatchBytes(long value) {
        return with(() -> inner.withMaxBatchBytes(value));
    }

    public Limits withMaxBatchRecords(long value) {
        return with(() -> inner.withMaxBatchRecords(value));
    }

    public long maxInputBytes() {
        return inner.maxInputBytes();
    }

    public long maxDepth() {
        return inner.maxDepth();
    }

    public long maxObjectMembers() {
        return inner.maxObjectMembers();
    }

    public long maxArrayElements() {
        return inner.maxArrayElements();
    }

    public long maxStringBytes() {
        return inner.maxStringBytes();
    }

    public long maxCanonicalBytes() {
        return inner.maxCanonicalBytes();
    }

    public long maxBatchBytes() {
        return inner.maxBatchBytes();
    }

    public long maxBatchRecords() {
        return inner.maxBatchRecords();
    }
}
