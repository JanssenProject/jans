/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.saml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.gluu.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Loads and validates SAML response
 *
 * @author Yuriy Movchan Date: 24/04/2014
 */
public class Response {
    private static final SimpleNamespaceContext NAMESPACES;

    public static final String SAML_RESPONSE_STATUS_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";
    public static final String SAML_RESPONSE_STATUS_RESPONDER = "urn:oasis:names:tc:SAML:2.0:status:Responder";
    public static final String SAML_RESPONSE_STATUS_AUTHNFAILED = "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed";

    static {
        HashMap<String, String> preferences = new HashMap<String, String>() {
            {
                put("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
                put("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
            }
        };
        NAMESPACES = new SimpleNamespaceContext(preferences);
    }

    private Document xmlDoc;
    private SamlConfiguration samlSettings;

    public Response(SamlConfiguration samlSettings) throws CertificateException {
        this.samlSettings = samlSettings;
    }

    public void loadXml(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory fty = DocumentBuilderFactory.newInstance();

        fty.setNamespaceAware(true);

        // Fix XXE vulnerability
        fty.setXIncludeAware(false);
        fty.setExpandEntityReferences(false);
        fty.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fty.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fty.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        DocumentBuilder builder = fty.newDocumentBuilder();
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        xmlDoc = builder.parse(bais);
    }

    public void loadXmlFromBase64(String response) throws ParserConfigurationException, SAXException, IOException {
        Base64 base64 = new Base64();
        byte[] decodedResponse = base64.decode(response);
        String decodedS = new String(decodedResponse);
        loadXml(decodedS);
    }

    public boolean isValid() throws Exception {
        NodeList nodes = xmlDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

        if (nodes == null || nodes.getLength() == 0) {
            throw new Exception("Can't find signature in document.");
        }

        if (setIdAttributeExists()) {
            tagIdAttributes(xmlDoc);
        }

        X509Certificate cert = samlSettings.getCertificate();
        DOMValidateContext ctx = new DOMValidateContext(cert.getPublicKey(), nodes.item(0));
        XMLSignatureFactory sigF = XMLSignatureFactory.getInstance("DOM");
        XMLSignature xmlSignature = sigF.unmarshalXMLSignature(ctx);

        return xmlSignature.validate(ctx);
    }

    public boolean isAuthnFailed() throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();

        xPath.setNamespaceContext(NAMESPACES);
        XPathExpression query = xPath.compile("/samlp:Response/samlp:Status/samlp:StatusCode");
        NodeList nodes = (NodeList) query.evaluate(xmlDoc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getAttributes().getNamedItem("Value") == null) {
                continue;
            }

            String statusCode = node.getAttributes().getNamedItem("Value").getNodeValue();
            if (SAML_RESPONSE_STATUS_SUCCESS.equalsIgnoreCase(statusCode)) {
                return false;
            } else if (SAML_RESPONSE_STATUS_AUTHNFAILED.equalsIgnoreCase(statusCode)) {
                return true;
            } else if (SAML_RESPONSE_STATUS_RESPONDER.equalsIgnoreCase(statusCode)) {
                // nothing?
                continue;
            }
        }

        return false;
    }

    private void tagIdAttributes(Document xmlDoc) {
        NodeList nodeList = xmlDoc.getElementsByTagName("*");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getAttributes().getNamedItem("ID") != null) {
                    ((Element) node).setIdAttribute("ID", true);
                }
            }
        }
    }

    private boolean setIdAttributeExists() {
        for (Method method : Element.class.getDeclaredMethods()) {
            if (method.getName().equals("setIdAttribute")) {
                return true;
            }
        }
        return false;
    }

    public String getNameId() throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        xPath.setNamespaceContext(NAMESPACES);
        XPathExpression query = xPath.compile("/samlp:Response/saml:Assertion/saml:Subject/saml:NameID");
        return query.evaluate(xmlDoc);
    }

    public Map<String, List<String>> getAttributes() throws XPathExpressionException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        XPath xPath = XPathFactory.newInstance().newXPath();

        xPath.setNamespaceContext(NAMESPACES);
        XPathExpression query = xPath.compile("/samlp:Response/saml:Assertion/saml:AttributeStatement/saml:Attribute");
        NodeList nodes = (NodeList) query.evaluate(xmlDoc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            Node nameNode = node.getAttributes().getNamedItem("Name");
            if (nameNode == null) {
                continue;
            }

            String attributeName = nameNode.getNodeValue();
            List<String> attributeValues = new ArrayList<String>();

            NodeList nameChildNodes = node.getChildNodes();
            for (int j = 0; j < nameChildNodes.getLength(); j++) {
                Node nameChildNode = nameChildNodes.item(j);

                if ("urn:oasis:names:tc:SAML:2.0:assertion".equalsIgnoreCase(nameChildNode.getNamespaceURI())
                        && "AttributeValue".equals(nameChildNode.getLocalName())) {
                    NodeList valueChildNodes = nameChildNode.getChildNodes();
                    for (int k = 0; k < valueChildNodes.getLength(); k++) {
                        Node valueChildNode = valueChildNodes.item(k);
                        attributeValues.add(valueChildNode.getNodeValue());
                    }
                }
            }

            result.put(attributeName, attributeValues);
        }

        return result;
    }

    public void printDocument(OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(xmlDoc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }
}
