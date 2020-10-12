package io.jans.configapi.auth;

//import io.jans.as.client.uma.wrapper.UmaClient;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.configapi.service.UmaService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;

@ApplicationScoped
@Named("umaAuthorizationService")
public class UmaAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;
    private Token umaPat;
    private long umaPatAccessTokenExpiration = 0l; // When the "accessToken" will expire;
    private final ReentrantLock lock = new ReentrantLock();

    @Inject
    private Logger logger;

    @Inject
    private EncryptionService encryptionService;

    @Inject
    UmaResource configApiResource;

    @Inject
    UmaService umaService;

    @Inject
    private UmaMetadata umaMetadata;

    public void validateAuthorization(String token, ResourceInfo resourceInfo) throws Exception {
        logger.debug(" UmaAuthorizationService::validateAuthorization() - token = " + token
                + " , resourceInfo.getClass().getName() = " + resourceInfo.getClass().getName() + "\n");

        if (StringUtils.isBlank(token)) {
            logger.info("Token is blank");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        List<String> resourceScopes = getRequestedScopes(resourceInfo);
        logger.debug(" UmaAuthorizationService::validateAuthorization() - resourceScopes = " + resourceScopes + "\n");

        Token patToken = null;
        try {
            //patToken = getPatToken(); // WIP ??
        } catch (Exception ex) {
            logger.error("Failed to obtain PAT token", ex);
            throw new WebApplicationException("Failed to obtain PAT token", ex);
        }

        logger.debug("this.umaService.getUmaMetadata() = " + this.umaService.getUmaMetadata());

        logger.debug(
                "AuthClientFactory::createUmaMetadataService() - resourceScopes = " + resourceScopes + "\n\n");

        /*
        if (!resourceScopes.isEmpty()) {
            this.umaService.validateRptToken(patToken, token, getUmaResourceId(), resourceScopes);
        } else {
            this.umaService.validateRptToken(patToken, token, getUmaResourceId(), getUmaScope());
        }
        */
    }

    public String getUmaResourceId() {
        return configApiResource.getId();
    }

    public String getUmaScope() {
        String scopes = new String();
        List<String> scopeList = configApiResource.getScopes();
        if (scopeList != null && !scopeList.isEmpty()) {
            scopes = scopeList.stream().map(s -> s.concat(" ")).collect(Collectors.joining());
            logger.debug(scopes);
        }

        return scopes.trim();
    }

    private String getClientId() {
        return configurationFactory.getApiClientId(); // TBD
    }

    private String getClientKeyId() {
        return null; // TBD
    }

    public Token getPatToken() throws Exception {
        if (isValidPatToken(this.umaPat, this.umaPatAccessTokenExpiration)) {
            return this.umaPat;
        }

        lock.lock();
        try {
            if (isValidPatToken(this.umaPat, this.umaPatAccessTokenExpiration)) {
                return this.umaPat;
            }

            retrievePatToken();
        } finally {
            lock.unlock();
        }

        return this.umaPat;
    }

    private boolean isEnabledUmaAuthentication() {
        return (this.umaMetadata != null) && isExistPatToken();
    }

    private boolean isExistPatToken() {
        try {
            return getPatToken() != null;
        } catch (Exception ex) {
            logger.error("Failed to check UMA PAT token status", ex);
        }

        return false;
    }

    private String getIssuer() {
        if (umaMetadata == null) {
            return "";
        }
        return umaMetadata.getIssuer();
    }

    private void retrievePatToken() throws Exception {
        this.umaPat = null;
        if (umaMetadata == null) {
            return;
        }
        logger.debug("\n\n getClientKeyStoreFile() = " + getClientKeyStoreFile() + " , getClientKeyStorePassword() = "
                + getClientKeyStorePassword() + " , getClientId() =" + getClientId() + " , getClientKeyId() = "
                + getClientKeyId() + "\n\n");
        String umaClientKeyStoreFile = getClientKeyStoreFile();
        String umaClientKeyStorePassword = getClientKeyStorePassword();
        if (StringHelper.isEmpty(umaClientKeyStoreFile) || StringHelper.isEmpty(umaClientKeyStorePassword)) {
            throw new Exception("UMA JKS keystore path or password is empty");
        }
        /*
         * if (umaClientKeyStorePassword != null) { try { umaClientKeyStorePassword =
         * encryptionService.decrypt(umaClientKeyStorePassword); } catch
         * (EncryptionException ex) {
         * logger.error("Failed to decrypt UmaClientKeyStorePassword password", ex); } }
         */
        try {
            this.umaPat = UmaClient.requestPat(umaMetadata.getTokenEndpoint(), umaClientKeyStoreFile,
                    umaClientKeyStorePassword, getClientId(), getClientKeyId());
            if (this.umaPat == null) {
                this.umaPatAccessTokenExpiration = 0l;
            } else {
                this.umaPatAccessTokenExpiration = computeAccessTokenExpirationTime(this.umaPat.getExpiresIn());
            }
        } catch (Exception ex) {
            throw new Exception("Failed to obtain valid UMA PAT token", ex);
        }

        if ((this.umaPat == null) || (this.umaPat.getAccessToken() == null)) {
            throw new Exception("Failed to obtain valid UMA PAT token");
        }
    }

    private boolean isValidPatToken(Token validatePatToken, long validatePatTokenExpiration) {
        final long now = System.currentTimeMillis();

        // Get new access token only if is the previous one is missing or expired
        return !((validatePatToken == null) || (validatePatToken.getAccessToken() == null)
                || (validatePatTokenExpiration <= now));
    }

}