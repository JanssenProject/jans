/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.service.UmaService;
import io.jans.configapi.util.ApiConstants;
import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@ApplicationScoped
@Named("umaAuthorizationService")
public class UmaAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;
    private Token umaPat;
    private long umaPatAccessTokenExpiration = 0L; // When the "accessToken" will expire;
    private final ReentrantLock lock = new ReentrantLock();

    @Inject
    private Logger log;

    UmaResource umaResource;

    @Inject
    UmaService umaService;

    @Inject
    private UmaMetadata umaMetadata;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceManager;

    @Inject
    StaticConfiguration staticConf;

    public void validateAuthorization(String token, ResourceInfo resourceInfo) throws Exception {
        log.debug(" UmaAuthorizationService::validateAuthorization() - token = " + token
                + " , resourceInfo.getClass().getName() = " + resourceInfo.getClass().getName() + "\n");

        if (StringUtils.isBlank(token)) {
            log.info("Token is blank");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        List<String> resourceScopes = getRequestedScopes(resourceInfo);
        log.debug(" UmaAuthorizationService::validateAuthorization() - resourceScopes = " + resourceScopes + "\n");

        Token patToken = null;
        try {
            patToken = getPatToken(); // WIP ??
        } catch (Exception ex) {
            log.error("Failed to obtain PAT token", ex);
            throw new WebApplicationException("Failed to obtain PAT token", ex);
        }

        log.debug("this.umaService.getUmaMetadata() = " + this.umaService.getUmaMetadata());

        log.debug("AuthClientFactory::createUmaMetadataService() - resourceScopes = " + resourceScopes + "\n\n");

        if (!resourceScopes.isEmpty()) {
            this.umaService.validateRptToken(patToken, token, getUmaResourceId(), resourceScopes);
        } else {
            this.umaService.validateRptToken(patToken, token, getUmaResourceId(), getUmaScope());
        }
    }

    public UmaResource getUmaResource() {
        if (umaResource != null)
            return umaResource;
        umaResource = loadUmaResource();
        return umaResource;
    }

    public String getUmaResourceId() {
        if (umaResource == null)
            return null;

        return umaResource.getId();
    }

    public String getUmaScope() {
        if (umaResource == null)
            return null;

        String scopes = "";
        List<String> scopeList = umaResource.getScopes();
        if (scopeList != null && !scopeList.isEmpty()) {
            scopes = scopeList.stream().map(s -> s.concat(" ")).collect(Collectors.joining());
            log.debug(scopes);
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
            log.error("Failed to check UMA PAT token status", ex);
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
        log.debug("\n\n getClientKeyStoreFile() = " + getClientKeyStoreFile() + " , getClientKeyStorePassword() = "
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

    public UmaResource loadUmaResource() {
        if (StringHelper.isEmpty(ConfigurationFactory.getApiResourceName())) {
            log.trace("Config API Resource not defined");
            return null;
        }

        try {
            String[] targetArray = new String[] { ConfigurationFactory.getApiResourceName() };
            Filter jsIdFilter = Filter.createSubstringFilter("jansId", null, targetArray, null);
            // Filter jsIdFilter = Filter.createSubstringFilter("oxId", null, targetArray,
            // null);
            Filter displayNameFilter = Filter.createSubstringFilter(ApiConstants.DISPLAY_NAME, null, targetArray, null);
            Filter searchFilter = Filter.createORFilter(jsIdFilter, displayNameFilter);

            List<UmaResource> umaResourceList = persistenceManager.get()
                    .findEntries(staticConf.getBaseDn().getUmaBase(), UmaResource.class, searchFilter);
            log.trace(" \n umaResourceList = " + umaResourceList + "\n");

            if (umaResourceList == null || umaResourceList.isEmpty()) {
                log.trace("Unable to find UMA resource by name: " + ConfigurationFactory.getApiResourceName());
                return null;
            }

            return umaResourceList.get(0);
        } catch (Exception ex) {
            log.error("Failed to load Config API Resource.", ex);
            throw new ConfigurationException("Failed to load Config API Resource.", ex);
        }
    }

}