// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class ProfilesTest {

    @Test
    public void profilesHaveTheSpecValues() {
        assertEquals(Profiles.assertionProfile(), "tag:jans.io,2026:trace-v1", "TRACE assertion profile");
        assertEquals(Profiles.receiptProfile(), "tag:jans.io,2026:lock-trace-receipt-v1", "Lock receipt profile");
    }
}
