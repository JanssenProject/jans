package io.jans.saml.metadata.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.jans.saml.metadata.model.*;
import io.jans.saml.metadata.builder.EntityDescriptorBuilder;
import io.jans.saml.metadata.builder.IndexedEndpointBuilder;
import io.jans.saml.metadata.builder.KeyDescriptorBuilder;
import io.jans.saml.metadata.builder.LocalizedTextBuilder;
import io.jans.saml.metadata.builder.OrganizationBuilder;
import io.jans.saml.metadata.builder.SAMLMetadataBuilder;
import io.jans.saml.metadata.builder.RoleDescriptorBuilder;
import io.jans.saml.metadata.builder.SPSSODescriptorBuilder;
import io.jans.saml.metadata.builder.SSODescriptorBuilder;
import io.jans.saml.metadata.builder.ContactPersonBuilder;
import io.jans.saml.metadata.builder.EndpointBuilder;

import io.jans.saml.metadata.builder.ds.KeyInfoBuilder;
import io.jans.saml.metadata.builder.ds.X509DataBuilder;
import io.jans.saml.metadata.builder.enc.EncryptionMethodBuilder;

import io.jans.saml.metadata.util.SAXUtils;
import io.jans.saml.metadata.util.XPathUtils;

public class SAMLMetadataParser  {

    private final SAXParser saxParser;
    private final Schema schema;

    public SAMLMetadataParser() {
        try {
            saxParser = SAXUtils.createParser();
            schema = saxParser.getSchema();
        }catch(ParserConfigurationException | SAXException e) {
            throw new ParserCreateError("Could not create parser",e);
        }
    }

    public SAMLMetadata parse(File metadatafile) {


        try {
            final Validator validator = schema.newValidator();
            validator.validate(new StreamSource(metadatafile));

            final DocumentBuilderFactory docbuilderfactory = DocumentBuilderFactory.newInstance();
            docbuilderfactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",false);
            docbuilderfactory.setSchema(schema);
            docbuilderfactory.setNamespaceAware(true);
            final DocumentBuilder docbuilder = docbuilderfactory.newDocumentBuilder();
            final Document doc =  docbuilder.parse(metadatafile);
            final XPath xpath = XPathUtils.newXPath();
            final SAMLMetadataBuilder builder = new SAMLMetadataBuilder();

            if(XPathUtils.entityDescriptorIsDocumentRoot(xpath,doc)) {
                Node entitydescnode = XPathUtils.entityDescriptorFromParentNode(xpath, doc);
                parseEntityDescriptor(xpath,entitydescnode,builder.entityDescriptor());
            }else {
                Node entitiesdescnode = XPathUtils.entitiesDescriptorFromParentNode(xpath, doc);
                flattenEntitiesDescriptor(xpath, entitiesdescnode,builder);
            }

            return builder.build();

        }catch(IllegalArgumentException | IOException | SAXException | XPathExpressionException | ParserConfigurationException e) {

            throw new ParseError("Metadata parsing failed",e);
        }
    }

    private final void flattenEntitiesDescriptor(final XPath xpath, 
        final Node entitiescdescriptor, final SAMLMetadataBuilder builder) throws XPathExpressionException {
        
        NodeList entitydescriptorlist  = XPathUtils.entityDescriptorListFromParentNode(xpath, entitiescdescriptor);
        for(int i=0; i < entitydescriptorlist.getLength(); i++) {
            parseEntityDescriptor(xpath,entitydescriptorlist.item(i),builder.entityDescriptor());
        }

        NodeList entitiesdescriptorlist = XPathUtils.entitiesDescriptorListFromParentNode(xpath, entitiescdescriptor);
        for(int i=0; i< entitiesdescriptorlist.getLength(); i++) {
            flattenEntitiesDescriptor(xpath,entitiesdescriptorlist.item(i), builder);
        }
    }

