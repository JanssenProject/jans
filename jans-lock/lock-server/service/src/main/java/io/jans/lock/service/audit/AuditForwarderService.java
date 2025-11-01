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
    
	/**
     * Initializes the token cache used by this service.
     *
     * Configures `issuedTokens` as an ExpiringMap that expires entries based on creation time and allows variable, per-entry expirations (used to store tokens with individual TTLs).
     */
    @PostConstruct
    public void init() {
        this.issuedTokens = ExpiringMap.builder().expirationPolicy(ExpirationPolicy.CREATED).variableExpiration().build();
    }

    /**
     * Forwards audit data to the configured endpoint for the given request type, obtaining and caching an access token as needed.
     *
     * <p>If no valid cached token exists for the request type this method requests a new token and caches it with an expiration
     * based on the token's lifetime.</p>
     *
     * @param responseBuilder the Response builder that will be updated (set to 400) when forwarding fails
     * @param requestType     the audit endpoint type that determines which configured endpoint and token scope to use
     * @param postData        the payload to send to the endpoint
     * @param contentType     the content type of the payload; defaults to application/json when null
     * @return the response body returned by the endpoint as a String, or `null` if the request failed (responseBuilder will be set to 400)
     */
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

            log.debug("Obtained access token for requestType: {}", requestType);
            issuedTokens.put(requestType, token, ExpirationPolicy.CREATED, token.getExpiresIn(), TimeUnit.SECONDS);
        }

        log.debug("Sending data to config-api endpoint: {}, data: {}", requestType, postData);

        return post(responseBuilder, requestType, postData, contentType, token.getAccessToken());
    }

    /**
     * Fetches an access token for the specified audit endpoint type.
     *
     * @param requestType the audit endpoint type to obtain a token for
     * @return the Token for the given endpoint type, or {@code null} if no token could be obtained
     */
    private Token getTokenForEndpoint(AuditEndpointType requestType) {
        log.debug("Attempting to get token for requestType: {}", requestType);
        Token token = tokenEndpointService.getAccessToken(requestType);
        log.debug("Get token for requestType: {}, token: {}", requestType, token);
        
        return token;
    }

    /**
     * Forwards the given payload to the endpoint associated with the specified audit request type using the provided token.
     *
     * If the downstream request fails or the response is invalid, the method sets the provided ResponseBuilder's status to 400 (BAD_REQUEST) and returns `null`.
     *
     * @param responseBuilder a JAX-RS Response builder whose status may be updated on error
     * @param requestType the audit endpoint type whose configured path determines the target URL
     * @param postData the payload to post to the endpoint
     * @param contentType the content type of the payload; if null, defaults to application/json
     * @param token the authorization token to include in the request's Authorization header (may be null or blank)
     * @return the response body returned by the endpoint as a string if the request succeeds, `null` otherwise
     */
    public String post(Response.ResponseBuilder responseBuilder, AuditEndpointType requestType, String postData, ContentType contentType, String token) {
        log.debug("postData - requestType: {}, postData: {}", requestType, postData);

        String endpointUrl = getEndpointUrl(requestType);

        log.debug("Posting data for - requestType: {}, endpointPath: {},this.getEndpointUrl(endpointPath): {}", requestType,
        		requestType.getPath(), endpointUrl);

        return postData(responseBuilder, endpointUrl, null, token, null, contentType, postData);
    }

    /**
     * Send an HTTP POST to the given URL and read the response body.
     *
     * If `authType` is blank, "Bearer " is used. If `contentType` is null, application/json is used.
     * On error conditions (missing/invalid HTTP response, non-200 status, or failure reading the response body)
     * this method sets the provided ResponseBuilder's status to Status.BAD_REQUEST and returns `null`.
     *
     * @param responseBuilder builder used to set an HTTP status on error
     * @param url the full endpoint URL to post to
     * @param authType the authorization scheme prefix (e.g., "Bearer "); a trailing space may be required
     * @param token the access token to place after the authType in the Authorization header
     * @param headers additional headers to include; may be null (Content-Type and Authorization will be set/overwritten)
     * @param contentType the request content type; if null, application/json is used
     * @param postData the request body to send
     * @return the response body as a string, or `null` if the request failed or the response could not be read
     */
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
		headers.put(CONTENT_TYPE, contentType.toString());
        headers.put(AUTHORIZATION, authType + token);

        HttpServiceResponse response = httpService.executePost(url, token, headers, postData, contentType, authType);
		if (response == null || response.getHttpResponse() == null) {
			log.error("Get invalid response from config-api URI {}", url);
			responseBuilder.status(Status.BAD_REQUEST);
			return null;
		}
        	
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

    /**
     * Builds the full endpoint URL for the given audit endpoint type.
     *
     * @param requestType the audit endpoint type whose configured path will be appended to the OpenID issuer
     * @return the full endpoint URL composed of the configured OpenID issuer and the endpoint's config path
     */
    private String getEndpointUrl(AuditEndpointType requestType) {
        StringBuilder sb = new StringBuilder();
        sb.append(appConfiguration.getOpenIdIssuer());
        sb.append("/");
        sb.append(requestType.getConfigPath());

        log.debug("Endpoint: {} configApi endpoint: {}", requestType, sb);

        return sb.toString();
    }

}