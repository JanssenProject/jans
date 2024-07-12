package io.jans.lock.service.util;

import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ScopeType;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.net.HttpService;
import io.jans.model.net.HttpServiceResponse;
import io.jans.service.EncryptionService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.entity.ContentType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class AuthUtil {

    private static final String CONTENT_TYPE = "Content-Type";

    @Inject
    Logger log;

    @Inject
    AppConfiguration appConfiguration;

    @Inject
    HttpService httpService;

    @Inject
    EncryptionService encryptionService;

    public String getToken(String endpoint) {

        log.error("\n\n Request for token  for endpoint:{}", endpoint);
        String tokenUrl = this.appConfiguration.getTokenUrl();
        String clientId = this.appConfiguration.getClientId();

        String clientSecret = this.getDecryptedPassword(appConfiguration.getClientPassword());
        String scopes = this.getScopes(endpoint);

        return this.getToken(tokenUrl, clientId, clientSecret, scopes);
    }

    public TokenResponse requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String scope) {
        log.error("Request for Access Token -  tokenUrl:{}, clientId:{}, clientSecret:{}, scope:{} ", tokenUrl,
                clientId, clientSecret, scope);
        Response response = null;
        try {
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(scope);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            Builder request = getClientBuilder(tokenUrl);
            request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>(
                    tokenRequest.getParameters());
            response = request.post(Entity.form(multivaluedHashMap));
            log.trace("Response for Access Token -  response:{}", response);
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                TokenResponse tokenResponse = new TokenResponse();
                tokenResponse.setEntity(entity);
                tokenResponse.injectDataFromJson(entity);
                return tokenResponse;
            }
        } finally {

            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    public String getToken(String tokenUrl, String clientId, String clientSecret, String scopes) {
        log.error("\n\n Request for token tokenUrl:{}, clientId:{}, clientSecret:{}, scopes:{}", tokenUrl, clientId,
                clientSecret, scopes);

        String accessToken = null;
        Integer expiresIn = 0;
        TokenResponse tokenResponse = this.requestAccessToken(tokenUrl, clientId, clientSecret, scopes);
        if (tokenResponse != null) {

            log.error("Token Response - tokenScope: {}, tokenAccessToken: {} ", tokenResponse.getScope(),
                    tokenResponse.getAccessToken());
            accessToken = tokenResponse.getAccessToken();
            expiresIn = tokenResponse.getExpiresIn();

        }
        log.error(" accessToken:{}, expiresIn:{}", accessToken, expiresIn);

        return accessToken;
    }

    public HttpServiceResponse postData(String endpoint, String postData) {
        log.error("postData - endpoint:{}, postData:{}", endpoint, postData);
        String token = this.getToken(this.getEndpointUrl(endpoint));
        return postData(this.getAuditEndpoint(endpoint), null, token, null, null, postData);
    }

    public HttpServiceResponse postData(String uri, String authType, String token, Map<String, String> headers,
            ContentType contentType, String postData) {
        log.error("postData - uri:{}, token:{}, data", uri, token);

        if (StringUtils.isBlank(authType)) {
            authType = "Bearer ";
        }
        if (contentType == null) {
            contentType = ContentType.APPLICATION_JSON;
        }

        HttpServiceResponse response = httpService.executePost(uri, token, headers, postData, contentType, authType);

        log.error("response:{}", response);
        return response;
    }

    public String getResponseEntityString(HttpServiceResponse serviceResponse) {
        String jsonString = null;

        if (serviceResponse == null) {
            return jsonString;
        }

        if (serviceResponse.getHttpResponse() != null && serviceResponse.getHttpResponse().getStatusLine() != null
                && serviceResponse.getHttpResponse().getStatusLine().getStatusCode() == Status.OK.getStatusCode()) {
            HttpEntity entity = serviceResponse.getHttpResponse().getEntity();
            if (entity == null) {
                return jsonString;
            }
            jsonString = entity.toString();

        }
        return jsonString;
    }

    public JSONObject getJSONObject(HttpServletRequest request) {
        log.error("getJSONObject() - request:{}", request);
        JSONObject jsonBody = null;
        if (request == null) {
            return jsonBody;
        }
        try {
            String jsonBodyStr = IOUtils.toString(request.getInputStream());
            log.error(" jsonBodyStr:{}", jsonBodyStr);
            jsonBody = new JSONObject(jsonBodyStr);
            log.error(" jsonBody:{}", jsonBody);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Exception while retriving json from request is - ", ex);
        }
        return jsonBody;
    }

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

    public String getDecryptedPassword(String clientPassword) {
        String decryptedPassword = null;
        if (clientPassword != null) {
            try {
                decryptedPassword = encryptionService.decrypt(clientPassword);
            } catch (EncryptionException ex) {
                log.error("Failed to decrypt password", ex);
            }
        }
        return decryptedPassword;
    }

    public String getScopes(String endpoint) {
        log.error("Get scope for endpoint:{}", endpoint);
        String scopes = null;
        List<String> scopeList = null;
        Map<String, List<String>> endpointMap = this.appConfiguration.getEndpointDetails();
        log.error("Get scope for endpointMap:{}", endpointMap);

        if (endpointMap == null || endpointMap.isEmpty()) {
            return scopes;
        }

        scopeList = endpointMap.get(endpoint);

        if (scopeList == null || scopeList.isEmpty()) {
            return scopes;
        }

        Set<String> scopesSet = new HashSet<>(scopeList);

        StringBuilder scope = new StringBuilder(ScopeType.OPENID.getValue());
        for (String s : scopesSet) {
            scope.append(" ").append(s);
        }

        return scopes;
    }

    private String getEndpointUrl(String endpoint) {
        log.error("Get endpoint URL for endpoint:{}", endpoint);
        Map<String, List<String>> endpointMap = this.appConfiguration.getEndpointDetails();
        log.error("Get endpoint URL for endpointMap:{}", endpointMap);

        if (endpointMap == null || endpointMap.isEmpty()) {
            return endpoint;
        }

        Set<String> keys = endpointMap.keySet();
        log.error("endpointMap keys:{}", keys);

        String key = keys.stream().filter(e -> e.endsWith("/" + endpoint)).toString();
        log.error("endpointMap key:{} for endpoint:{}", key, endpoint);
        return key;
    }

    private String getAuditEndpoint(String endpoint) {
        if (StringUtils.isBlank(endpoint)) {
            return endpoint;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(appConfiguration.getIssuerUrl());
        sb.append("/");
        sb.append(endpoint);
        return sb.toString();
    }

}
