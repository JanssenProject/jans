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
    SamlConfigService samlConfigService;

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
            logger.debug("samlSchema", samlSchema);
        } catch (Exception ex) {
            logger.error("Failed to load SAMLSchema - ", ex);
        }
    }

    public boolean isLocalDocumentStoreType() {
        return documentStoreService.getProviderType() == DocumentStoreType.LOCAL;
    }

    public String getSpMetadataFilePath(String spMetaDataFN) {
        if (StringUtils.isBlank(getIdpMetadataDir())) {
            throw new InvalidConfigurationException("Failed to return IDP metadata file path as undefined!");
        }

        String idpMetadataFolder = getIdpMetadataDir();
        return idpMetadataFolder + spMetaDataFN;
    }

    public String getIdpMetadataDir() {
        if (StringUtils.isBlank(samlConfigService.getSelectedIdpConfigMetadataDir())) {
            throw new InvalidConfigurationException("Failed to return IDP metadata file path as undefined!");
        }
        return samlConfigService.getSelectedIdpConfigMetadataDir() + File.separator;
    }

    public String getSpMetadataFile() {
        if (StringUtils.isBlank(samlConfigService.getSpMetadataFile())) {
            throw new InvalidConfigurationException("Failed to return IDP SP metadata file name as undefined!");
        }
        return samlConfigService.getSpMetadataFile();
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
            throw new InvalidConfigurationException("Failed to return IDP metadata Temp directory as undefined!");
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
            throw new InvalidConfigurationException("Failed to save SP metadata file due to undefined!");
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
