/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.client.*;
import io.jans.as.client.uma.exception.UmaException;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.util.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class UmaClient {

	public static Token requestPat(final String tokenUrl, final String clientKeyStoreFile,
			final String clientKeyStorePassword, final String clientId, final String keyId) throws UmaException {
		TokenRequest tokenRequest = TokenRequest.builder().pat().grantType(GrantType.CLIENT_CREDENTIALS).build();

		return request(tokenUrl, clientKeyStoreFile, clientKeyStorePassword, clientId, keyId, tokenRequest);
	}

	public static Token requestPat(final String tokenUrl, final String umaClientId, final String umaClientSecret,
			String... scopeArray) throws Exception {
		return request(tokenUrl, umaClientId, umaClientSecret, UmaScopeType.PROTECTION, scopeArray);
	}

	public static Token request(final String tokenUrl, final String umaClientId, final String umaClientSecret,
			UmaScopeType scopeType, String... scopeArray) throws Exception {

		String scope = scopeType.getValue();
		if (scopeArray != null && scopeArray.length > 0) {
			for (String s : scopeArray) {
				scope = scope + " " + s;
			}
		}

		TokenClient tokenClient = new TokenClient(tokenUrl);

		TokenResponse response = tokenClient.execClientCredentialsGrant(scope, umaClientId, umaClientSecret);

		if (response.getStatus() == 200) {
			final String patToken = response.getAccessToken();
			final Integer expiresIn = response.getExpiresIn();
			if (Util.allNotBlank(patToken)) {
				return new Token(null, null, patToken, scopeType.getValue(), expiresIn);
			}
		}
		return null;
	}

	private static Token request(final String tokenUrl, final String clientKeyStoreFile,
			final String clientKeyStorePassword, final String clientId, final String keyId, TokenRequest tokenRequest)
			throws UmaException {
		AuthCryptoProvider cryptoProvider;
		try {
			cryptoProvider = new AuthCryptoProvider(clientKeyStoreFile, clientKeyStorePassword, null);
		} catch (Exception ex) {
			throw new UmaException("Failed to initialize crypto provider");
		}

		try {
			String tmpKeyId = keyId;
			if (StringHelper.isEmpty(tmpKeyId)) {
				// Get first key
				List<String> aliases = cryptoProvider.getKeys();
				if (aliases.size() > 0) {
					tmpKeyId = aliases.get(0);
				}
			}

			if (StringHelper.isEmpty(tmpKeyId)) {
				throw new UmaException("UMA keyId is empty");
			}

			SignatureAlgorithm algorithm = cryptoProvider.getSignatureAlgorithm(tmpKeyId);

			tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
			tokenRequest.setAuthUsername(clientId);
			tokenRequest.setCryptoProvider(cryptoProvider);
			tokenRequest.setAlgorithm(algorithm);
			tokenRequest.setKeyId(tmpKeyId);
			tokenRequest.setAudience(tokenUrl);

			Token umaPat = UmaClient.request(tokenUrl, tokenRequest);

			return umaPat;
		} catch (Exception ex) {
			throw new UmaException("Failed to obtain valid UMA PAT token", ex);
		}
	}

	public static Token request(final String tokenUrl, final TokenRequest tokenRequest) throws Exception {
		if (tokenRequest.getGrantType() != GrantType.CLIENT_CREDENTIALS) {
			return null;
		}

		TokenClient tokenClient = new TokenClient(tokenUrl);

		tokenClient.setRequest(tokenRequest);

		TokenResponse response = tokenClient.exec();

		if (response.getStatus() == 200) {
			final String patToken = response.getAccessToken();
			final Integer expiresIn = response.getExpiresIn();
			if (Util.allNotBlank(patToken)) {
				return new Token(null, null, patToken, response.getScope(), expiresIn);
			}
		}

		return null;
	}

}
