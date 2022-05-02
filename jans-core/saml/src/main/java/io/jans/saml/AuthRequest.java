/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.saml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import jakarta.xml.bind.DatatypeConverter;
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

import io.jans.zip.CompressionHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.SecurityConfiguration;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Preapres SAML request
 *
 * @author Yuriy Movchan Date: 24/04/2014
 */
public class AuthRequest {

	private static final Logger LOG = LoggerFactory.getLogger(AuthRequest.class);
    private static final SimpleDateFormat SIMPLE_DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss");

    static {
        SIMPLE_DATA_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private String id;
    private String issueInstant;
    private SamlConfiguration samlSettings;

    public AuthRequest(SamlConfiguration samlConfiguration) {
        this.samlSettings = samlConfiguration;
        this.id = "_" + UUID.randomUUID().toString();
        this.issueInstant = SIMPLE_DATA_FORMAT.format(new Date());
    }

    public String getRequest(boolean useBase64, String assertionConsumerServiceUrl)
            throws ParserConfigurationException, XMLStreamException, IOException, TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();

        // Add AuthnRequest
        Element authnRequestElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "samlp:AuthnRequest");
        authnRequestElement.setAttribute("ID", this.id);
        authnRequestElement.setAttribute("Version", "2.0");
        authnRequestElement.setAttribute("IssueInstant", this.issueInstant);
        authnRequestElement.setAttribute("ProtocolBinding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        authnRequestElement.setAttribute("Destination", this.samlSettings.getIdpSsoTargetUrl());
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("Genereated Saml Request " + new String(baos.toByteArray(), "UTF-8"));
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("Genereated Saml Request " + new String(baos.toByteArray(), "UTF-8"));
        }

        if (useBase64) {
            byte[] deflated = CompressionHelper.deflate(baos.toByteArray(), true);
            String base64 = Base64.encodeBase64String(deflated);
            String encoded = URLEncoder.encode(base64, "UTF-8");

            return encoded;
        }

        return new String(baos.toByteArray(), "UTF-8");
    }

    /**
     * This will generate the proper Redirect Query String as input for signing
     *
     * @param samlRequest
     * @param relayState
     *            Optional
     * @return
     * @throws Exception
     */
    private String generateQueryString(String samlRequest, String relayState) throws Exception {
        if (null == samlRequest || null == this.samlSettings.getSigAlgUrl()) {
            throw new Exception("SAMLRequest or sigAlgUrl cannot be null");
        }

        StringBuilder buf = new StringBuilder();
        buf.append("SAMLRequest=").append(samlRequest);
        if (null != relayState && 0 < relayState.length()) {
            buf.append("&RelayState=").append(URLEncoder.encode(relayState, "UTF-8"));
        }
        buf.append("&SigAlg=").append(URLEncoder.encode(this.samlSettings.getSigAlgUrl(), "UTF-8").trim());

        String bf = buf.toString();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated Query: " + bf);
        }

        return bf;
    }

    /**
     * This will return just the signature from the query string based on SAML
     * Redirect signature requirment.
     *
     * @param samlRequest
     * @param relayState
     *            optional
     * @return
     * @throws Exception
     */
    public String signRequest(String samlRequest, String relayState) throws Exception {

        String queryString = generateQueryString(samlRequest, relayState);
        if (null != queryString && 0 < queryString.length()) {
            // text to bytes
            byte[] data = queryString.getBytes();

            // signature
            Signature signature = Signature.getInstance(this.samlSettings.getSigAlg());
            signature.initSign(this.samlSettings.getPrivateKey());
            signature.update(data);
            byte[] signatureBytes = signature.sign();

            String b64 = org.opensaml.xml.util.Base64.encodeBytes(signatureBytes, org.opensaml.xml.util.Base64.DONT_BREAK_LINES);
            return b64;
        } else {
            return null;
        }
    }

    /**
     * This will generate the Redirect Query String Params with the signature that
     * you can append to your IDP sso URL.
     *
     * @param assertionConsumerServiceUrl
     * @param relayState
     *            optional
     * @return
     * @throws Exception
     */
    public String getRedirectRequestSignedQueryParams(String assertionConsumerServiceUrl, String relayState) throws Exception {
        String samlRequest = getRequest(true, assertionConsumerServiceUrl);
        String b64 = signRequest(samlRequest, relayState);
        String qry = generateQueryString(samlRequest, relayState);
        String ret = qry + "&Signature=" + URLEncoder.encode(b64, "UTF-8").trim();
        return ret;
    }

    public boolean verifyRedirectSignature(String samlRequest, String relayState, String sig) throws Exception {
        byte[] v = DatatypeConverter.parseBase64Binary(sig);
        String queryString = generateQueryString(samlRequest, relayState);

        Signature signature = Signature.getInstance(this.samlSettings.getSigAlg());
        signature.initVerify(this.samlSettings.getCertificate().getPublicKey());
        signature.update(queryString.getBytes());

        return signature.verify(v);
    }

    /**
     * This will generate an Enveloped Digital Signature xml String that you can use
     * for a POST SAML AuthnRequest.
     *
     * @param assertionConsumerServiceUrl
     * @param relayState
     *            optional
     * @return
     * @throws WSSecurityException
     * @throws SecurityException
     * @throws MarshallingException
     * @throws org.opensaml.xml.signature.SignatureException
     * @throws IOException
     * @throws TransformerException
     * @throws XMLStreamException
     * @throws ParserConfigurationException
     */
    public String getEnvelopedSignatureRequest(String assertionConsumerServiceUrl, String relayState)
            throws WSSecurityException, SecurityException, MarshallingException, org.opensaml.xml.signature.SignatureException, IOException,
            TransformerException, XMLStreamException, ParserConfigurationException {
        String samlRequest = getRequest(false, assertionConsumerServiceUrl);
        AuthnRequest authReq = (AuthnRequest) string2XMLObject(samlRequest);

        Credential credential = this.samlSettings.getCredential();
        org.opensaml.xml.signature.Signature signature = (org.opensaml.xml.signature.Signature) Configuration.getBuilderFactory()
                .getBuilder(org.opensaml.xml.signature.Signature.DEFAULT_ELEMENT_NAME)
                .buildObject(org.opensaml.xml.signature.Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(credential);
        signature.setSignatureAlgorithm(this.samlSettings.getSigAlgUrl());
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        SecurityConfiguration secConfig = Configuration.getGlobalSecurityConfiguration();
        SecurityHelper.prepareSignatureParams(signature, credential, secConfig, null);
        authReq.setSignature(signature);
        Configuration.getMarshallerFactory().getMarshaller(authReq).marshall(authReq);
        Signer.signObject(signature);

        String signedRequest = convertDocumentToString(authReq.getDOM().getOwnerDocument());
        LOG.info("\n\n**************************\nSigned Post AuthnRequest:\n" + signedRequest + "\n**************************\n\n");

        return signedRequest;
    }

    protected static XMLObject string2XMLObject(String val) throws WSSecurityException {
        Document eaRequest = convertStringToDocument(val);
        Element ar = eaRequest.getDocumentElement();
        if (null != ar) {
            LOG.debug("AuthnRequest: \n" + convertDocumentToString(ar.getOwnerDocument()));
        } else {
            LOG.error("XML Object element is null!");
        }

        return OpenSAMLUtil.fromDom(ar);
    }

    protected static String convertDocumentToString(Document doc) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            // below code to remove XML declaration
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            return output;
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static String b64compressed(boolean compress, byte[] val) throws IOException {
        if (compress) {
            val = CompressionHelper.deflate(val, true);
        }
        String base64 = Base64.encodeBase64String(val);

        return base64;
    }

}
