/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.model.config;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Constants {

    private Constants() {
    }

    public static final String SERVER_KEY_OF_CONFIGURATION_ENTRY = "jansAuth_ConfigurationEntryDN";

    public static final String BASE_PROPERTIES_FILE_NAME = "jans.properties";
    public static final String LDAP_PROPERTIES_FILE_NAME = "jans-ldap.properties";
    public static final String COUCHBASE_PROPERTIES_FILE_NAME = "jans-couchbase.properties";
    public static final String SALT_FILE_NAME = "salt";
    public static final String CERTS_DIR = "certsDir";

    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String ERROR_URI = "error_uri";
    public static final String REASON = "reason";

    public static final String STATE = "state";
    public static final String REDIRECT = "redirect";
    public static final String AUTH_STEP = "auth_step";
    public static final String PROMPT = "prompt";
    public static final String OPENID = "openid";

    public static final String LOG_FOUND = "Found '{}' entries";

    public static final String MONTH = "month";
    public static final String GRANTTYPE = "grantType";

    public static final String HTTP_11 = "HTTP/1.1";
    public static final String SPACE_HTTP_11 = " " + HTTP_11;
    public static final String UNKNOWN = "Unknown";
    public static final String UNKNOWN_DOT = UNKNOWN + ".";
    public static final String PRAGMA = "Pragma";
    public static final String NO_CACHE = "no-cache";
    public static final String X_CLIENTCERT = "X-ClientCert";

    public static final String AUTHORIZATION_BEARER = "Authorization: Bearer ";
    public static final String AUTHORIZATION_BASIC = "Authorization: Basic ";

    public static final String REASON_CLIENT_NOT_AUTHORIZED = "The client is not authorized.";
}
