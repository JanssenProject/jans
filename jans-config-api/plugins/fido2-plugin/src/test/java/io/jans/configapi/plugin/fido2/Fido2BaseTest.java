/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2;

import io.jans.configapi.core.test.BaseTest;

import java.util.Map;

import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

public class Fido2BaseTest extends BaseTest {

    protected boolean isAvailable(final String url, Map<String, String> headers, final Map<String, String> parameters) {
        return isEndpointAvailable(url, headers, parameters);
    }

    // Execute before each test is run
    @BeforeMethod
    public void before() {
        boolean isAvailable = isAvailable(propertiesMap.get("fido2Url"), null, null);
        log.info("\n\n\n *** FIDO2 Plugin isAvailable{}", isAvailable);
        // check condition, note once you condition is met the rest of the tests will be
        // skipped as well
        if (!isAvailable) {
            throw new SkipException("FIDO2 Plugin Not deployed");
        }
    }

}
