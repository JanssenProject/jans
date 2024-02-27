package io.jans.saml.metadata.parser;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.time.Duration;
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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import io.jans.saml.metadata.model.*;
import io.jans.saml.metadata.model.ds.*;
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
        }catch(IllegalArgumentException | IOException e) {
            throw new ParseError("Metadata parsing failed",e);
        }catch(SAXException | XPathExpressionException | ParserConfigurationException e) {
            throw new ParseError("Metadata parsing failed",e);
        }
    }

    private final void flattenEntitiesDescriptor(final XPath xpath, 
        final Node entitiescdescriptor, final SAMLMetadataBuilder builder) throws XPathExpressionException {
        
        NodeList entitydescriptor_list  = XPathUtils.entityDescriptorListFromParentNode(xpath, entitiescdescriptor);
        for(int i=0; i < entitydescriptor_list.getLength(); i++) {
            parseEntityDescriptor(xpath,entitydescriptor_list.item(i),builder.entityDescriptor());
        }

        NodeList entitiesdescriptor_list = XPathUtils.entitiesDescriptorListFromParentNode(xpath, entitiescdescriptor);
        for(int i=0; i< entitiesdescriptor_list.getLength(); i++) {
            flattenEntitiesDescriptor(xpath,entitiesdescriptor_list.item(i), builder);
        }
    }

    private final void parseEntityDescriptor(final XPath xpath, 
        final Node node,final EntityDescriptorBuilder builder) throws XPathExpressionException {

        builder.id(XPathUtils.IDAttributeValue(xpath,node))
               .entityId(XPathUtils.entityIDAttributeValue(xpath,node))
               .cacheDuration(XPathUtils.cacheDurationAttributeValue(xpath, node))
               .validUntil(XPathUtils.validUntilAttributeValue(xpath, node));
        
        NodeList spssodescriptors_n = XPathUtils.roleDescriptorFromParentNode(
                XPathUtils.RoleDescriptorNodeType.SPSSO_DESCRIPTOR_NODE, xpath, node);
        
        for(int i = 0 ; i < spssodescriptors_n.getLength() ; i++) {
            parseSPSSODescriptor(xpath, spssodescriptors_n.item(i),builder.spssoDescriptor());
        }
    }

    private final void parseSPSSODescriptor(final XPath xpath, 
        final Node node, final SPSSODescriptorBuilder builder) throws XPathExpressionException {
        
        builder.authnRequestsSigned(XPathUtils.authnRequestsSignedAttributeValue(xpath, node));
        builder.wantAssertionsSigned(XPathUtils.wantAssertionsSignedAttributeValue(xpath, node));

        parseSSODescriptor(xpath,node,builder);

        NodeList assertionconsumerservice_list = XPathUtils.assertionConsumerServiceListFromParentNode(xpath, node);
        for(int i=0; i < assertionconsumerservice_list.getLength(); i++) {
            parseIndexedEndpoint(xpath,assertionconsumerservice_list.item(i),builder.assersionConsumerService());
        }
    }

    private final void parseSSODescriptor(final XPath xpath,
        final Node node, final SSODescriptorBuilder builder) throws XPathExpressionException {
        
        parseRoleDescriptor(xpath,node,builder);

        NodeList sloservice_nodes = XPathUtils.singleLogoutServiceListFromParentNode(xpath, node);
        for(int i = 0; i < sloservice_nodes.getLength(); i++) {
            parseEndpoint(xpath,sloservice_nodes.item(i),builder.singleLogoutService());
        }

        builder.nameIDFormats(XPathUtils.nameIDFormatListFromParentNode(xpath, node));
    }

    private final void parseRoleDescriptor(final XPath xpath,
        final Node node, final RoleDescriptorBuilder builder) throws XPathExpressionException {
        
        //todo implement

        builder.id(XPathUtils.IDAttributeValue(xpath,node))
                .cacheDuration(XPathUtils.cacheDurationAttributeValue(xpath, node))
                .validUntil(XPathUtils.validUntilAttributeValue(xpath, node))
                .supportedProtocols(XPathUtils.protocolSupportEnumerationAttributeValue(xpath, node))
                .errorUrl(XPathUtils.errorUrlAttributeValue(xpath,node));
        
        Node organization_node = XPathUtils.organizationFromParentNode(xpath, node);
        if(organization_node != null) {
            parseOrganization(xpath, organization_node,builder.organization());
        }

        NodeList contactperson_node_list = XPathUtils.contactPersonListFromParentNode(xpath, node);
        for(int i = 0; i < contactperson_node_list.getLength(); i++) {
            parseContactPerson(xpath,contactperson_node_list.item(i),builder.contactPerson());
        }

        NodeList keydescriptor_node_list = XPathUtils.keyDescriptorListFromParentNode(xpath,node);
        for(int i =0; i < keydescriptor_node_list.getLength(); i++) {
            parseKeyDescriptor(xpath,keydescriptor_node_list.item(i),builder.keyDescriptor());
        }
    }

    private final void parseOrganization(final XPath xpath, final Node node, final OrganizationBuilder builder) throws XPathExpressionException {

        NodeList namenodes = XPathUtils.organizationNameListFromParentNode(xpath, node);
        for(int i = 0; i < namenodes.getLength(); i++) {
            parseLocalizedText(xpath,namenodes.item(i),builder.name());
        }

        NodeList displaynamenodes = XPathUtils.organizationDisplayNameListFromParentNode(xpath, node);
        for(int i = 0; i < displaynamenodes.getLength(); i++) {
            parseLocalizedText(xpath,displaynamenodes.item(i),builder.diplayName());
        }

        NodeList urlnodes  = XPathUtils.organizationUrlListFromParentNode(xpath, node);
        for(int i = 0; i < urlnodes.getLength(); i++) {
            parseLocalizedText(xpath,urlnodes.item(i),builder.url());
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

        Node keyinfo_node = XPathUtils.keyInfoFromParentNode(xpath, node);
        if(keyinfo_node != null) {
            parseKeyInfo(xpath, keyinfo_node,builder.keyInfo());
        }
    }

    private final void parseEncryptionMethod(final XPath xpath, final Node node, final EncryptionMethodBuilder builder) throws XPathExpressionException {

        builder.algorithm(XPathUtils.algorithmAttributeValue(xpath, node))
               .keySize(XPathUtils.keySizeFromParentNode(xpath, node))
               .oaepParams(XPathUtils.oaepParamsFromParentNode(xpath, node));
    }

    private final void parseKeyInfo(final XPath xpath, final Node node, final KeyInfoBuilder builder) throws XPathExpressionException {
        
        builder.id(XPathUtils.dsigIdAttributeValue(xpath, node));
        NodeList x509data_nodes = XPathUtils.x509DataListFromParentNode(xpath, node);
        for(int i = 0; i < x509data_nodes.getLength(); i++) {
            parseX509Data(xpath,x509data_nodes.item(i),builder.x509Data());
        }
    }

    private final void parseX509Data(final XPath xpath, final Node node,final X509DataBuilder builder) throws XPathExpressionException {

        List<String> certificates = XPathUtils.x509CertificatesFromParentNode(xpath,node);
        builder.x509Certificates(certificates);
    }

    private final void parseEndpoint(final XPath xpath, final Node node, final EndpointBuilder builder) throws XPathExpressionException {

        builder.binding(XPathUtils.bindingAttributeValue(xpath, node))
               .location(XPathUtils.locationAttributeValue(xpath, node))
               .responseLocation(XPathUtils.responseLocationAttributeValue(xpath, node));
    }

    private final void parseIndexedEndpoint(final XPath xpath, final Node node, final IndexedEndpointBuilder builder) throws XPathExpressionException {

        parseEndpoint(xpath, node,builder);
        builder.index(XPathUtils.indexAttributeValue(xpath, node))
                .isDefault(XPathUtils.isDefaultAttributeValue(xpath, node));
    }
}