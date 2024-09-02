package io.jans.lock.service.audit;

import io.jans.as.model.uma.wrapper.Token;
import io.jans.lock.service.TokenEndpointService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
public class AuditService {

    @Inject
    private Logger log;

    @Inject
    TokenEndpointService tokenEndpointService;

    private Map<String, Date> tokenDetails = new HashMap<>();

    public Response post(String endpoint, String postData, ContentType contentType) {
        log.error("postData - endpoint:{}, postData:{}", endpoint, postData);

        String accessToken = null;

        Date tokenExpiryDate = this.getTokenExpiryDate();
        log.error("postData - tokenExpiryDate:{}", tokenExpiryDate);
        boolean isTokenValid = this.tokenEndpointService.isTokenValid(tokenExpiryDate);
        log.error("postData - tokenDetails:{}, tokenExpiryDate:{}, isTokenValid:{}", tokenDetails, tokenExpiryDate,
                isTokenValid);
        
        if (tokenDetails != null && !tokenDetails.isEmpty() && isTokenValid) {
            log.error("Reusing token as still valid!");
            accessToken = this.getToken();
        } else {
            log.error("Generating new token !");
            accessToken = this.getAccessTokenForAudit(endpoint);
        }
        return this.tokenEndpointService.post(endpoint, postData, contentType, accessToken);
    }

    public JSONObject getJSONObject(HttpServletRequest request) {
        return this.tokenEndpointService.getJSONObject(request);
    }

    private String getAccessTokenForAudit(String endpoint) {
        log.error("Get Access Token For Audit endpoint:{}", endpoint);
        String accessToken = null;
        Token token = this.tokenEndpointService.getAccessToken(endpoint, true);
        log.error("Get Access Token For Audit endpoint:{}, token:{}", endpoint, token);

        if (token != null) {
            accessToken = token.getAccessToken();
            Integer expiresIn = token.getExpiresIn();
            log.error("Get Access Token For Audit endpoint:{}, accessToken:{}, expiresIn", endpoint, accessToken);

            tokenDetails.put(accessToken, this.tokenEndpointService.computeTokenExpiryTime(expiresIn));
        }
        return accessToken;
    }

    private Date getTokenExpiryDate() {
        Date tokenExpiryDate = null;
        if (tokenDetails != null && !tokenDetails.isEmpty() && tokenDetails.values() != null
                && !tokenDetails.values().isEmpty()) {
            Optional<Date> expiryDate = tokenDetails.values().stream().findFirst();

            if (expiryDate.isPresent()) {
                tokenExpiryDate = expiryDate.get();
            }
            log.debug("tokenExpiryDate:{}", tokenExpiryDate);
        }
        return tokenExpiryDate;
    }

    private String getToken() {
        log.error("tokenDetails:{}", tokenDetails);
        String accessToken = null;
        if (tokenDetails != null && !tokenDetails.isEmpty() && tokenDetails.keySet() != null
                && !tokenDetails.keySet().isEmpty()) {
            Optional<String> token = tokenDetails.keySet().stream().findFirst();

            if (token.isPresent() && StringUtils.isNotBlank(token.get())) {
                accessToken = token.get();
            }            
        }
        log.error("accessToken:{}", accessToken);
        return accessToken;
    }
}
