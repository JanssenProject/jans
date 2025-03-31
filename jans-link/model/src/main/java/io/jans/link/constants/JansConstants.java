/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package io.jans.link.constants;

import io.jans.util.OxConstants;

/**
 * Constants loads the LDAP schema attribute names like uid, iname
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
public final class JansConstants extends OxConstants {

	public static final String objectClassPerson = "jansPerson";
	public static final String objectClassInumMap = "jansInumMap";

	public static final String inumDelimiter = "!";

	public static final String PERSON_INUM = "personInum";

	public static final String inum = "inum";
	public static final String displayName = "displayName";
	public static final String description = "description";
	public static final String origin = "jansAttrOrigin";
	public static final String mail = "mail";
	public static final String ppid = "oxPPID";

	public static final String jansStatus = "jansStatus";
	public static final String sn = "sn";

	public static final String attributeName = "jansAttrName";
	public static final String oxAuthUserId = "oxAuthUserId";


	public static final String CACHE_ATTRIBUTE_PERSON_KEY_LIST = "personAttributeList";
	public static final String CACHE_ATTRIBUTE_CONTACT_KEY_LIST = "contactAttributeList";
	public static final String CACHE_ATTRIBUTE_CUSTOM_KEY_LIST = "customAttributeList";
	public static final String CACHE_ATTRIBUTE_ORIGIN_KEY_LIST = "attributeOriginList";
	public static final String CACHE_ORGANIZATION_CUSTOM_MESSAGE_KEY = "organizationCustomMessage";
	public static final String LINK_DEFAULT_BASE_DN = "ou=link,o=site";
	public static final String JANS_KEYCLOAK_LINK_DEFAULT_BASE_DN = "ou=keycloak-link,o=site";

	public static final String INUM_TYPE_PEOPLE_SLUG = "people";

}
