package org.xdi.oxauth.client.uma.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.uma.UmaScopeType;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.model.util.Util;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class UmaClient {

	@Deprecated
    public static Token requestAat(final String authorizeUrl, final String tokenUrl,
                                   final String umaUserId, final String umaUserSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri) throws Exception {
        return request(authorizeUrl, tokenUrl, umaUserId, umaUserSecret, umaClientId, umaClientSecret, umaRedirectUri, UmaScopeType.AUTHORIZATION);
    }

    public static Token requestAat(final String tokenUrl, final String umaClientId, final String umaClientSecret) throws Exception {
    	return request(tokenUrl, umaClientId, umaClientSecret, UmaScopeType.AUTHORIZATION);
    }

	@Deprecated
    public static Token requestPat(final String authorizeUrl, final String tokenUrl,
                                   final String umaUserId, final String umaUserSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri) throws Exception {
        return request(authorizeUrl, tokenUrl, umaUserId, umaUserSecret, umaClientId, umaClientSecret, umaRedirectUri, UmaScopeType.PROTECTION);
    }

    public static Token requestPat(final String tokenUrl, final String umaClientId, final String umaClientSecret) throws Exception {
    	return request(tokenUrl, umaClientId, umaClientSecret, UmaScopeType.PROTECTION);
    }

	@Deprecated
    public static Token request(final String authorizeUrl, final String tokenUrl,
                                final String umaUserId, final String umaUserSecret,
                                final String umaClientId, final String umaClientSecret,
                                final String umaRedirectUri, UmaScopeType p_type) throws Exception {
        // 1. Request authorization and receive the authorization code.
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);
        List<String> scopes = new ArrayList<String>();
        scopes.add(p_type.getValue());

        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, umaClientId, scopes, umaRedirectUri, null);
        request.setState(state);
        request.setAuthUsername(umaUserId);
        request.setAuthPassword(umaUserSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizeUrl);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        String scope = response1.getScope();
        String authorizationCode = response1.getCode();

        if (Util.allNotBlank(authorizationCode)) {

            // 2. Request access token using the authorization code.
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(umaRedirectUri);
            tokenRequest.setAuthUsername(umaClientId);
            tokenRequest.setAuthPassword(umaClientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            tokenRequest.setScope(scope);

            TokenClient tokenClient1 = new TokenClient(tokenUrl);
            tokenClient1.setRequest(tokenRequest);
            TokenResponse response2 = tokenClient1.exec();

            if (response2.getStatus() == 200) {
                final String patToken = response2.getAccessToken();
                final String patRefreshToken = response2.getRefreshToken();
                final Integer expiresIn = response2.getExpiresIn();
                if (Util.allNotBlank(patToken, patRefreshToken)) {
                    return new Token(authorizationCode, patRefreshToken, patToken, scope, expiresIn);
                }
            }
        }

        return null;
    }

    public static Token request(final String tokenUrl, final String umaClientId, final String umaClientSecret, UmaScopeType scopeType) throws Exception {
    	String umaScope = scopeType.getValue();

    	TokenClient tokenClient = new TokenClient(tokenUrl);
        TokenResponse response = tokenClient.execClientCredentialsGrant(umaScope, umaClientId, umaClientSecret);

        if (response.getStatus() == 200) {
            final String patToken = response.getAccessToken();
            final Integer expiresIn = response.getExpiresIn();
            if (Util.allNotBlank(patToken)) {
                return new Token(null, null, patToken, umaScope, expiresIn);
            }
        }
        
        return null;
    }

}
