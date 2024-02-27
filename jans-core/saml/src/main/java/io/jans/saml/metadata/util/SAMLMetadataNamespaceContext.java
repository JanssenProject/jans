package io.jans.saml.metadata.util;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class SAMLMetadataNamespaceContext implements NamespaceContext {

    private static Map<String,String> PREFIX_TO_NAMESPACE = new HashMap<String,String>(){{
        put("md","urn:oasis:names:tc:SAML:2.0:metadata");
        put("ds","http://www.w3.org/2000/09/xmldsig#");
        put("xenc","http://www.w3.org/2001/04/xmlenc#");
        put("saml","urn:oasis:names:tc:SAML:2.0:assertion");
    }};

    @Override
    public String getNamespaceURI(String prefix) {

        if(XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            System.out.println("Default namespace");
            return PREFIX_TO_NAMESPACE.get("md");
        }

        String ret = PREFIX_TO_NAMESPACE.get(prefix);
        return ret;
    }

    @Override
    public String getPrefix(String namespaceURI) {

        return null;
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {

        return null;
    }
}