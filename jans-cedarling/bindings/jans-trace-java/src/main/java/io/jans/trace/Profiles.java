// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.Jans_trace_uniffiKt;

/**
 * Profile ids that fix the output: no library release changes the output for an existing
 * profile. Read from the native library, which defines them, so Java never holds a second copy.
 */
public final class Profiles {

    private Profiles() {
    }

    /** The profile of Jans TRACE assertions. */
    public static String assertionProfile() {
        return Jans_trace_uniffiKt.assertionProfile();
    }

    /** The profile of Lock receipts. */
    public static String receiptProfile() {
        return Jans_trace_uniffiKt.receiptProfile();
    }
}
