package io.jans.saml.metadata.util;

import java.io.InputStream;
import java.io.Reader;

import java.util.Map;
import java.util.HashMap;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

public class LSResourceResolverImpl implements LSResourceResolver {
    
    private static final Map<String,String> namespacemap = new HashMap<String,String>() {{
        put("http://www.w3.org/XML/1998/namespace","/schema/www.w3.org/xml.xsd");
        put("urn:oasis:names:tc:SAML:2.0:assertion","/schema/saml/saml-schema-assertion-2.0.xsd");
        put("http://www.w3.org/2000/09/xmldsig#","/schema/www.w3.org/xmldsig-core-schema.xsd");
        put("http://www.w3.org/2001/04/xmlenc#","/schema/www.w3.org/xenc-schema.xsd");
    }};

    private static final Map<String,String> publicidmap = new HashMap<String,String>() {{
        put("-//W3C//DTD XMLSchema 200102//EN","/schema/www.w3.org/XMLSchema.dtd");
        put("datatypes","/schema/www.w3.org/datatypes.dtd");
    }};

    private static final String CHARACTER_ENCODING = "UTF-8";

    @Override
    public LSInput resolveResource(String type, String nameSpaceURI, String publicId, String systemId, String baseURI) {

        if(nameSpaceURI != null) {
            String resourcepath = namespacemap.get(nameSpaceURI);
            if(resourcepath!=null) {
                
                return new LSInputImpl(getClass().getResourceAsStream(resourcepath));
            }
        }

        if(publicId != null) {
            String resourcepath = publicidmap.get(publicId);
            if(resourcepath != null) {

                return new LSInputImpl(getClass().getResourceAsStream(resourcepath));
            }
        }
        return null;
    }

    private static class LSInputImpl implements LSInput {

        private final InputStream byteStream;

        public LSInputImpl(InputStream byteStream) {

            this.byteStream = byteStream;
        }

        @Override
        public String getPublicId() {

            return null;
        }

        @Override
        public void setPublicId(String publicId) {

        }

        @Override
        public InputStream getByteStream() {

            return this.byteStream;
        }

        @Override
        public void setByteStream(InputStream byteStream) {

        }

        @Override
        public boolean getCertifiedText() {

            return true;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {

        }

        @Override
        public Reader getCharacterStream() {

            return null;
        }

        public void setCharacterStream(Reader reader) {

        }

        @Override
        public String getEncoding() {

            return CHARACTER_ENCODING;
        }

        @Override
        public void setEncoding(String encoding) {

        }

        @Override
        public String getSystemId() {

            return null;
        }

        @Override
        public void setSystemId(String systemId) {

        }

        @Override
        public String getBaseURI() {

            return null;
        }

        @Override
        public void setBaseURI(String baseURI) {

        }

        @Override
        public String getStringData() {

            return null;
        }

        @Override
        public void setStringData(String stringData) {

        }
    }
}