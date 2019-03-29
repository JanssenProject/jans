/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.gluu.model.GluuImage;
import org.gluu.model.TrustContact;
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
@Named
public class XmlService implements Serializable {

    private static final long serialVersionUID = -4805285557592935972L;

    @Inject
    private Logger log;

    private JAXBContext jaxbContext;
    private Marshaller jaxbMarshaller;
    private Unmarshaller jaxbUnmarshaller;

    @PostConstruct
    public void init() {
        try {
            this.jaxbContext = JAXBContext.newInstance(GluuImage.class, TrustContact.class);
            this.jaxbMarshaller = this.jaxbContext.createMarshaller();
            this.jaxbUnmarshaller = this.jaxbContext.createUnmarshaller();
        } catch (JAXBException ex) {
            log.error("Failed to create JAXB marshaller and unmarshaller", ex);
        }
    }

    public String getXMLFromGluuImage(GluuImage photo) {
        if (photo == null) {
            return null;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            this.jaxbMarshaller.marshal(photo, bos);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception ex) {
            log.error("Failed to convert GluuImage {} to XML", ex, photo);
        }

        return null;
    }

    public GluuImage getGluuImageFromXML(String xml) {
        if (xml == null) {
            return null;
        }

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            return (GluuImage) this.jaxbUnmarshaller.unmarshal(bis);
        } catch (Exception ex) {
            log.error("Failed to create GluuImage from XML {}", ex, xml);
        }

        return null;
    }

    public TrustContact getTrustContactFromXML(String xml) {
        if (xml == null) {
            return null;
        }

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            return (TrustContact) this.jaxbUnmarshaller.unmarshal(bis);
        } catch (Exception ex) {
            log.error("Failed to create TrustContact from XML {}", ex, xml);
        }

        return null;
    }

    // public String
    // getXMLFromDeconstructedTrustRelationship(DeconstructedTrustRelationship
    // deconstructedTR) {
    // if (deconstructedTR == null) {
    // return null;
    // }
    //
    // ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // try {
    // this.jaxbMarshaller.marshal(deconstructedTR, bos);
    // return new String(bos.toByteArray(), "UTF-8");
    // } catch (Exception ex) {
    // log.error("Failed to convert DeconstructedTrustRelationship {} to XML",
    // ex, deconstructedTR);
    // }
    //
    // return null;
    // }
    //
    // public DeconstructedTrustRelationship
    // getDeconstructedTrustRelationshipFromXML(String xml) {
    // if (xml == null) {
    // return null;
    // }
    //
    // try {
    // ByteArrayInputStream bis = new
    // ByteArrayInputStream(xml.getBytes("UTF-8"));
    // return (DeconstructedTrustRelationship)
    // this.jaxbUnmarshaller.unmarshal(bis);
    // } catch (Exception ex) {
    // log.error("Failed to create DeconstructedTrustRelationship from XML {}",
    // ex, xml);
    // }
    //
    // return null;
    // }

    public String getXMLFromTrustContact(TrustContact contact) {
        if (contact == null) {
            return null;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            this.jaxbMarshaller.marshal(contact, bos);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception ex) {
            log.error("Failed to convert TrustContact {} to XML", ex, contact);
        }

        return null;
    }

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
