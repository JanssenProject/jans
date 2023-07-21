/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.plugin.saml.model.TrustRelationship;
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
        if (StringUtils.isBlank(getIdpMetadataDir())) {
            throw new InvalidConfigurationException("Failed to return IDP Metadata file path as undefined!");
        }

        String idpMetadataFolder = getIdpMetadataDir();
        return idpMetadataFolder + spMetaDataFN;
    }

    public String getIdpMetadataDir() {
        if (StringUtils.isBlank(samlConfigService.getSelectedIdpConfigMetadataDir())) {
            throw new InvalidConfigurationException("Failed to return IDP Metadata file path as undefined!");
        }
        return samlConfigService.getSelectedIdpConfigMetadataDir() + File.separator;
    }

    public String getSpNewMetadataFileName(TrustRelationship trustRel) {
        return getSpNewMetadataFileName(trustRel.getInum());
    }

    public String getSpNewMetadataFileName(String inum) {
        String relationshipInum = StringHelper.removePunctuation(inum);
        return String.format(samlConfigService.getSpMetadataFilePattern(), relationshipInum);
    }

    public String getIdpMetadataTempDir() {
        if (StringUtils.isBlank(samlConfigService.getSelectedIdpConfigMetadataTempDir())) {
            throw new InvalidConfigurationException("Failed to return IDP Metadata Temp directory as undefined!");
        }

        return samlConfigService.getSelectedIdpConfigMetadataTempDir() + File.separator;
    }

    private String getTempMetadataFilename(String idpMetadataFolder, String fileName) {
        logger.info("documentStoreService:{}, localDocumentStoreService:{}, idpMetadataFolder:{}, fileName:{}",
                documentStoreService, localDocumentStoreService, idpMetadataFolder, fileName);
        synchronized (SamlIdpService.class) {
            String possibleTemp;
            do {
                possibleTemp = fileName + INumGenerator.generate(2);
                logger.debug("possibleTemp:{}", possibleTemp);
            } while (documentStoreService.hasDocument(idpMetadataFolder + possibleTemp));
            return possibleTemp;
        }
    }

    public String saveSpMetadataFile(String spMetadataFileName, InputStream stream) {
        logger.info("spMetadataFileName:{}, stream:{}", spMetadataFileName, stream);

        if (StringUtils.isBlank(samlConfigService.getSelectedIdpConfigRootDir())) {
            throw new InvalidConfigurationException("Failed to save SP meta-data file due to undefined!");
        }

        String idpMetadataTempFolder = getIdpMetadataTempDir();
        logger.debug("idpMetadataTempFolder:{}", idpMetadataTempFolder);
        
        String tempFileName = getTempMetadataFilename(idpMetadataTempFolder, spMetadataFileName);
        logger.debug("idpMetadataTempFolder:{}, tempFileName:{}", idpMetadataTempFolder, tempFileName);
        
        String spMetadataFile = idpMetadataTempFolder + tempFileName;
        logger.debug("documentStoreService:{}, spMetadataFile:{}, localDocumentStoreService:{} ", documentStoreService,
                spMetadataFile, localDocumentStoreService);
        try {
            boolean result = documentStoreService.saveDocumentStream(spMetadataFile, stream,
                    List.of("jans-server", samlConfigService.getSelectedIdpConfigID()));
            logger.debug("SP File saving result:{}", result);

            InputStream newFile = documentStoreService.readDocumentAsStream(spMetadataFile);
            logger.debug("SP File read newFile:{}", newFile);

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

}
