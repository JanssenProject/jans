package io.jans.configapi.util;

public class ApiAccessConstants {

    private ApiAccessConstants() {
    }

    public static final String JANS_AUTH_CONFIG_READ_ACCESS = "https://jans.io/oauth/jans-auth-server/config/properties.readonly";
    public static final String JANS_AUTH_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/jans-auth-server/config/properties.write";

    public static final String FIDO2_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/fido2.readonly";
    public static final String FIDO2_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/fido2.write";

    public static final String ATTRIBUTES_READ_ACCESS = "https://jans.io/oauth/config/attributes.readonly";
    public static final String ATTRIBUTES_WRITE_ACCESS = "https://jans.io/oauth/config/attributes.write";
    public static final String ATTRIBUTES_DELETE_ACCESS = "https://jans.io/oauth/config/attributes.delete";

    public static final String ACRS_READ_ACCESS = "https://jans.io/oauth/config/acrs.readonly";
    public static final String ACRS_WRITE_ACCESS = "https://jans.io/oauth/config/acrs.write";

    public static final String DATABASE_LDAP_READ_ACCESS = "https://jans.io/oauth/config/database/ldap.readonly";
    public static final String DATABASE_LDAP_WRITE_ACCESS = "https://jans.io/oauth/config/database/ldap.write";
    public static final String DATABASE_LDAP_DELETE_ACCESS = "https://jans.io/oauth/config/database/ldap.delete";

    public static final String SCRIPTS_READ_ACCESS = "https://jans.io/oauth/config/scripts.readonly";
    public static final String SCRIPTS_WRITE_ACCESS = "https://jans.io/oauth/config/scripts.write";
    public static final String SCRIPTS_DELETE_ACCESS = "https://jans.io/oauth/config/scripts.delete";

    public static final String CACHE_READ_ACCESS = "https://jans.io/oauth/config/cache.readonly";
    public static final String CACHE_WRITE_ACCESS = "https://jans.io/oauth/config/cache.write";

    public static final String SMTP_READ_ACCESS = "https://jans.io/oauth/config/smtp.readonly";
    public static final String SMTP_WRITE_ACCESS = "https://jans.io/oauth/config/smtp.write";
    public static final String SMTP_DELETE_ACCESS = "https://jans.io/oauth/config/smtp.delete";

    public static final String LOGGING_READ_ACCESS = "https://jans.io/oauth/config/logging.readonly";
    public static final String LOGGING_WRITE_ACCESS = "https://jans.io/oauth/config/logging.write";

    public static final String JWKS_READ_ACCESS = "https://jans.io/oauth/config/jwks.readonly";
    public static final String JWKS_WRITE_ACCESS = "https://jans.io/oauth/config/jwks.write";
    public static final String JWKS_DELETE_ACCESS = "https://jans.io/oauth/config/jwks.delete";

    public static final String OPENID_CLIENTS_READ_ACCESS = "https://jans.io/oauth/config/openid/clients.readonly";
    public static final String OPENID_CLIENTS_WRITE_ACCESS = "https://jans.io/oauth/config/openid/clients.write";
    public static final String OPENID_CLIENTS_DELETE_ACCESS = "https://jans.io/oauth/config/openid/clients.delete";

    public static final String UMA_RESOURCES_READ_ACCESS = "https://jans.io/oauth/config/uma/resources.readonly";
    public static final String UMA_RESOURCES_WRITE_ACCESS = "https://jans.io/oauth/config/uma/resources.write";
    public static final String UMA_RESOURCES_DELETE_ACCESS = "https://jans.io/oauth/config/uma/resources.delete";

    public static final String SCOPES_READ_ACCESS = "https://jans.io/oauth/config/scopes.readonly";
    public static final String SCOPES_WRITE_ACCESS = "https://jans.io/oauth/config/scopes.write";
    public static final String SCOPES_DELETE_ACCESS = "https://jans.io/oauth/config/scopes.delete";

    public static final String STATS_USER_READ_ACCESS = "https://jans.io/oauth/config/stats.readonly";
    public static final String JANS_STAT = "jans_stat";

    public static final String ORG_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/organization.readonly";
    public static final String ORG_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/organization.write";

    public static final String USER_READ_ACCESS = "https://jans.io/oauth/config/user.readonly";
    public static final String USER_WRITE_ACCESS = "https://jans.io/oauth/config/user.write";
    public static final String USER_DELETE_ACCESS = "https://jans.io/oauth/config/user.delete";
    
    public static final String AGAMA_READ_ACCESS = "https://jans.io/oauth/config/agama.readonly";
    public static final String AGAMA_WRITE_ACCESS = "https://jans.io/oauth/config/agama.write";
    public static final String AGAMA_DELETE_ACCESS = "https://jans.io/oauth/config/agama.delete";

    public static final String JANS_AUTH_SESSION_READ_ACCESS = "https://jans.io/oauth/jans-auth-server/session.readonly";
    public static final String JANS_AUTH_SESSION_DELETE_ACCESS = "https://jans.io/oauth/jans-auth-server/session.delete";
    public static final String JANS_AUTH_REVOKE_SESSION  = "revoke_session";
    
    // Super Scopes
    public static final String SUPER_ADMIN_READ_ACCESS  = "https://jans.io/oauth/config/read-all";
    public static final String SUPER_ADMIN_WRITE_ACCESS  = "https://jans.io/oauth/config/write-all";
    public static final String SUPER_ADMIN_DELETE_ACCESS  = "https://jans.io/oauth/config/delete-all";
    
    // Feature Scope
    public static final String OPENID_READ_ACCESS  = "https://jans.io/oauth/config/openid-read";
    public static final String OPENID_WRITE_ACCESS = "https://jans.io/oauth/config/openid/openid-write";
    public static final String OPENID_DELETE_ACCESS = "https://jans.io/oauth/config/openid/openid-delete";
    
    public static final String UMA_READ_ACCESS  = "https://jans.io/oauth/config/uma-read";
    public static final String UMA_WRITE_ACCESS = "https://jans.io/oauth/config/uma-write";
    public static final String UMA_DELETE_ACCESS = "https://jans.io/oauth/config/uma-delete";
    
    public static final String PLUGIN_READ_ACCESS = "https://jans.io/oauth/config/plugin.readonly";
    
    public static final String CONFIG_READ_ACCESS = "https://jans.io/oauth/config/properties.readonly";
    public static final String CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/properties.write";

    
}
