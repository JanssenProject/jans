/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service;

import org.apache.commons.io.IOUtils;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xdi.model.GluuImage;
import org.xdi.model.TrustContact;
import org.xdi.util.StringHelper;
import org.xml.sax.SAXException;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service class to work with images in photo repository
 *
 * @author Yuriy Movchan Date: 01.11.2011
 */
@Name("xmlService")
@Scope(ScopeType.APPLICATION)
@AutoCreate
public class XmlService {

    @Logger
    private Log log;

    private JAXBContext jaxbContext;
    private Marshaller jaxbMarshaller;
    private Unmarshaller jaxbUnmarshaller;

    @Create
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
            log.error("Failed to convert GluuImage {0} to XML", ex, photo);
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
            log.error("Failed to create GluuImage from XML {0}", ex, xml);
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
            log.error("Failed to create TrustContact from XML {0}", ex, xml);
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
    // log.error("Failed to convert DeconstructedTrustRelationship {0} to XML",
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
    // log.error("Failed to create DeconstructedTrustRelationship from XML {0}",
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
            log.error("Failed to convert TrustContact {0} to XML", ex, contact);
        }

        return null;
    }

    public Document getXmlDocument(byte[] xmlDocumentBytes) throws SAXException, IOException, ParserConfigurationException {
    	ByteArrayInputStream bis = new ByteArrayInputStream(xmlDocumentBytes);
    	try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bis);
    	} finally {
    		IOUtils.closeQuietly(bis);
    	}
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

    /**
     * Get xmlService instance
     *
     * @return XmlService instance
     */
    public static XmlService instance() {
        return (XmlService) Component.getInstance(XmlService.class);
    }

}
