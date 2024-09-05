package io.jans.kc.api.config.client.impl;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;

import io.jans.kc.api.config.client.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class OAuthApiCredentialsProvider implements ApiCredentialsProvider {

    private URI tokenEndpoint;
    private ClientAuthentication clientAuthn;
    private Scope scope;
    private AuthorizationGrant grant;

    private OAuthApiCredentialsProvider(URI tokenEndpoint, ClientAuthentication clientAuthn,Scope scope) {

        this.tokenEndpoint = tokenEndpoint;
        this.clientAuthn = clientAuthn;
        this.scope = scope;
        this.grant = new ClientCredentialsGrant();
    }

    @Override
    public ApiCredentials getApiCredentials() {

        try {
            TokenRequest request = new TokenRequest(tokenEndpoint,clientAuthn,grant,scope); 
            TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());
            if(!response.indicatesSuccess()) {
                TokenErrorResponse error = response.toErrorResponse();
                if(error.getErrorObject() != null) {
                    throw new ApiCredentialsError("Error fetching API credentials. " + error.getErrorObject().toString());
                }else {
                    throw new ApiCredentialsError("Error fetching API credentials.");
                }
            }
            AccessTokenResponse accesstoken = response.toSuccessResponse();
            return new ApiCredentials(accesstoken.getTokens().getAccessToken().toString());
        }catch(ParseException e) {
            throw new ApiCredentialsError("Could not process response containing API credentials from server",e);
        }catch(IOException e) {
            throw new ApiCredentialsError("An I/O error occured while retrieving the API Credentials",e);
        }
    }

    private static ClientAuthentication clientAuthenticationFromAuthnParams(TokenEndpointAuthnParams authnParams) {

        if(authnParams.isBasicAuthn()) {

            ClientID clientId = new ClientID(authnParams.clientId());
            Secret clientSecret = new Secret(authnParams.clientSecret());
            return new ClientSecretBasic(clientId,clientSecret);
        }else if(authnParams.isPostAuthn()) {

            ClientID clientId = new ClientID(authnParams.clientId());
            Secret clientSecret = new Secret(authnParams.clientSecret());
            return new ClientSecretPost(clientId,clientSecret);
        }else if(authnParams.isPrivateKeyJwtAuthn()) {
            throw new CredentialsProviderError("Private key JWT authentication not supported");
        }else {
            throw new CredentialsProviderError("Unsupported authentication method specified");
        }
    }
    
    public static final ApiCredentialsProvider create(String tokenEndpoint, TokenEndpointAuthnParams authnParams) {

        try {
            URI endpoint = new URI(tokenEndpoint);
            return new OAuthApiCredentialsProvider(endpoint,clientAuthenticationFromAuthnParams(authnParams),Scope.parse(authnParams.scopes()));
        }catch(URISyntaxException e) {
            throw new CredentialsProviderError("Malformed token endpoint specified",e);
        }
    }
}
