/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.saml;

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

	@Override
	public String getNamespaceURI(String prefix) {
		return prefMap.get(prefix);
	}

	@Override
	public String getPrefix(String namespaceURI) {
		return prefMap.get(namespaceURI);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator getPrefixes(String namespaceURI) {
		// TODO Auto-generated method stub
		return null;
	}

	public SimpleNamespaceContext(final Map<String, String> prefMap) {
		prefMap.putAll(prefMap);
	}

}
