/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.saml.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.gluu.service.document.store.service.DocumentStoreService;
import org.gluu.util.io.HTTPFileDownloader;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

/**
 * SAML metadata parser.
 *
 * @author Dmitry Ognyannikov
 */

@ApplicationScoped
public class SAMLMetadataParser {

	@Inject
	private Logger log;

	@Inject
	private DocumentStoreService documentStoreService;

    public List<String> getEntityIdFromMetadataFile(String metadataFile) {
        if (!documentStoreService.hasDocument(metadataFile)) {
            return null;
        }

        EntityIDHandler handler = parseMetadata(metadataFile);
        if(handler!=null){
            List<String> entityIds = handler.getEntityIDs();
            if (entityIds == null || entityIds.isEmpty()) {
                log.error("Failed to find entityId in metadata file: " + metadataFile);
            }
            return entityIds;
        }else{
           return null;
        }
    }

    public List<String> getSpEntityIdFromMetadataFile(String metadataFile) {
        EntityIDHandler handler = parseMetadata(metadataFile);
        if(handler!=null){
            List<String> entityIds = handler.getSpEntityIDs();

            if (entityIds == null || entityIds.isEmpty()) {
            	log.error("Failed to find entityId in metadata file: " + metadataFile);
            }

            return entityIds;
        }else {
            return null;
        }

    }

    public EntityIDHandler parseMetadata(String metadataFile) {
        if (!documentStoreService.hasDocument(metadataFile)) {
            log.error("Failed to get entityId from metadata file: " + metadataFile);
            return null;
        }

        InputStream is = null;
        try {
            is = documentStoreService.readDocumentAsStream(metadataFile);

            return parseMetadata(is);
        } catch (IOException ex) {
            log.error("Failed to read SAML metadata file: " + metadataFile, ex);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public EntityIDHandler parseMetadata(InputStream is) {
        InputStreamReader isr = null;
        EntityIDHandler handler = null;
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            handler = new EntityIDHandler();
            saxParser.parse(is, handler);
        } catch (IOException ex) {
            log.error("Failed to read SAML metadata", ex);
        } catch (ParserConfigurationException e) {
            log.error("Failed to confugure SAX parser", e);
        } catch (SAXException e) {
            log.error("Failed to parse SAML metadata", e);
        } finally {
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(is);
        }


        return handler;
    }

    public EntityIDHandler parseMetadata(URL metadataURL) {
        String metadataFileContent = HTTPFileDownloader.getResource(metadataURL.toExternalForm(), "application/xml, text/xml", null, null);

        if (metadataFileContent == null) {
            return null;
        }

        InputStream is = new StringBufferInputStream(metadataFileContent);

        return parseMetadata(is);
    }

}

