package io.jans.ca.server;

import com.google.common.base.Preconditions;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.arquillian.ClientIterfaceImpl;
import io.jans.ca.server.tests.PathTestEndPoint;
import io.jans.ca.server.tests.SetupClientTest;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

public class SetUpTest {

    private static final Logger LOG = LoggerFactory.getLogger(SetUpTest.class);

    public static void beforeSuite(String url, String host, String opHost, String redirectUrls) {
        try {

            RegisterSiteResponse setupClient = SetupClientTest.setupClient(Tester.newClient(url), opHost, redirectUrls);
            Tester.setSetupClient(setupClient, host, opHost);
            LOG.debug("SETUP_CLIENT is set in Tester.");

            Preconditions.checkNotNull(Tester.getSetupAuthorization(url));
            LOG.debug("Tester's authorization is set.");

            LOG.debug("Finished beforeSuite!");
        } catch (Exception e) {
            LOG.error("Failed to start suite.", e);
            throw new AssertionError("Failed to start suite.");
        }
    }

    public static void afterSuite() {
        LOG.debug("Running afterSuite ... SetupTest");
    }

}
