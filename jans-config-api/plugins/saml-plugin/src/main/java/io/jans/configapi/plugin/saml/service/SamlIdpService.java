/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.configapi.plugin.saml.service.SamlConfigService;
import io.jans.configapi.plugin.saml.model.config.SamlAppConfiguration;
import io.jans.configapi.plugin.saml.model.config.SamlConf;
import io.jans.service.document.store.service.DocumentStoreService;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.service.LocalDocumentStoreService;
import io.jans.util.exception.InvalidConfigurationException;
import io.jans.util.StringHelper;
import io.jans.util.INumGenerator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class SamlIdpService {

    private final String IDP_ROOT_DIR = getIdpRoot();
    private final String KEYCLOAK_IDP_ROOT_DIR = IDP_ROOT_DIR + "/keycloak";
    private final String KEYCLOAK_IDP_METADATA_DIR = "metadatafile";
    private final String KEYCLOAK_IDP_METADATA_TEMP_DIR = "temp_metadata";
    private final String SAML_SP_METADATA_FILE_PATTERN = "%s-sp-metadata.xml";

    @Inject
    Logger logger;
    
    @Inject 
    SamlConfigService samlConfigService;

    @Inject
    private DocumentStoreService documentStoreService;

    @Inject
    private LocalDocumentStoreService localDocumentStoreService;

    public boolean isLocalDocumentStoreType() {

        return documentStoreService.getProviderType() == DocumentStoreType.LOCAL;
    }

    public String getSpMetadataFilePath(String spMetaDataFN) {
        // if (appConfiguration.getShibboleth3IdpRootDir() == null) {
        if (StringUtils.isBlank(KEYCLOAK_IDP_ROOT_DIR)) {
            throw new InvalidConfigurationException(
                    "Failed to return SP meta-data file due to undefined IDP root folder");
        }

        String idpMetadataFolder = getIdpMetadataDir();
        return idpMetadataFolder + spMetaDataFN;
    }

    public String getIdpMetadataDir() {
        return KEYCLOAK_IDP_ROOT_DIR + File.separator + KEYCLOAK_IDP_METADATA_DIR + File.separator;
    }

    public String getSpNewMetadataFileName(TrustRelationship trustRel) {
        return getSpNewMetadataFileName(trustRel.getInum());
    }

    public String getSpNewMetadataFileName(String inum) {
        String relationshipInum = StringHelper.removePunctuation(inum);
        return String.format(SAML_SP_METADATA_FILE_PATTERN, relationshipInum);
    }

    public String getIdpMetadataTempDir() {
        return KEYCLOAK_IDP_ROOT_DIR + File.separator + KEYCLOAK_IDP_METADATA_TEMP_DIR + File.separator;
    }

    private String getTempMetadataFilename(String idpMetadataFolder, String fileName) {
        logger.error("documentStoreService:{}, localDocumentStoreService:{}, idpMetadataFolder:{}, fileName:{}",documentStoreService, localDocumentStoreService, idpMetadataFolder, fileName);
        synchronized (SamlIdpService.class) {
            String possibleTemp;
            do {
                possibleTemp = fileName + INumGenerator.generate(2);
                logger.error("possibleTemp:{}",possibleTemp);
            } while (documentStoreService.hasDocument(idpMetadataFolder + possibleTemp));
            return possibleTemp;
        }
    }

    public String saveSpMetadataFile(String spMetadataFileName, InputStream stream) {
        logger.error("spMetadataFileName:{}, stream:{}",spMetadataFileName, stream);
        // if (appConfiguration.getShibboleth3IdpRootDir() == null) {
        if (StringUtils.isBlank(KEYCLOAK_IDP_ROOT_DIR)) {
            throw new InvalidConfigurationException(
                    "Failed to save SP meta-data file due to undefined IDP root folder");
        }

        String idpMetadataTempFolder = getIdpMetadataTempDir();
        logger.error("idpMetadataTempFolder:{}",idpMetadataTempFolder);
        String tempFileName = getTempMetadataFilename(idpMetadataTempFolder, spMetadataFileName);
        logger.error("idpMetadataTempFolder:{}, tempFileName:{}",idpMetadataTempFolder, tempFileName);
        String spMetadataFile = idpMetadataTempFolder + tempFileName;
        logger.error("documentStoreService:{}, spMetadataFile:{}, localDocumentStoreService:{} ",documentStoreService, spMetadataFile, localDocumentStoreService);
        try {
            boolean result = documentStoreService.saveDocumentStream(spMetadataFile, stream,
                    List.of("jans-server", "Keycloak"));
            logger.error("SP File saving result:{}",result);
            
            InputStream newFile = documentStoreService.readDocumentAsStream(spMetadataFile);      
            logger.error("SP File read newFile:{}",newFile);
                     
            if (result) {
                return tempFileName;
            }
        } catch (Exception ex) {
            logger.error("Failed to write SP meta-data file '{}'", spMetadataFile, ex);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return null;
    }
    
    private SamlAppConfiguration find() {
        SamlAppConfiguration samlAppConfiguration = samlConfigService.find();
        logger.error("  samlAppConfiguration():{}", samlAppConfiguration);
        return samlAppConfiguration;
    }
    
    private String getIdpRoot() {
        SamlAppConfiguration samlAppConfiguration = samlConfigService.find();
        String idpRoot = null;
        if(samlAppConfiguration==null) {
            return idpRoot;
        }
        return samlAppConfiguration.getIdpRootDir();
    }

}
