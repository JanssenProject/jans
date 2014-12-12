/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.util;

import org.xdi.util.LDAPConstants;

/**
 * Constants loads the LDAP schema attribute names like uid, iname
 * 
 * @author Yuriy Movchan
 * @version 0.1, 10/14/2010
 */
public  class OxConstants extends LDAPConstants {
	public static final String schemaDN = "cn=schema";

	public static final String CACHE_ORGANIZATION_KEY = "organization";
	public static final String CACHE_APPLICATION_NAME = "ApplicationCache";
	public static final String CACHE_ATTRIBUTE_NAME = "AttributeCache";
	public static final String CACHE_LOOKUP_NAME = "LookupCache";

	public static final String CACHE_ATTRIBUTE_KEY_LIST = "attributeList";
	public static final String CACHE_ACTIVE_ATTRIBUTE_KEY_LIST = "activeAttributeList";
	public static final String CACHE_ACTIVE_ATTRIBUTE_NAME = "ActiveAttributeCache";
}
