package io.jans.scim2.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.factory.ScimClientFactory;
import io.jans.scim2.client.rest.ClientSideService;

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by jgomer on 2017-06-09.
 */
public class BaseTest {

    protected static ClientSideService client = null;
    protected Logger logger = LogManager.getLogger(getClass());
    protected ObjectMapper mapper = new ObjectMapper();

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

    public UserResource getDeepCloneUsr(UserResource bean) throws Exception {
        return mapper.readValue(mapper.writeValueAsString(bean), UserResource.class);
    }

}
