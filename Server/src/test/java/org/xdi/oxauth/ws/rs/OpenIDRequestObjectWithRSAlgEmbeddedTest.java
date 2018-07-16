/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
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
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.ResponseAsserter;
import org.xdi.oxauth.client.model.authorize.Claim;
import org.xdi.oxauth.client.model.authorize.ClaimValue;
import org.xdi.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * Functional tests for OpenID Request Object (embedded)
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public class OpenIDRequestObjectWithRSAlgEmbeddedTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static String clientId1;
	private static String clientId2;
	private static String clientId3;
	private static String clientId4;
	private static String clientId5;
	private static String clientId6;

	@Parameters({ "registerPath", "redirectUris", "clientJwksUri" })
	@Test
	public void requestParameterMethodRS256Step1(final String registerPath, final String redirectUris,
			final String jwksUri) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setJwksUri(jwksUri);
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodRS256Step1", response, entity);

		ResponseAsserter responseAsserter = ResponseAsserter.of(response.getStatus(), entity);
		responseAsserter.assertRegisterResponse();
		clientId1 = responseAsserter.getJson().getJson().getString(RegisterResponseParam.CLIENT_ID.toString());
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri", "RS256_keyId", "dnName", "keyStoreFile",
			"keyStoreSecret" })
	@Test(dependsOnMethods = "requestParameterMethodRS256Step1")
	public void requestParameterMethodRS256Step2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri, final String keyId, final String dnName,
			final String keyStoreFile, final String keyStoreSecret) throws Exception {
		Builder request = null;
		try {
			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
			List<String> scopes = Arrays.asList("openid");
			String nonce = UUID.randomUUID().toString();
			String state = UUID.randomUUID().toString();

			AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
					redirectUri, nonce);
			authorizationRequest.setState(state);
			authorizationRequest.getPrompts().add(Prompt.NONE);
			authorizationRequest.setAuthUsername(userId);
			authorizationRequest.setAuthPassword(userSecret);

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.RS256, cryptoProvider);
			jwtAuthorizationRequest.setKeyId(keyId);
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
		showResponse("requestParameterMethodRS256Step2", response, entity);

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
			e.printStackTrace();
			fail("Response URI is not well formed");
		}
	}

	@Parameters({ "registerPath", "redirectUris", "clientJwksUri" })
	@Test
	public void requestParameterMethodRS384Step1(final String registerPath, final String redirectUris,
			final String jwksUri) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setJwksUri(jwksUri);
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodRS384Step1", response, entity);

		ResponseAsserter responseAsserter = ResponseAsserter.of(response.getStatus(), entity);
		responseAsserter.assertRegisterResponse();
		clientId2 = responseAsserter.getJson().getJson().getString(RegisterResponseParam.CLIENT_ID.toString());
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri", "RS384_keyId", "dnName", "keyStoreFile",
			"keyStoreSecret" })
	@Test(dependsOnMethods = "requestParameterMethodRS384Step1")
	public void requestParameterMethodRS384Step2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri, final String keyId, final String dnName,
			final String keyStoreFile, final String keyStoreSecret) throws Exception {
		Builder request = null;
		try {
			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.RS384, cryptoProvider);
			jwtAuthorizationRequest.setKeyId(keyId);
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

		showResponse("requestParameterMethodRS384Step2", response, entity);

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
			e.printStackTrace();
			fail("Response URI is not well formed");
		}
	}

	@Parameters({ "registerPath", "redirectUris", "clientJwksUri" })
	@Test
	public void requestParameterMethodRS512Step1(final String registerPath, final String redirectUris,
			final String jwksUri) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setJwksUri(jwksUri);
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodRS512Step1", response, entity);

		ResponseAsserter responseAsserter = ResponseAsserter.of(response.getStatus(), entity);
		responseAsserter.assertRegisterResponse();
		clientId3 = responseAsserter.getJson().getJson().getString(RegisterResponseParam.CLIENT_ID.toString());
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri", "RS512_keyId", "dnName", "keyStoreFile",
			"keyStoreSecret" })
	@Test(dependsOnMethods = "requestParameterMethodRS512Step1")
	public void requestParameterMethodRS512Step2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri, final String keyId, final String dnName,
			final String keyStoreFile, final String keyStoreSecret) throws Exception {
		Builder request = null;
		try {
			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.RS512, cryptoProvider);
			jwtAuthorizationRequest.setKeyId(keyId);
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

		showResponse("requestParameterMethodRS512Step2", response, entity);

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
			e.printStackTrace();
			fail("Response URI is not well formed");
		}
	}

	@Parameters({ "registerPath", "redirectUris", "clientJwksUri" })
	@Test
	public void requestParameterMethodRS256X509CertStep1(final String registerPath, final String redirectUris,
			final String jwksUri) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setJwksUri(jwksUri);
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodRS256X509CertStep1", response, entity);

		ResponseAsserter responseAsserter = ResponseAsserter.of(response.getStatus(), entity);
		responseAsserter.assertRegisterResponse();
		clientId4 = responseAsserter.getJson().getJson().getString(RegisterResponseParam.CLIENT_ID.toString());
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri", "RS256_keyId", "dnName", "keyStoreFile",
			"keyStoreSecret" })
	@Test(dependsOnMethods = "requestParameterMethodRS256X509CertStep1")
	public void requestParameterMethodRS256X509CertStep2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri, final String keyId, final String dnName,
			final String keyStoreFile, final String keyStoreSecret) throws Exception {
		Builder request = null;
		try {
			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
			List<String> scopes = Arrays.asList("openid");
			String nonce = UUID.randomUUID().toString();
			String state = UUID.randomUUID().toString();

			AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId4, scopes,
					redirectUri, nonce);
			authorizationRequest.setState(state);
			authorizationRequest.getPrompts().add(Prompt.NONE);
			authorizationRequest.setAuthUsername(userId);
			authorizationRequest.setAuthPassword(userSecret);

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.RS256, cryptoProvider);
			jwtAuthorizationRequest.setKeyId(keyId);
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

		showResponse("requestParameterMethodRS256X509CertStep2", response, entity);

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
			e.printStackTrace();
			fail("Response URI is not well formed");
		}
	}

	@Parameters({ "registerPath", "redirectUris", "clientJwksUri" })
	@Test
	public void requestParameterMethodRS384X509CertStep1(final String registerPath, final String redirectUris,
			final String jwksUri) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setJwksUri(jwksUri);
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodRS384X509CertStep1", response, entity);

		ResponseAsserter responseAsserter = ResponseAsserter.of(response.getStatus(), entity);
		responseAsserter.assertRegisterResponse();
		clientId5 = responseAsserter.getJson().getJson().getString(RegisterResponseParam.CLIENT_ID.toString());
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri", "RS384_keyId", "dnName", "keyStoreFile",
			"keyStoreSecret" })
	@Test(dependsOnMethods = "requestParameterMethodRS384X509CertStep1")
	public void requestParameterMethodRS384X509CertStep2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri, final String keyId, final String dnName,
			final String keyStoreFile, final String keyStoreSecret) throws Exception {
		Builder request = null;
		try {
			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
			List<String> scopes = Arrays.asList("openid");
			String nonce = UUID.randomUUID().toString();
			String state = UUID.randomUUID().toString();

			AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId5, scopes,
					redirectUri, nonce);
			authorizationRequest.setState(state);
			authorizationRequest.getPrompts().add(Prompt.NONE);
			authorizationRequest.setAuthUsername(userId);
			authorizationRequest.setAuthPassword(userSecret);

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.RS384, cryptoProvider);
			jwtAuthorizationRequest.setKeyId(keyId);
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

		showResponse("requestParameterMethodRS384X509CertStep2", response, entity);

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
			e.printStackTrace();
			fail("Response URI is not well formed");
		}
	}

	@Parameters({ "registerPath", "redirectUris", "clientJwksUri" })
	@Test
	public void requestParameterMethodRS512X509CertStep1(final String registerPath, final String redirectUris,
			final String jwksUri) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

		String registerRequestContent = null;
		try {
			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

			RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
					StringUtils.spaceSeparatedToList(redirectUris));
			registerRequest.setJwksUri(jwksUri);
			registerRequest.setResponseTypes(responseTypes);
			registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
			registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

			registerRequestContent = registerRequest.getJSONParameters().toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = request.post(Entity.json(registerRequestContent));
		String entity = response.readEntity(String.class);

		showResponse("requestParameterMethodRS512X509CertStep1", response, entity);

		ResponseAsserter responseAsserter = ResponseAsserter.of(response.getStatus(), entity);
		responseAsserter.assertRegisterResponse();
		clientId6 = responseAsserter.getJson().getJson().getString(RegisterResponseParam.CLIENT_ID.toString());
	}

	@Parameters({ "authorizePath", "userId", "userSecret", "redirectUri", "RS512_keyId", "dnName", "keyStoreFile",
			"keyStoreSecret" })
	@Test(dependsOnMethods = "requestParameterMethodRS512X509CertStep1")
	public void requestParameterMethodRS512X509CertStep2(final String authorizePath, final String userId,
			final String userSecret, final String redirectUri, final String keyId, final String dnName,
			final String keyStoreFile, final String keyStoreSecret) throws Exception {
		Builder request = null;
		try {
			OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

			List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
			List<String> scopes = Arrays.asList("openid");
			String nonce = UUID.randomUUID().toString();
			String state = UUID.randomUUID().toString();

			AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId6, scopes,
					redirectUri, nonce);
			authorizationRequest.setState(state);
			authorizationRequest.getPrompts().add(Prompt.NONE);
			authorizationRequest.setAuthUsername(userId);
			authorizationRequest.setAuthPassword(userSecret);

			JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
					SignatureAlgorithm.RS512, cryptoProvider);
			jwtAuthorizationRequest.setKeyId(keyId);
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

		showResponse("requestParameterMethodRS512X509CertStep2", response, entity);

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
			e.printStackTrace();
			fail("Response URI is not well formed");
		}
	}

}
