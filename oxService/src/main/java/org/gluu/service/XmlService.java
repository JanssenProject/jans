/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Service class to work with images in photo repository
 *
 * @author Yuriy Movchan Date: 01.11.2011
 */
@ApplicationScoped
public class XmlService {

    private static final long serialVersionUID = -4805285557592935972L;

    @Inject
    private Logger log;

    public Document getXmlDocument(byte[] xmlDocumentBytes) throws SAXException, IOException, ParserConfigurationException {
        return getXmlDocument(xmlDocumentBytes, false);
    }

    public Document getXmlDocument(byte[] xmlDocumentBytes, boolean skipValidation) throws SAXException, IOException, ParserConfigurationException {
        ByteArrayInputStream bis = new ByteArrayInputStream(xmlDocumentBytes);
        try {
            DocumentBuilderFactory fty = createDocumentBuilderFactory(skipValidation);

            return fty.newDocumentBuilder().parse(bis);
        } finally {
            IOUtils.closeQuietly(bis);
        }
    }

    public Document getXmlDocument(InputStream is) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory fty = createDocumentBuilderFactory();

        return fty.newDocumentBuilder().parse(is);
    }

    public Document getXmlDocument(InputSource is) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory fty = createDocumentBuilderFactory();

        return fty.newDocumentBuilder().parse(is);
    }

    public Document getXmlDocument(String uri) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory fty = createDocumentBuilderFactory();

        return fty.newDocumentBuilder().parse(uri);
    }

    private DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
        return createDocumentBuilderFactory(false);
    }

    private DocumentBuilderFactory createDocumentBuilderFactory(boolean skipValidation) throws ParserConfigurationException {
        DocumentBuilderFactory fty = DocumentBuilderFactory.newInstance();

        if (skipValidation) {
            return fty;
        }

        fty.setNamespaceAware(true);

        // Fix XXE vulnerability
        fty.setXIncludeAware(false);
        fty.setExpandEntityReferences(false);
        fty.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fty.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fty.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        return fty;
    }

    public String getNodeValue(Document xmlDocument, String xPathExpression, String attributeName) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression formXPathExpression = xPath.compile(xPathExpression);

        if (StringHelper.isEmpty(attributeName)) {
            String nodeValue = (String) formXPathExpression.evaluate(xmlDocument, XPathConstants.STRING);

            return nodeValue;
        }

        Node node = ((Node) formXPathExpression.evaluate(xmlDocument, XPathConstants.NODE));
        if (node == null) {
            return null;
        }

        Node attributeNode = node.getAttributes().getNamedItem(attributeName);
        if (attributeNode == null) {
            return null;
        }

        return attributeNode.getNodeValue();
    }

}
