package io.jans.lock.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import jakarta.ws.rs.WebApplicationException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.http.entity.ContentType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import org.json.JSONObject;
import org.slf4j.Logger;


@ApplicationScoped
public class AuthUtil {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";

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

    public HttpServiceResponse postData(String endpoint, String postData, ContentType contentType) {
        log.error("postData - endpoint:{}, postData:{}", endpoint, postData);
        String endpointPath = this.getEndpointPath(endpoint);
        String token = this.getToken(endpointPath);

        return postData(this.getEndpointUrl(endpointPath), null, token, null, contentType, postData);
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

        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        headers.put(AUTHORIZATION, authType + token);

        HttpServiceResponse response = httpService.executePost(uri, token, headers, postData, contentType, authType);

        log.error("response:{}", response);
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
            log.error("serviceResponse.getHttpResponse().getEntity():{}",
                    serviceResponse.getHttpResponse().getEntity());
            String responseMsg = EntityUtils.toString(serviceResponse.getHttpResponse().getEntity(), "UTF-8");
            log.error("New responseMsg:{}", responseMsg);
            log.error("serviceResponse.getHttpResponse().getAllHeaders():{}",
                    serviceResponse.getHttpResponse().getAllHeaders());
        } catch (Exception ex) {
            log.error("Error while getting entity using EntityUtils is ", ex);
        }
        return jsonString;
    }

    public JSONObject getResponseJson(HttpServiceResponse serviceResponse) {
        JSONObject jsonObj = null;
        if (serviceResponse == null || serviceResponse.getHttpResponse() == null) {
            return jsonObj;
        }

        HttpResponse httpResponse = serviceResponse.getHttpResponse();
        if (httpResponse != null) {
            log.error("getResponseJson() - httpResponse.getStatusLine():{}", httpResponse.getStatusLine());
            log.error("getResponseJson() - .httpResponse.getEntity():{}", httpResponse.getEntity());
            if (httpResponse.getEntity() != null) {
                jsonObj = new JSONObject(httpResponse.getEntity());
                log.error("getResponseJson() - .jsonObj:{}", jsonObj);
            }
        }

        return jsonObj;
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
        log.error("scope:{}", scope);
        return scope.toString();
    }

    private String getEndpointPath(String endpoint) {
        log.error("Get endpoint URL for endpoint:{}", endpoint);
        Map<String, List<String>> endpointMap = this.appConfiguration.getEndpointDetails();
        log.error("Get endpoint URL for endpointMap:{}", endpointMap);

        if (StringUtils.isBlank(endpoint) || endpointMap == null || endpointMap.isEmpty()) {
            return endpoint;
        }

        Set<String> keys = endpointMap.keySet();
        log.error("endpointMap keys:{}", keys);

        String endpointPath = keys.stream()
                .filter(e -> e != null && e.toLowerCase().endsWith("/" + endpoint.toLowerCase())).findFirst()
                .orElse(null);
        log.error("Final endpoint:{}, endpointPath:{}", endpoint, endpointPath);
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

        log.error("Get Endpoint - sb:{}", sb);
        return sb.toString();
    }

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }
    
    
    public Response post(String endpoint, String postData, ContentType contentType) {
        log.error("postData - endpoint:{}, postData:{}", endpoint, postData);
        String endpointPath = this.getEndpointPath(endpoint);
        String token = this.getToken(endpointPath);

        return post(this.getEndpointUrl(endpointPath), null, token, null, contentType, postData);
    }

    private Response post(String url, String authType, String token, Map<String, String> headers,
            ContentType contentType, String postData) {
        log.error("postData - url:{}, authType:{}, token:{}, headers:{}, contentType:{}, postData:{}", url, authType,
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
        log.error(" \n\n\n request:{}, request.toString():{}", request, request.toString());
        
        Response response = request.post(Entity.entity(postData, MediaType.APPLICATION_JSON));
        log.error(" \n\n\n response:{}", response);
        
       
        return response;
    }

}
