/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.saml.metadata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xdi.util.StringHelper;
import org.xdi.util.io.FileUploadWrapper;
import org.xdi.util.io.HTTPFileDownloader;
import org.xml.sax.SAXException;

/**
 * SAML metadata parser. 
 * 
 * @author Dmitry Ognyannikov
 */
public class SAMLMetadataParser {
    private static final Log log = LogFactory.getLog(SAMLMetadataParser.class);
    
    public static List<String> getEntityIdFromMetadataFile(File metadataFile) {
        if (!metadataFile.isFile()) {
            return null;
        }
        EntityIDHandler handler = parseMetadata(metadataFile);

        List<String> entityIds = handler.getEntityIDs();

        if (entityIds == null || entityIds.isEmpty()) {
            log.error("Failed to find entityId in metadata file: " + metadataFile.getAbsolutePath());
        }

        return entityIds;
    }

    public static List<String> getSpEntityIdFromMetadataFile(File metadataFile) {
        EntityIDHandler handler = parseMetadata(metadataFile);

        List<String> entityIds = handler.getSpEntityIDs();

        if (entityIds == null || entityIds.isEmpty()) {
            log.error("Failed to find entityId in metadata file: " + metadataFile.getAbsolutePath());
        }

        return entityIds;
    }

    public static EntityIDHandler parseMetadata(File metadataFile) {
        if (!metadataFile.exists()) {
            log.error("Failed to get entityId from metadata file: " + metadataFile.getAbsolutePath());
            return null;
        }

        InputStream is = null;
        InputStreamReader isr = null;
        EntityIDHandler handler = null;
        try {
            is = FileUtils.openInputStream(metadataFile);
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();

            handler = new EntityIDHandler();
            is = FileUtils.openInputStream(metadataFile);
            saxParser.parse(is, handler);

        } catch (IOException ex) {
            log.error("Failed to read metadata file: " + metadataFile.getAbsolutePath(), ex);
        } catch (ParserConfigurationException e) {
            log.error("Failed to confugure SAX parser for file: " + metadataFile.getAbsolutePath(), e);
        } catch (SAXException e) {
            log.error("Failed to parse file: " + metadataFile.getAbsolutePath(), e);
        } finally {
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(is);
        }

        return handler;
    }
    
    

//    public boolean saveMetadataFile(String spMetaDataURL, String metadataFileName) {
//        if (StringHelper.isEmpty(spMetaDataURL)) {
//            return false;
//        }
//
//        String metadataFileContent = HTTPFileDownloader.getResource(spMetaDataURL, "application/xml, text/xml", null, null);
//
//        if (StringHelper.isEmpty(metadataFileContent)) {
//            return false;
//        }
//
//        // Save new file
//        ByteArrayInputStream is;
//        try {
//            byte[] metadataFileContentBytes = metadataFileContent.getBytes("UTF-8");
//            is = new ByteArrayInputStream(metadataFileContentBytes);
//        } catch (UnsupportedEncodingException ex) {
//            return false;
//        }
//
//        FileUploadWrapper tmpfileWrapper = new FileUploadWrapper();
//        tmpfileWrapper.setStream(is);
//
//        return saveMetadataFile(metadataFileName, tmpfileWrapper.getStream());
//    }
}
