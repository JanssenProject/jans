package io.jans.kc.spi.storage.util;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.kc.spi.storage.config.PluginConfiguration;

import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.jboss.logging.Logger;

import org.keycloak.broker.provider.util.SimpleHttp;

public class JansUtil {

    private static Logger log = Logger.getLogger(JansUtil.class);
    private PluginConfiguration pluginConfiguration;

    public JansUtil(PluginConfiguration pluginConfiguration) {

        this.pluginConfiguration = pluginConfiguration;
        if (this.pluginConfiguration == null || !this.pluginConfiguration.isValid()) {
            throw new RuntimeException("Plugin configuration missing or invalid");
        }
    }

    public String getTokenEndpoint() {
        log.debugv("JansUtil::getTokenEndpoint() - {0}",
                pluginConfiguration.getAuthTokenEndpoint());

        return pluginConfiguration.getAuthTokenEndpoint();
    }

    public String getScimUserEndpoint() {
        log.debugv("JansUtil::getScimUserEndpoint() - {0}",
                pluginConfiguration.getScimUserEndpoint());
        
        return pluginConfiguration.getScimUserEndpoint();
    }

    public String getScimUserSearchEndpoint() {
        log.debugv(
                "JansUtil::getScimUserSearchEndpoint() - {0}",
                pluginConfiguration.getScimUserSearchEndpoint());
        return pluginConfiguration.getScimUserSearchEndpoint();
    }

    public String getScimClientId() {
        log.debugv("JansUtil::getScimClientId() - {0}",
               pluginConfiguration.getScimClientId());
        return pluginConfiguration.getScimClientId();
    }

    public String getScimClientSecret() {
        log.debugv("JansUtil::getClientPassword() - {0}",
                pluginConfiguration.getScimClientSecret());
        return pluginConfiguration.getScimClientSecret();
    }

    public List<String> getScimOauthScopes() {
        log.debugv("JansUtil::getScimOauthScope() - {0}",
                pluginConfiguration.getScimOauthScopes());
        return pluginConfiguration.getScimOauthScopes();
    }

    public String requestScimAccessToken() throws IOException {
        log.debug("JansUtil::requestScimAccessToken() ");
        List<String> scopes = getScimOauthScopes();
        String token = requestAccessToken(getScimClientId(), scopes);
        log.debugv("JansUtil::requestScimAccessToken() - token:{0} ", token);
        return token;
    }

    public String requestAccessToken(final String clientId, final List<String> scope) throws IOException {
        log.debugv("JansUtil::requestAccessToken() - Request for AccessToken - clientId:{0}, scope:{1} ", clientId,
                scope);

        String tokenUrl = getTokenEndpoint();
        String token = getAccessToken(tokenUrl, clientId, scope);
        log.debugv("JansUtil::requestAccessToken() - oAuth AccessToken response - token:{0}", token);

        return token;
    }

    public String getAccessToken(final String tokenUrl, final String clientId, final List<String> scopes)
            throws IOException {
        log.debugv("JansUtil::getAccessToken() - Access Token Request - tokenUrl:{0}, clientId:{1}, scopes:{2}", tokenUrl,
                clientId, scopes);

        // Get clientSecret
        String clientSecret = getScimClientSecret();
        log.debugv("JansUtil::getAccessToken() - Access Token Request - clientId:{0}, clientSecret:{1}", clientId,
                clientSecret);

        // distinct scopes
        Set<String> scopesSet = new HashSet<>(scopes);
        StringBuilder scope = new StringBuilder(Constants.SCOPE_TYPE_OPENID);
        for (String s : scopesSet) {
            scope.append(" ").append(s);
        }

        log.debugv("JansUtil::getAccessToken() - Scope required  - {0}", scope);

        String token = requestAccessToken(tokenUrl, clientId, clientSecret, scope.toString(),
                Constants.CLIENT_CREDENTIALS, Constants.CLIENT_SECRET_BASIC, MediaType.APPLICATION_FORM_URLENCODED);
        log.debugv("JansUtil::getAccessToken() - Final token token  - {0}", token);
        return token;
    }

