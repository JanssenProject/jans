package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.RemoveSiteParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RemoveSiteResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author yuriyz
 */
public class RemoveSiteTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "redirectUrls"})
    @Test
    public void removeSiteTest(String host, String opHost, String redirectUrls) {
        ClientInterface client = getClientInterface(url);

        RegisterSiteResponse resp = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        assertNotNull(resp);

        notEmpty(resp.getRpId());

        RemoveSiteResponse removeResponse = client.removeSite(Tester.getAuthorization(getApiTagetURL(url), resp), null, new RemoveSiteParams(resp.getRpId()));
        assertNotNull(removeResponse);
        assertNotNull(removeResponse.getRpId());
    }
}
