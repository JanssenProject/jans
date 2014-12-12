/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.saml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xdi.zip.CompressionHelper;

/**
 * Preapres SAML request
 * 
 * @author Yuriy Movchan Date: 24/04/2014
 */
public class AuthRequest {

	private static final Logger log = Logger.getLogger(AuthRequest.class);
	private static final SimpleDateFormat simpleDataFormat = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss");

	static {
		simpleDataFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private String id;
	private String issueInstant;
	private SamlConfiguration samlSettings;

	public AuthRequest(SamlConfiguration samlConfiguration) {
		this.samlSettings = samlConfiguration;
		this.id = "_" + UUID.randomUUID().toString();
		this.issueInstant = simpleDataFormat.format(new Date());
	}

	public String getRequest(boolean useBase64, String assertionConsumerServiceUrl) throws ParserConfigurationException, XMLStreamException, IOException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
 
		Document doc = docBuilder.newDocument();

		// Add AuthnRequest
		Element authnRequestElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "samlp:AuthnRequest");
		authnRequestElement.setAttribute("ID", this.id);
		authnRequestElement.setAttribute("Version", "2.0");
		authnRequestElement.setAttribute("IssueInstant", this.issueInstant);
		authnRequestElement.setAttribute("ProtocolBinding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
		authnRequestElement.setAttribute("AssertionConsumerServiceURL", assertionConsumerServiceUrl);

		doc.appendChild(authnRequestElement);

		// Add AuthnRequest
		Element issuerElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml:Issuer");
		issuerElement.appendChild(doc.createTextNode(this.samlSettings.getIssuer()));
		
		authnRequestElement.appendChild(issuerElement);

		// Add NameIDPolicy
		Element nameIDPolicyElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "samlp:NameIDPolicy");
		nameIDPolicyElement.setAttribute("Format", this.samlSettings.getNameIdentifierFormat());
		nameIDPolicyElement.setAttribute("AllowCreate", "true");

		authnRequestElement.appendChild(nameIDPolicyElement);
		
		if (this.samlSettings.isUseRequestedAuthnContext()) {
			// Add RequestedAuthnContext
			Element requestedAuthnContextElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "samlp:RequestedAuthnContext");
			requestedAuthnContextElement.setAttribute("Comparison", "exact");
	
			authnRequestElement.appendChild(requestedAuthnContextElement);
	
			// Add AuthnContextClassRef
			Element authnContextClassRefElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml:AuthnContextClassRef");
			authnContextClassRefElement.appendChild(doc.createTextNode("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"));
	
			requestedAuthnContextElement.appendChild(authnContextClassRefElement);
		}

		// Convert the content into xml
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		DOMSource source = new DOMSource(doc);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(baos);
		
 		transformer.transform(source, result);
		

		if (log.isDebugEnabled()) {
			log.debug("Genereated Saml Request " + new String(baos.toByteArray(), "UTF-8"));
		}

		if (useBase64) {
			byte[] deflated = CompressionHelper.deflate(baos.toByteArray(), true);
			String base64 = Base64.encodeBase64String(deflated);
			String encoded = URLEncoder.encode(base64, "UTF-8");

			return encoded;
		}

		return new String(baos.toByteArray(), "UTF-8");
	}

	public String getRequest(boolean useBase64) throws ParserConfigurationException, XMLStreamException, IOException, TransformerException {
		return getRequest(useBase64, this.samlSettings.getAssertionConsumerServiceUrl());
    }
																																												
	public String getStreamedRequest(boolean useBase64) throws XMLStreamException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(baos);

		writer.writeStartElement("samlp", "AuthnRequest", "urn:oasis:names:tc:SAML:2.0:protocol");
		writer.writeNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");

		writer.writeAttribute("ID", id);
		writer.writeAttribute("Version", "2.0");
		writer.writeAttribute("IssueInstant", this.issueInstant);
		writer.writeAttribute("ProtocolBinding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
		writer.writeAttribute("AssertionConsumerServiceURL", this.samlSettings.getAssertionConsumerServiceUrl());

		writer.writeStartElement("saml", "Issuer", "urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeCharacters(this.samlSettings.getIssuer());
		writer.writeEndElement();

		writer.writeStartElement("samlp", "NameIDPolicy", "urn:oasis:names:tc:SAML:2.0:protocol");
		writer.writeNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");

		writer.writeAttribute("Format", this.samlSettings.getNameIdentifierFormat());
		writer.writeAttribute("AllowCreate", "true");
		writer.writeEndElement();

		writer.writeStartElement("samlp", "RequestedAuthnContext", "urn:oasis:names:tc:SAML:2.0:protocol");
		writer.writeNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");

		writer.writeAttribute("Comparison", "exact");

		writer.writeStartElement("saml", "AuthnContextClassRef", "urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeCharacters("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
		writer.writeEndElement();

		writer.writeEndElement();

		writer.writeEndElement();
		writer.flush();

		if (log.isDebugEnabled()) {
			log.debug("Genereated Saml Request " + new String(baos.toByteArray(), "UTF-8"));
		}

		if (useBase64) {
			byte[] deflated = CompressionHelper.deflate(baos.toByteArray(), true);
			String base64 = Base64.encodeBase64String(deflated);
			String encoded = URLEncoder.encode(base64, "UTF-8");

			return encoded;
		}

		return new String(baos.toByteArray(), "UTF-8");
	}

}
