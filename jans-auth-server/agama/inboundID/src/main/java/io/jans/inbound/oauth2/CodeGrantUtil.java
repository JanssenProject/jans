package io.jans.inbound.oauth2;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.*;

import io.jans.util.Pair;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGrantUtil {
    
    private OAuthParams p;
    
    public CodeGrantUtil() { }
    
    public CodeGrantUtil(OAuthParams p) {
        this.p = p;
    }

    public Pair<String, String> makeAuthzRequest() throws URISyntaxException {
        
        URI authzEndpoint = new URI(p.getAuthzEndpoint());
        ClientID clientID = new ClientID(p.getClientId());
        
        URI callback = new URI(p.getRedirectUri());
        State state = new State();
        Scope scope = new Scope(p.getScopes().toArray(new String[0]));

        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
            new ResponseType(ResponseType.Value.CODE), clientID)
            .scope(scope)
            .state(state)
            .redirectionURI(callback)
            .endpointURI(authzEndpoint);
        
        if (p.getCustParamsAuthReq() != null) {
            p.getCustParamsAuthReq().forEach(builder::customParameter);
        }
        return new Pair<>(builder.build().toURI().toString(), state.getValue());
        
    }

    public String parseCode(Map<String, Object> urlParams, String state)
            throws URISyntaxException, GeneralException {
        
        URI callback = new URI(p.getRedirectUri());
        AuthorizationResponse response = AuthorizationResponse.parse(callback, toMultivaluedMap(urlParams));
        
        if (!state.equals(response.getState().getValue())) {
            throw new GeneralException("Unexpected or tampered response");
        }
        if (!response.indicatesSuccess()) {
            // The request was denied or some error occurred
            AuthorizationErrorResponse errorResponse = response.toErrorResponse();
            throw exFromError(errorResponse.getErrorObject());
        }
        
        return response.toSuccessResponse().getAuthorizationCode().getValue();

    }
    
    public Map<String, Object> getTokenResponse(String authzCode)
            throws URISyntaxException, IOException, GeneralException {

        AuthorizationCode code = new AuthorizationCode(authzCode);
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, new URI(p.getRedirectUri()));

        ClientID clientID = new ClientID(p.getClientId());
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, new Secret(p.getClientSecret()));

        URI tokenEndpoint = new URI(p.getTokenEndpoint());

        Map<String, List<String>> params = new HashMap<>();
        if (p.getCustParamsTokenReq() != null) {
            p.getCustParamsTokenReq().forEach((k, v) -> params.put(k, Collections.singletonList(v)));
        }
        
        TokenRequest request;
        if (p.isClientCredsInRequestBody()) {
            params.put("client_id", Collections.singletonList(p.getClientId()));
            params.put("client_secret", Collections.singletonList(p.getClientSecret()));

            request = new TokenRequest(tokenEndpoint, clientID, codeGrant, null, null, null, params);
        } else {
            request = new TokenRequest(tokenEndpoint, clientAuth, codeGrant, null, null, params);
        }

        HTTPRequest httpRequest = request.toHTTPRequest();
        httpRequest.setAccept(MediaType.APPLICATION_JSON);
        
        TokenResponse response = TokenResponse.parse(httpRequest.send());
        if (!response.indicatesSuccess()) {
            throw exFromError(response.toErrorResponse().getErrorObject());
        }
        return response.toSuccessResponse().toJSONObject();

    }

    private MultivaluedMap<String, String> toMultivaluedMap(Map<String, Object> map) {
    
        MultivaluedMap<String,String> m = new MultivaluedHashMap<>();

        for (String key : map.keySet()) {
            Object val = map.get(key);
            
            if (val != null) {
                if (String.class.isInstance(val)) {
                    m.putSingle(key, val.toString());

                } else if (Collection.class.isAssignableFrom(val.getClass())) {
                    Collection<?> col = (Collection<?>) val;
                    col.forEach(v -> m.add(key, v.toString()));
                }
            }
        }
        return m;

    }

    private static GeneralException exFromError(ErrorObject o) {
        
        Map<String, String> map = new HashMap<>();

        String s = "" + o.getHTTPStatusCode();
        map.put("HTTP status", s);
        
        s = o.getCode();
        if (s != null) {
            map.put("error code", s);
        }

        s = o.getDescription();
        if (s != null) {
            map.put("description", s);
        }

        s = map.toString(); 
        return new GeneralException(s.substring(1, s.length() - 1));
        
    }
    
}
