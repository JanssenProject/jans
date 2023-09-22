package io.jans.idp.keycloak.util;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.idp.keycloak.config.JansConfigSource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.broker.provider.util.SimpleHttp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JansUtil {
    private static Logger logger = LoggerFactory.getLogger(JansUtil.class);
    private JansConfigSource jansConfigSource = new JansConfigSource();
    private Map<String, String> configProperties = null;

    public JansUtil() {
        logger.debug("\nJ ansUtil() - Getting properties");
        configProperties = jansConfigSource.getProperties();
        if (configProperties == null || configProperties.isEmpty()) {
            throw new WebApplicationException("Config properties is null!!!");
        }
    }

    public String getTokenEndpoint() {
        logger.debug("\n JansUtil::getTokenEndpoint() - configProperties.get(\"token.endpoint\")():{}",
                configProperties.get("token.endpoint"));
        return configProperties.get("token.endpoint");
    }

    public String getScimUserEndpoint() {
        logger.debug(" \n JansUtil::getScimUserEndpoint() - configProperties.get(\"scim.user.endpoint\")():{}",
                configProperties.get("scim.user.endpoint"));
        return configProperties.get("scim.user.endpoint");
    }

    public String getScimUserSearchEndpoint() {
        logger.debug(
                "\n JansUtil::getScimUserSearchEndpoint() - configProperties.get(\"scim.user.search.endpoint\")():{}",
                configProperties.get("scim.user.search.endpoint"));
        return configProperties.get("scim.user.search.endpoint");
    }

    public String getClientId() {
        logger.debug(" \n JansUtil::getClientId() - configProperties.get(\"client.id\")():{}",
                configProperties.get("client.id"));
        return configProperties.get("client.id");
    }

    public String getClientPassword() {
        logger.debug(" \n JansUtil::getClientPassword() - configProperties.get(\"client.password\")():{}",
                configProperties.get("client.password"));
        return configProperties.get("client.password");
    }

    public String getScimOauthScope() {
        logger.debug(" \n  JansUtil::getScimOauthScope() - configProperties.get(\"scim.oauth.scope\")():{}",
                configProperties.get("scim.oauth.scope"));
        return configProperties.get("scim.oauth.scope");
    }

    public String requestScimAccessToken() throws IOException {
        logger.info(" \n JansUtil::requestScimAccessToken() ");
        List<String> scopes = new ArrayList<>();
        scopes.add(getScimOauthScope());
        String token = requestAccessToken(getClientId(), scopes);
        logger.info("JansUtil::requestScimAccessToken() - token:{} ", token);
        return token;
    }

    public String requestAccessToken(final String clientId, final List<String> scope) throws IOException {
        logger.info("JansUtil::requestAccessToken() - Request for AccessToken - clientId:{}, scope:{} ", clientId,
                scope);

        String tokenUrl = getTokenEndpoint();
        String token = getAccessToken(tokenUrl, clientId, scope);
        logger.info("JansUtil::requestAccessToken() - oAuth AccessToken response - token:{}", token);

        return token;
    }

    public String getAccessToken(final String tokenUrl, final String clientId, final List<String> scopes)
            throws IOException {
        logger.info("JansUtil::getAccessToken() - Access Token Request - tokenUrl:{}, clientId:{}, scopes:{}", tokenUrl,
                clientId, scopes);

        // Get clientSecret
        String clientSecret = this.getClientPassword();
        logger.info("JansUtil::getAccessToken() - Access Token Request - clientId:{}, clientSecret:{}", clientId,
                clientSecret);

        // distinct scopes
        Set<String> scopesSet = new HashSet<>(scopes);
        StringBuilder scope = new StringBuilder(Constants.SCOPE_TYPE_OPENID);
        for (String s : scopesSet) {
            scope.append(" ").append(s);
        }

        logger.info("JansUtil::getAccessToken() - Scope required  - {}", scope);

        String token = requestAccessToken(tokenUrl, clientId, clientSecret, scope.toString(),
                Constants.CLIENT_CREDENTIALS, Constants.CLIENT_SECRET_BASIC, MediaType.APPLICATION_FORM_URLENCODED);
        logger.info("JansUtil::getAccessToken() - Final token token  - {}", token);
        return token;
    }

    public String requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String scope, String grantType, String authenticationMethod, String mediaType) throws IOException {
        logger.info(
                "JansUtil::requestAccessToken() - Request for Access Token -  tokenUrl:{}, clientId:{}, clientSecret:{}, scope:{}, grantType:{}, authenticationMethod:{}, mediaType:{}",
                tokenUrl, clientId, clientSecret, scope, grantType, authenticationMethod, mediaType);
        String token = null;
        try {

            logger.info(" JansUtil::requestAccessToken() - this.getEncodedCredentials():{}",
                    this.getEncodedCredentials(clientId, clientSecret));
            HttpClient client = HttpClientBuilder.create().build();
            JsonNode jsonNode = SimpleHttp.doPost(tokenUrl, client)
                    .header("Authorization", "Basic " + this.getEncodedCredentials(clientId, clientSecret))
                    .header("Content-Type", mediaType).param("grant_type", "client_credentials")
                    .param("username", clientId + ":" + clientSecret).param("scope", scope).param("client_id", clientId)
                    .param("client_secret", clientSecret).param("authorization_method", "client_secret_basic").asJson();
            logger.info("\n JansUtil::requestAccessToken() - POST Request for Access Token -  jsonNode:{} ", jsonNode);

            token = this.getToken(jsonNode);

            logger.info("\n JansUtil::requestAccessToken() - After Post request for Access Token -  token:{} ", token);

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("\n JansUtil::requestAccessToken() - Post error is ", ex);
        }
        return token;
    }

    public String requestUserToken(final String tokenUrl, final String username, final String password,
            final String scope, String grantType, String authenticationMethod, String mediaType) throws IOException {
        logger.info(
                "JansUtil::requestUserToken() - Request for Access Token -  tokenUrl:{}, username:{}, password:{}, scope:{}, grantType:{}, authenticationMethod:{}, mediaType:{}",
                tokenUrl, username, password, scope, grantType, authenticationMethod, mediaType);
        String token = null;
        try {
            String clientId = this.getClientId();
            String clientSecret = this.getClientPassword();

            logger.info(
                    " JansUtil::requestUserToken() - clientId:{} , clientSecret:{}, this.getEncodedCredentials():{}",
                    clientId, clientSecret, this.getEncodedCredentials(clientId, clientSecret));
            HttpClient client = HttpClientBuilder.create().build();
            JsonNode jsonNode = SimpleHttp.doPost(tokenUrl, client)
                    .header("Authorization", "Basic " + this.getEncodedCredentials(clientId, clientSecret))
                    .header("Content-Type", mediaType).param("grant_type", grantType).param("username", username)
                    .param("password", password).asJson();

            logger.info("\n JansUtil::requestUserToken() - After invoking post request for user token -  jsonNode:{} ",
                    jsonNode);

            token = this.getToken(jsonNode);

            logger.info("\n JansUtil::requestUserToken() -POST Request for Access Token -  token:{} ", token);

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("\n JansUtil::requestUserToken() - Post error is ", ex);
        }
        return token;
    }

    private boolean validateTokenScope(JsonNode jsonNode, String scope) {

        logger.info(" \n\n JansUtil::validateTokenScope() - jsonNode:{}, scope:{}", jsonNode, scope);
        boolean validScope = false;
        try {

            List<String> scopeList = Stream.of(scope.split(" ", -1)).collect(Collectors.toList());

            if (jsonNode != null && jsonNode.get("scope") != null) {
                JsonNode value = jsonNode.get("scope");
                logger.info("\n\n *** JansUtil::validateTokenScope() -  value:{}", value);

                if (value != null) {
                    String responseScope = value.toString();
                    logger.info(
                            "JansUtil::validateTokenScope() - scope:{}, responseScope:{}, responseScope.contains(scope):{}",
                            scope, responseScope, responseScope.contains(scope));
                    if (scopeList.contains(responseScope)) {
                        validScope = true;
                    }
                }

            }
            logger.info("JansUtil::validateTokenScope() - validScope:{}", validScope);

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("\n JansUtil::validateTokenScope() - Error while validating token scope from response is ",
                    ex);
        }
        return validScope;

    }

    private String getToken(JsonNode jsonNode) {
        logger.info(" \n\n JansUtil::getToken() - jsonNode:{}", jsonNode);

        String token = null;
        try {

            if (jsonNode != null && jsonNode.get("access_token") != null) {
                JsonNode value = jsonNode.get("access_token");
                logger.info("\n\n *** JansUtil::getToken() - value:{}", value);

                if (value != null) {
                    token = value.asText();
                }
                logger.info("getToken() - token:{}", token);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("\n\n Error while getting token from response is ", ex);
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
        logger.info("getCredentials() - authUsername:{}, authPassword:{}", authUsername, authPassword);
        return URLEncoder.encode(authUsername, Constants.UTF8_STRING_ENCODING) + ":"
                + URLEncoder.encode(authPassword, Constants.UTF8_STRING_ENCODING);
    }

    private String getEncodedCredentials(String authUsername, String authPassword) {
        logger.info("getEncodedCredentials() - authUsername:{}, authPassword:{}", authUsername, authPassword);
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
