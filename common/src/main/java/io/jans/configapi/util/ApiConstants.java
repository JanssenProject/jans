/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.util;

public class ApiConstants {

    public static final String BASE_API_URL = "/";
    public static final String CONFIG = "/config";
    public static final String JWKS = "/jwks";
    public static final String JANS_AUTH = "/jans-auth-server";
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
    public static final String PAIRWISE = "/pairwise";
    public static final String FIDO2 = "/fido2";
    public static final String CIBA = "/ciba";
    public static final String CORS = "/cors";
    public static final String ACRS = "/acrs";
    public static final String ENDPOINTS = "/endpoints";
    public static final String GRANT = "/grant";
    public static final String SUBJECT = "/subject";
    public static final String TOKEN = "/token";
    public static final String SEPARATOR = "/";
    public static final String SERVER_CONFIG = "/server-config";
    public static final String SERVER_CLEANUP = "/server-cleanup";
    public static final String KEY_REGENERATION = "/key-regen";
    public static final String EXPIRATION = "/expiration";
    public static final String RESOURCES = "/resources";
    public static final String ATTRIBUTES = "/attributes";
    public static final String SCRIPTS = "/scripts";
    public static final String SMTP = "/smtp";
    public static final String GRANT_TYPES = "/grant-types";
    public static final String CACHE = "/cache";
    public static final String PERSISTENCE = "/persistence";
    public static final String DATABASE = "/database";
    public static final String LDAP = "/ldap";
    public static final String COUCHBASE = "/couchbase";
    public static final String SQL = "/sql";
    public static final String REDIS = "/redis";
    public static final String IN_MEMORY = "/in-memory";
    public static final String NATIVE_PERSISTENCE = "/native-persistence";
    public static final String MEMCACHED = "/memcached";
    public static final String TEST = "/test";
    public static final String INUM_PATH = "{inum}";
    public static final String ID_PATH = "{id}";
    public static final String SCOPE_INUM_PATH = "{scope_inum}";
    public static final String TYPE_PATH = "/{type}";
    public static final String NAME_PARAM_PATH = "/{name}";
    public static final String KEY_PATH = "/key";
    public static final String KID_PATH = "/{kid}";
    public static final String HEALTH = "/health";
    public static final String LIVE = "/live";
    public static final String READY = "/ready";

    public static final String LIMIT = "limit";
    public static final String PATTERN = "pattern";
    public static final String STATUS = "status";
    public static final String INUM = "inum";
    public static final String ID = "id";
    public static final String SCOPE_INUM = "scope_inum";
    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String DISPLAY_NAME = "displayName";
    public static final String KID = "kid";

    public static final String ALL = "all";
    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";

    public static final String MISSING_ATTRIBUTE_MESSAGE = "A required attribute is missing.";

    // Custom CODE
    public static final String MISSING_ATTRIBUTE_CODE = "OCA001";

    // API Protection
    public static final String PROTECTION_TYPE_OAUTH2 = "oauth2";
    public static final String PROTECTION_TYPE_UMA = "uma";
    public static final String ISSUER = "issuer";

    // Connection pool properties
    public static final int CONNECTION_POOL_MAX_TOTAL = 200;
    public static final int CONNECTION_POOL_DEFAULT_MAX_PER_ROUTE = 20;
    public static final int CONNECTION_POOL_VALIDATE_AFTER_INACTIVITY = 10;
    public static final int CONNECTION_POOL_CUSTOM_KEEP_ALIVE_TIMEOUT = 5;

}