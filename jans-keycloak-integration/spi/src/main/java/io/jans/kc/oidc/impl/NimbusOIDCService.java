package io.jans.kc.oidc.impl;

import java.io.IOException;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.ResponseType.Value;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import io.jans.kc.oidc.OIDCAccessToken;
import io.jans.kc.oidc.OIDCAuthRequest;
import io.jans.kc.oidc.OIDCMetaCache;
import io.jans.kc.oidc.OIDCMetaCacheKeys;
import io.jans.kc.oidc.OIDCMetaError;
import io.jans.kc.oidc.OIDCService;
import io.jans.kc.oidc.OIDCTokenRequest;
import io.jans.kc.oidc.OIDCTokenRequestError;
import io.jans.kc.oidc.OIDCTokenResponse;
import io.jans.kc.oidc.OIDCUserInfoRequestError;
import io.jans.kc.oidc.OIDCUserInfoResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;


public class NimbusOIDCService implements OIDCService {

    
    private OIDCMetaCache metaCache;

    public NimbusOIDCService(OIDCMetaCache metaCache) {

        this.metaCache = metaCache;
    }
    
    @Override
    public URI getAuthorizationEndpoint(String issuerUrl) throws OIDCMetaError {

        URI ret = getAuthorizationEndpointFromCache(issuerUrl);
        if(ret == null) {
            return getAuthorizationEndpointFromServer(issuerUrl);
        }
        return ret;
    }

    @Override
    public URI getTokenEndpoint(String issuerUrl) throws OIDCMetaError {

        URI ret = getTokenEndpointFromCache(issuerUrl);
        if(ret == null) {
            return getTokenEndpointFromServer(issuerUrl);
        }
        return ret;
    }

    @Override
    public URI getUserInfoEndpoint(String issuerUrl) throws OIDCMetaError {

        URI ret = getUserInfoEndpointFromCache(issuerUrl);
        if(ret == null) {
            return getUserInfoEndpointFromServer(issuerUrl);
        }
        return ret;
    }

    @Override
    public URI createAuthorizationUrl(String issuerUrl, OIDCAuthRequest request) throws OIDCMetaError {

        try {
            
           return new AuthenticationRequest.Builder(
                extractResponseType(request.getResponseTypes()),
                extractScope(request.getScopes()),
                new ClientID(request.getClientId()),
                new URI(request.getRedirectUri())
           )
           .endpointURI(getAuthorizationEndpoint(issuerUrl))
           .state(new State(request.getState()))
           .nonce(new Nonce(request.getNonce()))
           .build().toURI();
        }catch(URISyntaxException e) {
            throw new OIDCMetaError("Error building the authentication url",e);
        }
    }

    @Override
    public OIDCTokenResponse requestTokens(String issuerUrl,OIDCTokenRequest tokenrequest) throws OIDCTokenRequestError {

        try {
            AuthorizationCode code = new AuthorizationCode(tokenrequest.getCode());
            AuthorizationCodeGrant grant = new AuthorizationCodeGrant(code,tokenrequest.getRedirectUri());
            ClientID clientId = new ClientID(tokenrequest.getClientId());
            Secret secret = new Secret(tokenrequest.getClientSecret());
            ClientAuthentication auth = new ClientSecretBasic(clientId,secret);
            TokenRequest request = new TokenRequest(getTokenEndpoint(issuerUrl),auth,grant);
            TokenResponse tokenresponse = TokenResponse.parse(request.toHTTPRequest().send());
            return new NimbusOIDCTokenResponse(tokenresponse);
        }catch(ParseException e) {
            throw new OIDCTokenRequestError("Error parsing token response",e);
        }catch(IOException e) {
            throw new OIDCTokenRequestError("I/O error while retrieving token data",e);   
        }catch(OIDCMetaError e) {
            throw new OIDCTokenRequestError("Error retrieving token endpoint from server", e);
        }
    }

