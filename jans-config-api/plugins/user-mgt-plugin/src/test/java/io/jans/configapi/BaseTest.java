/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;


import java.nio.file.Paths;
import java.util.Map;

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import org.slf4j.Logger;

public class BaseTest {

@BeforeSuite
    public void initTestSuite(ITestContext context) throws Exception {

        logger.info("Invoked initTestSuite of '{}'", context.getCurrentXmlTest().getName());
        if (client == null) {
            setupClient(context.getSuite().getXmlSuite().getParameters());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        
    }

    @AfterSuite
    public void finalize() {
        client.close();
    }

    private void setupClient(Map<String, String> params) throws Exception {

        logger.info("Initializing client...");
        String tokenEndpointAuthnMethod = params.get("tokenEndpointAuthnMethod");
        String domainURL = params.get("domainURL");
        String OIDCMetadataUrl = params.get("OIDCMetadataUrl");
        String clientId = params.get("clientId");

        if (tokenEndpointAuthnMethod.equals("client_secret_basic")) {
        	client = ScimClientFactory.getClient(domainURL, OIDCMetadataUrl, clientId, params.get("clientSecret"));

        } else if (tokenEndpointAuthnMethod.equals("client_secret_post")) {
        	client = ScimClientFactory.getClient(domainURL, OIDCMetadataUrl, clientId, params.get("clientSecret"), true);

        } else if (tokenEndpointAuthnMethod.equals("private_key_jwt")) {
        	client = ScimClientFactory.getClient(domainURL, OIDCMetadataUrl, clientId, 
        		Paths.get(params.get("keyStorePath")), params.get("keyStorePassword"), params.get("keyId"));
        } else {
           throw new Exception("Unsupported method for token endpoint authentication: " + tokenEndpointAuthnMethod);
        }

    }

}
