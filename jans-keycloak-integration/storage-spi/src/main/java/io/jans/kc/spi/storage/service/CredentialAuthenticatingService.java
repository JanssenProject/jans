package io.jans.kc.spi.storage.service;

import io.jans.kc.spi.storage.util.Constants;
import io.jans.kc.spi.storage.util.JansUtil;

import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CredentialAuthenticatingService {

    private static Logger logger = LoggerFactory.getLogger(CredentialAuthenticatingService.class);
    
    private JansUtil jansUtil;

    public CredentialAuthenticatingService(JansUtil jansUtil) {
        this.jansUtil = jansUtil;
    }

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
