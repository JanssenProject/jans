package io.jans.ca.server.tests;

import io.jans.ca.server.SetUpTest;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;

public class ClearAndPrepareTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(ClearAndPrepareTest.class);

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "redirectUrls"})
    @Test
    public void clearAndPrepareTests(String host, String opHost, String redirectUrls) throws IOException {
        showTitle("clearAndPrepareTests");
        SetUpTest.beforeSuite(url.toString(), host, opHost, redirectUrls);
    }

}
