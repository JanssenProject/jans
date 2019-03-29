/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.util;

/**
 * Constants loads the LDAP schema attribute names like uid, iname
 *
 * @author Yuriy Movchan
 * @version 0.1, 10/14/2010
 */
public class OxConstants {

    public static final String UID = "uid";
    public static final String OBJECT_CLASS = "objectClass";

    public static final String INUM = "inum";
    public static final String INAME = "iname";
    public static final String DISPLAY_NAME = "displayName";
    public static final String DESCRIPTION = "description";
    public static final String ORIGIN = "gluuAttributeOrigin";
    public static final String MAIL = "mail";

    public static final String CACHE_ORGANIZATION_KEY = "organization";
    public static final String CACHE_METRICS_KEY = "metrics";
    public static final String CACHE_APPLICATION_NAME = "ApplicationCache";
    public static final String CACHE_ATTRIBUTE_NAME = "AttributeCache";
    public static final String CACHE_LOOKUP_NAME = "LookupCache";
    public static final String CACHE_METRICS_NAME = "metricsCache";

    public static final String CACHE_ATTRIBUTE_KEY_LIST = "attributeList";
    public static final String CACHE_ACTIVE_ATTRIBUTE_KEY_LIST = "activeAttributeList";
    public static final String CACHE_ACTIVE_ATTRIBUTE_NAME = "ActiveAttributeCache";

    public static final String SCRIPT_TYPE_INTERNAL_RESERVED_NAME = "simple_password_auth";

}
