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
import io.jans.util.StringHelper;
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
            logger.error("Failed to load SAMLSchema - ", ex);
        }
    }

    public boolean isLocalDocumentStoreType() {
        return documentStoreService.getProviderType() == DocumentStoreType.LOCAL;
    }

    public String getSpMetadataFilePath(String metadataDir, String spMetaDataFN) {
        logger.debug("metadataDir:{}, spMetaDataFN:{}", metadataDir, spMetaDataFN);
        if (StringUtils.isBlank(getIdpMetadataDir(metadataDir))) {
            throw new InvalidConfigurationException("Failed to return IDP metadata file path as undefined!");
        }

        String idpMetadataFolder = getIdpMetadataDir(metadataDir);
        return idpMetadataFolder + spMetaDataFN;
    }

    public String getIdpMetadataDir(String metadataDir) {
        logger.debug("metadataDir:{}", metadataDir);
        if (StringUtils.isBlank(metadataDir)) {
            throw new InvalidConfigurationException("Failed to return IDP metadata file path as undefined!");
        }
        return metadataDir + File.separator;
    }

    public String getMetadataFile(String metadataFile) {
        logger.debug("metadataFile:{}", metadataFile);
        if (StringUtils.isBlank(metadataFile)) {
            throw new InvalidConfigurationException("Failed to return IDP SP metadata file name as undefined!");
        }
        return metadataFile;
    }

    public String getSpNewMetadataFileName(String inum) {
        logger.debug("inum:{}", inum);
        return getSpNewMetadataFileName(inum);
    }

    public String getSpNewMetadataFileName(String inum, String metadataFilePattern) {
        logger.debug("inum:{}, metadataFilePattern:{}", inum, metadataFilePattern);
        if (StringUtils.isBlank(inum) || StringUtils.isBlank(metadataFilePattern)) {
            throw new InvalidConfigurationException("Metadata file num or metadataFilePattern are undefined!");
        }
        String relationshipInum = StringHelper.removePunctuation(inum);
        return String.format(metadataFilePattern, relationshipInum);
    }

    public String getIdpMetadataTempDir(String metadataTempDir) {
        logger.debug("metadataTempDir:{}", metadataTempDir);
        if (StringUtils.isBlank(metadataTempDir)) {
            throw new InvalidConfigurationException("Failed to return IDP metadata Temp directory as undefined!");
        }

        return (metadataTempDir + File.separator);
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

    public String saveSpMetadataFile(String rootDir, String metadataTempDir, String spMetadataFileName, InputStream stream) {
        logger.info("rootDir:{}, spMetadataFileName:{}, stream:{}", rootDir, spMetadataFileName, stream);

        if (StringUtils.isBlank(rootDir)) {
            throw new InvalidConfigurationException("Failed to save SP metadata file due to undefined!");
        }

        String idpMetadataTempFolder = getIdpMetadataTempDir(metadataTempDir);
        logger.debug("idpMetadataTempFolder:{}", idpMetadataTempFolder);

        String tempFileName = getTempMetadataFilename(idpMetadataTempFolder, spMetadataFileName);
        logger.debug("idpMetadataTempFolder:{}, tempFileName:{}", idpMetadataTempFolder, tempFileName);

        String spMetadataFile = idpMetadataTempFolder + tempFileName;
        logger.debug("documentStoreService:{}, spMetadataFile:{}, localDocumentStoreService:{} ", documentStoreService,
                spMetadataFile, localDocumentStoreService);
        try {
            boolean result = documentStoreService.saveDocumentStream(spMetadataFile, stream,
                    List.of("jans-server", "KC-IDP"));
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
        logger.debug("Rename metadata file documentStoreService:{},metadataPath:{}, destinationMetadataPath:{}", documentStoreService, metadataPath, destinationMetadataPath);
        try {
            return documentStoreService.renameDocument(metadataPath, destinationMetadataPath);
        } catch (Exception ex) {
            logger.error("Failed to rename metadata '{}' to '{}'", metadataPath, destinationMetadataPath, ex);
        }

        return false;
    }

}
