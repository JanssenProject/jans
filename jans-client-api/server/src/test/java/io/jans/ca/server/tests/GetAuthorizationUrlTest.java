package io.jans.ca.server.tests;

import com.google.common.collect.Lists;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.params.GetAuthorizationUrlParams;
import io.jans.ca.common.response.GetAuthorizationUrlResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class GetAuthorizationUrlTest extends BaseTest {
    
    @ArquillianResource
    private URI url;
    
    @Parameters({"host", "redirectUrls", "opHost"})
    @Test
    public void test(String host, String redirectUrls, String opHost) {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setRpId(site.getRpId());

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(getApiTagetURL(url), site), null, commandParams);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
    }

    @Parameters({"host", "opHost", "redirectUrls", "postLogoutRedirectUrl", "logoutUrl", "paramRedirectUrl"})
    @Test
    public void testWithParameterAuthorizationUrl(String host, String opHost, String redirectUrls, String postLogoutRedirectUrl, String logoutUrl, String paramRedirectUrl) {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, postLogoutRedirectUrl,
                logoutUrl, false);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setRpId(site.getRpId());
        commandParams.setRedirectUri(paramRedirectUrl);

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(getApiTagetURL(url), site), null, commandParams);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        assertTrue(resp.getAuthorizationUrl().contains(paramRedirectUrl));
    }

    @Parameters({"host", "redirectUrls", "opHost"})
    @Test
    public void testWithResponseType(String host, String redirectUrls, String opHost) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setRpId(site.getRpId());
        commandParams.setResponseTypes(Lists.newArrayList("code", "token"));

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(getApiTagetURL(url), site), null, commandParams);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());

        Map<String, String> parameters = CoreUtils.splitQuery(resp.getAuthorizationUrl());
        assertTrue(parameters.get("response_type").contains("code"));
        assertTrue(parameters.get("response_type").contains("token"));
    }

    @Parameters({"host", "redirectUrls", "opHost"})
    @Test
    public void testWithParams(String host, String redirectUrls, String opHost) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setRpId(site.getRpId());

        Map<String, String> params = new HashMap<>();
        params.put("max_age", "70");
        params.put("is_valid", "true");
        commandParams.setParams(params);

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(getApiTagetURL(url), site), null, commandParams);
        notEmpty(resp.getAuthorizationUrl());

        Map<String, String> parameters = CoreUtils.splitQuery(resp.getAuthorizationUrl());

        assertTrue(StringUtils.isNotBlank(parameters.get("max_age")));
        assertEquals(parameters.get("max_age"), "70");
        assertTrue(StringUtils.isNotBlank(parameters.get("is_valid")));
        assertEquals(parameters.get("is_valid"), "true");
        assertNotNull(resp);
    }


    @Parameters({"host", "opHost", "redirectUrls", "postLogoutRedirectUrl", "logoutUrl", "paramRedirectUrl", "state"})
    @Test
    public void testWithCustomStateParameter(String host, String opHost, String redirectUrls, String postLogoutRedirectUrl, String logoutUrl, String paramRedirectUrl, String state) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, postLogoutRedirectUrl, logoutUrl, false);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setRpId(site.getRpId());
        commandParams.setRedirectUri(paramRedirectUrl);
        commandParams.setState(state);

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(getApiTagetURL(url), site), null, commandParams);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        assertTrue(resp.getAuthorizationUrl().contains(paramRedirectUrl));

        Map<String, String> parameters = CoreUtils.splitQuery(resp.getAuthorizationUrl());
        assertTrue(StringUtils.isNotBlank(parameters.get("state")));
        assertEquals(parameters.get("state"), state);
    }

    @Parameters({"host", "opHost", "redirectUrls", "postLogoutRedirectUrl", "logoutUrl", "paramRedirectUrl"})
    @Test
    public void testWithNonceParameter(String host, String opHost, String redirectUrls, String postLogoutRedirectUrl, String logoutUrl, String paramRedirectUrl) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, postLogoutRedirectUrl, logoutUrl, false);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setRpId(site.getRpId());
        commandParams.setRedirectUri(paramRedirectUrl);
        commandParams.setNonce("dummy_nonce");

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(getApiTagetURL(url), site), null, commandParams);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        assertTrue(resp.getAuthorizationUrl().contains(paramRedirectUrl));

        Map<String, String> parameters = CoreUtils.splitQuery(resp.getAuthorizationUrl());
        assertTrue(StringUtils.isNotBlank(parameters.get("nonce")));
        assertEquals(parameters.get("nonce"), "dummy_nonce");
    }
}
