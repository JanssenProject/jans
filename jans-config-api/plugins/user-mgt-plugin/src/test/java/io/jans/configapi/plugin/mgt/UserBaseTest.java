/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.mgt;

import io.jans.configapi.core.test.BaseTest;

import java.util.Map;

import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

public class UserBaseTest extends BaseTest {

    protected boolean isAvailable(final String url, Map<String, String> headers, final Map<String, String> parameters) {
        return isEndpointAvailable(url, headers, parameters);
    }
    
    // Execute before each test is run
    @BeforeMethod
    public void before() {
        boolean isAvailable = isAvailable(propertiesMap.get("userUrl"), null, null);
        log.error("\n\n\n ******************** USER-MGT Plugin isAvailable{} {}", isAvailable, "\n\n\n");
        // check condition, note once you condition is met the rest of the tests will be
        // skipped as well
        if (!isAvailable) {
            throw new SkipException("USER-MGT Plugin not deployed");
        }else {
            log.info("\n\n\n *** USER-MGT Plugin is Deployed{} {}", "\n\n");
        }
    }
}
