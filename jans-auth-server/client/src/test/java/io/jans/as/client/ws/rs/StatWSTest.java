package io.jans.as.client.ws.rs;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.as.client.BaseTest;
import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.StatService;
import io.jans.as.client.uma.wrapper.UmaClient;
import io.jans.as.model.uma.wrapper.Token;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class StatWSTest extends BaseTest {

    private static final String STAT_PATH = "/jans-auth/restv1/internal/stat";

    @Test(enabled = false)
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void stat(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        final Token authorization = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);

        final StatService service = ClientFactory.instance().createStatService(issuer + STAT_PATH);
        final JsonNode node = service.stat("Bearer " + authorization.getAccessToken(), "202101", null, null, null);
        assertTrue(node != null && node.hasNonNull("response"));
    }

    @Test(enabled = false)
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void statWithDateRange(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        final Token authorization = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);

        final StatService service = ClientFactory.instance().createStatService(issuer + STAT_PATH);
        final JsonNode node = service.stat("Bearer " + authorization.getAccessToken(), null, "202201", "202204", null);
        assertTrue(node != null && node.hasNonNull("response"));
    }

    @Test(enabled = false)
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void statPost(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        final Token authorization = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);
        final StatService service = ClientFactory.instance().createStatService(issuer + "/jans-auth/restv1/internal/stat");
        final JsonNode node = service.stat(authorization.getAccessToken(), "202101", null, null, null);
        assertTrue(node != null && node.hasNonNull("response"));
    }
}