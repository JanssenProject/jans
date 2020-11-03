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
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.auth.AuthClientFactory;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.util.init.Initializable;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

@ApplicationScoped
public class UmaClientService extends Initializable implements Serializable {

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
            this.umaMetadata = loadUmaMetadata();
            this.umaPermissionService = AuthClientFactory.getUmaPermissionService(this.umaMetadata, false);
            this.umaRptIntrospectionService = AuthClientFactory.getUmaRptIntrospectionService(this.umaMetadata, false);
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to load oxAuth UMA configuration", ex);
        }
    }

    private UmaMetadata loadUmaMetadata() throws OxIntializationException {
        UmaMetadataService umaMetadataService = AuthClientFactory.getUmaMetadataService(configurationService.find().getUmaConfigurationEndpoint(), false);

        UmaMetadata umaMetadata = umaMetadataService.getMetadata();
        logger.debug("umaMetadata = " + umaMetadata);

        if (umaMetadata == null) {
            throw new OxIntializationException("UMA meta data configuration is invalid!");
        }

        return umaMetadata;
    }

    public RptIntrospectionResponse introspectRpt(Token patToken, String rptToken) {
        try {
            return umaRptIntrospectionService.requestRptStatus("Bearer " + patToken.getAccessToken(), rptToken, "");
        } catch (Exception ex) {
            logger.error("Failed to determine RPT status", ex);
            return null;
        }
    }

    public UmaMetadata getUmaMetadata() {
        return umaMetadata;
    }
}
