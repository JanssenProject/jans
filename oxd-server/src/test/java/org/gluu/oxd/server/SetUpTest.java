package org.gluu.oxd.server;

import com.google.common.base.Preconditions;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.server.persistence.PersistenceService;
import org.gluu.oxd.server.service.RpService;

/**
 * Main class to set up and tear down suite.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class SetUpTest {

    private static final Logger LOG = LoggerFactory.getLogger(SetUpTest.class);

    public static DropwizardTestSupport<OxdServerConfiguration> SUPPORT = null;


    @Parameters({"host", "opHost", "redirectUrl"})
    @BeforeSuite
    public static void beforeSuite(String host, String opHost, String redirectUrl) {
        try {
            LOG.debug("Running beforeSuite ...");
            ServerLauncher.setSetUpSuite(true);

            SUPPORT = new DropwizardTestSupport<OxdServerConfiguration>(OxdServerApplication.class,
                    ResourceHelpers.resourceFilePath("oxd-server-jenkins.yml"),
                    ConfigOverride.config("server.applicationConnectors[0].port", "0") // Optional, if not using a separate testing-specific configuration file, use a randomly selected port
            );
            SUPPORT.before();
            LOG.debug("HTTP server started.");

            removeExistingRps();
            LOG.debug("Existing RPs are removed.");

            RegisterSiteResponse setupClient = SetupClientTest.setupClient(Tester.newClient(host), opHost, redirectUrl);
            Tester.setSetupClient(setupClient, host, opHost);
            LOG.debug("SETUP_CLIENT is set in Tester.");

            Preconditions.checkNotNull(Tester.getAuthorization());
            LOG.debug("Tester's authorization is set.");

            setupSwaggerSuite(Tester.getTargetHost(host), opHost, redirectUrl);
            LOG.debug("Finished beforeSuite!");
        } catch (Exception e) {
            LOG.error("Failed to start suite.", e);
            throw new AssertionError("Failed to start suite.");
        }
    }

    private static void setupSwaggerSuite(String host, String opHost, String redirectUrl) {
        try {
            if (StringUtils.countMatches(host, ":") < 2 && "http://localhost".equalsIgnoreCase(host) || "http://127.0.0.1".equalsIgnoreCase(host) ) {
                host = host + ":" + SetUpTest.SUPPORT.getLocalPort();
            }
            io.swagger.client.api.SetUpTest.beforeSuite(host, opHost, redirectUrl); // manual swagger tests setup
            io.swagger.client.api.SetUpTest.setTokenProtectionEnabled(SUPPORT.getConfiguration().getProtectCommandsWithAccessToken());
        } catch (Throwable e) {
            LOG.error("Failed to setup swagger suite.");
        }
    }

    private static void removeExistingRps() {
        try {
            ServerLauncher.getInjector().getInstance(PersistenceService.class).create();
            ServerLauncher.getInjector().getInstance(RpService.class).removeAllRps();
            ServerLauncher.getInjector().getInstance(RpService.class).load();
            LOG.debug("Finished removeExistingRps successfullly.");
        } catch (Exception e) {
            LOG.error("Failed to removed existing RPs.", e);
        }
    }

    @AfterSuite
    public static void afterSuite() {
        try {
            LOG.debug("Running afterSuite ...");
            SUPPORT.after();
            ServerLauncher.shutdown(false);
            LOG.debug("HTTP server is successfully stopped.");
        } catch (Exception e) {
            LOG.error("Failed to stop HTTP server.", e);
        }
    }

}
