/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.gluu.oxauth.model.authorize.AuthorizeResponseParam;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseMode;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;

/**
 * @author Javier Rojas Blum
 * @version December 12, 2016
 */
public class AuthorizeWithResponseModeEmbeddedTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static String clientId;

	@Parameters({ "registerPath", "redirectUris" })
	@Test
	public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.TOKEN,
					ResponseType.ID_TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("dynamicClientRegistration", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
		assertNotNull(entity, "Unexpected result: " + entity);
		try {
			final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
			ClientTestUtil.assert_(registerResponse);

			clientId = registerResponse.getClientId();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage() + "\nResponse was: " + entity);
		}
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri" })
	@Test(dependsOnMethods = "dynamicClientRegistration")
	public void requestAuthorizationCodeWithResponseModeQuery(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri) throws Exception {
		final String state = UUID.randomUUID().toString();

		List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
		List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

		AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
				redirectUri, null);
		authorizationRequest.setState(state);
		authorizationRequest.getPrompts().add(Prompt.NONE);
		authorizationRequest.setAuthUsername(userId);
		authorizationRequest.setAuthPassword(userSecret);
		authorizationRequest.setResponseMode(ResponseMode.QUERY);

		Builder request = ResteasyClientBuilder.newClient()
				.target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
		request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
		request.header("Accept", MediaType.TEXT_PLAIN);

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestAuthorizationCodeWithResponseModeQuery", response, entity);

		assertEquals(response.getStatus(), 302, "Unexpected response code.");
		assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

		try {
			URI uri = new URI(response.getLocation().toString());
			assertNotNull(uri.getQuery(), "Query string is null");

			Map<String, String> params = QueryStringDecoder.decode(uri.getQuery());

			assertNotNull(params.get(AuthorizeResponseParam.CODE), "The code is null");
			assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
			assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
			assertEquals(params.get(AuthorizeResponseParam.STATE), state);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail("Response URI is not well formed");
		}
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri" })
	@Test(dependsOnMethods = "dynamicClientRegistration")
	public void requestAuthorizationCodeWithResponseModeFragment(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri) throws Exception {

		final String state = UUID.randomUUID().toString();

		List<ResponseType> responseTypes = new ArrayList<ResponseType>();
		responseTypes.add(ResponseType.CODE);
		List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

		AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
				redirectUri, null);
		authorizationRequest.setState(state);
		authorizationRequest.getPrompts().add(Prompt.NONE);
		authorizationRequest.setAuthUsername(userId);
		authorizationRequest.setAuthPassword(userSecret);
		authorizationRequest.setResponseMode(ResponseMode.FRAGMENT);

		Builder request = ResteasyClientBuilder.newClient()
				.target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
		request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
		request.header("Accept", MediaType.TEXT_PLAIN);

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestAuthorizationCodeWithResponseModeFragment", response, entity);

		assertEquals(response.getStatus(), 302, "Unexpected response code.");
		assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

		try {
			URI uri = new URI(response.getLocation().toString());
			assertNotNull(uri.getFragment(), "Fragment is null");

			Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

			assertNotNull(params.get(AuthorizeResponseParam.CODE), "The code is null");
			assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
			assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");

			assertEquals(params.get(AuthorizeResponseParam.STATE), state);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail("Response URI is not well formed");
		}
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri" })
	@Test(dependsOnMethods = "dynamicClientRegistration")
	public void requestAuthorizationTokenWithResponseModeQuery(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri) throws Exception {

		final String state = UUID.randomUUID().toString();

		List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
		List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
		String nonce = UUID.randomUUID().toString();

		AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
				redirectUri, nonce);
		authorizationRequest.setState(state);
		authorizationRequest.getPrompts().add(Prompt.NONE);
		authorizationRequest.setAuthUsername(userId);
		authorizationRequest.setAuthPassword(userSecret);
		authorizationRequest.setResponseMode(ResponseMode.QUERY);

		Builder request = ResteasyClientBuilder.newClient()
				.target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
		request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
		request.header("Accept", MediaType.TEXT_PLAIN);

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestAuthorizationTokenWithResponseModeQuery", response, entity);

		assertEquals(response.getStatus(), 302, "Unexpected response code.");
		assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

		if (response.getLocation() != null) {
			try {
				URI uri = new URI(response.getLocation().toString());
				assertNotNull(uri.getQuery(), "Query is null");

				Map<String, String> params = QueryStringDecoder.decode(uri.getQuery());

				assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The access token is null");
				assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
				assertNotNull(params.get(AuthorizeResponseParam.TOKEN_TYPE), "The token type is null");
				assertNotNull(params.get(AuthorizeResponseParam.EXPIRES_IN), "The expires in value is null");
				assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope must be null");
				assertNull(params.get("refresh_token"), "The refresh_token must be null");
				assertEquals(params.get(AuthorizeResponseParam.STATE), state);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				fail("Response URI is not well formed");
			}
		}
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri" })
	@Test(dependsOnMethods = "dynamicClientRegistration")
	public void requestAuthorizationTokenWithResponseModeFragment(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri) throws Exception {

		final String state = UUID.randomUUID().toString();

		List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
		List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
		String nonce = UUID.randomUUID().toString();

		AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
				redirectUri, nonce);
		authorizationRequest.setState(state);
		authorizationRequest.getPrompts().add(Prompt.NONE);
		authorizationRequest.setAuthUsername(userId);
		authorizationRequest.setAuthPassword(userSecret);
		authorizationRequest.setResponseMode(ResponseMode.FRAGMENT);

		Builder request = ResteasyClientBuilder.newClient()
				.target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
		request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
		request.header("Accept", MediaType.TEXT_PLAIN);

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestAuthorizationTokenWithResponseModeFragment", response, entity);

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
			} catch (URISyntaxException e) {
				e.printStackTrace();
				fail("Response URI is not well formed");
			}
		}
	}

}