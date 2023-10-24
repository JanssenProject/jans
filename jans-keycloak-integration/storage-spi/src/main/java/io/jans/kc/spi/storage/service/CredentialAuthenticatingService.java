package io.jans.kc.spi.storage.service;

import io.jans.kc.spi.storage.util.Constants;
import io.jans.kc.spi.storage.util.JansUtil;

import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;

import org.jboss.logging.Logger;

public class CredentialAuthenticatingService {

    private static Logger log = Logger.getLogger(CredentialAuthenticatingService.class);
    
    private JansUtil jansUtil;

    public CredentialAuthenticatingService(JansUtil jansUtil) {
        this.jansUtil = jansUtil;
    }

    public boolean authenticateUser(final String username, final String password) {
        log.debugv("CredentialAuthenticatingService::authenticateUser() -  username:{0}, password:{1} ", username,
                password);
        boolean isValid = false;
        try {

            String token = jansUtil.requestUserToken(jansUtil.getTokenEndpoint(), username, password, null,
                    Constants.RESOURCE_OWNER_PASSWORD_CREDENTIALS, null, MediaType.APPLICATION_FORM_URLENCODED);

            log.debugv("CredentialAuthenticatingService::authenticateUser() -  Final token token  - {0}", token);

            if (StringUtils.isNotBlank(token)) {
                isValid = true;
            }
        } catch (Exception ex) {
            log.debug("CredentialAuthenticatingService::authenticateUser() - Error while authenticating", ex);
        }
        return isValid;
    }

}
