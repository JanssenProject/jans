package io.jans.lock.service.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import io.jans.as.model.uma.wrapper.Token;
import io.jans.lock.model.AuditEndpointType;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.TokenEndpointService;
import io.jans.model.net.HttpServiceResponse;
import io.jans.service.net.BaseHttpService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * Provides interface for audit REST web services
 *  
 * @author Yuriy Movchan Date: 06/06/2024
 */
@ApplicationScoped
public class AuditForwarderService {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private TokenEndpointService tokenEndpointService;
    
    @Inject
    private BaseHttpService httpService;

    private ExpiringMap<AuditEndpointType, Token> issuedTokens;
    
	@PostConstruct
    public void init() {
        this.issuedTokens = ExpiringMap.builder().expirationPolicy(ExpirationPolicy.CREATED).variableExpiration().build();
    }

    public String post(Response.ResponseBuilder responseBuilder, AuditEndpointType requestType, String postData, ContentType contentType) {
        log.debug("postData - requestType: {}, postData: {}, contentType: {}", requestType, postData, contentType);
        
        Token token = issuedTokens.get(requestType);
        if ((token == null) || (token.getAccessToken() == null)) {
            log.info("Generating new token for endpoint {}", requestType);
            token = this.getTokenForEndpoint(requestType);
            if ((token == null) || (token.getAccessToken() == null)) {
                log.error("Failed to get token for endpoint {}", requestType);
                return null;
            }

            log.debug("Get access token for requestType: {}, accessToken: {}", requestType, token.getAccessToken());
            issuedTokens.put(requestType, token, ExpirationPolicy.CREATED, token.getExpiresIn(), TimeUnit.SECONDS);
        }

        log.debug("Sending data to config-api endpoint: {}, data: {}", requestType, postData);

        return post(responseBuilder, requestType, postData, contentType, token.getAccessToken());
    }

    private Token getTokenForEndpoint(AuditEndpointType requestType) {
        log.debug("Attempting to get token for requestType: {}", requestType);
        Token token = tokenEndpointService.getAccessToken(requestType);
        log.debug("Get token for requestType: {}, token: {}", requestType, token);
        
        return token;
    }

    public String post(Response.ResponseBuilder responseBuilder, AuditEndpointType requestTypet, String postData, ContentType contentType, String token) {
        log.debug("postData - requestTypet: {}, postData: {}", requestTypet, postData);

        String endpointUrl = getEndpointUrl(requestTypet);

        log.debug("Posting data for - requestTypet: {}, endpointPath: {},this.getEndpointUrl(endpointPath): {}", requestTypet,
        		requestTypet.getPath(), endpointUrl);

        return postData(responseBuilder, endpointUrl, null, token, null, contentType, postData);
    }

    private String postData(Response.ResponseBuilder responseBuilder, String url, String authType, String token, Map<String, String> headers,
            ContentType contentType, String postData) {
        log.debug("postData - url: {}, authType: {}, token: {}, headers: {}, contentType: {}, postData: {}", url, authType,
                token, headers, contentType, postData);

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

        HttpServiceResponse response = httpService.executePost(url, token, headers, postData, contentType, authType);

        log.debug("response:{}", response);

        HttpEntity entity = response.getHttpResponse().getEntity();
        if ((response.getHttpResponse() == null) || (response.getHttpResponse().getEntity() == null)) {
        	log.error("Get invalid response from config-api URI {}", url);
        	responseBuilder.status(Status.BAD_REQUEST);
        	return null;
        }

		StatusLine statusLine = response.getHttpResponse().getStatusLine();
		log.debug("Get response with statusLine {} from {}", statusLine, url);

        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
    		log.error("Response from {} with status code {} in invalid!", url, statusLine.getStatusCode());
        	responseBuilder.status(Status.BAD_REQUEST);
        	return null;
        }

        String responseStr = null;
		try {
			responseStr = EntityUtils.toString(entity, "UTF-8");
		} catch (Exception ex) {
			log.error("Failed to read response from config-api URI {}", url, ex);
        	responseBuilder.status(Status.BAD_REQUEST);
		}

        return responseStr;
    }

    private String getEndpointUrl(AuditEndpointType requestTypet) {
        StringBuilder sb = new StringBuilder();
        sb.append(appConfiguration.getOpenIdIssuer());
        sb.append("/");
        sb.append(requestTypet.getConfigPath());

        log.debug("Endpoint: {} configApi endpoint: {}", requestTypet, sb);

        return sb.toString();
    }

}
