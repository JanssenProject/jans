package io.jans.lock.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.model.net.HttpServiceResponse;
import io.jans.service.EncryptionService;
import io.jans.service.net.BaseHttpService;
import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@ApplicationScoped
public class TokenEndpointService {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";

    @Inject
    Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private BaseHttpService httpService;

    @Inject
    private EncryptionService encryptionService;

    public Token getAccessToken(String endpoint, boolean allGroupScopes) {
        log.info("Request for token  for endpoint:{}, allGroupScopes:{}", endpoint, allGroupScopes);

        String tokenUrl = this.appConfiguration.getTokenUrl();
        String clientId = this.appConfiguration.getClientId();

        String clientSecret = this.getDecryptedPassword(appConfiguration.getClientPassword());
        String scopes = this.getScopeForToken(endpoint, allGroupScopes);

        log.info("Scope  for endpoint:{}, allGroupScopes:{}, scopes:{}", endpoint, allGroupScopes, scopes);
        return this.getToken(tokenUrl, clientId, clientSecret, scopes);
    }

    public Token getToken(String tokenUrl, String clientId, String clientSecret, String scopes) {
        log.info("Request for token tokenUrl:{}, clientId:{},scopes:{}", tokenUrl, clientId, scopes);
        Token token = null;
        TokenResponse tokenResponse = this.requestAccessToken(tokenUrl, clientId, clientSecret, scopes);
        if (tokenResponse != null) {
            final String accessToken = tokenResponse.getAccessToken();
            final Integer expiresIn = tokenResponse.getExpiresIn();
            log.trace("accessToken:{}, expiresIn:{}", accessToken, expiresIn);
            if (Util.allNotBlank(accessToken)) {
                return new Token(null, null, accessToken, ScopeType.OPENID.getValue(), expiresIn);
            }
        }

        return token;
    }

