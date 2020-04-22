package io.swagger.client.api;

import com.google.common.collect.Lists;
import io.swagger.client.model.GetAuthorizationUrlParams;
import io.swagger.client.model.GetAuthorizationUrlResponse;
import io.swagger.client.model.RegisterSiteResponse;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxd.common.CoreUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class GetAuthorizationUrlTest {
    @Parameters({"redirectUrls", "opHost"})
    @Test
    public void test(String redirectUrls, String opHost) throws Exception {
        DevelopersApi api = Tester.api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(api, opHost, redirectUrls);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(site.getOxdId());

        final GetAuthorizationUrlResponse resp = api.getAuthorizationUrl(commandParams, Tester.getAuthorization(site), null);
        assertNotNull(resp);
        Tester.notEmpty(resp.getAuthorizationUrl());
    }

    @Parameters({"redirectUrls", "opHost", "state"})
    @Test
    public void testWithCustomStateParameter(String redirectUrls, String opHost, String state) throws Exception {
        DevelopersApi api = Tester.api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(api, opHost, redirectUrls);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setState(state);

        final GetAuthorizationUrlResponse resp = api.getAuthorizationUrl(commandParams, Tester.getAuthorization(site), null);
        assertNotNull(resp);
        Tester.notEmpty(resp.getAuthorizationUrl());

        Map<String, String> parameters = CoreUtils.splitQuery(resp.getAuthorizationUrl());
        assertTrue(StringUtils.isNotBlank(parameters.get("state")));
        assertEquals(parameters.get("state"), state);
    }

    @Parameters({"redirectUrls", "opHost"})
    @Test
    public void testWithNonceParameter(String redirectUrls, String opHost) throws Exception {
        DevelopersApi api = Tester.api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(api, opHost, redirectUrls);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setNonce("dummy_nonce");

        final GetAuthorizationUrlResponse resp = api.getAuthorizationUrl(commandParams, Tester.getAuthorization(site), null);
        assertNotNull(resp);
        Tester.notEmpty(resp.getAuthorizationUrl());

        Map<String, String> parameters = CoreUtils.splitQuery(resp.getAuthorizationUrl());
        assertTrue(StringUtils.isNotBlank(parameters.get("nonce")));
        assertEquals(parameters.get("nonce"), "dummy_nonce");
    }

    @Parameters({"redirectUrls", "opHost"})
    @Test
    public void testWithResposeType(String redirectUrls, String opHost) throws Exception {
        DevelopersApi api = Tester.api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(api, opHost, redirectUrls);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setResponseTypes(Lists.newArrayList("code", "token"));

        final GetAuthorizationUrlResponse resp = api.getAuthorizationUrl(commandParams, Tester.getAuthorization(site), null);
        assertNotNull(resp);
        Tester.notEmpty(resp.getAuthorizationUrl());

        Map<String, String> parameters = CoreUtils.splitQuery(resp.getAuthorizationUrl());
        assertTrue(parameters.get("response_type").contains("code"));
        assertTrue(parameters.get("response_type").contains("token"));
    }
}
