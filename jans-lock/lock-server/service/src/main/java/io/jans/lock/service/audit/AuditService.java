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

@ApplicationScoped
public class AuditService {

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

        String accessToken;
        if (token == null) {
            log.info("Generating new token for endpoint '{}'", endpoint);
            accessToken = this.getAccessTokenForAudit(endpoint);
        } else {
            accessToken = token.getAccessToken();
            log.debug("Reusing token for endpoint '{}' : {}", endpoint, accessToken);
        }

        return this.tokenEndpointService.post(endpoint, postData, contentType, accessToken);
    }

    private String getAccessTokenForAudit(String endpoint) {
        log.info("Get Access Token For Audit endpoint:{}", endpoint);
        String accessToken = null;
        Token token = this.tokenEndpointService.getAccessToken(endpoint, true);
        log.debug("Get Access Token For Audit endpoint:{}, token:{}", endpoint, token);

        if (token != null) {
            issuedTokens.put(endpoint, token, ExpirationPolicy.CREATED, token.getExpiresIn(), TimeUnit.SECONDS);

            accessToken = token.getAccessToken();
            log.debug("Get Access Token For Audit endpoint:{}, accessToken:{}, expiresIn", endpoint, accessToken);
        }

        return accessToken;
    }

}