    public String requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String scope, String grantType, String authenticationMethod, String mediaType) throws IOException {
        log.debugv(
                "JansUtil::requestAccessToken() - Request for Access Token -  tokenUrl:{0}, clientId:{1}, clientSecret:{2}, scope:{3}, grantType:{4}, authenticationMethod:{5}, mediaType:{6}",
                tokenUrl, clientId, clientSecret, scope, grantType, authenticationMethod, mediaType);
        String token = null;
        try {

            log.debugv("JansUtil::requestAccessToken() - this.getEncodedCredentials():{0}",
                    this.getEncodedCredentials(clientId, clientSecret));
            HttpClient client = HttpClientBuilder.create().build();
            JsonNode jsonNode = SimpleHttp.doPost(tokenUrl, client)
                    .header("Authorization", "Basic " + this.getEncodedCredentials(clientId, clientSecret))
                    .header("Content-Type", mediaType).param("grant_type", "client_credentials")
                    .param("username", clientId + ":" + clientSecret).param("scope", scope).param("client_id", clientId)
                    .param("client_secret", clientSecret).param("authorization_method", "client_secret_basic").asJson();
            log.debugv("JansUtil::requestAccessToken() - POST Request for Access Token -  jsonNode:{0} ", jsonNode);

            token = this.getToken(jsonNode);

            log.debugv("\n JansUtil::requestAccessToken() - After Post request for Access Token -  token:{0} ", token);

        } catch (Exception ex) {
            log.error("JansUtil::requestAccessToken() - Post error is ",ex);
        }
        return token;
    }

    public String requestUserToken(final String tokenUrl, final String username, final String password,
            final String scope, String grantType, String authenticationMethod, String mediaType) throws IOException {
        log.debugv(
                "JansUtil::requestUserToken() - Request for Access Token -  tokenUrl:{0}, username:{1}, password:{2}, scope:{3}, grantType:{4}, authenticationMethod:{5}, mediaType:{6}",
                tokenUrl, username, password, scope, grantType, authenticationMethod, mediaType);
        String token = null;
        try {
            String clientId = this.getScimClientId();
            String clientSecret = this.getScimClientSecret();

            log.debugv(
                    " JansUtil::requestUserToken() - clientId:{0} , clientSecret:{1}, this.getEncodedCredentials():{2}",
                    clientId, clientSecret, this.getEncodedCredentials(clientId, clientSecret));
            HttpClient client = HttpClientBuilder.create().build();
            JsonNode jsonNode = SimpleHttp.doPost(tokenUrl, client)
                    .header("Authorization", "Basic " + this.getEncodedCredentials(clientId, clientSecret))
                    .header("Content-Type", mediaType).param("grant_type", grantType).param("username", username)
                    .param("password", password).asJson();

            log.debugv("JansUtil::requestUserToken() - After invoking post request for user token -  jsonNode:{0}",
                    jsonNode);

            token = this.getToken(jsonNode);

            log.debugv("\n JansUtil::requestUserToken() -POST Request for Access Token -  token:{0} ", token);

        } catch (Exception ex) {
            log.errorv("\n JansUtil::requestUserToken() - Error getting user token", ex);
        }
        return token;
    }

    private boolean validateTokenScope(JsonNode jsonNode, String scope) {

        log.debugv("JansUtil::validateTokenScope() - jsonNode:{0}, scope:{1}", jsonNode, scope);
        boolean validScope = false;
        try {

            List<String> scopeList = Stream.of(scope.split(" ", -1)).collect(Collectors.toList());

            if (jsonNode != null && jsonNode.get("scope") != null) {
                JsonNode value = jsonNode.get("scope");
                log.debugv("JansUtil::validateTokenScope() -  value:{0}", value);

                if (value != null) {
                    String responseScope = value.toString();
                    log.debugv(
                            "JansUtil::validateTokenScope() - scope:{0}, responseScope:{1}, responseScope.contains(scope):{2}",
                            scope, responseScope, responseScope.contains(scope));
                    if (scopeList.contains(responseScope)) {
                        validScope = true;
                    }
                }

            }
            log.debugv("JansUtil::validateTokenScope() - validScope:{0}", validScope);

        } catch (Exception ex) {
            log.error("JansUtil::validateTokenScope() - Error while validating token scope from response is ",
                    ex);
        }
        return validScope;

    }

    private String getToken(JsonNode jsonNode) {
        log.debugv("JansUtil::getToken() - jsonNode:{0}", jsonNode);

        String token = null;
        try {

            if (jsonNode != null && jsonNode.get("access_token") != null) {
                JsonNode value = jsonNode.get("access_token");
                log.debugv("JansUtil::getToken() - value:{0}", value);

                if (value != null) {
                    token = value.asText();
                }
                log.debugv("getToken() - token:{0}", token);
            }
        } catch (Exception ex) {
            log.errorv("Error while getting token from response", ex);
        }
        return token;
    }

    private boolean hasCredentials(String authUsername, String authPassword) {
        return (StringUtils.isNotBlank(authUsername) && StringUtils.isNotBlank(authPassword));
    }

    /**
     * Returns the client credentials (URL encoded).
     *
     * @return The client credentials.
     */
    private String getCredentials(String authUsername, String authPassword) throws UnsupportedEncodingException {
        log.debugv("getCredentials() - authUsername:{0}, authPassword:{1}", authUsername, authPassword);
        return URLEncoder.encode(authUsername, Constants.UTF8_STRING_ENCODING) + ":"
                + URLEncoder.encode(authPassword, Constants.UTF8_STRING_ENCODING);
    }

    private String getEncodedCredentials(String authUsername, String authPassword) {
        log.debugv("getEncodedCredentials() - authUsername:{0}, authPassword:{1}", authUsername, authPassword);
        try {
            if (hasCredentials(authUsername, authPassword)) {
                return Base64.encodeBase64String(getBytes(getCredentials(authUsername, authPassword)));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static byte[] getBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

}
