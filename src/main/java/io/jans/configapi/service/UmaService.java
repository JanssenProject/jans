/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service;

import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.client.uma.UmaPermissionService;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.client.uma.exception.UmaException;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.auth.AuthClientFactory;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.as.model.util.Pair;
import io.jans.util.StringHelper;
import io.jans.util.init.Initializable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import org.slf4j.Logger;

@ApplicationScoped
public class UmaService extends Initializable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    private UmaMetadata umaMetadata;
    private UmaPermissionService umaPermissionService;
    private UmaRptIntrospectionService umaRptIntrospectionService;

    @Override
    protected void initInternal() {
        try {
            loadUmaConfigurationService();
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to load oxAuth UMA configuration", ex);
        }
    }

    public UmaMetadata getUmaMetadata() throws Exception {
        init();
        return this.umaMetadata;
    }

    public void loadUmaConfigurationService() throws Exception {
        this.umaMetadata = getUmaMetadataConfiguration();
        this.umaPermissionService = AuthClientFactory.getUmaPermissionService(this.umaMetadata, false);
        this.umaRptIntrospectionService = AuthClientFactory.getUmaRptIntrospectionService(this.umaMetadata, false);
    }

    @Produces
    @ApplicationScoped
    @Named("umaMetadataConfiguration")
    public UmaMetadata getUmaMetadataConfiguration() throws OxIntializationException {

        logger.info("##### Getting UMA Metadata Service ...");
        logger.debug(
                "\n\n UmaService::initUmaMetadataConfiguration() - configurationService.find().getUmaConfigurationEndpoint() = "
                        + configurationService.find().getUmaConfigurationEndpoint());
        UmaMetadataService umaMetadataService = AuthClientFactory
                .getUmaMetadataService(configurationService.find().getUmaConfigurationEndpoint(), false);
        logger.debug("\n\n UmaService::initUmaMetadataConfiguration() - umaMetadataService = " + umaMetadataService);

        logger.info("##### Getting UMA Metadata ...");
        UmaMetadata umaMetadata = umaMetadataService.getMetadata();
        logger.debug("\n\n UmaService::initUmaMetadataConfiguration() - umaMetadata = " + umaMetadata);
        logger.info("##### Getting UMA metadata ... DONE");

        if (umaMetadata == null) {
            throw new OxIntializationException("UMA meta data configuration is invalid!");
        }

        return umaMetadata;
    }

    public void validateRptToken(Token patToken, String authorization, String umaResourceId, String scopeId)
            throws UmaException {
        validateRptToken(patToken, authorization, umaResourceId, Arrays.asList(scopeId));
        return;
    }

    public void validateRptToken(Token patToken, String authorization, String resourceId, List<String> scopeIds)
            throws UmaException {
        logger.debug("\n\n\n UmaService::validateRptToken() - Entry - patToken = " + patToken + " , authorization = "
                + authorization + " , resourceId = " + resourceId + " , scopeIds = " + scopeIds + "\n\n\n");

        if (patToken == null || patToken.getIdToken() == null) {
            throw new UmaException("PAT cannot be null");
        }

        logger.trace("Validating RPT, resourceId: {}, scopeIds: {}, authorization: {}", resourceId, scopeIds,
                authorization);

        if (StringHelper.isNotEmpty(authorization) && authorization.startsWith("Bearer ")) {
            String rptToken = authorization.substring(7);

            RptIntrospectionResponse rptStatusResponse = getStatusResponse(patToken, rptToken);
            logger.debug("\n\n\n UmaService::validateRptToken() - rptStatusResponse = " + rptStatusResponse + "\n\n\n");
            logger.trace("RPT status response: {} ", rptStatusResponse);
            if ((rptStatusResponse == null) || !rptStatusResponse.getActive()) {
                logger.warn("Status response for RPT token: '{}' is invalid, will do a retry", rptToken);
            } else {
                boolean rptHasPermissions = isRptHasPermissions(rptStatusResponse);
                logger.debug(
                        "\n\n\n UmaService::validateRptToken() - rptHasPermissions = " + rptHasPermissions + "\n\n\n");
                if (rptHasPermissions) {
                    // Collect all scopes
                    List<String> returnScopeIds = new LinkedList<String>();
                    for (UmaPermission umaPermission : rptStatusResponse.getPermissions()) {
                        if (umaPermission.getScopes() != null) {
                            returnScopeIds.addAll(umaPermission.getScopes());
                        }
                    }

                    if (!returnScopeIds.containsAll(scopeIds)) {
                        logger.error("Insufficient scopes. RPT token: " + rptToken + " does not have required scope: "
                                + scopeIds + ", token scopes: " + returnScopeIds);
                        throw new UmaException("Status response for RPT token: '{}' not contains right permissions.");
                    }

                }
            }
        }

    }

    private RptIntrospectionResponse getStatusResponse(Token patToken, String rptToken) {
        String authorization = "Bearer " + patToken.getAccessToken();

        // Determine RPT token to status
        RptIntrospectionResponse rptStatusResponse = null;
        try {
            rptStatusResponse = this.umaRptIntrospectionService.requestRptStatus(authorization, rptToken, "");
        } catch (Exception ex) {
            logger.error("Failed to determine RPT status", ex);
            ex.printStackTrace();
        }

        // Validate RPT status response
        if ((rptStatusResponse == null) || !rptStatusResponse.getActive()) {
            return null;
        }

        return rptStatusResponse;
    }

    private boolean isRptHasPermissions(RptIntrospectionResponse umaRptStatusResponse) {
        return !((umaRptStatusResponse.getPermissions() == null) || umaRptStatusResponse.getPermissions().isEmpty());
    }

}
