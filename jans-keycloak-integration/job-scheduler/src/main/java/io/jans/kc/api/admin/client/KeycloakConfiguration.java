package io.jans.kc.api.admin.client;

import io.jans.kc.scheduler.config.AppConfiguration;

public class KeycloakConfiguration {
    
    private String serverUrl;
    private String realm;
    private String username;
    private String password;
    private String clientId;
    private Integer connPoolSize = 0;

    private KeycloakConfiguration() {

    }

    public String serverUrl() {

        return this.serverUrl;
    }

    public String realm() {

        return this.realm;
    }

    public String username() {

        return this.username;
    }

    public String password() {

        return password;
    }

    public String clientId() {

        return this.clientId;
    }


    public Integer connPoolSize() {
        return this.connPoolSize;
    }

    public static KeycloakConfiguration fromAppConfiguration(AppConfiguration config) {

        nullOrEmptyConstraint(config.keycloakAdminUrl(),"Missing keycloak admin url");
        nullOrEmptyConstraint(config.keycloakAdminRealm(),"Missing keycloak admin realm");
        nullOrEmptyConstraint(config.keycloakAdminClientId(), "Mising keycloak admin client id");
        nullOrEmptyConstraint(config.keycloakAdminUsername(),"Missing keycloak admin username");
        nullOrEmptyConstraint(config.keycloakAdminPassword(),"Missing keycloak admin password");

         KeycloakConfiguration ret = new KeycloakConfiguration();
         ret.serverUrl = config.keycloakAdminUrl();
         ret.realm = config.keycloakAdminRealm();
         ret.username = config.keycloakAdminUsername();
         ret.password = config.keycloakAdminPassword();
         ret.clientId = config.keycloakAdminClientId();
         ret.connPoolSize = config.keycloakAdminConnPoolSize();

         return ret;
    }
        

    private static final void nullOrEmptyConstraint(String value, String errormsg) {

        if(value == null || value.isEmpty()) {
            throw new KeycloakConfigurationError(errormsg);
        }
    }
}