    @Override
    public OIDCUserInfoResponse requestUserInfo(String issuerUrl, OIDCAccessToken accesstoken) throws OIDCUserInfoRequestError {

        if(!(accesstoken instanceof NimbusOIDCAccessToken)){
            throw new OIDCUserInfoRequestError("The specified access token is not supported by the Nimbus Backend");
        }

        BearerAccessToken bearertoken  = ((NimbusOIDCAccessToken) accesstoken).asBearerToken();
        try {
            HTTPResponse httpResponse = new UserInfoRequest(getUserInfoEndpoint(issuerUrl),bearertoken).toHTTPRequest().send();
            UserInfoResponse userinforesponse = UserInfoResponse.parse(httpResponse);
            return new NimbusOIDCUserInfoResponse(userinforesponse);
        } catch (IOException e) {
           throw new OIDCUserInfoRequestError("I/O error trying to obtain user info",e);
        }catch(OIDCMetaError e) {
            throw new OIDCUserInfoRequestError("Metadata fetch error trying to obtain user info",e);
        }catch(ParseException e) {
            throw new OIDCUserInfoRequestError("Parse error trying to obtain user info",e);
        }
    }

    private ResponseType extractResponseType(List<String> rtypes) {

        ResponseType rtype = new ResponseType();
        for(String val : rtypes) {
            rtype.add(new Value(val));
        }
        return rtype;
    }

    private Scope extractScope(List<String> scopes) {
        
        Scope scope = new Scope();
        for(String val :  scopes) {
            scope.add(val);
        }
        return scope;
    }

    private URI getAuthorizationEndpointFromCache(String issuerUrl) {

        return (URI) metaCache.get(issuerUrl, OIDCMetaCacheKeys.AUTHORIZATION_URL);
    }

    private URI  getAuthorizationEndpointFromServer(String issuerUrl) throws OIDCMetaError {

        OIDCProviderMetadata meta = obtainMetadataFromServer(issuerUrl);
        cacheMetadataFromServer(issuerUrl,meta);
        return getAuthorizationEndpointFromCache(issuerUrl);
    }

    private URI getTokenEndpointFromCache(String issuerUrl) {

        return (URI) metaCache.get(issuerUrl,OIDCMetaCacheKeys.TOKEN_URL);
    }

    private URI getTokenEndpointFromServer(String issuerUrl) throws OIDCMetaError {

        OIDCProviderMetadata meta = obtainMetadataFromServer(issuerUrl);
        cacheMetadataFromServer(issuerUrl, meta);
        return getTokenEndpointFromCache(issuerUrl);
    }

    private URI getUserInfoEndpointFromServer(String issuerUrl) throws OIDCMetaError {

        OIDCProviderMetadata meta = obtainMetadataFromServer(issuerUrl);
        cacheMetadataFromServer(issuerUrl, meta);
        return getUserInfoEndpointFromCache(issuerUrl);
    }

    private URI getUserInfoEndpointFromCache(String issuerUrl) throws OIDCMetaError {

        return (URI) metaCache.get(issuerUrl,OIDCMetaCacheKeys.USERINFO_URL);
    }

    private OIDCProviderMetadata obtainMetadataFromServer(String issuerUrl) throws OIDCMetaError {

        try {
            Issuer issuer = new Issuer(issuerUrl);
            return OIDCProviderMetadata.resolve(issuer);
        }catch(GeneralException | IOException e) {
            throw new OIDCMetaError("Could not obtain metadata from server",e);
        }
    }

    private void cacheMetadataFromServer(String issuerUrl,OIDCProviderMetadata metadata) {

        metaCache.put(issuerUrl,OIDCMetaCacheKeys.AUTHORIZATION_URL,metadata.getAuthorizationEndpointURI());
        metaCache.put(issuerUrl,OIDCMetaCacheKeys.TOKEN_URL,metadata.getTokenEndpointURI());
        metaCache.put(issuerUrl,OIDCMetaCacheKeys.USERINFO_URL,metadata.getUserInfoEndpointURI());
    }

}
