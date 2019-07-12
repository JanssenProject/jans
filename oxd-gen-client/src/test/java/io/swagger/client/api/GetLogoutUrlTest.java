package io.swagger.client.api;

import io.swagger.client.ApiException;
import io.swagger.client.model.GetLogoutUriParams;
import io.swagger.client.model.GetLogoutUriResponse;
import io.swagger.client.model.RegisterSiteResponse;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URLEncoder;
import java.util.UUID;

import static io.swagger.client.api.Tester.api;
import static org.testng.Assert.*;

/**
 * Dummy test because we can't check real session management which is handled via browser cookies.
 *
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 * @version 10/31/2018
 */

public class GetLogoutUrlTest {

    @Parameters({"opHost", "redirectUrls", "postLogoutRedirectUrl"})
    @Test
    public void test(String opHost, String redirectUrls, String postLogoutRedirectUrl) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, postLogoutRedirectUrl, "","","" );

        final GetLogoutUriParams params = new GetLogoutUriParams();
        params.setOxdId(site.getOxdId());
        params.setIdTokenHint("dummy_token");
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setState(UUID.randomUUID().toString());
        params.setSessionState(UUID.randomUUID().toString()); // here must be real session instead of dummy UUID

        final GetLogoutUriResponse resp = client.getLogoutUri(Tester.getAuthorization(), params);
        assertNotNull(resp);
        assertNotNull(resp.getUri());
        assertTrue(resp.getUri().contains(URLEncoder.encode(postLogoutRedirectUrl, "UTF-8")));
    }


    @Test
    public void testWithInvalidOxdId() throws Exception {
        final DevelopersApi client = api();

        final GetLogoutUriParams params = new GetLogoutUriParams();
        params.setOxdId(UUID.randomUUID().toString());

        try {
            client.getLogoutUri(Tester.getAuthorization(), params);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);  // fixme should be 404 (NOT_FOUND) instead of BAD_REQUEST,
        }
    }

    @Test
    public void testWithNullOxdId() throws Exception {
        final DevelopersApi client = api();

        final GetLogoutUriParams params = new GetLogoutUriParams();
        params.setOxdId(null);

        try {
            client.getLogoutUri(Tester.getAuthorization(), params);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);  //BAD_REQUEST
        }
    }

}