    private final void parseEntityDescriptor(final XPath xpath, 
        final Node node,final EntityDescriptorBuilder builder) throws XPathExpressionException {

        builder.id(XPathUtils.idAttributeValue(xpath,node))
               .entityId(XPathUtils.entityIDAttributeValue(xpath,node))
               .cacheDuration(XPathUtils.cacheDurationAttributeValue(xpath, node))
               .validUntil(XPathUtils.validUntilAttributeValue(xpath, node));
        
        NodeList spssodescriptorslist = XPathUtils.roleDescriptorFromParentNode(
                XPathUtils.RoleDescriptorNodeType.SPSSO_DESCRIPTOR_NODE, xpath, node);
        
        for(int i = 0 ; i < spssodescriptorslist.getLength() ; i++) {
            parseSPSSODescriptor(xpath, spssodescriptorslist.item(i),builder.spssoDescriptor());
        }
    }

    private final void parseSPSSODescriptor(final XPath xpath, 
        final Node node, final SPSSODescriptorBuilder builder) throws XPathExpressionException {
        
        builder.authnRequestsSigned(XPathUtils.authnRequestsSignedAttributeValue(xpath, node));
        builder.wantAssertionsSigned(XPathUtils.wantAssertionsSignedAttributeValue(xpath, node));

        parseSSODescriptor(xpath,node,builder);

        NodeList assertionconsumerservicelist = XPathUtils.assertionConsumerServiceListFromParentNode(xpath, node);
        for(int i=0; i < assertionconsumerservicelist.getLength(); i++) {
            parseIndexedEndpoint(xpath,assertionconsumerservicelist.item(i),builder.assertionConsumerService());
        }
    }

    private final void parseSSODescriptor(final XPath xpath,
        final Node node, final SSODescriptorBuilder builder) throws XPathExpressionException {
        
        parseRoleDescriptor(xpath,node,builder);

        NodeList sloservicelist = XPathUtils.singleLogoutServiceListFromParentNode(xpath, node);
        for(int i = 0; i < sloservicelist.getLength(); i++) {
            parseEndpoint(xpath,sloservicelist.item(i),builder.singleLogoutService());
        }

        builder.nameIDFormats(XPathUtils.nameIDFormatListFromParentNode(xpath, node));
    }

    private final void parseRoleDescriptor(final XPath xpath,
        final Node node, final RoleDescriptorBuilder builder) throws XPathExpressionException {
        
        //todo implement

        builder.id(XPathUtils.idAttributeValue(xpath,node))
                .cacheDuration(XPathUtils.cacheDurationAttributeValue(xpath, node))
                .validUntil(XPathUtils.validUntilAttributeValue(xpath, node))
                .supportedProtocols(XPathUtils.protocolSupportEnumerationAttributeValue(xpath, node))
                .errorUrl(XPathUtils.errorUrlAttributeValue(xpath,node));
        
        Node organization = XPathUtils.organizationFromParentNode(xpath, node);
        if(organization != null) {
            parseOrganization(xpath, organization,builder.organization());
        }

        NodeList contactpersonlist = XPathUtils.contactPersonListFromParentNode(xpath, node);
        for(int i = 0; i < contactpersonlist.getLength(); i++) {
            parseContactPerson(xpath,contactpersonlist.item(i),builder.contactPerson());
        }

        NodeList keydescriptorlist = XPathUtils.keyDescriptorListFromParentNode(xpath,node);
        for(int i =0; i < keydescriptorlist.getLength(); i++) {
            parseKeyDescriptor(xpath,keydescriptorlist.item(i),builder.keyDescriptor());
        }
    }

