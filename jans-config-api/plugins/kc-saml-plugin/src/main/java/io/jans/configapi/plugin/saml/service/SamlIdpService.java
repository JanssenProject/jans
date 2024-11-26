/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.service.document.store.service.DocumentStoreService;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.service.LocalDocumentStoreService;
import io.jans.util.exception.InvalidConfigurationException;
import io.jans.util.INumGenerator;
import io.jans.xml.GluuErrorHandler;
import io.jans.xml.XMLValidator;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.opensaml.saml.common.xml.SAMLSchemaBuilder;
import org.opensaml.saml.common.xml.SAMLSchemaBuilder.SAML1Version;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.xml.validation.Schema;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class SamlIdpService {

    @Inject
    Logger logger;
    @Inject
    private DocumentStoreService documentStoreService;

    @Inject
    private LocalDocumentStoreService localDocumentStoreService;

    private Schema samlSchema;

    @PostConstruct
    public void create() {
        SAMLSchemaBuilder samlSchemaBuilder = new SAMLSchemaBuilder(SAML1Version.SAML_11);
        try {
            this.samlSchema = samlSchemaBuilder.getSAMLSchema();
            logger.debug("samlSchema:{}", samlSchema);
        } catch (Exception ex) {
            logger.warn("Failed to load SAMLSchema - ", ex);
        }
    }

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
            documentStoreModuleName = "SAML";
        }

        String metadataFile = metadataDir + File.separator + metadataFileName;
        logger.info("documentStoreService:{}, metadataFile:{}, localDocumentStoreService:{} ", documentStoreService,
                metadataFile, localDocumentStoreService);
        try {
            String result = documentStoreService.saveDocumentStream(metadataFile, null,
                    stream, documentStoreModuleName);
            logger.info("SAML file saving result:{}", result);

            InputStream newFile = documentStoreService.readDocumentAsStream(metadataFile);
            logger.info("SAML file read newFile:{}", newFile);

            if (result != null) {
                return metadataFile;
            }
        } catch (Exception ex) {
            logger.error("Failed to write SAML metadata file '{}'", metadataFile, ex);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return null;
    }

    public GluuErrorHandler validateMetadata(String metadataPath)
            throws ParserConfigurationException, SAXException, IOException {
        if (samlSchema == null) {
            final List<String> validationLog = new ArrayList<>();
            validationLog.add(GluuErrorHandler.SCHEMA_CREATING_ERROR_MESSAGE);
            validationLog.add("Failed to load SAML schema");
            return new GluuErrorHandler(false, true, validationLog);
        }

        try (InputStream stream = documentStoreService.readDocumentAsStream(metadataPath)) {
            return XMLValidator.validateMetadata(stream, samlSchema);
        }
    }

    public boolean renameMetadata(String metadataPath, String destinationMetadataPath) {
        logger.debug("Rename metadata file documentStoreService:{},metadataPath:{}, destinationMetadataPath:{}",
                documentStoreService, metadataPath, destinationMetadataPath);
        try {
            return documentStoreService.renameDocument(metadataPath, destinationMetadataPath) != null;
        } catch (Exception ex) {
            logger.error("Failed to rename metadata '{}' to '{}'", metadataPath, destinationMetadataPath, ex);
        }

        return false;
    }

    public InputStream getFileFromDocumentStore(String path) {

        logger.debug("Get file from DocumentStore. Path: {}",path);
        try {
            return documentStoreService.readDocumentAsStream(path);
        }catch(Exception e) {
            logger.error("Failed to get file '{}' from DocumentStore",path);
            return null;
        }
    }

    private String getTempMetadataFilename(String metadataFolder, String fileName) {
        logger.info("documentStoreService:{}, localDocumentStoreService:{}, metadataFolder:{}, fileName:{}",
                documentStoreService, localDocumentStoreService, metadataFolder, fileName);
        synchronized (SamlIdpService.class) {
            String possibleTemp;
            do {
                possibleTemp = fileName + INumGenerator.generate(2);
                logger.debug("possibleTemp:{}", possibleTemp);
            } while (documentStoreService.hasDocument(metadataFolder + possibleTemp));
            return possibleTemp;
        }
    }

}
