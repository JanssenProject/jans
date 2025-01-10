/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.scim;

import io.jans.configapi.core.test.BaseTest;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import jakarta.ws.rs.core.Response;

import org.apache.http.entity.ContentType;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

public class ScimBaseTest extends BaseTest {

    // Execute before each test is run
    @BeforeMethod
    public void before(Method methodName) {
        boolean isServiceDeployed = isServiceDeployed("io.jans.configapi.plugin.scim.rest.ApiApplication");
        log.info("\n\n\n *** SCIM Plugin isServiceDeployed{}", isServiceDeployed);
        // check condition, note once you condition is met the rest of the tests will be
        // skipped as well
        if (!isServiceDeployed) {
            throw new SkipException("SCIM Plugin not deployed");
        }
    }

    
}
