package io.jans.saml.metadata.util;
 
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.format.DateTimeParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathUtils {

    public enum RoleDescriptorNodeType {
        ROLE_DESCRIPTOR_NODE,
        IDPSSO_DESCRIPTOR_NODE,
        SPSSO_DESCRIPTOR_NODE,
        AUTHN_AUTHORITY_DESCRIPTOR_NODE,
        ATTRIBUTE_AUTHORITY_DESCRIPTOR_NODE,
        PDP_DESCRIPTOR_NODE
    }

    private static final Object xpathFactorysync = new Object();

    private static final String ENTITIES_DESCRIPTOR_SELECTOR = "md:EntitiesDescriptor";
    private static final String ENTITY_DESCRIPTOR_SELECTOR = "md:EntityDescriptor";

    private static final String ROLE_DESCRIPTOR_SELECTOR = "md:RoleDescriptor";
    private static final String IDPSSO_DESCRIPTOR_SELECTOR = "md:IDPSSODescriptor";
    private static final String SPSSO_DESCRIPTOR_SELECTOR = "md:SPSSODescriptor";
    private static final String AUTHN_AUTHORITY_DESCRIPTOR_SELECTOR = "md:AuthnAuthorityDescriptor";
    private static final String ATTRIBUTE_AUTHORITY_DESCRIPTOR_SELECTOR = "md:AttributeAuthorityDescriptor";
    private static final String PDP_DESCRIPTOR_SELECTOR = "md:PDPDescriptor";
    private static final String ORGANIZATION_SELECTOR = "md:Organization";
    private static final String ORGANIZATION_NAME_SELECTOR = "md:OrganizationName";
    private static final String ORGANIZATION_DISPLAY_NAME_SELECTOR = "md:OrganizationDisplayName";
    private static final String ORGANIZATION_URL_SELECTOR = "md:OrganizationURL";
    private static final String CONTACT_PERSON_SELECTOR = "md:ContactPerson";
    private static final String COMPANY_SELECTOR = "md:Company";
    private static final String GIVEN_NAME_SELECTOR = "md:GivenName";
    private static final String SURNAME_SELECTOR  = "md:SurName";
    private static final String EMAIL_ADDRESS_SELECTOR = "md:EmailAddress";
    private static final String TELEPHONE_NUMBER_SELECTOR = "md:TelephoneNumber";
    private static final String NAMEID_FORMAT_SELECTOR = "md:NameIDFormat";

    private static final String KEYDESCRIPTOR_SELECTOR = "md:KeyDescriptor";
    private static final String KEYINFO_SELECTOR = "ds:KeyInfo";
    private static final String ENCRYPTION_METHOD_SELECTOR = "md:EncryptionMethod";
    private static final String KEYSIZE_SELECTOR = "xenc:KeySize";
    private static final String OAEP_PARAMS_SELECTOR = "xenc:OEAPparams";
    private static final String X509_DATA_SELECTOR = "ds:X509Data";
    private static final String X509_CERTIFICATE_SELECTOR = "ds:X509Certificate";
    private static final String SINGLE_LOGOUT_SERVICE_SELECTOR = "md:SingleLogoutService";
    private static final String ASSERTION_CONSUMER_SERVICE_SELECTOR = "md:AssertionConsumerService";

    private static final String ID_ATTR_SELECTOR = "./@ID";
    private static final String ENTITY_ID_ATTR_SELECTOR = "./@entityID";
    private static final String VALID_UNTIL_ATTR_SELECTOR = "./@validUntil";
    private static final String CACHEDURATION_ATTR_SELECTOR = "./@cacheDuration";
    private static final String AUTHN_REQUESTS_SIGNED_ATTR_SELECTOR = "./@AuthnRequestsSigned";
    private static final String WANT_ASSERTIONS_SIGNED_ATTR_SELECTOR = "./@WantAssertionsSigned";
    private static final String PROTOCOL_SUPPORT_ENUMERATION_ATTR_SELECTOR = "./@protocolSupportEnumeration";
    private static final String ERROR_URL_ATTR_SELECTOR = "./@errorURL";
    private static final String LANGUAGE_ATTR_SELECTOR = "./@xml:lang";
    private static final String CONTACT_TYPE_ATTR_SELECTOR = "./@contactType";
    private static final String USE_ATTR_SELECTOR = "./@use";
    private static final String ALGORITHM_ATTR_SELECTOR = "./@Algorithm";
    private static final String DSIG_ID_ATTR_SELECTOR = "./@Id";
    private static final String BINDING_ATTR_SELECTOR = "./@Binding";
    private static final String LOCATION_ATTR_SELECTOR = "./@Location";
    private static final String RESPONSE_LOCATION_ATTR_SELECTOR = "./@ResponseLocation";
    private static final String INDEX_ATTR_SELECTOR = "./@index";
    private static final String IS_DEFAULT_ATTR_SELECTOR  = "./@isDefault";
    
    
    public static final XPath newXPath() {

        synchronized(xpathFactorysync) {
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new SAMLMetadataNamespaceContext());
            return xpath;
        }
    }

    public static boolean entityDescriptorIsDocumentRoot(final XPath xpath, final Document doc) throws XPathExpressionException{
        
        return xpath.evaluate(ENTITY_DESCRIPTOR_SELECTOR,doc,XPathConstants.NODE) != null;
    }

    public static Node entityDescriptorFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (Node)xpath.evaluate(ENTITY_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODE);
    }

    public static NodeList entityDescriptorListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(ENTITY_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static Node entitiesDescriptorFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (Node) xpath.evaluate(ENTITIES_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODE);
    }

    public static NodeList entitiesDescriptorListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(ENTITIES_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static Node keyDescriptorFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (Node) xpath.evaluate(KEYDESCRIPTOR_SELECTOR,parent,XPathConstants.NODE);
    }

    public static NodeList roleDescriptorFromParentNode(RoleDescriptorNodeType nodetype,
          final XPath xpath, final Node parent) throws XPathExpressionException {

        NodeList ret = null;
        switch(nodetype) {
            case ROLE_DESCRIPTOR_NODE:
                ret = (NodeList) xpath.evaluate(ROLE_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
                break;
            case IDPSSO_DESCRIPTOR_NODE:
                ret = (NodeList) xpath.evaluate(IDPSSO_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
                break;
            case SPSSO_DESCRIPTOR_NODE:
                ret = (NodeList) xpath.evaluate(SPSSO_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
                break;
            case AUTHN_AUTHORITY_DESCRIPTOR_NODE:
                ret = (NodeList) xpath.evaluate(AUTHN_AUTHORITY_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
                break;
            case ATTRIBUTE_AUTHORITY_DESCRIPTOR_NODE:
                ret = (NodeList) xpath.evaluate(ATTRIBUTE_AUTHORITY_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
                break;
            case PDP_DESCRIPTOR_NODE:
                ret = (NodeList) xpath.evaluate(PDP_DESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
                break;
        }
        return ret;
    }

    public static Node organizationFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (Node) xpath.evaluate(ORGANIZATION_SELECTOR,parent,XPathConstants.NODE);
    }

    public static NodeList organizationNameListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(ORGANIZATION_NAME_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static NodeList organizationDisplayNameListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(ORGANIZATION_DISPLAY_NAME_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static NodeList organizationUrlListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(ORGANIZATION_URL_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static NodeList contactPersonListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(CONTACT_PERSON_SELECTOR,parent,XPathConstants.NODESET);
    }


    public static String companyValueFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        Node node =  (Node) xpath.evaluate(COMPANY_SELECTOR,parent,XPathConstants.NODE);
        if(node == null) {
            return null;
        }
        return nodeValueAsString(node);
    }

    public static String givenNameValueFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        Node node = (Node) xpath.evaluate(GIVEN_NAME_SELECTOR,parent,XPathConstants.NODE);
        if(node == null) {
            return null;
        }
        return nodeValueAsString(node);
    }

    public static String surnameValueFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        Node node = (Node) xpath.evaluate(SURNAME_SELECTOR,parent,XPathConstants.NODE);
        if(node == null) {
            return null;
        }
        return nodeValueAsString(node);
    }

    public static List<String> emailAddressListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        NodeList nodes = (NodeList) xpath.evaluate(EMAIL_ADDRESS_SELECTOR,parent,XPathConstants.NODESET);
        return nodesAsStringList(nodes);
    }

    public static List<String> telephoneNumberListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        NodeList nodes = (NodeList) xpath.evaluate(TELEPHONE_NUMBER_SELECTOR,parent,XPathConstants.NODESET);
        return nodesAsStringList(nodes);
    }

    public static NodeList keyDescriptorListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(KEYDESCRIPTOR_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static Node keyInfoFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (Node) xpath.evaluate(KEYINFO_SELECTOR,parent,XPathConstants.NODE);
    }

    public static NodeList encryptionMethodListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(ENCRYPTION_METHOD_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static Integer keySizeFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {
        
        Node node = (Node) xpath.evaluate(KEYSIZE_SELECTOR,parent,XPathConstants.NODE);
        if(node == null) {
            return null;
        }
        return nodeValueAsInt(node);
    }

    public static NodeList x509DataListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(X509_DATA_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static String oaepParamsFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        Node node = (Node) xpath.evaluate(OAEP_PARAMS_SELECTOR,parent,XPathConstants.NODE);
        if(node == null) {
            return null;
        }
        return nodeValueAsString(node);
    }

    public static List<String> x509CertificatesFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        NodeList nodes = (NodeList) xpath.evaluate(X509_CERTIFICATE_SELECTOR,parent,XPathConstants.NODESET);
        return nodesAsStringList(nodes);
    }

    public static List<String> nameIDFormatListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        NodeList nodes = (NodeList) xpath.evaluate(NAMEID_FORMAT_SELECTOR,parent,XPathConstants.NODESET);
        return nodesAsStringList(nodes);
    }

    public static NodeList singleLogoutServiceListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(SINGLE_LOGOUT_SERVICE_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static NodeList assertionConsumerServiceListFromParentNode(final XPath xpath, final Node parent) throws XPathExpressionException {

        return (NodeList) xpath.evaluate(ASSERTION_CONSUMER_SERVICE_SELECTOR,parent,XPathConstants.NODESET);
    }

    public static String idAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,ID_ATTR_SELECTOR,node);
    }

    public static String entityIDAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,ENTITY_ID_ATTR_SELECTOR,node);
    }

    public static Duration cacheDurationAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return durationAttributeValue(xpath, CACHEDURATION_ATTR_SELECTOR, node);
    }

    public static Date validUntilAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return dateAttributeValue(xpath,VALID_UNTIL_ATTR_SELECTOR, node);
    }

    public static Boolean authnRequestsSignedAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return booleanAttributeValue(xpath,AUTHN_REQUESTS_SIGNED_ATTR_SELECTOR, node);
    }

    public static Boolean wantAssertionsSignedAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return booleanAttributeValue(xpath,WANT_ASSERTIONS_SIGNED_ATTR_SELECTOR, node);
    }

    public static String protocolSupportEnumerationAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,PROTOCOL_SUPPORT_ENUMERATION_ATTR_SELECTOR, node);
    }

    public static String errorUrlAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,ERROR_URL_ATTR_SELECTOR, node);
    }

    public static String languageAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath, LANGUAGE_ATTR_SELECTOR,node);
    }

    public static String contactTypeAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath, CONTACT_TYPE_ATTR_SELECTOR, node);
    }

    public static String useAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException { 

        return stringAttributeValue(xpath,USE_ATTR_SELECTOR,node);
    }

    public static String algorithmAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,ALGORITHM_ATTR_SELECTOR, node);
    }

    public static String dsigIdAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,DSIG_ID_ATTR_SELECTOR, node);
    }

    public static String bindingAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,BINDING_ATTR_SELECTOR, node);
    }

    public static String locationAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,LOCATION_ATTR_SELECTOR,node);
    }

    public static String responseLocationAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return stringAttributeValue(xpath,RESPONSE_LOCATION_ATTR_SELECTOR, node);
    }

    public static Integer indexAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return intAttributeValue(xpath,INDEX_ATTR_SELECTOR, node);
    }

    public static Boolean isDefaultAttributeValue(final XPath xpath, final Node node) throws XPathExpressionException {

        return booleanAttributeValue(xpath,IS_DEFAULT_ATTR_SELECTOR, node);
    }


    private static String stringAttributeValue(final XPath xpath, final String selector, final Node node) throws XPathExpressionException {

        Node attrnode = (Node) xpath.evaluate(selector,node,XPathConstants.NODE);
        if(attrnode == null) {
            return null;
        }
        return attrnode.getNodeValue();
    }

    private static Date dateAttributeValue(final XPath xpath, final String selector, final Node node) throws XPathExpressionException {

        try {
            String strvalue = stringAttributeValue(xpath, selector, node);
            if(strvalue == null) {
                return null;
            }
            DateFormat df = createDateTimeFormat();
            return df.parse(strvalue);
        }catch(ParseException e) {
            return null;
        }
    }

    private static Duration durationAttributeValue(final XPath xpath, final String selector, final Node node) throws XPathExpressionException {

        try {
            String strvalue =  stringAttributeValue(xpath, selector, node);
            if(strvalue == null) {
                return null;
            }
            return Duration.parse(strvalue);
        }catch(DateTimeParseException e) {
            return null;
        }
    }

    public static Boolean booleanAttributeValue(final XPath xpath, final String selector, final Node node) throws XPathExpressionException {

        String strvalue = stringAttributeValue(xpath, selector, node);
        if(strvalue == null) {
            return false;
        }
        return Boolean.parseBoolean(strvalue);
    }

    public static Integer intAttributeValue(final XPath xpath, final String selector, final Node node) throws XPathExpressionException {

        String strvalue = stringAttributeValue(xpath, selector, node);
        if(strvalue == null) {
            return null;
        }
        return Integer.parseInt(strvalue);
    }

    private static DateFormat createDateTimeFormat() {

        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") {
            @Override
            public Date parse(String source, ParsePosition pos) {
                return super.parse(source.replaceFirst(":(?=[0-9]{2}$)",""),pos);
            }
        };
    }

    private static String nodeValueAsString(final Node node) {

        return node.getTextContent();
    }

    private static List<String> nodesAsStringList(final NodeList nodes) {

        List<String> ret = new ArrayList<>();
        for(int i = 0; i < nodes.getLength(); i++) {
            ret.add(nodeValueAsString(nodes.item(i)));
        }
        return ret;
    }
    
    private static Integer nodeValueAsInt(final Node node) {

        return Integer.parseInt(node.getNodeValue());
    }
}