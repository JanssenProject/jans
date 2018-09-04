package io.swagger.client.api;

import com.google.common.base.Preconditions;
import io.swagger.client.model.RegisterSiteResponseData;
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

            RegisterSiteResponseData clientSetupInfo = RegisterSiteTest.registerSite(Tester.api(), opHost, redirectUrl);
            Tester.setSetupData(clientSetupInfo);

            Preconditions.checkNotNull(Tester.getAuthorization());
            LOG.debug("Tester's authorization is set.");

            LOG.debug("Finished beforeSuite!");
        } catch (Exception e) {
            LOG.error("Failed to start swagger suite.", e);
        }
    }
}
