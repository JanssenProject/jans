/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.saml.model.config.KeycloakConfig;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidConfigurationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class SamlIdpService {

    String idpRootDir = "/opt/saml/idp";
    String idpMetadataTempDir = "/opt/saml/idp/metadata/temp";
    
    @Inject
    Logger logger;

    public String saveSpMetadataFile(String spMetadataFileName, InputStream stream) {
       // if (appConfiguration.getShibboleth3IdpRootDir() == null) {
     /*   if(StringUtils.isBlank(samlIdpRootDir)) {
            throw new InvalidConfigurationException(
                    "Failed to save SP meta-data file due to undefined IDP root folder");
        }

        String idpMetadataTempFolder = getIdpMetadataTempDir();
        String tempFileName = getTempMetadataFilename(idpMetadataTempFolder, spMetadataFileName);
        String spMetadataFile = idpMetadataTempFolder + tempFileName;
        try {
            boolean result = documentStoreService.saveDocumentStream(spMetadataFile, stream, List.of("oxtrust-server","Shibboleth"));
            if (result) {
                return tempFileName;
            }
        } catch (Exception ex) {
            log.error("Failed to write SP meta-data file '{}'", spMetadataFile, ex);
        } finally {
            IOUtils.closeQuietly(stream);
        }
*/
        return null;
    }

}

