package io.jans.configapi.plugin.saml.model.config;

import java.io.Serializable;

public class SamlConfig implements Serializable {
    

    private static final long serialVersionUID = 2187359904897235211L;
    
    private String serverUrl;
    private String realm;
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    
     public String getServerUrl() {
         return serverUrl;
     }
     public void setServerUrl(String serverUrl) {
         this.serverUrl = serverUrl;
     }
     public String getRealm() {
         return realm;
     }
     public void setRealm(String realm) {
         this.realm = realm;
     }
     public String getUsername() {
         return username;
     }
     public void setUsername(String username) {
         this.username = username;
     }
     public String getPassword() {
         return password;
     }
     public void setPassword(String password) {
         this.password = password;
     }
     public String getClientId() {
         return clientId;
     }
     public void setClientId(String clientId) {
         this.clientId = clientId;
     }
     public String getClientSecret() {
         return clientSecret;
     }
     public void setClientSecret(String clientSecret) {
         this.clientSecret = clientSecret;
     }
     
     @Override
     public String toString() {
         return "KeycloakConfig [serverUrl=" + serverUrl + ", realm=" + realm + ", username=" + username + ", clientId="
                 + clientId + "]";
     }
    
}
