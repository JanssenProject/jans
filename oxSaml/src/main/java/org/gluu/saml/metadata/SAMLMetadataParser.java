/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.saml.metadata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gluu.util.io.HTTPFileDownloader;
import org.xml.sax.SAXException;

/**
 * SAML metadata parser.
 *
 * @author Dmitry Ognyannikov
 */
public final class SAMLMetadataParser {
    private static final Log LOG = LogFactory.getLog(SAMLMetadataParser.class);

    private SAMLMetadataParser() { }

    public static List<String> getEntityIdFromMetadataFile(File metadataFile) {
        if (!metadataFile.isFile()) {
            return null;
        }
        EntityIDHandler handler = parseMetadata(metadataFile);

        List<String> entityIds = handler.getEntityIDs();

        if (entityIds == null || entityIds.isEmpty()) {
            LOG.error("Failed to find entityId in metadata file: " + metadataFile.getAbsolutePath());
        }

        return entityIds;
    }

    public static List<String> getSpEntityIdFromMetadataFile(File metadataFile) {
        EntityIDHandler handler = parseMetadata(metadataFile);

        List<String> entityIds = handler.getSpEntityIDs();

        if (entityIds == null || entityIds.isEmpty()) {
            LOG.error("Failed to find entityId in metadata file: " + metadataFile.getAbsolutePath());
        }

        return entityIds;
    }

    public static EntityIDHandler parseMetadata(File metadataFile) {
        if (!metadataFile.exists()) {
            LOG.error("Failed to get entityId from metadata file: " + metadataFile.getAbsolutePath());
            return null;
        }

        InputStream is = null;
        try {
            is = FileUtils.openInputStream(metadataFile);

            return parseMetadata(is);
        } catch (IOException ex) {
            LOG.error("Failed to read SAML metadata file: " + metadataFile.getAbsolutePath(), ex);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static EntityIDHandler parseMetadata(InputStream is) {
        InputStreamReader isr = null;
        EntityIDHandler handler = null;
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();

            handler = new EntityIDHandler();
            saxParser.parse(is, handler);
        } catch (IOException ex) {
            LOG.error("Failed to read SAML metadata", ex);
        } catch (ParserConfigurationException e) {
            LOG.error("Failed to confugure SAX parser", e);
        } catch (SAXException e) {
            LOG.error("Failed to parse SAML metadata", e);
        } finally {
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(is);
        }

        return handler;
    }

    public static EntityIDHandler parseMetadata(URL metadataURL) {
        String metadataFileContent = HTTPFileDownloader.getResource(metadataURL.toExternalForm(), "application/xml, text/xml", null, null);
        return parseMetadata(metadataFileContent);
    }

    public static EntityIDHandler parseMetadata(String metadata) {
        if (metadata == null) {
            return null;
        }

        InputStream is = new StringBufferInputStream(metadata);
        return parseMetadata(is);
    }
}
