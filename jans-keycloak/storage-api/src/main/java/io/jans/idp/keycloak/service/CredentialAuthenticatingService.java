package io.jans.idp.keycloak.service;

import io.jans.idp.keycloak.util.Constants;
import io.jans.idp.keycloak.util.JansUtil;

import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CredentialAuthenticatingService {

    private static Logger logger = LoggerFactory.getLogger(CredentialAuthenticatingService.class);
    private static JansUtil jansUtil = new JansUtil();

    public boolean authenticateUser(final String username, final String password) {
        logger.info("CredentialAuthenticatingService::authenticateUser() -  username:{}, password:{} ", username,
                password);
        boolean isValid = false;
        try {

            String token = jansUtil.requestUserToken(jansUtil.getTokenEndpoint(), username, password, null,
                    Constants.RESOURCE_OWNER_PASSWORD_CREDENTIALS, null, MediaType.APPLICATION_FORM_URLENCODED);

            logger.info("CredentialAuthenticatingService::authenticateUser() -  Final token token  - {}", token);

            if (StringUtils.isNotBlank(token)) {
                isValid = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("CredentialAuthenticatingService::authenticateUser() - Error while authenticating is ", ex);
        }
        return isValid;
    }

}