    private final void parseOrganization(final XPath xpath, final Node node, final OrganizationBuilder builder) throws XPathExpressionException {

        NodeList orgnamelist = XPathUtils.organizationNameListFromParentNode(xpath, node);
        for(int i = 0; i < orgnamelist.getLength(); i++) {
            parseLocalizedText(xpath,orgnamelist.item(i),builder.name());
        }

        NodeList displaynamelist = XPathUtils.organizationDisplayNameListFromParentNode(xpath, node);
        for(int i = 0; i < displaynamelist.getLength(); i++) {
            parseLocalizedText(xpath,displaynamelist.item(i),builder.diplayName());
        }

        NodeList urls  = XPathUtils.organizationUrlListFromParentNode(xpath, node);
        for(int i = 0; i < urls.getLength(); i++) {
            parseLocalizedText(xpath,urls.item(i),builder.url());
        }
    }

    private final void parseLocalizedText(final XPath xpath, final Node node, final LocalizedTextBuilder builder) throws XPathExpressionException {

        builder.language(XPathUtils.languageAttributeValue(xpath, node))
                .text(node.getTextContent());
    }

    
    private final void parseContactPerson(final XPath xpath, final Node node, final ContactPersonBuilder builder) throws XPathExpressionException {

        builder.type(XPathUtils.contactTypeAttributeValue(xpath, node))
               .company(XPathUtils.companyValueFromParentNode(xpath, node))
               .givenName(XPathUtils.givenNameValueFromParentNode(xpath, node))
               .surName(XPathUtils.surnameValueFromParentNode(xpath, node))
               .emailAddresses(XPathUtils.emailAddressListFromParentNode(xpath, node))
               .telephoneNumbers(XPathUtils.telephoneNumberListFromParentNode(xpath, node));
    }

    private final void parseKeyDescriptor(final XPath xpath, final Node node, final KeyDescriptorBuilder builder) throws XPathExpressionException {

        builder.use(XPathUtils.useAttributeValue(xpath, node));
        NodeList encmethods = XPathUtils.encryptionMethodListFromParentNode(xpath, node);
        for(int i=0; i<encmethods.getLength(); i++) {
            parseEncryptionMethod(xpath,encmethods.item(i),builder.encryptionMethod());
        }

        Node keyinfo = XPathUtils.keyInfoFromParentNode(xpath, node);
        if(keyinfo != null) {
            parseKeyInfo(xpath, keyinfo,builder.keyInfo());
        }
    }

    private final void parseEncryptionMethod(final XPath xpath, final Node node, final EncryptionMethodBuilder builder) throws XPathExpressionException {

        builder.algorithm(XPathUtils.algorithmAttributeValue(xpath, node))
               .keySize(XPathUtils.keySizeFromParentNode(xpath, node))
               .oaepParams(XPathUtils.oaepParamsFromParentNode(xpath, node));
    }

    private final void parseKeyInfo(final XPath xpath, final Node node, final KeyInfoBuilder builder) throws XPathExpressionException {
        
        builder.id(XPathUtils.dsigIdAttributeValue(xpath, node));
        NodeList x509datalist = XPathUtils.x509DataListFromParentNode(xpath, node);
        for(int i = 0; i < x509datalist.getLength(); i++) {
            parseX509Data(xpath,x509datalist.item(i),builder.x509Data());
        }
    }

    private final void parseX509Data(final XPath xpath, final Node node,final X509DataBuilder builder) throws XPathExpressionException {

        List<String> certificates = XPathUtils.x509CertificatesFromParentNode(xpath,node);
        builder.x509Certificates(certificates);
    }

    private final void parseEndpoint(final XPath xpath, final Node node, final EndpointBuilder builder) throws XPathExpressionException {
        
        builder.binding(SAMLBinding.fromString(XPathUtils.bindingAttributeValue(xpath, node)))
               .location(XPathUtils.locationAttributeValue(xpath, node))
               .responseLocation(XPathUtils.responseLocationAttributeValue(xpath, node));
    }

    private final void parseIndexedEndpoint(final XPath xpath, final Node node, final IndexedEndpointBuilder builder) throws XPathExpressionException {

        parseEndpoint(xpath, node,builder);
        builder.index(XPathUtils.indexAttributeValue(xpath, node))
                .isDefault(XPathUtils.isDefaultAttributeValue(xpath, node));
    }
}