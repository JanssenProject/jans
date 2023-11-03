/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.service;

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
import org.opensaml.xml.parse.XMLParserException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.xml.validation.Schema;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class SamlService {

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
            logger.error("Failed to load SAMLSchema - ", ex);
        }
    }

    public boolean isLocalDocumentStoreType() {
        return documentStoreService.getProviderType() == DocumentStoreType.LOCAL;
    }

    private String getTempMetadataFilename(String metadataFolder, String fileName) {
        logger.info("documentStoreService:{}, localDocumentStoreService:{}, metadataFolder:{}, fileName:{}",
                documentStoreService, localDocumentStoreService, metadataFolder, fileName);
        synchronized (SamlService.class) {
            String possibleTemp;
            do {
                possibleTemp = fileName + INumGenerator.generate(2);
                logger.debug("possibleTemp:{}", possibleTemp);
            } while (documentStoreService.hasDocument(metadataFolder + possibleTemp));
            return possibleTemp;
        }
    }

    public String saveMetadataFile(String module, String metadataTempFolder, String metadataFileName,
            InputStream stream) {
        logger.info("module:{}, metadataTempFolder:{}, metadataFileName:{}, stream:{}", module, metadataTempFolder,
                metadataFileName, stream);

        if (StringUtils.isBlank(metadataFileName)) {
            throw new InvalidConfigurationException("Cannot save metadata file as metadataFileName is null!");
        }

        if (stream == null) {
            throw new InvalidConfigurationException("Cannot save metadata file as file stream is null!");
        }

        String tempFileName = getTempMetadataFilename(metadataTempFolder, metadataFileName);
        logger.debug("metadataTempFolder:{}, tempFileName:{}", metadataTempFolder, tempFileName);

        String spMetadataFile = metadataTempFolder + tempFileName;
        logger.debug("documentStoreService:{}, spMetadataFile:{}, localDocumentStoreService:{} ", documentStoreService,
                spMetadataFile, localDocumentStoreService);
        try {
            boolean result = documentStoreService.saveDocumentStream(spMetadataFile, stream,
                    List.of("jans-server", module));
            logger.debug("SP File saving result:{}", result);

            InputStream newFile = documentStoreService.readDocumentAsStream(spMetadataFile);
            logger.debug("SP File read newFile:{}", newFile);

            if (result) {
                return tempFileName;
            }
        } catch (Exception ex) {
            logger.error("Failed to write SP metadata file '{}'", spMetadataFile, ex);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return null;
    }

    public GluuErrorHandler validateMetadata(String metadataPath)
            throws ParserConfigurationException, SAXException, IOException, XMLParserException {
        if (samlSchema == null) {
            final List<String> validationLog = new ArrayList<String>();
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
            return documentStoreService.renameDocument(metadataPath, destinationMetadataPath);
        } catch (Exception ex) {
            logger.error("Failed to rename metadata '{}' to '{}'", metadataPath, destinationMetadataPath, ex);
        }

        return false;
    }

}
