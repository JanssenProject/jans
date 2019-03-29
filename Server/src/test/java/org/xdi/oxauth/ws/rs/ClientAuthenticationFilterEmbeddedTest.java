/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.authorize.AuthorizeResponseParam;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.register.RegisterResponseParam;
import org.gluu.oxauth.model.util.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * Functional tests for the Client Authentication Filter (embedded)
 *
 * @author Javier Rojas Blum
 * @version September 3, 2018
 */
public class ClientAuthenticationFilterEmbeddedTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static String clientId;
	private static String authorizationCode1;
	private static String customAttrValue1;

	@Parameters({"registerPath", "redirectUris"})
	@Test
	public void requestClientRegistrationWithCustomAttributes(final String registerPath, final String redirectUris)
			throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.TOKEN,
					ResponseType.ID_TOKEN);

			customAttrValue1 = UUID.randomUUID().toString();
			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
			registerRequest.addCustomAttribute("myCustomAttr1", customAttrValue1);

			List<GrantType> grantTypes = Arrays.asList(
					GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
			);
			registerRequest.setGrantTypes(grantTypes);

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestClientRegistrationWithCustomAttributes", response, entity);

		ResponseAsserter responseAsserter = ResponseAsserter.of(response.getStatus(), entity);
		responseAsserter.assertRegisterResponse();
		clientId = responseAsserter.getJson().getJson().getString(RegisterResponseParam.CLIENT_ID.toString());
	}

	@Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
	@Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
	public void requestAccessTokenCustomClientAuth1Step1(final String authorizePath, final String userId,
														 final String userSecret, final String redirectUri) throws Exception {
		final String state = UUID.randomUUID().toString();
		final String nonce = UUID.randomUUID().toString();

		List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
		List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

		AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
				redirectUri, nonce);
		authorizationRequest.setState(state);
		authorizationRequest.setAuthUsername(userId);
		authorizationRequest.setAuthPassword(userSecret);
		authorizationRequest.getPrompts().add(Prompt.NONE);

		Builder request = ResteasyClientBuilder.newClient()
				.target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
		request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
		request.header("Accept", MediaType.TEXT_PLAIN);

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestAccessTokenCustomClientAuth1Step1", response, entity);

		assertEquals(response.getStatus(), 302, "Unexpected response code.");
		assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

		try {
			URI uri = new URI(response.getLocation().toString());
			assertNotNull(uri.getFragment(), "Query string is null");

			Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

			assertNotNull(params.get(AuthorizeResponseParam.CODE), "The code is null");
			assertNotNull(params.get(AuthorizeResponseParam.ID_TOKEN), "The id token is null");
			assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
			assertEquals(params.get(AuthorizeResponseParam.STATE), state);

			authorizationCode1 = params.get(AuthorizeResponseParam.CODE);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail("Response URI is not well formed");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Parameters({"tokenPath", "redirectUri"})
	@Test(dependsOnMethods = {"requestAccessTokenCustomClientAuth1Step1"})
	public void requestAccessTokenCustomClientAuth1Step2(final String tokenPath, final String redirectUri)
			throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

		TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
		tokenRequest.setCode(authorizationCode1);
		tokenRequest.setRedirectUri(redirectUri);
		tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
		tokenRequest.addCustomParameter("myCustomAttr1", customAttrValue1);

		request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

		Response response = request
				.post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
		String entity = response.readEntity(String.class);

		showResponse("requestAccessTokenCustomClientAuth1Step2", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code.");
		assertTrue(
				response.getHeaderString("Cache-Control") != null
						&& response.getHeaderString("Cache-Control").equals("no-store"),
				"Unexpected result: " + response.getHeaderString("Cache-Control"));
		assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
				"Unexpected result: " + response.getHeaderString("Pragma"));
		assertNotNull(entity, "Unexpected result: " + entity);
		try {
			JSONObject jsonObj = new JSONObject(entity);
			assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
			assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
			assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
			assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage() + "\nResponse was: " + entity);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Parameters({"tokenPath", "userId", "userSecret"})
	@Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
	public void requestAccessTokenCustomClientAuth2(final String tokenPath, final String userId,
													final String userSecret) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

		TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
		tokenRequest.setUsername(userId);
		tokenRequest.setPassword(userSecret);
		tokenRequest.setScope("profile email");
		tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
		tokenRequest.addCustomParameter("myCustomAttr1", customAttrValue1);

		Response response = request
				.post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
		String entity = response.readEntity(String.class);

		showResponse("requestAccessTokenCustomClientAuth2", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code.");
		assertTrue(
				response.getHeaderString("Cache-Control") != null
						&& response.getHeaderString("Cache-Control").equals("no-store"),
				"Unexpected result: " + response.getHeaderString("Cache-Control"));
		assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
				"Unexpected result: " + response.getHeaderString("Pragma"));
		assertTrue(!entity.equals(null), "Unexpected result: " + entity);
		try {
			JSONObject jsonObj = new JSONObject(entity);
			assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
			assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
			assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
			assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage() + "\nResponse was: " + entity);
		}
	}

}