/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.util;

import io.jans.util.OxConstants;

/**
 * Constants loads the LDAP schema attribute names like uid, iname
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
public final class OxTrustConstants extends OxConstants {

	public static final String CURRENT_PERSON = "currentPerson";

	public static final String top = "top";
	public static final String objectClassPerson = "gluuPerson";
	public static final String objectClassInumMap = "gluuInumMap";

	public static final String inumDelimiter = "!";
	public static final String inameDelimiter = "*";

	public static final String PERSON_INUM = "personInum";

	public static final String ouPeople = "ou=people";

	public static final String inum = "inum";
	public static final String displayName = "displayName";
	public static final String description = "description";
	public static final String origin = "jansAttrOrigin";
	public static final String mail = "mail";
	public static final String ppid = "jansPPID";
	public static final String jsId = "jansId";
	public static final String SCRYPT_TYPE = "jansScrTyp";

	public static final String status = "status";
	public static final String jsStatus = "jansStatus";
	public static final String sn = "sn";
	public static final String cn = "cn";
	public static final String owner = "owner";
	public static final String member = "member";

	public static final String attributeName = "jansAttrName";

	/**
	 * oxAsimba fields
	 */
	public static final String uniqueIdentifier = "uniqueIdentifier";
	public static final String friendlyName = "friendlyName";
	public static final String identificationURL = "identificationURL";
	public static final String organizationId = "organizationId";

	public static final String RESULT_SUCCESS = "success";
	public static final String RESULT_FAILURE = "failure";
	public static final String RESULT_DUPLICATE = "duplicate";
	public static final String RESULT_DISABLED = "disabled";
	public static final String RESULT_NO_PERMISSIONS = "no_permissions";
	public static final String RESULT_VALIDATION_ERROR = "validation_error";
	public static final String RESULT_REGISTER = "register";
	public static final String RESULT_CONFIRM = "confirm";
	public static final String RESULT_EXISTS = "exists";
	public static final String RESULT_LOGOUT = "logout";
	public static final String RESULT_CLEAR = "clear";
	public static final String RESULT_UPDATE = "update";

	public static final String CACHE_ATTRIBUTE_PERSON_KEY_LIST = "personAttributeList";
	public static final String CACHE_ATTRIBUTE_CONTACT_KEY_LIST = "contactAttributeList";
	public static final String CACHE_ATTRIBUTE_CUSTOM_KEY_LIST = "customAttributeList";
	public static final String CACHE_ATTRIBUTE_ORIGIN_KEY_LIST = "attributeOriginList";
	public static final String CACHE_ORGANIZATION_CUSTOM_MESSAGE_KEY = "organizationCustomMessage";
	public static final String CACHE_REFRESH_DEFAULT_BASE_DN = "ou=cache-refresh,o=site";

	public static final String EVENT_CLEAR_ATTRIBUTES = "eventClearAttributes";
	public static final String EVENT_CLEAR_ORGANIZATION = "eventClearOrganization";
	public static final String RESULT_LOGOUT_SSO = "logout_sso";
	public static final String RESULT_CAPTCHA_VALIDATION_FAILED = "captcha_validation_failed";

	public static final String APPLICATION_AUTHORIZATION_TYPE = "applicationAuthorizationType";
	public static final String APPLICATION_AUTHORIZATION_NAME_SHIBBOLETH3 = "applicationAuthorizationName_Shibboleth3";

	public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
	public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
	public static final String CONTENT_TYPE_APPLICATION_XML = "application/xml";
	public static final String CONTENT_TYPE_APPLICATION_ZIP = "application/zip";

	public static final String PROGRAM_FACTER = "facter";
	public static final String PROGRAM_DF = "df";

	public static final String FACTER_PARAM_VALUE_DIVIDER = " => ";
	public static final String FACTER_FREE_MEMORY_MB = "memoryfree_mb";
	public static final String FACTER_FREE_MEMORY = "memoryfree";
	public static final String FACTER_MEMORY_SIZE = "memorysize";
	public static final String FACTER_MEMORY_SIZE_MB = "memorysize_mb";
	public static final String FACTER_FREE_SWAP = "swapfree";
	public static final String FACTER_FREE_SWAP_TOTAL = "swapsize";
	public static final String FACTER_HOST_NAME = "hostname";
	public static final String FACTER_IP_ADDRESS = "ipaddress";
	public static final String FACTER_SYSTEM_UP_TIME = "uptime_seconds";
	public static final String FACTER_BANDWIDTH_USAGE = "bandwidth_usage";
	public static final String FACTER_LOAD_AVERAGE = "load_average";

	public static final String HTTPD_TEST_PAGE_NAME = "/index.html";

	public static final String HTTPD_TEST_PAGE_CONTENT = "<html>\n<head>\n<script type=\"text/javascript\">\n<!--\nfunction delayer(){\n    window.location = \"../identity/\"\n}\n//-->\n</script>\n</head>\n<body onLoad=\"setTimeout('delayer()', 5000)\">\n<h2>You are being redirected to configuration login page. Please click this <a href=\"../identity/\">link</a> if your browser does not support javascript.</h2>\n</body>\n</html>\n";

	public static final String PROGRAM_LDAPSEARCH = "/usr/bin/ldapsearch";

	public static final String EVENT_LDAP_CONNECTION_CHECKER_TIMER = "ldapConnectionCheckerTimerEvent";
	public static final String EVENT_METADATA_ENTITY_ID_UPDATE = "metadataEntityIdUpdate";
	public static final String EVENT_CACHE_REFRESH_TIMER = "cacheRefreshTimerEvent";
	public static final String CUSTOM_MESSAGE_LOGIN_PAGE = "loginPage";
	public static final String CUSTOM_MESSAGE_WELCOME_PAGE = "welcomePage";
	public static final String CUSTOM_MESSAGE_TITLE_TEXT = "welcomeTitle";

	public static final char[] HEX_CHARACTERS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
			'e', 'f' };

	public static String ouConfigurations = "ou=configurations";

	public static final String INUM_TYPE_PEOPLE_SLUG = "people";
	public static final String INUM_TYPE_GROUP_SLUG = "group";
	public static final String INUM_TYPE_ATTRIBUTE_SLUG = "attribute";
	public static final String INUM_TYPE_TRUST_RELATNSHIP_SLUG = "trelationship";
	public static final String OXAUTH_CLIENT_ID = "client_id";
	public static final String OXAUTH_CLIENT_PASSWORD = "client_password";
	public static final String OXAUTH_CLIENT_CREDENTIALS = "client_credentials";
	public static final String OXAUTH_REDIRECT_URI = "redirect_uri";
	public static final String OXAUTH_POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";
	public static final String OXAUTH_RESPONSE_TYPE = "response_type";
	public static final String OXAUTH_SCOPE = "scope";
	public static final String OXAUTH_STATE = "state";
	public static final String OXAUTH_CODE = "code";
	public static final String OXAUTH_ID_TOKEN = "id_token";
	public static final String OXAUTH_ERROR = "error";
	public static final String OXAUTH_NONCE = "nonce";
	public static final String OXAUTH_ERROR_DESCRIPTION = "error_description";
	public static final String OXAUTH_ID_TOKEN_HINT = "id_token_hint";
	public static final String OXAUTH_ACCESS_TOKEN = "access_token";
	public static final String OXAUTH_ACR_VALUES = "acr_values";

	public static final String OXAUTH_SESSION_STATE = "session_state";
	public static final String OXAUTH_SSO_SESSION_STATE = "session_sso_state";

	public static final String INUM_PERSON_OBJECTTYPE = "0000";
	public static final String INUM_GROUP_OBJECTTYPE = "0003";
	public static final String INUM_SECTOR_IDENTIFIER_OBJECTTYPE = "0012";
	public static final String INAME_PERSON_OBJECTTYPE = "person";
	public static final String INAME_OXPLUS = "oxplus";
	public static final String INAME_CLASS_OBJECTTYPE = "class";

	public static final String SELF_LINK_CONTRACT = "$self";
	public static final String ADMIN_LINK_CONTRACT = "*linkcontract*manager";

	public static final int searchClientsSizeLimit = 200;
	public static final int searchPersonsSizeLimit = 200;
	public static final int searchGroupSizeLimit = 200;
	public static final int searchSectorIdentifierSizeLimit = 200;
	public static final int searchSizeLimit = 200;

	public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Unexpected processing error; please check the input parameters";

	public static final String PRE_REGISTRATION_SCRIPT = "PreRegistrationScript";

	public static final String POST_REGISTRATION_SCRIPT = "PostRegistrationScript";

	public static final String INIT_REGISTRATION_SCRIPT = "InitRegistrationScript";

	public static final String CONFIRM_REGISTRATION_SCRIPT = "ConfirmRegistrationScript";

}