    public TokenResponse requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String scope) {
        log.info("Request for access token tokenUrl:{}, clientId:{},scope:{}", tokenUrl, clientId, scope);
        Response response = null;
        try {
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(scope);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            Builder request = getClientBuilder(tokenUrl);
            request.header(AUTHORIZATION, "Basic " + tokenRequest.getEncodedCredentials());
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

    public HttpServiceResponse postData(String uri, String authType, String token, Map<String, String> headers,
            ContentType contentType, String postData) {
        log.debug("postData - uri:{}, token:{}, data", uri, token);

        if (StringUtils.isBlank(authType)) {
            authType = "Bearer ";
        }
        if (contentType == null) {
            contentType = ContentType.APPLICATION_JSON;
        }

        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        headers.put(AUTHORIZATION, authType + token);

        HttpServiceResponse response = httpService.executePost(uri, token, headers, postData, contentType, authType);

        log.debug("response:{}", response);
        return response;
    }

    public String getResponseEntityString(HttpServiceResponse serviceResponse, Status status) {
        String jsonString = null;

        if (serviceResponse == null) {
            return jsonString;
        }

        if (serviceResponse.getHttpResponse() != null && serviceResponse.getHttpResponse().getStatusLine() != null
                && serviceResponse.getHttpResponse().getStatusLine().getStatusCode() == status.getStatusCode()) {
            HttpEntity entity = serviceResponse.getHttpResponse().getEntity();
            if (entity == null) {
                return jsonString;
            }
            jsonString = entity.toString();

        }
        return jsonString;
    }

    public String getResponseEntityString(HttpServiceResponse serviceResponse) {
        String jsonString = null;

        if (serviceResponse == null || serviceResponse.getHttpResponse() == null) {
            return jsonString;
        }

        HttpEntity entity = serviceResponse.getHttpResponse().getEntity();
        if (entity == null) {
            return jsonString;
        }
        jsonString = entity.toString();

        try {
            log.debug("serviceResponse.getHttpResponse().getEntity():{}",
                    serviceResponse.getHttpResponse().getEntity());
            String responseMsg = EntityUtils.toString(serviceResponse.getHttpResponse().getEntity(), "UTF-8");
            log.debug("New responseMsg:{}", responseMsg);
        } catch (Exception ex) {
            log.error("Error while getting entity using EntityUtils is ", ex);
        }
        return jsonString;
    }

    public Status getResponseStatus(HttpServiceResponse serviceResponse) {
        Status status = Status.INTERNAL_SERVER_ERROR;

        if (serviceResponse == null || serviceResponse.getHttpResponse() == null) {
            return status;
        }

        int statusCode = serviceResponse.getHttpResponse().getStatusLine().getStatusCode();

        status = Status.fromStatusCode(statusCode);
        if (status == null) {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        return status;
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

    public List<String> getScopes(String endpoint) {
        log.info("Get scope for endpoint:{} ", endpoint);
        List<String> scopeList = null;
        Map<String, List<String>> endpointMap = this.appConfiguration.getEndpointDetails();
        log.debug("Get scope for endpoint:{} from endpointMap:{}", endpoint, endpointMap);

        if (endpointMap == null || endpointMap.isEmpty()) {
            return scopeList;
        }

        for (Map.Entry<String, List<String>> entry : endpointMap.entrySet()) {
            log.info(" entry.getKey():{}, entry.getValue():{}", entry.getKey(), entry.getValue());
            if (entry.getKey() != null && entry.getKey().toLowerCase().endsWith(endpoint)) {
                scopeList = entry.getValue();
                break;
            }
        }

        log.info("Scope for endpoint:{} scopeList:{} ", endpoint, scopeList);

        return scopeList;
    }

    private String getEndpointPath(String endpoint) {
        Map<String, List<String>> endpointMap = this.appConfiguration.getEndpointDetails();
        log.debug("Get endpoint URL for endpoint:{} from endpointMap:{}", endpoint, endpointMap);

        if (StringUtils.isBlank(endpoint) || endpointMap == null || endpointMap.isEmpty()) {
            return endpoint;
        }

        Set<String> keys = endpointMap.keySet();
        String endpointPath = keys.stream()
                .filter(e -> e != null && e.toLowerCase().endsWith("/" + endpoint.toLowerCase())).findFirst()
                .orElse(null);
        log.debug("Final endpoint:{}, keys:{}, endpointPath:{}", endpoint, keys, endpointPath);
        return endpointPath;
    }

    private String getEndpointUrl(String endpoint) {
        if (StringUtils.isBlank(endpoint)) {
            return endpoint;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(appConfiguration.getOpenIdIssuer());
        sb.append("/");
        sb.append(endpoint);

        log.debug("endpoint:{} url is  sb:{}", endpoint, sb);
        return sb.toString();
    }

    private String getScopeForToken(String endpoint, boolean allGroupScopes) {
        log.info("Request for token  for endpoint:{}, allGroupScopes:{}", endpoint, allGroupScopes);
        StringBuilder sb = new StringBuilder();
        sb.append(ScopeType.OPENID.getValue());
        List<String> scopeList = null;

        if (allGroupScopes) {
            scopeList = this.getAllGroupScope(endpoint);
        } else {
            scopeList = this.getScopes(endpoint);
        }
        log.debug("Scope  for endpoint:{}, allGroupScopes:{}, scopeList:{}", endpoint, allGroupScopes,
                scopeList);
        
        if(scopeList==null || scopeList.isEmpty()) {
            return sb.toString();
        }
        
        HashSet<String> scopeSet = new HashSet(scopeList);
        for (String scope : scopeSet) {
            sb.append(" ").append(scope);
        }

        return sb.toString();
    }

    private List<String> getAllGroupScope(String endpoint) {
        log.info(" Get group scopes for String endpoint:{}", endpoint);
        List<String> scopeList = null;
        String groupName = this.getEndpointGroup(endpoint);
        Map<String, List<String>> endpointGroups = this.appConfiguration.getEndpointGroups();
        log.debug(" groupName for endpoint:{} is {}", endpoint, groupName);
        scopeList = this.getScopes(endpoint);

        if (StringUtils.isBlank(groupName)) {
            // since group is null get scope for endpoint itself
            return scopeList;
        }

        List<String> endpoints = endpointGroups.get(groupName);
        log.debug("groupName:{}, endpoints:{}", groupName, endpoints);

        if (endpoints == null || endpoints.isEmpty()) {
            return scopeList;
        }

        for (String url : endpoints) {
            scopeList.addAll(this.getScopes(url));
        }

        log.info("Scope for groupName:{}, scopeList:{}", groupName, scopeList);

        return scopeList;

    }

    private String getEndpointGroup(String endpoint) {
        log.info("Get groupName for  endpoint:{}", endpoint);
        String groupName = null;
        if (StringUtils.isBlank(endpoint)) {
            return groupName;
        }
        Map<String, List<String>> endpointGroups = this.appConfiguration.getEndpointGroups();
        log.debug(" endpointGroups:{}", endpointGroups);

        if (endpointGroups == null || endpointGroups.isEmpty()) {
            return groupName;
        }

        for (Map.Entry<String, List<String>> entry : endpointGroups.entrySet()) {
            log.debug(" entry.getKey():{}, entry.getValue():{}", entry.getKey(), entry.getValue());
            
            if (entry.getValue() != null && entry.getValue().contains(endpoint.toLowerCase())) {
                groupName = entry.getKey();
                break;
            }
        }
        log.info(" endpoint:{} groupName:{}", endpoint, groupName);
        return groupName;
    }

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

    public Response post(String endpoint, String postData, ContentType contentType, String token) {
        log.info("postData - endpoint:{}, postData:{}", endpoint, postData);
        String endpointPath = this.getEndpointPath(endpoint);

        log.debug("Posting data for - endpoint:{}, endpointPath:{},this.getEndpointUrl(endpointPath):{}", endpoint,
                endpointPath, this.getEndpointUrl(endpointPath));
        return post(this.getEndpointUrl(endpointPath), null, token, null, contentType, postData);
    }

    private Response post(String url, String authType, String token, Map<String, String> headers,
            ContentType contentType, String postData) {
        log.info("postData - url:{}, authType:{}, token:{}, headers:{}, contentType:{}, postData:{}", url, authType,
                token, headers, contentType, postData);

        if (StringUtils.isBlank(authType)) {
            authType = "Bearer ";
        }
        if (contentType == null) {
            contentType = ContentType.APPLICATION_JSON;
        }

        Builder request = getClientBuilder(url);
        request.header(AUTHORIZATION, authType + token);
        request.header(CONTENT_TYPE, contentType);

        if (headers != null) {
            for (Entry<String, String> headerEntry : headers.entrySet()) {
                request.header(headerEntry.getKey(), headerEntry.getValue());
            }
        }

        log.debug(" request:{}}", request);

        Response response = request.post(Entity.entity(postData, MediaType.APPLICATION_JSON));
        log.debug(" response:{}", response);

        return response;
    }

}
