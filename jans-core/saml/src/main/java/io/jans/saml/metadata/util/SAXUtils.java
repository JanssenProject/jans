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

import io.jans.saml.metadata.parser.ParserCreateError;

public class SAXUtils  {

    private static final Source [] schemasources = new Source [] {
        new StreamSource(SAXUtils.class.getResourceAsStream("/META-INF/saml.schemas/saml-schema-metadata-2.0.xsd"))
    };

    private static SAXParserFactory parserfactory = null;

    private SAXUtils() {

    }

    public static void init() throws ParserConfigurationException, SAXException {
        if(parserfactory == null) {
            parserfactory = SAXParserFactory.newInstance();
            parserfactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            parserfactory.setSchema(newSchemaFactory().newSchema(schemasources))    ;
            parserfactory.setNamespaceAware(true);
        }
    }


    public static final SAXParser createParser() throws ParserConfigurationException, SAXException {
        
        if(parserfactory == null) {
            throw new ParserCreateError("Please call SAXParser.init() first");
        }
        return parserfactory.newSAXParser();
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
}