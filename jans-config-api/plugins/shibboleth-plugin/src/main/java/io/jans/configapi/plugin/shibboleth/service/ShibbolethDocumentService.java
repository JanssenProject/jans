/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth.service;

import io.jans.service.document.store.service.DocumentStoreService;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.service.LocalDocumentStoreService;
import io.jans.util.exception.InvalidConfigurationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ShibbolethDocumentService {

    @Inject
    Logger logger;
    @Inject
    private DocumentStoreService documentStoreService;

    @Inject
    private LocalDocumentStoreService localDocumentStoreService;

    public boolean isLocalDocumentStoreType() {
        return documentStoreService.getProviderType() == DocumentStoreType.LOCAL;
    }

    public String saveMetadataFile(String metadataDir, String metadataFileName, String documentStoreModuleName,
            InputStream stream) {
        logger.info("metadataDir:{}, metadataFileName:{}, documentStoreModuleName:{}, stream:{}", metadataDir,
                metadataFileName, documentStoreModuleName, stream);

        if (StringUtils.isBlank(metadataDir)) {
            throw new InvalidConfigurationException("Failed to save file as metadata directory provided is null!");
        }

        if (StringUtils.isBlank(metadataFileName)) {
            throw new InvalidConfigurationException("Failed to save file as metadataFileName is null!");
        }

        if (stream == null) {
            throw new InvalidConfigurationException("Failed to save metadat as file is null!");
        }

        if (StringUtils.isBlank(documentStoreModuleName)) {
            documentStoreModuleName = "Shibboleth";
        }

        String metadataFile = metadataDir + File.separator + metadataFileName;
        logger.info("documentStoreService:{}, metadataFile:{}, localDocumentStoreService:{} ", documentStoreService,
                metadataFile, localDocumentStoreService);
        try {
            String result = documentStoreService.saveDocumentStream(metadataFile, null, stream,
                    documentStoreModuleName);
            logger.info("Shibboleth file saving result:{}", result);

            InputStream newFile = documentStoreService.readDocumentAsStream(metadataFile);
            logger.info("Shibboleth file read newFile:{}", newFile);

            if (result != null) {
                return metadataFile;
            }
        } catch (Exception ex) {
            logger.error("Failed to write Shibboleth metadata file '{}'", metadataFile, ex);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return null;
    }

    public InputStream getFileFromDocumentStore(String path) {

        logger.debug("Get file from DocumentStore. Path: {}", path);
        try {
            return documentStoreService.readDocumentAsStream(path);
        } catch (Exception e) {
            logger.error("Failed to get file '{}' from DocumentStore", path);
            return null;
        }
    }

}
