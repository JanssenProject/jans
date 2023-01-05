/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.client.EndSessionRequest;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.BaseTest;
import io.jans.as.server.model.TClientService;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Yuriy Zabrovarnyy
 */
public class EndSessionBackchannelRestServerTest extends BaseTest {

    private static io.jans.as.client.RegisterResponse registerResponse;
    private static String idToken;
    private static String sid;
    @ArquillianResource
    private URI url;

    @Parameters({"redirectUris", "postLogoutRedirectUri"})
    @Test
    public void requestEndSessionStep1(final String redirectUris, final String postLogoutRedirectUri) throws Exception {

        io.jans.as.client.RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
        registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));
        registerRequest.setBackchannelLogoutUris(Lists.newArrayList(postLogoutRedirectUri));
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        registerResponse = TClientService.register(registerRequest, getApiTagetURL(url));
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestEndSessionStep1")
    public void requestEndSessionStep2(final String authorizePath, final String userId, final String userSecret,
                                       final String redirectUri) {

        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, registerResponse.getClientId(), scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Invocation.Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestEndSessionStep2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The access token is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertNotNull(params.get(AuthorizeResponseParam.TOKEN_TYPE), "The token type is null");
                assertNotNull(params.get(AuthorizeResponseParam.EXPIRES_IN), "The expires in value is null");
                assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope must be null");
                assertNull(params.get("refresh_token"), "The refresh_token must be null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);

                idToken = params.get(AuthorizeResponseParam.ID_TOKEN);
                sid = Jwt.parseOrThrow(idToken).getClaims().getClaimAsString("sid");
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Parameters({"endSessionPath", "postLogoutRedirectUri"})
    @Test(dependsOnMethods = "requestEndSessionStep2")
    public void requestEndSessionStep3(final String endSessionPath, final String postLogoutRedirectUri)
            throws Exception {
        String state = UUID.randomUUID().toString();

        EndSessionRequest endSessionRequest = new EndSessionRequest(idToken, postLogoutRedirectUri, state);
        endSessionRequest.setSid(sid);

        Invocation.Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + endSessionPath + "?" + endSessionRequest.getQueryString()).request();
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestEndSessionStep3", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation());
        assertTrue(response.getLocation().toString().contains(postLogoutRedirectUri));
        assertTrue(response.getLocation().toString().contains("state=" + state));
    }
}
