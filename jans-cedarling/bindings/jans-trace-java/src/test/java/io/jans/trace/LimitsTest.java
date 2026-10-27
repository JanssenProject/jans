// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;

public class LimitsTest {

    @Test
    public void newLimitsHoldTheDefaults() {
        Limits limits = new Limits();
        long[] actual = {
            limits.maxInputBytes(), limits.maxDepth(), limits.maxObjectMembers(),
            limits.maxArrayElements(), limits.maxStringBytes(), limits.maxCanonicalBytes(),
            limits.maxBatchBytes(), limits.maxBatchRecords(),
        };
        long[] expected = {256 * 1024, 32, 1024, 1024, 64 * 1024, 512 * 1024, 8 * 1024 * 1024, 500};
        assertEquals(actual, expected, "defaults should be the core library values");
    }

    @Test
    public void everySetterStoresItsOwnValue() {
        Limits limits = new Limits()
                .withMaxInputBytes(1)
                .withMaxDepth(2)
                .withMaxObjectMembers(3)
                .withMaxArrayElements(4)
                .withMaxStringBytes(5)
                .withMaxCanonicalBytes(6)
                .withMaxBatchBytes(7)
                .withMaxBatchRecords(8);
        long[] actual = {
            limits.maxInputBytes(), limits.maxDepth(), limits.maxObjectMembers(),
            limits.maxArrayElements(), limits.maxStringBytes(), limits.maxCanonicalBytes(),
            limits.maxBatchBytes(), limits.maxBatchRecords(),
        };
        assertEquals(actual, new long[] {1, 2, 3, 4, 5, 6, 7, 8}, "each setter should set its own limit");
    }

    @Test
    public void withMaxLeavesTheOriginalUnchanged() {
        Limits original = new Limits();
        Limits changed = original.withMaxDepth(7);
        assertEquals(changed.maxDepth(), 7, "the new value should be set");
        assertEquals(original.maxDepth(), 32, "Limits is immutable");
    }

    @Test
    public void negativeLimitIsIllegalArgument() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> new Limits().withMaxDepth(-1));
        assertEquals(e.getMessage(), "a limit must not be negative, got -1",
                "the message should name the rejected value");
    }
}
