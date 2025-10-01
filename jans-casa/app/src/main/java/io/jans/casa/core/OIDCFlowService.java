package io.jans.casa.core;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;

import net.minidev.json.JSONObject;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.conf.OIDCSettings;
import org.slf4j.Logger;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;

@Named
@ApplicationScoped
public class OIDCFlowService {
    
    /*
    The list of scopes required to be able to inspect the claims needed. See attributes of User class
     */
    public static final List<String> REQUIRED_SCOPES = Arrays.asList("openid", "profile", "user_name", "clientinfo");

    @Inject
    private Logger logger;

    @Inject
    private PersistenceService persistenceService;

    @Inject
    private MainSettings mainSettings;

    private OIDCSettings settings;

    private String authzEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String endSessionEndpoint;
    private String jwksUri;

    public Pair<String, String> getAuthnRequestUrl(String acr) throws GeneralException {
        return getAuthnRequestUrl(Collections.singletonList(acr), null);
    }
    
    public Pair<String, String> getAuthnRequestUrl(List<String> acrValues, String prompt) throws GeneralException {
        
        try {
            ClientID clientID = new ClientID(settings.getClient().getClientId());
            URI callback = new URI(settings.getRedirectUri());

            AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(
                    new ResponseType("code"),
                    new Scope(REQUIRED_SCOPES.toArray(new String[0])),
                    clientID,
                    callback);

            State state = new State();
            AuthenticationRequest request = builder.endpointURI(new URI(authzEndpoint))
                    .state(state)// .nonce(new Nonce())    // .prompt(new Prompt("?"))
                    .acrValues(acrValues.stream().map(ACR::new).collect(Collectors.toList()))
                    .build();
            
            return new Pair<>(request.toURI().toString(), state.toString());
        } catch (URISyntaxException e) {
            String msg = e.getMessage();
            logger.error(msg);
            throw new GeneralException(new ErrorObject(Labels.getLabel("general.error.authn_request"), msg));
        }
        
    }
    
    public String validateAuthnResponse(String uri, String expectedState) throws GeneralException {

        ErrorObject error = null;
        String code = null;
        try {
            AuthenticationResponse response = AuthenticationResponseParser.parse(new URI(uri));

            if (response instanceof AuthenticationErrorResponse) {
                error = response.toErrorResponse().getErrorObject();

            } else if (!response.getState().toString().equals(expectedState)) {
                error = new ErrorObject(null, Labels.getLabel("general.error.authn_response_unexpected"));

            } else {
                code = response.toSuccessResponse().getAuthorizationCode().getValue();

            }
        } catch (ParseException | URISyntaxException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            // e.getErrorObject() evaluates null for ParseException :(
            error = new ErrorObject(null, msg); 
        }

        if (error == null) {
            return code;
        } else {
            throw new GeneralException(Labels.getLabel("general.error.authn_response_validation"), error);
        }
        
    }

    public Pair<String, String> getTokens(String code) throws GeneralException {
        
        Pair<String, String> tokens = null;
        ErrorObject error = null;
        try {

            AuthorizationCode acode = new AuthorizationCode(code);
            URI callback = new URI(settings.getRedirectUri());
            AuthorizationGrant codeGrant = new AuthorizationCodeGrant(acode, callback);

            ClientID clientID = new ClientID(settings.getClient().getClientId());
            Secret clientSecret = new Secret(settings.getClient().getClientSecret());
            ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

            URI endpoint = new URI(tokenEndpoint);
            TokenRequest request = new TokenRequest(endpoint, clientAuth, codeGrant);

            TokenResponse response = OIDCTokenResponseParser.parse(request.toHTTPRequest().send());
            if (!response.indicatesSuccess()) {
                // We got an error response...
                TokenErrorResponse errorResponse = response.toErrorResponse();
                error = errorResponse.getErrorObject();
            } else {
                OIDCTokenResponse successResponse = (OIDCTokenResponse) response.toSuccessResponse();

                JWT idToken = successResponse.getOIDCTokens().getIDToken();
                AccessToken accessToken = successResponse.getOIDCTokens().getAccessToken();
                
                String validationMsg = validateIDToken(idToken);
                if (validationMsg != null) {
                    error = new ErrorObject(Labels.getLabel("general.error.idtoken_validation"), validationMsg);
                } else {                    
                    tokens = new Pair<>(accessToken.toString(), idToken.getParsedString());
                }
            }
        } catch (ParseException | URISyntaxException | IOException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            // e.getErrorObject() evaluates null for ParseException :(
            error = new ErrorObject(null, msg); 
        }

        if (error == null) {
            return tokens;
        } else {
            throw new GeneralException(error);
        }

    }

    public Map<String, Object> getUserClaims(String accessToken) throws GeneralException {

        Map<String, Object> claims = null;
        ErrorObject error = null;
        try {
            URI endpointUri = new URI(userInfoEndpoint);
            BearerAccessToken token = new BearerAccessToken(accessToken);

            HTTPResponse httpResponse = new UserInfoRequest(endpointUri, token).toHTTPRequest().send();
            UserInfoResponse userInfoResponse = UserInfoResponse.parse(httpResponse);
            
            if (!userInfoResponse.indicatesSuccess()) {
                error = userInfoResponse.toErrorResponse().getErrorObject();                    
            } else {
                
                UserInfoSuccessResponse successResponse = userInfoResponse.toSuccessResponse();
                UserInfo userInfo = Optional.ofNullable(successResponse.getUserInfo())
                    .orElse(new UserInfo(successResponse.getUserInfoJWT().getJWTClaimsSet()));

                JSONObject jsonObj = userInfo.toJSONObject();        
                claims = jsonObj.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            error = new ErrorObject(null, msg); 
        }

        if (error == null) {
            return claims;
        } else {
            throw new GeneralException(error);
        }

    }

    public String getLogoutUrl() {
        
        return UriBuilder.fromUri(endSessionEndpoint)   //.queryParam("id_token_hint", idTokenHint)
                .queryParam("post_logout_redirect_uri", settings.getPostLogoutUri())
                .build().toString();
        
    }
    
    private String validateIDToken(JWT idToken) {
        
        try {
            Issuer iss = new Issuer(persistenceService.getIssuerUrl());
            ClientID clientID = new ClientID(settings.getClient().getClientId());
            JWSAlgorithm jwsAlg = JWSAlgorithm.RS256;

            URL jwkSetURL = new URL(persistenceService.getJwksUri());
            IDTokenValidator validator = new IDTokenValidator(iss, clientID, jwsAlg, jwkSetURL);
            validator.validate(idToken, null);
            
            return null;
        } catch(MalformedURLException | JOSEException | BadJOSEException e) {
            logger.error(e.getMessage(), e);
            return e.getMessage();
        }

    }

    @PostConstruct
    private void init() {
        settings = mainSettings.getOidcSettings();
        
        authzEndpoint = persistenceService.getAuthorizationEndpoint();
        tokenEndpoint = persistenceService.getTokenEndpoint();
        userInfoEndpoint = persistenceService.getUserInfoEndpoint();
        endSessionEndpoint = persistenceService.getEndSessionEndpoint();
        //TODO: reformat URI: in CN internal url should be fetched differently
        jwksUri = persistenceService.getJwksUri(); 
    }

}
