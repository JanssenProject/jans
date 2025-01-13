/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.link;

import io.jans.configapi.core.test.BaseTest;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import jakarta.ws.rs.core.Response;

import org.apache.http.entity.ContentType;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

public class LinkBaseTest extends BaseTest {

    protected boolean isAvailable(final String url, Map<String, String> headers, final Map<String, String> parameters) {
        return isEndpointAvailable(url, headers, parameters);
    }

    @BeforeMethod
    public void before() {
        boolean isAvailable = isAvailable(propertiesMap.get("fido2Url"), null, null);
        log.error("\n\n\n *** JANS-LINK Plugin isAvailable{}", isAvailable);
        // check condition, note once you condition is met the rest of the tests will be
        // skipped as well
        if (!isAvailable) {
            throw new SkipException("JANS-LINK Plugin Not deployed");
        }
    }

}
