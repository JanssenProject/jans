package org.gluu.oxd.mock;

import io.dropwizard.testing.DropwizardTestSupport;
import org.gluu.oxd.mock.guice.MockAppModule;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.ServerLauncher;
import org.gluu.oxd.server.SetUpTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

public class SetUpMockServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(org.gluu.oxd.mock.SetUpMockServerTest.class);

    public static DropwizardTestSupport<OxdServerConfiguration> SUPPORT = null;


    @Parameters({"host", "opHost", "redirectUrls"})
    @BeforeSuite
    public static void beforeSuite(String host, String opHost, String redirectUrls) {
        try {
            LOG.debug("Running beforeSuite of Mock server...");
            ServerLauncher.setInjector(new MockAppModule());
            SetUpTest.beforeSuite(host, opHost, redirectUrls);
            LOG.debug("Finished beforeSuite of Mock server!");
        } catch (Exception e) {
            LOG.error("Failed to start suite of Mock server.", e);
            throw new AssertionError("Failed to start suite of Mock server.");
        }
    }

    @AfterSuite
    public static void afterSuite() {
        try {
            LOG.debug("Running afterSuite ...");
            SetUpTest.afterSuite();
            LOG.debug("HTTP server is successfully stopped.");
        } catch (Exception e) {
            LOG.error("Failed to stop HTTP server.", e);
        }
    }

}
