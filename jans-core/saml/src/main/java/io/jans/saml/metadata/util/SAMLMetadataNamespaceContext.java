package io.jans.saml.metadata.util;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class SAMLMetadataNamespaceContext implements NamespaceContext {

    private static final Map<String,String> prefixes;

    static {
        prefixes = new HashMap<>();
        prefixes.put("md","urn:oasis:names:tc:SAML:2.0:metadata");
        prefixes.put("ds","http://www.w3.org/2000/09/xmldsig#");
        prefixes.put("xenc","http://www.w3.org/2001/04/xmlenc#");
        prefixes.put("saml","urn:oasis:names:tc:SAML:2.0:assertion");
        prefixes.put("xml","http://www.w3.org/XML/1998/namespace");
    }

    @Override
    public String getNamespaceURI(String prefix) {

        if(XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            return prefixes.get("md");
        }
        return prefixes.get(prefix);
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