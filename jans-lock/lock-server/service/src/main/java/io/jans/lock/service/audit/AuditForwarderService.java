package io.jans.lock.service.audit;

import java.util.concurrent.TimeUnit;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import io.jans.as.model.uma.wrapper.Token;
import io.jans.lock.service.TokenEndpointService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * Provides interface for audit REST web services
 *  
 * @author Yuriy Movchan Date: 06/06/2024
 */
@ApplicationScoped
public class AuditForwarderService {

	public static final String AUDIT_TELEMETRY = "telemetry";
	public static final String AUDIT_TELEMETRY_BULK = "telemetry/bulk";

    public static final String AUDIT_LOG = "log";
	public static final String AUDIT_LOG_BULK = "log/bulk";

	public static final String AUDIT_HEALTH = "health";
	public static final String AUDIT_HEALTH_BULK = "health/bulk";

    @Inject
    private Logger log;

    @Inject
    private TokenEndpointService tokenEndpointService;

    private ExpiringMap<String, Token> issuedTokens;
    
	@PostConstruct
    public void init() {
        this.issuedTokens = ExpiringMap.builder().expirationPolicy(ExpirationPolicy.CREATED).variableExpiration().build();
    }

    public Response post(String endpoint, String postData, ContentType contentType) {
        log.info("postData - endpoint:{}, postData:{}, contentType:{}", endpoint, postData, contentType);
        
        Token token = issuedTokens.get(endpoint);
        if ((token == null) || (token.getAccessToken() == null)) {
            log.info("Generating new token for endpoint '{}'", endpoint);
            token = this.getTokenForEndpoint(endpoint);
            if ((token == null) || (token.getAccessToken() == null)) {
                log.error("Failed to get token for endpoint '{}'", endpoint);
                return null;
            }

            log.debug("Get access token for endpoint: {}, access_token: {}", endpoint, token.getAccessToken());
            issuedTokens.put(endpoint, token, ExpirationPolicy.CREATED, token.getExpiresIn(), TimeUnit.SECONDS);
        }

        log.debug("Sending data to config-api endpoint: {}, data: {}", endpoint, postData);
        return this.tokenEndpointService.post(endpoint, postData, contentType, token.getAccessToken());
    }

    private Token getTokenForEndpoint(String endpoint) {
        log.info("Attempting to get token for endpoint: {}", endpoint);
        Token token = this.tokenEndpointService.getAccessToken(endpoint, true);
        log.debug("Get token for endpoint: {}, token: {}", endpoint, token);
        
        return token;
    }

}
