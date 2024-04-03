package io.jans.kc.spi.storage.config;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.keycloak.Config;

public class PluginConfiguration {

    private static final String AUTH_TOKEN_ENDPOINT_KEY = "auth-token-endpoint";
    private static final String SCIM_USER_ENDPOINT_KEY = "scim-user-endpoint";
    private static final String SCIM_USER_SEARCH_ENDPOINT_KEY = "scim-user-search-endpoint";
    private static final String SCIM_OAUTH_SCOPES_KEY = "scim-oauth-scopes";
    private static final String SCIM_CLIENT_ID_KEY = "scim-client-id";
    private static final String SCIM_CLIENT_SECRET = "scim-client-secret";
    
    private String authTokenEndpoint;
    private String scimUserEndpoint;
    private String scimUserSearchEndpoint;
    private List<String> scimOauthScopes;
    private String scimClientId;
    private String scimClientSecret;

    private PluginConfiguration() {

    }

    public static PluginConfiguration fromKeycloakConfiguration(Config.Scope config) {

        PluginConfiguration ret = new PluginConfiguration();
        ret.authTokenEndpoint = config.get(AUTH_TOKEN_ENDPOINT_KEY);
        ret.scimUserEndpoint = config.get(SCIM_USER_ENDPOINT_KEY);
        ret.scimUserSearchEndpoint = config.get(SCIM_USER_SEARCH_ENDPOINT_KEY);
        ret.scimOauthScopes = new ArrayList<>();
        String tmpscopes = config.get(SCIM_OAUTH_SCOPES_KEY);
        if(tmpscopes != null) {
            ret.scimOauthScopes = Arrays.asList(tmpscopes.split(","));
        }
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

    public List<String> getScimOauthScopes() {

        return scimOauthScopes;
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
               && scimOauthScopes != null 
               && scimClientId != null
               && scimClientSecret != null;
    }
}
