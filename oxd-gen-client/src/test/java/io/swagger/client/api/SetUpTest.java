package io.swagger.client.api;

import com.google.common.base.Preconditions;
import io.swagger.client.ApiException;
import io.swagger.client.model.RegisterSiteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

/**
 * Main class to set up and tear down suite.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class SetUpTest {

    private static final Logger LOG = LoggerFactory.getLogger(SetUpTest.class);

    @Parameters({"host", "opHost", "redirectUrl"})
    @BeforeSuite
    public static void beforeSuite(String host, String opHost, String redirectUrl) {
        try {
            LOG.debug("Running beforeSuite ...");
            Tester.setHost(host);
            Tester.setOpHost(opHost);

            RegisterSiteResponse clientSetupInfo = RegisterSiteTest.registerSite(Tester.api(), opHost, redirectUrl);
            Tester.setSetupData(clientSetupInfo);

            Preconditions.checkNotNull(Tester.getAuthorization());
            LOG.debug("Tester's authorization is set.");

            LOG.debug("Finished beforeSuite!");
        } catch (ApiException e) {
            LOG.error("Code :"+ e.getCode());
            LOG.error("Message :"+ e.getMessage());
            LOG.error("Error :"+ e.getResponseBody().getError());
            LOG.error("Error Description :"+ e.getResponseBody().getErrorDescription());
            LOG.error("Details :"+ e.getResponseBody().getDetails());
            LOG.error("Failed to start swagger suite.", e);
            throw new AssertionError("Failed to start suite.");
        }
    }

    public static void setTokenProtectionEnabled(Boolean isTokenProtectionEnabled) {
        Tester.setTokenProtectionEnabled(isTokenProtectionEnabled);
    }

}
