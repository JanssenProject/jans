package io.jans.kc.spi.storage.config;

import org.keycloak.Config;

public class PluginConfiguration {

    private static final String AUTH_TOKEN_ENDPOINT_KEY = "jans-storage-auth-token-endpoint";
    private static final String SCIM_USER_ENDPOINT_KEY = "jans-storage-scim-user-endpoint";
    private static final String SCIM_USER_SEARCH_ENDPOINT_KEY = "jans-storage-search-endpoint";
    private static final String SCIM_OAUTH_SCOPE_KEY = "jans-storage-scim-oauth-scope";
    private static final String SCIM_CLIENT_ID_KEY = "jans-storage-client-id";
    private static final String SCIM_CLIENT_SECRET = "jans-storage-client-secret";

    private String authTokenEndpoint;
    private String scimUserEndpoint;
    private String scimUserSearchEndpoint;
    private String scimOauthScope;
    private String scimClientId;
    private String scimClientSecret;

    private PluginConfiguration() {

    }

    public static PluginConfiguration fromKeycloakConfiguration(Config.Scope config) {

        PluginConfiguration ret = new PluginConfiguration();
        ret.authTokenEndpoint = config.get(AUTH_TOKEN_ENDPOINT_KEY);
        ret.scimUserEndpoint = config.get(SCIM_USER_ENDPOINT_KEY);
        ret.scimUserSearchEndpoint = config.get(SCIM_USER_SEARCH_ENDPOINT_KEY);
        ret.scimOauthScope = config.get(SCIM_OAUTH_SCOPE_KEY);
        ret.scimClientId = config.get(SCIM_CLIENT_ID_KEY);
        ret.scimClientSecret = config.get(SCIM_CLIENT_SECRET);
        return ret;
    }

    public String getAuthTokenEndpoint() {

        return authTokenEndpoint;
    }

    public String getScimUserEndpoint() {

        return scimUserEndpoint;
    }


    public String getScimUserSearchEndpoint() {

        return scimUserSearchEndpoint;
    }

    public String getScimOauthScope() {

        return scimOauthScope;
    }

    public String getScimClientId() {

        return scimClientId;
    }

    public String getScimClientSecret() {

        return scimClientSecret;
    }

    public boolean isValid() {

        return authTokenEndpoint != null 
               && scimUserEndpoint != null
               && scimUserSearchEndpoint != null
               && scimOauthScope != null 
               && scimClientId != null
               && scimClientSecret != null;
    }
}
