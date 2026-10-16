// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import org.testng.annotations.Test;
import io.jans.trace.TraceException.SelfTestException;

public class SelfTestTest {

    @Test
    public void passesOnAWorkingBuild() throws SelfTestException {
        SelfTest.run();
    }
}
