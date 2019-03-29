/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.Assert;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Holder;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version @version June 23, 2015
 */

public class TTokenRequest {

	private final URI baseUri;
	private final Token token = new Token();

	public TTokenRequest(URI baseUri) {
		assertNotNull(baseUri); // must not be null
		this.baseUri = baseUri;
	}

	public Token pat(final String authorizePath, final String tokenPath, final String userId, final String userSecret,
			final String umaClientId, final String umaClientSecret, final String umaRedirectUri) {
		return internalRequest(authorizePath, tokenPath, userId, userSecret, umaClientId, umaClientSecret,
				umaRedirectUri, UmaScopeType.PROTECTION);
	}

	public Token newTokenByRefreshToken(final String tokenPath, final Token p_oldToken, final String umaClientId,
			final String umaClientSecret) {
		if (p_oldToken == null || StringUtils.isBlank(p_oldToken.getRefreshToken()) || StringUtils.isBlank(tokenPath)) {
			throw new IllegalArgumentException("Refresh token or tokenPath is empty.");
		}

		final Holder<Token> t = new Holder<Token>();
		try {
			TokenRequest tokenRequest = new TokenRequest(GrantType.REFRESH_TOKEN);
			tokenRequest.setAuthUsername(umaClientId);
			tokenRequest.setAuthPassword(umaClientSecret);
			tokenRequest.setRefreshToken(p_oldToken.getRefreshToken());
			tokenRequest.setScope(p_oldToken.getScope());

			Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + tokenPath).request();
			request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
			Response response = request
					.post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
			String entity = response.readEntity(String.class);

			BaseTest.showResponse("TTokenClient.requestToken() :", response, entity);

			assertEquals(response.getStatus(), 200, "Unexpected response code.");

			try {
				JSONObject jsonObj = new JSONObject(entity);
				assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
				assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
				assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
				// assertTrue(jsonObj.has("id_token"), "Unexpected result:
				// id_token not found");

				String accessToken = jsonObj.getString("access_token");
				String refreshToken = jsonObj.getString("refresh_token");
				// String idToken = jsonObj.getString("id_token");

				final Token newToken = new Token();
				newToken.setAccessToken(accessToken);
				newToken.setRefreshToken(refreshToken);

				t.setT(newToken);
			} catch (JSONException e) {
				e.printStackTrace();
				fail(e.getMessage() + "\nResponse was: " + entity);
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		return t.getT();
	}

	private Token internalRequest(final String authorizePath, final String tokenPath, final String userId,
			final String userSecret, final String umaClientId, final String umaClientSecret,
			final String umaRedirectUri, final UmaScopeType p_scopeType) {
		try {
			requestAuthorizationCode(authorizePath, userId, userSecret, umaClientId, umaRedirectUri, p_scopeType);
			requestToken(tokenPath, umaClientId, umaClientSecret, umaRedirectUri);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		UmaTestUtil.assert_(token);
		return token;
	}

	private void requestAuthorizationCode(final String authorizePath, final String userId, final String userSecret,
			final String umaClientId, final String umaRedirectUri, final UmaScopeType p_scopeType) throws Exception {
		requestAuthorizationCode(authorizePath, userId, userSecret, umaClientId, umaRedirectUri,
				p_scopeType.getValue());
	}

	private void requestAuthorizationCode(final String authorizePath, final String userId, final String userSecret,
			final String umaClientId, final String umaRedirectUri, final String p_scopeType) throws Exception {
		List<ResponseType> responseTypes = new ArrayList<ResponseType>();
		responseTypes.add(ResponseType.CODE);
		responseTypes.add(ResponseType.ID_TOKEN);

		List<String> scopes = new ArrayList<String>();
		scopes.add(p_scopeType);

		String state = UUID.randomUUID().toString();
		String nonce = UUID.randomUUID().toString();

		AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, umaClientId, scopes,
				umaRedirectUri, nonce);
		authorizationRequest.setState(state);
		authorizationRequest.setAuthUsername(userId);
		authorizationRequest.setAuthPassword(userSecret);
		authorizationRequest.getPrompts().add(Prompt.NONE);

		Builder request = ResteasyClientBuilder.newClient()
				.target(baseUri.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
		request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
		request.header("Accept", MediaType.TEXT_PLAIN);
		Response response = request.get();
		String entity = response.readEntity(String.class);

		BaseTest.showResponse("TTokenClient.requestAuthorizationCode() : ", response, entity);

		assertEquals(response.getStatus(), 302, "Unexpected response code.");
		assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

		if (response.getLocation() != null) {
			try {
				final String location = response.getLocation().toString();
				final int fragmentIndex = location.indexOf("#");

				Map<String, String> params = new HashMap<String, String>();
				if (fragmentIndex != -1) {
					String fragment = location.substring(fragmentIndex + 1);
					params = QueryStringDecoder.decode(fragment);
				} else {
					int queryStringIndex = location.indexOf("?");
					if (queryStringIndex != -1) {
						String queryString = location.substring(queryStringIndex + 1);
						params = QueryStringDecoder.decode(queryString);
					}
				}

				assertNotNull(params.get("code"), "The code is null");
				assertNotNull(params.get("scope"), "The scope is null");
				assertNotNull(params.get("state"), "The state is null");

				token.setAuthorizationCode(params.get("code"));
				token.setScope(params.get("scope"));
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
	}

	private void requestToken(final String tokenPath, final String umaClientId, final String umaClientSecret,
			final String umaRedirectUri) throws Exception {
		if (token == null || StringUtils.isBlank(token.getAuthorizationCode())) {
			throw new IllegalArgumentException("Authorization code is not initialized.");
		}

		TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
		tokenRequest.setCode(token.getAuthorizationCode());
		tokenRequest.setRedirectUri(umaRedirectUri);
		tokenRequest.setAuthUsername(umaClientId);
		tokenRequest.setAuthPassword(umaClientSecret);
		tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
		tokenRequest.setScope(token.getScope());

		Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + tokenPath).request();
		request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
		Response response = request
				.post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
		String entity = response.readEntity(String.class);

		BaseTest.showResponse("TTokenClient.requestToken() :", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code.");

		try {
			JSONObject jsonObj = new JSONObject(entity);
			assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
			assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
			assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
			// assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token
			// not found");

			String accessToken = jsonObj.getString("access_token");
			String refreshToken = jsonObj.getString("refresh_token");
			// String idToken = jsonObj.getString("id_token");

			token.setAccessToken(accessToken);
			token.setRefreshToken(refreshToken);
			// m_token.setIdToken(idToken);
		} catch (JSONException e) {
			e.printStackTrace();
			fail(e.getMessage() + "\nResponse was: " + entity);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	// todo UMA2
	public RPTResponse requestRpt(final String p_rptPath) {
		final Holder<RPTResponse> h = new Holder<RPTResponse>();

		try {
			Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + p_rptPath).request();
			request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
			//request.header("Authorization", "Bearer " + p_aat.getAccessToken());
			Response response = request.post(Entity.form(new Form()));
			String entity = response.readEntity(String.class);

			BaseTest.showResponse("UMA : TTokenRequest.requestRpt() : ", response, entity);

			assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode(), "Unexpected response code.");
			try {
				String tokenResponse = entity;
				final JSONObject jsonObj = new JSONObject(tokenResponse);
				if (jsonObj.has("requesterPermissionTokenResponse")) {
					tokenResponse = jsonObj.get("requesterPermissionTokenResponse").toString();
				}
				System.out.println("Token response = " + tokenResponse);
				RPTResponse result = ServerUtil.createJsonMapper().readValue(tokenResponse, RPTResponse.class);
				UmaTestUtil.assert_(result);

				h.setT(result);
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			} catch (JSONException e) {
				e.printStackTrace();
				fail();
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		return h.getT();
	}

	public RptIntrospectionResponse requestRptStatus(String p_umaRptStatusPath, final String rpt) {
		final Holder<RptIntrospectionResponse> h = new Holder<RptIntrospectionResponse>();

		try {
			Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + p_umaRptStatusPath)
					.request();
			request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
			// todo uma2
//			request.header("Authorization", "Bearer " + p_aat.getAccessToken());
			Response response = request.post(Entity.form(new Form("token", rpt)));
			String entity = response.readEntity(String.class);

			// try {
			// final String json =
			// ServerUtil.createJsonMapper().writeValueAsString(rpt);
			// request.setContent(Util.getBytes(json));
			// request.setContentType(UmaConstants.JSON_MEDIA_TYPE);
			// } catch (IOException e) {
			// e.printStackTrace();
			// fail();
			// }

			BaseTest.showResponse("UMA : TTokenRequest.requestRptStatus() : ", response, entity);

			assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Unexpected response code.");
			try {
				final RptIntrospectionResponse result = ServerUtil.createJsonMapper().readValue(entity,
						RptIntrospectionResponse.class);
				Assert.assertNotNull(result);

				h.setT(result);
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return h.getT();
	}

	// public static void main(String[] args) throws Exception {
	// final String s =
	// "{\"requesterPermissionTokenResponse\":{\"token\":\"75a3fc39-c487-4c46-afd7-c0d65916f4aa\\/CCE1.77DA.CB1A.B6BE.F536.731F.AAF1.4012\"}}";
	// String c = "";
	// final JSONObject jsonObj = new JSONObject(s);
	// if (jsonObj.has("requesterPermissionTokenResponse")) {
	// c = jsonObj.get("requesterPermissionTokenResponse").toString();
	// }
	//
	// System.out.println(c);
	// }
}
