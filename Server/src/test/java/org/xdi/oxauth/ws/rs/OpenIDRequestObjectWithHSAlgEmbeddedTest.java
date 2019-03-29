/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.gluu.oxauth.model.register.RegisterResponseParam.CLIENT_ID_ISSUED_AT;
import static org.gluu.oxauth.model.register.RegisterResponseParam.CLIENT_SECRET;
import static org.gluu.oxauth.model.register.RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT;
import static org.gluu.oxauth.model.register.RegisterResponseParam.REGISTRATION_ACCESS_TOKEN;
import static org.gluu.oxauth.model.register.RegisterResponseParam.REGISTRATION_CLIENT_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.client.AuthorizationRequest;
import org.gluu.oxauth.client.QueryStringDecoder;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.model.authorize.Claim;
import org.gluu.oxauth.client.model.authorize.ClaimValue;
import org.gluu.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.register.RegisterResponseParam;
import org.gluu.oxauth.model.util.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

/**
 * Functional tests for OpenID Request Object (embedded)
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public class OpenIDRequestObjectWithHSAlgEmbeddedTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static String clientId1;
	private static String clientSecret1;
	private static String clientId2;
	private static String clientSecret2;
	private static String clientId3;
	private static String clientSecret3;

	@Parameters({ "registerPath", "redirectUris" })
	@Test
	public void requestParameterMethodHS256Step1(final String registerPath, final String redirectUris)
			throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS256);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			fail(e.getMessage(), e);
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodHS256Step1", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
		assertNotNull(entity, "Unexpected result: " + entity);
		try {
			JSONObject jsonObj = new JSONObject(entity);
			assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
			assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
			assertTrue(jsonObj.has(REGISTRATION_ACCESS_TOKEN.toString()));
			assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
			assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
			assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

			clientId1 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
			clientSecret1 = jsonObj.getString(CLIENT_SECRET.toString());
		} catch (JSONException e) {
			fail(e.getMessage(), e);
		}
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri" })
	@Test(dependsOnMethods = "requestParameterMethodHS256Step1")
	public void requestParameterMethodHS256Step2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri) throws Exception {
		Builder request = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
			List<String> scopes = Arrays.asList("openid");
			String state = "STATE0";
			String nonce = UUID.randomUUID().toString();

			AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
					redirectUri, nonce);
			authorizationRequest.setState(state);
			authorizationRequest.getPrompts().add(Prompt.NONE);
			authorizationRequest.setAuthUsername(userId);
			authorizationRequest.setAuthPassword(userSecret);

			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.HS256, clientSecret1, cryptoProvider);
			jwtAuthorizationRequest
					.addIdTokenClaim(new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue(userId)));
			String authJwt = jwtAuthorizationRequest.getEncodedJwt();
			authorizationRequest.setRequest(authJwt);
			System.out.println("Request JWT: " + authJwt);

			request = ResteasyClientBuilder.newClient()
					.target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
			request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
			request.header("Accept", MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodHS256Step2", response, entity);

		assertEquals(response.getStatus(), 302, "Unexpected response code.");
		assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

		try {
			URI uri = new URI(response.getLocation().toString());
			assertNotNull(uri.getFragment(), "Query string is null");

			Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

			assertNotNull(params.get("access_token"), "The accessToken is null");
			assertNotNull(params.get("scope"), "The scope is null");
			assertNotNull(params.get("state"), "The state is null");
		} catch (URISyntaxException e) {
			fail(e.getMessage(), e);
		}
	}

	@Parameters({ "registerPath", "redirectUris" })
	@Test
	public void requestParameterMethodHS384Step1(final String registerPath, final String redirectUris)
			throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS384);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			fail(e.getMessage(), e);
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodHS384Step1", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
		assertNotNull(entity, "Unexpected result: " + entity);
		try {
			JSONObject jsonObj = new JSONObject(entity);
			assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
			assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
			assertTrue(jsonObj.has(REGISTRATION_ACCESS_TOKEN.toString()));
			assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
			assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
			assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

			clientId2 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
			clientSecret2 = jsonObj.getString(CLIENT_SECRET.toString());
		} catch (JSONException e) {
			fail(e.getMessage(), e);
		}
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri" })
	@Test(dependsOnMethods = "requestParameterMethodHS384Step1")
	public void requestParameterMethodHS384Step2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri) throws Exception {
		Builder request = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
			List<String> scopes = Arrays.asList("openid");
			String nonce = UUID.randomUUID().toString();
			String state = UUID.randomUUID().toString();

			AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId2, scopes,
					redirectUri, nonce);
			authorizationRequest.setState(state);
			authorizationRequest.getPrompts().add(Prompt.NONE);
			authorizationRequest.setAuthUsername(userId);
			authorizationRequest.setAuthPassword(userSecret);

			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.HS384, clientSecret2, cryptoProvider);
			jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
			jwtAuthorizationRequest
					.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
			jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
			jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
			jwtAuthorizationRequest
					.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
			jwtAuthorizationRequest
					.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
			jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
					ClaimValue.createValueList(new String[] { "2" })));
			String authJwt = jwtAuthorizationRequest.getEncodedJwt();
			authorizationRequest.setRequest(authJwt);
			System.out.println("Request JWT: " + authJwt);

			request = ResteasyClientBuilder.newClient()
					.target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
			request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
			request.header("Accept", MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodHS384Step2", response, entity);

		assertEquals(response.getStatus(), 302, "Unexpected response code.");
		assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

		try {
			URI uri = new URI(response.getLocation().toString());
			assertNotNull(uri.getFragment(), "Query string is null");

			Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

			assertNotNull(params.get("access_token"), "The accessToken is null");
			assertNotNull(params.get("scope"), "The scope is null");
			assertNotNull(params.get("state"), "The state is null");
		} catch (URISyntaxException e) {
			fail(e.getMessage(), e);
		}
	}

	@Parameters({ "registerPath", "redirectUris" })
	@Test
	public void requestParameterMethodHS512Step1(final String registerPath, final String redirectUris)
			throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS512);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			fail(e.getMessage(), e);
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodHS512Step1", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
		assertNotNull(entity, "Unexpected result: " + entity);
		try {
			JSONObject jsonObj = new JSONObject(entity);
			assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
			assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
			assertTrue(jsonObj.has(REGISTRATION_ACCESS_TOKEN.toString()));
			assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
			assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
			assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

			clientId3 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
			clientSecret3 = jsonObj.getString(CLIENT_SECRET.toString());
		} catch (JSONException e) {
			fail(e.getMessage(), e);
		}
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri" })
	@Test(dependsOnMethods = "requestParameterMethodHS512Step1")
	public void requestParameterMethodHS512Step2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri) throws Exception {
		Builder request = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
			List<String> scopes = Arrays.asList("openid");
			String nonce = UUID.randomUUID().toString();
			String state = UUID.randomUUID().toString();

			AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId3, scopes,
					redirectUri, nonce);
			authorizationRequest.setState(state);
			authorizationRequest.getPrompts().add(Prompt.NONE);
			authorizationRequest.setAuthUsername(userId);
			authorizationRequest.setAuthPassword(userSecret);

			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.HS512, clientSecret3, cryptoProvider);
			jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
			jwtAuthorizationRequest
					.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
			jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
			jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
			jwtAuthorizationRequest
					.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
			jwtAuthorizationRequest
					.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
			jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
					ClaimValue.createValueList(new String[] { "2" })));
			String authJwt = jwtAuthorizationRequest.getEncodedJwt();
			authorizationRequest.setRequest(authJwt);
			System.out.println("Request JWT: " + authJwt);

			request = ResteasyClientBuilder.newClient()
					.target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
			request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
			request.header("Accept", MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodHS512Step2", response, entity);

		assertEquals(response.getStatus(), 302, "Unexpected response code.");
		assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

		try {
			URI uri = new URI(response.getLocation().toString());
			assertNotNull(uri.getFragment(), "Query string is null");

			Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

			assertNotNull(params.get("access_token"), "The accessToken is null");
			assertNotNull(params.get("scope"), "The scope is null");
			assertNotNull(params.get("state"), "The state is null");
		} catch (URISyntaxException e) {
			fail(e.getMessage(), e);
		}
	}

}