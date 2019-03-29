/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Validate SAML metadata XML with predefined settings.
 *
 * @author Dmitry Ognyannikov
 */
public final class XMLValidator {

    private XMLValidator() { }

    /**
     * @param stream
     * @param validationSchema
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @return GluuErrorHandler
     */
    public static GluuErrorHandler validateMetadata(InputStream stream, Schema validationSchema)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory newFactory = DocumentBuilderFactory.newInstance();

        // Fix XXE vulnerability
        newFactory.setXIncludeAware(false);
        newFactory.setExpandEntityReferences(false);
        newFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        newFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        newFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        newFactory.setCoalescing(false);
        newFactory.setExpandEntityReferences(true);
        newFactory.setIgnoringComments(false);

        newFactory.setIgnoringElementContentWhitespace(false);
        newFactory.setNamespaceAware(true);
        newFactory.setValidating(false);
        DocumentBuilder xmlParser = newFactory.newDocumentBuilder();
        Document xmlDoc = xmlParser.parse(stream);
        Validator validator = validationSchema.newValidator();
        GluuErrorHandler handler = new GluuErrorHandler();
        validator.setErrorHandler(handler);
        validator.validate(new DOMSource(xmlDoc));

        return handler;
    }
}
