/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.util;

import java.util.Arrays;
import java.util.List;

public class ApiConstants {
    
    private ApiConstants() {}

    public static final String BASE_API_URL = "/";
    public static final String CONFIG_APP_NAME = "jans-config-api";
    public static final String ARTIFACT = "artifact";
    public static final String CONFIG = "/config";
    public static final String CONFIGS = "/configs";
    public static final String API_CONFIG = "/api-config";
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
    public static final String SESSION = "/session";
    public static final String CLIENT = "/client";
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
    public static final String REVOKE = "/revoke";
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
    public static final String MESSAGE = "/message";
    public static final String PERSISTENCE = "/persistence";
    public static final String FEATURE_FLAGS = "/feature-flags";
    public static final String DATABASE = "/database";
    public static final String LDAP = "/ldap";
    public static final String SQL = "/sql";
    public static final String REDIS = "/redis";
    public static final String IN_MEMORY = "/in-memory";
    public static final String NATIVE_PERSISTENCE = "/native-persistence";
    public static final String MEMCACHED = "/memcached";
    public static final String POSTGRES = "/postgres";
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
    public static final String STATISTICS = "/stat";
    public static final String USER = "/user";
    public static final String ORG = "/org";
    public static final String JANS_ASSETS = "/jans-assets";
    public static final String UPLOAD = "/upload";
    public static final String STREAM = "/stream";
    public static final String SERVICES = "/services";
    public static final String ASSET_TYPE = "/asset-type";
    public static final String ASSET_DIR_MAPPING = "/asset-dir-mapping";
    public static final String SERVICE = "/service";
    public static final String SEARCH = "/search";
    public static final String JANSID_PATH = "/id";
    public static final String SID_PATH = "/sid";
    public static final String SERVICE_STATUS_PATH = "/service-status";
        
    public static final String APP_VERSION = "/app-version";
    public static final String SERVER_STAT = "/server-stat";
    public static final String USERNAME_PATH = "/{username}";
    public static final String CLIENTID_PATH = "/{clientId}";
    public static final String CREATORID_PATH = "/{creatorId}";
    public static final String SESSIONID_PATH = "/{sessionId}";
    public static final String USERDN_PATH = "/{userDn}";
    public static final String AGAMA = "/agama";
    public static final String AGAMA_DEPLOYMENTS = "/agama-deployment";
    public static final String QNAME_PATH = "{qname}";
    public static final String ENABLED = "enabled";
    public static final String QNAME = "qname";
    public static final String INCLUDE_SOURCE = "includeSource";
    public static final String SOURCE = "/source/";
    public static final String PLUGIN = "/plugin";
    public static final String PLUGIN_NAME_PATH = "{pluginName}";
    public static final String AUTHORIZATIONS = "/authorizations";
    public static final String USERID_PATH = "{userId}";
    public static final String SERVICE_NAME_PARAM_PATH = "/{service-name}";
    public static final String TOKEN_PATH = "/{token}";
    public static final String TOKEN_CODE_PATH = "/tknCde";
    public static final String JANSID_PATH_PARAM = "/{jansId}";
    public static final String TOKEN_CODE_PATH_PARAM = "/{tknCde}";
    public static final String SID_PATH_PARAM = "/{sid}";
    
    public static final String USERID = "userId";
    public static final String USERNAME = "username";
    public static final String LIMIT = "limit";
    public static final String START_INDEX = "startIndex";
    public static final String PATTERN = "pattern";
    public static final String WITH_ASSOCIATED_CLIENTS = "withAssociatedClients";
    public static final String STATUS = "status";
    public static final String INUM = "inum";
    public static final String JANSID = "jansId";
    public static final String SID = "sid";
    public static final String JANS_USR_DN = "jansUsrDN";
    public static final String ID = "id";
    public static final String SCOPE_INUM = "scope_inum";
    public static final String TYPE = "type";
    public static final String TYPES = "types";
    public static final String SCRIPTS_TYPES = "script-types";
    public static final String NAME = "name";
    public static final String DISPLAY_NAME = "displayName";
    public static final String KID = "kid";
    public static final String CLIENTID = "clientId";
    public static final String CREATOR = "creator";
    public static final String CREATORID = "creatorId";
    public static final String SESSIONID = "sessionId";
    public static final String USERDN = "userDn";
    public static final String PLUGIN_NAME = "pluginName";
    public static final String SERVICE_NAME = "service-name";
    public static final String TOKEN_CODE = "tknCde";
    public static final String OUTSIDE_SID = "outsideSid";
    public static final String JANS_SESS_ATTR = "jansSessAttr";
    public static final String JANS_SERVICE_NAME = "service";


    public static final String ALL = "all";
    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive"; 
    public static final String ADD_SCRIPT_TEMPLATE = "addScriptTemplate";
    public static final String REMOVE_NON_LDAP_ATTRIBUTES = "removeNonLDAPAttributes";    

    // API Protection
    public static final String PROTECTION_TYPE_OAUTH2 = "oauth2";
    public static final String PROTECTION_TYPE_UMA = "uma";
    public static final String ISSUER = "issuer";

    // Connection pool properties
    public static final int CONNECTION_POOL_MAX_TOTAL = 200;
    public static final int CONNECTION_POOL_DEFAULT_MAX_PER_ROUTE = 20;
    public static final int CONNECTION_POOL_VALIDATE_AFTER_INACTIVITY = 10;
    public static final int CONNECTION_POOL_CUSTOM_KEEP_ALIVE_TIMEOUT = 5;
    
    //Pagination
    public static final String DEFAULT_LIST_SIZE = "50";
    public static final String DEFAULT_LIST_START_INDEX = "0";
    public static final int DEFAULT_MAX_COUNT = 200;
    public static final List<String> BLOCKED_URLS = Arrays.asList(
            "localhost",
            "127.0.",
            "192.168.",
            "172."
    );
    public static final String URL_PREFIX = "https://";
    public static final String SORT_BY = "sortBy";
    public static final String SORT_ORDER = "sortOrder";
    public static final String ASCENDING = "ascending";
    public static final String DESCENDING = "descending";
    public static final String TOTAL_ITEMS = "totalItems";
    public static final String ENTRIES_COUNT = "entriesCount";
    public static final String DATA = "data";
    public static final String FIELD_VALUE_PAIR = "fieldValuePair";

}