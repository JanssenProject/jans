package io.jans.lock.service.audit;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.as.model.uma.wrapper.Token;
import io.jans.lock.service.DataMapperService;
import io.jans.lock.service.TokenEndpointService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class AuditService {

    @Inject
    private Logger log;

    @Inject
    private TokenEndpointService tokenEndpointService;
    
    @Inject
    private DataMapperService dataMapperService;

    private Map<String, Date> tokenDetails = new HashMap<>();

    public Response post(String endpoint, String postData, ContentType contentType) {
        log.info("postData - endpoint:{}, postData:{}, contentType:{}", endpoint, postData, contentType);

        Date tokenExpiryDate = this.getTokenExpiryDate();
        log.debug("postData - tokenExpiryDate:{}", tokenExpiryDate);
        boolean isTokenValid = this.tokenEndpointService.isTokenValid(tokenExpiryDate);
        log.debug(" postData - tokenDetails:{}, tokenExpiryDate:{}, isTokenValid:{}", tokenDetails, tokenExpiryDate,
                isTokenValid);
        String accessToken = null;
        if (tokenDetails != null && !tokenDetails.isEmpty() && isTokenValid) {
            log.info("Reusing token as still valid!");
            accessToken = this.getToken();
        } else {
            log.info("Generating new token !");
            accessToken = this.getAccessTokenForAudit(endpoint);
        }
        return this.tokenEndpointService.post(endpoint, postData, contentType, accessToken);
    }

    public JsonNode getJsonNode(HttpServletRequest request) {
    	JsonNode jsonBody = null;
        if (request == null) {
            return jsonBody;
        }

        try {
        	jsonBody = dataMapperService.readTree(request.getInputStream());
            log.debug(" jsonBody:{}", jsonBody);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Exception while retriving json from request is - ", ex);
        }

        return jsonBody;
    }

    private String getAccessTokenForAudit(String endpoint) {
        log.info("Get Access Token For Audit endpoint:{}", endpoint);
        String accessToken = null;
        Token token = this.tokenEndpointService.getAccessToken(endpoint, true);
        log.debug("Get Access Token For Audit endpoint:{}, token:{}", endpoint, token);

        if (token != null) {
            accessToken = token.getAccessToken();
            Integer expiresIn = token.getExpiresIn();
            log.debug("Get Access Token For Audit endpoint:{}, accessToken:{}, expiresIn", endpoint, accessToken);

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
        log.debug("tokenDetails:{}", tokenDetails);
        String accessToken = null;
        if (tokenDetails != null && !tokenDetails.isEmpty() && tokenDetails.keySet() != null
                && !tokenDetails.keySet().isEmpty()) {
            Optional<String> token = tokenDetails.keySet().stream().findFirst();

            if (token.isPresent() && StringUtils.isNotBlank(token.get())) {
                accessToken = token.get();
            }            
        }
        log.debug("accessToken:{}", accessToken);
        return accessToken;
    }
}
