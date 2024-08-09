package io.jans.lock.util;

import io.jans.as.model.uma.wrapper.Token;

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
    LockUtil lockUtil;

    private Map<String, Date> tokenDetails = new HashMap<>();

    public Response post(String endpoint, String postData, ContentType contentType) {
        log.debug("postData - endpoint:{}, postData:{}", endpoint, postData);

        String accessToken = null;

        Date tokenExpiryDate = this.getTokenExpiryDate();
        log.debug("postData - tokenExpiryDate:{}", tokenExpiryDate);
        boolean isTokenValid = this.lockUtil.isTokenValid(tokenExpiryDate);
        log.debug("postData - tokenDetails:{}, tokenExpiryDate:{}, isTokenValid:{}", tokenDetails, tokenExpiryDate,
                isTokenValid);
        if (tokenDetails != null && !tokenDetails.isEmpty() && isTokenValid) {
            log.debug("Reusing token as still valid!");
            accessToken = this.getToken();
        } else {
            log.debug("Generating new token !");
            accessToken = this.getAccessTokenForAudit(endpoint);
        }
        return this.lockUtil.post(endpoint, postData, contentType, accessToken);
    }

    public JSONObject getJSONObject(HttpServletRequest request) {
        return this.lockUtil.getJSONObject(request);
    }

    private String getAccessTokenForAudit(String endpoint) {
        log.debug("Get Access Token For Audit endpoint:{}", endpoint);
        String accessToken = null;
        Token token = this.lockUtil.getAccessToken(endpoint, true);
        log.debug("Get Access Token For Audit endpoint:{}, token:{}", endpoint, token);

        if (token != null) {
            accessToken = token.getAccessToken();
            Integer expiresIn = token.getExpiresIn();
            log.debug("Get Access Token For Audit endpoint:{}, accessToken:{}, expiresIn", endpoint, accessToken);

            tokenDetails.put(accessToken, this.lockUtil.computeTokenExpiryTime(expiresIn));
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
        String accessToken = null;
        if (tokenDetails != null && !tokenDetails.isEmpty() && tokenDetails.keySet() != null
                && !tokenDetails.keySet().isEmpty()) {
            Optional<String> token = tokenDetails.keySet().stream().findFirst();

            if (token.isPresent() && StringUtils.isNotBlank(token.get())) {
                accessToken = token.get();
            }
            log.debug("accessToken:{}", accessToken);
        }
        return accessToken;
    }
}
