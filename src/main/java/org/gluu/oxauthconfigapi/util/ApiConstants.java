package org.gluu.oxauthconfigapi.util;

public class ApiConstants {

	private ApiConstants() {
	}

	public static final String BASE_API_URL = "/oxauth";
	public static final String CONFIG = "/config";
	public static final String PROPERTIES = "/properties";
	public static final String LOGGING = "/logging";
	public static final String METRICS = "/metrics";
	public static final String BACKCHANNEL = "/backchannel";
	public static final String RESPONSES_TYPES = "/responses_types";
	public static final String RESPONSES_MODES = "/responses_modes";
	public static final String JANSSENPKCS = "/janssenpkcs";
	public static final String USER_INFO = "/user_info";
	public static final String IDTOKEN = "/idtoken";
	public static final String REQUEST_OBJECT = "/request_object";
	public static final String UMA = "/uma";
	public static final String DYN_REGISTRATION = "/dyn_registration";
	public static final String SESSIONID = "/sessionid";
	public static final String CLIENTS = "/clients";
	public static final String OPENID = "/openid";
	public static final String SCOPES = "/scopes";
	public static final String SECTORS = "/sectoridentifiers";
	public static final String PAIRWISE = "/pairwise";
	public static final String FIDO2 = "/fido2";
	public static final String CIBA = "/ciba";
	public static final String CORS = "/cors";
	public static final String ACRS = "/acrs";
	public static final String ENDPOINTS = "/endpoints";
	public static final String GRANT = "/grant";
	public static final String SUBJECT = "/subject";
	public static final String TOKEN = "/token";
	public static final String SERVER_CONFIG = "/server-config";
	public static final String SERVER_CLEANUP = "/server-cleanup";
	public static final String KEY_REGENERATION = "/key-regen";
	public static final String RESOURCES = "/resources";
	public static final String ATTRIBUTES = "/attributes";
	public static final String SCRIPTS = "/scripts";
	public static final String SMTP = "/smtp";
	public static final String PERSON_AUTH = "/person_authn";
	public static final String INUM_PATH = "{inum}";

	public static final String LIMIT = "limit";
	public static final String PATTERN = "pattern";
	public static final String STATUS = "status";
	public static final String INUM = "inum";

	public static final String ALL = "all";
	public static final String ACTIVE = "active";
	public static final String INACTIVE = "inactive";

	public static final String MISSING_ATTRIBUTE_MESSAGE = "A required attribute is missing.";

	// Custom CODE
	public static final String MISSING_ATTRIBUTE_CODE = "OCA001";

}