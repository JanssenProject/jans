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

    private static final Source [] SCHEMA_SOURCES = new Source [] {
        new StreamSource(SAXUtils.class.getResourceAsStream("/schema/saml/saml-schema-metadata-2.0.xsd"))
    };

    private static final SchemaFactory instance;

    static {

        try {
            instance = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            instance.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            instance.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            instance.setResourceResolver(new LSResourceResolverImpl());
        }catch(SAXException e) {
            throw new RuntimeException("Could not properly initialize SAX",e);
        }
    }


    public static final SAXParser createParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setSchema(createSchema());
        factory.setNamespaceAware(true);
        return factory.newSAXParser();
    }

    private static final Schema createSchema() throws SAXException {

        return instance.newSchema(SCHEMA_SOURCES);
    }

    private SAXUtils() {

    }
}