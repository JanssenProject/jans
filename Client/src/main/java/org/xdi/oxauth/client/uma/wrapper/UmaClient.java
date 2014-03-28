package org.xdi.oxauth.client.uma.wrapper;

import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.uma.UmaScopeType;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.model.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class UmaClient {

    public static Token requestAat(final String authorizeUrl, final String tokenUrl,
                                   final String umaUserId, final String umaUserSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri) throws Exception {
        return request(authorizeUrl, tokenUrl, umaUserId, umaUserSecret, umaClientId, umaClientSecret, umaRedirectUri, UmaScopeType.AUTHORIZATION);
    }

    public static Token requestPat(final String authorizeUrl, final String tokenUrl,
                                   final String umaUserId, final String umaUserSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri) throws Exception {
        return request(authorizeUrl, tokenUrl, umaUserId, umaUserSecret, umaClientId, umaClientSecret, umaRedirectUri, UmaScopeType.PROTECTION);
    }


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

        ClientUtils.showClient(authorizeClient);

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
            ClientUtils.showClient(authorizeClient);

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

}
