/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

/**
 * Allow to work with XML name contexts
 *
 * @author Yuriy Movchan Date: 04/24/2014
 */
public class SimpleNamespaceContext implements NamespaceContext {

    private final Map<String, String> prefMap = new HashMap<String, String>();

    public SimpleNamespaceContext(final Map<String, String> prefMap) {
        prefMap.putAll(prefMap);
    }

    public String getNamespaceURI(String prefix) {
        return prefMap.get(prefix);
    }

    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    public Iterator<?> getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }

}
