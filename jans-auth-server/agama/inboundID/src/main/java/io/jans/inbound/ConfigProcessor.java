package io.jans.inbound;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.client.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.oauth2.sdk.token.*;
import com.nimbusds.openid.connect.sdk.rp.*;
import com.nimbusds.openid.connect.sdk.op.*;

import io.jans.inbound.oauth2.*;
import io.jans.service.CacheService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.*;

import net.minidev.json.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigProcessor {
    
    private static String KEY_PREFIX = "agama-openid-";
    private static ConfigProcessor instance;
    
    private static Logger logger = LoggerFactory.getLogger(ConfigProcessor.class);
    
    public static ConfigProcessor getInstance() {
        
        if (instance == null) {
            instance = new ConfigProcessor();
        }
        return instance;

    }
    
    public OAuthParams exec(Provider p) throws Exception {
        
        logger.info("Processing configurations of provider {}", p.getDisplayName());
        OpenIdParams oip = p.getOpenIdParams();

        String opHost = Optional.ofNullable(oip).map(OpenIdParams::getHost).orElse(null);
        OAuthParams oap = p.getOAuthParams();
        
        if (oap == null) {
            logger.warn("OAuth properties were missing for this provider!");
            oap = new OAuthParams();
        }
        
        if (opHost == null) return oap;

        logger.info("Issuing a configuration request to OP {}", opHost);        
        OIDCProviderMetadata opMetadata = OIDCProviderMetadata.resolve(new Issuer(opHost), 3000, 3000);
        fillMissingEndpoints(oap, opMetadata);

        Pair<String, String> clientCreds = new Pair<>(oap.getClientId(), oap.getClientSecret());
        boolean credsMissing = clientCreds.getFirst() == null || clientCreds.getSecond() == null;
        
        if (credsMissing && oip.isUseDCR()) {

            String key = KEY_PREFIX + opHost;            
            if (oip.isUseCachedClient()) {
                clientCreds = retrieveCredsFromCache(key, opHost);
            }
            
            if (clientCreds == null) {
                clientCreds = registerClient(opMetadata.getRegistrationEndpointURI(),
                        oap.getRedirectUri(), oap.getScopes(), key);
            } else {
                logger.info("Using the client credentials already present in cache or configuration");
            }

            oap.setClientId(clientCreds.getFirst());
            oap.setClientSecret(clientCreds.getSecond());        
        }
        return oap;
        
    }
    
    private void fillMissingEndpoints(OAuthParams oap, OIDCProviderMetadata opMetadata) {

        String s = oap.getAuthzEndpoint();
        if (s == null) {
            logger.info("Grabbing authorization endpoint from OP configuration document");
            oap.setAuthzEndpoint(opMetadata.getAuthorizationEndpointURI().toString());
        }
        
        s = oap.getTokenEndpoint();
        if (s == null) {
            logger.info("Grabbing token endpoint from OP configuration document");
            oap.setTokenEndpoint(opMetadata.getTokenEndpointURI().toString());
        }
        
        s = oap.getUserInfoEndpoint();
        if (s == null) {
            logger.info("Grabbing userInfo endpoint from OP configuration document");
            oap.setUserInfoEndpoint(opMetadata.getUserInfoEndpointURI().toString());
        }
        
        s = oap.getRedirectUri();
        if (s == null) {
            logger.info("Using Agama's default redirect uri");
            oap.setRedirectUri(NetworkUtils.makeRedirectUri());
        }
        
    }
    
    private Pair<String, String> retrieveCredsFromCache(String key, String opHost) {

        CacheService cache = CdiUtil.bean(CacheService.class);
        
        try {
            //Check if key is in cache
            logger.info("Parsing client creds from cache...");
            return (Pair<String, String>) Optional.ofNullable(cache.get(key)).orElse(null);
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.info("Removing entry from cache");

            cache.remove(key);
            return null;
        }

    }
    
    private void storeCredentials(String key, Pair<String, String> creds, Long expSeconds) {

        CacheService cache = CdiUtil.bean(CacheService.class);
        try {
            logger.info("Writing SimpleOAuthParams instance to cache...");
            cache.put(expSeconds.intValue() - 1, key, creds);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
    
    private Pair<String, String> registerClient(URI registrationURI, String redirectUri,
            List<String> scopes, String key) throws Exception {
        
        if (registrationURI == null) {
            String msg = "Unable to determine client registration endpoint URI.";
            logger.error(msg + " OP does not support dynamic client registration?");
            throw new IOException(msg);
        }
        
        logger.info("Sending a client registration request to {}", registrationURI);
        OIDCClientMetadata clMetadata = makeClientMetadata(redirectUri, scopes);
        OIDCClientRegistrationRequest regRequest = new OIDCClientRegistrationRequest(
                registrationURI, clMetadata, null);
        
        ClientRegistrationResponse response = OIDCClientRegistrationResponseParser.parse(
                getRegistrationResponse(regRequest));

        if (!response.indicatesSuccess()) {
            throw CodeGrantUtil.exFromError(
                        ClientRegistrationErrorResponse.class.cast(response).getErrorObject());
        }

        OIDCClientInformation clientInfo = OIDCClientInformationResponse.class.cast(response)
                .getOIDCClientInformation();
        checkScopes(clientInfo.getOIDCMetadata(), scopes);

        String clId = clientInfo.getID().getValue();
        Secret secret = clientInfo.getSecret();
        Date expd = secret.getExpirationDate();
        boolean notExpiring = expd == null; 
        
        logger.debug("Client ID is {}. Expiring {}", clId, notExpiring ? "NEVER" : expd);        
        long expSeconds;
        
        if (notExpiring) {
            expSeconds = Integer.MAX_VALUE;
        } else {
            expSeconds = (expd.getTime() - System.currentTimeMillis()) / 1000L;
        }
        
        Pair<String, String> creds = new Pair<>(clId, secret.getValue());
        storeCredentials(key, creds, expSeconds);
        
        return creds;
        
    }
    
    private OIDCClientMetadata makeClientMetadata(String redirectUri,
                List<String> scopes) {
    
        logger.debug("Building client metadata");        
        OIDCClientMetadata clientMetadata = new OIDCClientMetadata();
        
        //See https://javadoc.io/static/com.nimbusds/oauth2-oidc-sdk/11.7/com/nimbusds/openid/connect/sdk/rp/OIDCClientMetadata.html#applyDefaults()
        clientMetadata.applyDefaults();
        clientMetadata.setResponseTypes(Collections.singleton(ResponseType.CODE));
        clientMetadata.setScope(new Scope(scopes.toArray(new String[0])));
        clientMetadata.setRedirectionURI(URI.create(redirectUri));
        clientMetadata.setName(KEY_PREFIX + System.currentTimeMillis());
        
        return clientMetadata;
        
    }
    
    private HTTPResponse getRegistrationResponse(OIDCClientRegistrationRequest request) throws Exception {

        HTTPResponse response = request.toHTTPRequest().send();
        //"fix" apparently Jans non-compliance, see jans#7581
        String property = "backchannel_logout_uri";
        JSONObject json = response.getBodyAsJSONObject();
        Object blu = json.get(property);
        boolean nullify = blu != null;
        
        if (!nullify || String.class.isInstance(blu)) return response;
        
        if (JSONArray.class.isInstance(blu)) {
            JSONArray list = (JSONArray) blu;
            
            if (!list.isEmpty()) {
                Object first = list.get(0);
            
                if (String.class.isInstance(first)) {
                    nullify = false;
                    logger.debug("Setting {} to {}", property, first.toString());
                    json.put(property, first.toString());
                }
            }
        }
        if (nullify) {
            logger.debug("Nullifying {}", property);
            json.put(property, null);
        }
        //Update body of response        
        response.setBody(json.toString());
        
        return response;
        
    }

    private void checkScopes(ClientMetadata clientMetadata, List<String> originalScopes) {
        
        Set<String> scopes = clientMetadata.getScope().toStringList().stream().collect(Collectors.toSet());
        Set<String> originalScopesSet = originalScopes.stream().collect(Collectors.toSet());
        if (!scopes.equals(originalScopesSet)) {
            logger.warn("Scopes differ!. Original: {}; scopes now: {}", originalScopesSet, scopes);
        }
        
    }
    
    private ConfigProcessor() {
    }
    
}
