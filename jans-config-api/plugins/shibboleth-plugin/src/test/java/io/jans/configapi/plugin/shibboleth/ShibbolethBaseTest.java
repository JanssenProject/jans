/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth;

import io.jans.configapi.core.test.BaseTest;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import static org.testng.Assert.*;

public class ShibbolethBaseTest extends BaseTest {

    protected static final String SHIBBOLETH_CONFIG_ENDPOINT = "/shibboleth/config";
    protected static final String SHIBBOLETH_TRUST_ENDPOINT = "/shibboleth/trust";
    protected static final String SHIBBOLETH_METADATA_ENDPOINT = "/shibboleth/metadata";

    @BeforeClass
    public void validateTestEnvironment() {
        log.info("=== Shibboleth Plugin Test Environment Validation ===");
        
        String shibbolethUrl = propertiesMap.get("shibbolethUrl");
        assertNotNull(shibbolethUrl, "shibbolethUrl must be configured in test.properties");
        assertFalse(shibbolethUrl.isEmpty(), "shibbolethUrl must not be empty");
        log.info("Shibboleth URL: {}", shibbolethUrl);
        
        assertNotNull(accessToken, "Access token must be obtained - check tokenEndpoint, clientId, clientSecret");
        assertFalse(accessToken.isEmpty(), "Access token must not be empty - token acquisition failed");
        log.info("Access token obtained successfully (length: {})", accessToken.length());
        
        log.info("=== Test Environment Validated ===");
    }

    @BeforeMethod
    public void before() {
        log.info("Checking Shibboleth Plugin availability at: {}", propertiesMap.get("shibbolethUrl"));
        
        boolean isAvailable = isEndpointAvailable(propertiesMap.get("shibbolethUrl"), accessToken);
        log.info("Shibboleth Plugin available: {}", isAvailable);
        
        if (!isAvailable) {
            throw new SkipException("Shibboleth Plugin Not deployed or not accessible at: " + propertiesMap.get("shibbolethUrl"));
        }
        
        log.info("Shibboleth Plugin is deployed and accessible");
    }
}
