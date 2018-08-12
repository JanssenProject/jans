package io.swagger.client.api;

import com.google.common.base.Preconditions;
import io.swagger.client.model.SetupClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

/**
 * Main class to set up and tear down suite.
 *
 * @author Yuriy Zabrovarnyy
 * @author Shoeb Khan
 * @version  02/08/2018
 */

public class SetUpTest {

    private static final Logger LOG = LoggerFactory.getLogger(SetUpTest.class);

    @Parameters({"host", "opHost", "redirectUrl"})
    @BeforeSuite
    public static void beforeSuite(String host, String opHost, String redirectUrl) {
        try {
            LOG.debug("Running beforeSuite ...");

            SetupClientResponse setupClient = SetupClientTestGen.setupClient(Tester.api(host), opHost, redirectUrl);
            Tester.setSetupClient(setupClient, host, opHost);
            LOG.debug("SETUP_CLIENT is set in Tester.");

            Preconditions.checkNotNull(Tester.getAuthorization());
            LOG.debug("Tester's authorization is set.");

            LOG.debug("Finished beforeSuite!");
        } catch (Exception e) {
            LOG.error("Failed to start suite.", e);
            throw new AssertionError(String.format("Failed to start suite:%s", e.getMessage()));
        }
    }


    @AfterSuite
    public static void afterSuite() {
        try {
            LOG.debug("Running afterSuite ...");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
