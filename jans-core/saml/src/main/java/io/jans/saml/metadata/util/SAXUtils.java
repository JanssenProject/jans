package io.jans.saml.metadata.util;


import javax.management.RuntimeErrorException;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;

public class SAXUtils  {

    private static final Source [] schemasources = new Source [] {
        new StreamSource(SAXUtils.class.getResourceAsStream("/META-INF/saml.schemas/saml-schema-metadata-2.0.xsd"))
    };


    public static final SAXParser createParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",false);
        factory.setSchema(newSchemaFactory().newSchema(schemasources));
        factory.setNamespaceAware(true);
        return factory.newSAXParser();
    }

    private static final SchemaFactory newSchemaFactory() throws SAXException {
        
        final SchemaFactory schemafactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemafactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",false);
        schemafactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        schemafactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        schemafactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        schemafactory.setResourceResolver(new LSResourceResolverImpl());
        return schemafactory;
    }

    private SAXUtils() {

    }
}