/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.jwk.JSONWebKeySet;
import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import io.jans.configapi.util.Jackson;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthClientService {

    private static Logger log = LoggerFactory.getLogger(AuthClientService.class);

    public static String getIntrospectionEndpoint(String issuer) throws Exception {
        log.debug("\n\n AuthClientService::getIntrospectionEndpoint() - issuer = " + issuer);
        String configurationEndpoint = issuer + "/.well-known/openid-configuration";
        log.trace("\n\n AuthClientService::getIntrospectionEndpoint() - configurationEndpoint = "
                + configurationEndpoint);

        Builder request = ResteasyClientBuilder.newClient().target(configurationEndpoint).request();
        request.header("Content-Type", MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.debug("\n\n AuthClientService::getIntrospectionEndpoint() - response = " + response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("\n\n AuthClientService::getIntrospectionEndpoint() - entity = " + entity);
            return Jackson.getElement(entity, "introspection_endpoint");
        }
        return null;
    }

    public static IntrospectionResponse getIntrospectionResponse(String url, String header, String token,
            boolean followRedirects) {
        log.debug("\n\n AuthClientService:::getIntrospectionResponse() - url = " + url + " , header = " + header
                + " , token = " + token + " , followRedirects = " + followRedirects);

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(url);
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken(header, token);
        log.debug("\n\n AuthClientService:::getIntrospectionResponse() - url = " + url + " , header = " + header
                + " , token = " + token + " , followRedirects = " + followRedirects + " , introspectionResponse = "
                + introspectionResponse);

        return introspectionResponse;

    }

    public static String getJwksUri(String issuer) throws Exception {
        log.debug("\n\n AuthClientService::getJwksUri() - issuer = " + issuer);
        String configurationEndpoint = issuer + "/.well-known/openid-configuration";
        log.trace("\n\n AuthClientService::getJwksUri() - configurationEndpoint = " + configurationEndpoint);

        Builder request = ResteasyClientBuilder.newClient().target(configurationEndpoint).request();
        request.header("Content-Type", MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.debug("\n\n AuthClientService::getJwksUri() - response = " + response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("\n\n AuthClientService::getJwksUri() - entity = " + entity);
            return Jackson.getElement(entity, "jwks_uri");
        }
        return null;
    }

    public static JSONWebKeySet getJSONWebKeys(String jwksUri) throws Exception {
        log.trace("\n\n AuthClientService::getJSONWebKeys() - jwksUri = " + jwksUri);

        Builder request = ResteasyClientBuilder.newClient().target(jwksUri).request();
        request.header("Content-Type", MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.trace("\n\n AuthClientService::getJSONWebKeys() - response = " + response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("\n\n AuthClientService::getJSONWebKeys() - entity = " + entity);
            JwkResponse jwkResponse = new JwkResponse(200);
            JSONWebKeySet jwks = null;
            if (StringUtils.isNotBlank(entity)) {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(JSON_WEB_KEY_SET)) {
                    jwks = JSONWebKeySet.fromJSONObject(jsonObj);
                    jwkResponse.setJwks(jwks);
                }
                log.trace("\n\n AuthClientService::getJSONWebKeys() - jwkResponse = " + jwkResponse + " , jwks = "
                        + jwks);
                return jwks;
            }

        }
        return null;
    }

    public static TokenResponse requestAccessToken(final String tokenUrl, final String clientId,
            final String clientSecret, final String scope) {

        log.debug("\n\n AuthClientService::requestAccessToken() - tokenUrl = " + tokenUrl + " , clientId = " + clientId
                + " , clientSecret = " + clientSecret + " , scope = " + scope);

        Builder request = ResteasyClientBuilder.newClient().target(tokenUrl).request();
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope(scope);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        TokenClient tokenClient = new TokenClient(tokenUrl);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        log.debug("\n\n AuthClientService::requestAccessToken() - tokenResponse = " + tokenResponse);

        if (tokenResponse.getStatus() == 200) {

            return tokenResponse;
        }

        return null;

    }

    /*
     * public static IntrospectionService getIntrospectionService(String url,
     * boolean followRedirects) { return createIntrospectionService(url,
     * followRedirects); }
     * 
     * public static UmaMetadataService getUmaMetadataService(String umaMetadataUri,
     * boolean followRedirects) { return createUmaMetadataService(umaMetadataUri,
     * followRedirects); }
     * 
     * public static UmaPermissionService getUmaPermissionService(UmaMetadata
     * umaMetadata, boolean followRedirects) { return
     * createUmaPermissionService(umaMetadata); }
     * 
     * public static UmaRptIntrospectionService
     * getUmaRptIntrospectionService(UmaMetadata umaMetadata, boolean
     * followRedirects) { return createUmaRptIntrospectionService(umaMetadata); }
     * 
     * public static IntrospectionResponse getIntrospectionResponse(String url,
     * String header, String token, boolean followRedirects) {
     * log.info("\n\n AuthClientService:::getIntrospectionResponse() - url = "
     * +url+" , header = "+header+" , token = "+token+" , followRedirects = "
     * +followRedirects);
     * 
     * ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
     * RestClientBuilder restClient =
     * RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
     * .property("Content-Type", MediaType.APPLICATION_JSON).register(engine);
     * restClient.property("Authorization", "Basic " + header);
     * 
     * ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration())
     * .property("Content-Type", MediaType.APPLICATION_JSON).target(url);
     * 
     * IntrospectionService proxy = target.proxy(IntrospectionService.class);
     * 
     * return proxy.introspectToken(header, token);
     * 
     * }
     * 
     * public static TokenResponse revokeToken(final String revokeTokenUrl, final
     * String clientId, final String clientSecret, final String token) {
     * 
     * Builder request =
     * ResteasyClientBuilder.newClient().target(revokeTokenUrl).request();
     * TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
     * tokenRequest.setAuthUsername(clientId);
     * tokenRequest.setAuthPassword(clientSecret);
     * 
     * final MultivaluedHashMap<String, String> multivaluedHashMap = new
     * MultivaluedHashMap( tokenRequest.getParameters());
     * multivaluedHashMap.add(token, "token"); multivaluedHashMap.add(clientId,
     * "clientId"); request.header("Authorization", "Basic " +
     * tokenRequest.getEncodedCredentials());
     * 
     * request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
     * 
     * ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
     * RestClientBuilder restClient = RestClientBuilder.newBuilder()
     * .baseUri(UriBuilder.fromPath(revokeTokenUrl).build()).register(engine);
     * restClient.property("Authorization", "Basic " +
     * tokenRequest.getEncodedCredentials()); restClient.property("Content-Type",
     * MediaType.APPLICATION_FORM_URLENCODED);
     * 
     * ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration())
     * .target(revokeTokenUrl);
     * 
     * Response response = request.post(Entity.form(multivaluedHashMap));
     * 
     * if (response.getStatus() == 200) { String entity =
     * response.readEntity(String.class);
     * 
     * TokenResponse tokenResponse = new TokenResponse();
     * tokenResponse.setEntity(entity); tokenResponse.injectDataFromJson(entity);
     * 
     * return tokenResponse; } return null;
     * 
     * }
     * 
     * public static TokenResponse requestAccessToken(final String tokenUrl, final
     * String clientId, final String clientSecret, final String scope) {
     * 
     * Builder request =
     * ResteasyClientBuilder.newClient().target(tokenUrl).request(); TokenRequest
     * tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
     * tokenRequest.setScope(scope); tokenRequest.setAuthUsername(clientId);
     * tokenRequest.setAuthPassword(clientSecret);
     * 
     * final MultivaluedHashMap<String, String> multivaluedHashMap = new
     * MultivaluedHashMap( tokenRequest.getParameters());
     * request.header("Authorization", "Basic " +
     * tokenRequest.getEncodedCredentials());
     * 
     * request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
     * 
     * ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
     * RestClientBuilder restClient =
     * RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(tokenUrl).build())
     * .register(engine); restClient.property("Authorization", "Basic " +
     * tokenRequest.getEncodedCredentials()); restClient.property("Content-Type",
     * MediaType.APPLICATION_FORM_URLENCODED);
     * 
     * ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration())
     * .target(tokenUrl); Response response =
     * request.post(Entity.form(multivaluedHashMap));
     * 
     * if (response.getStatus() == 200) { String entity =
     * response.readEntity(String.class);
     * 
     * TokenResponse tokenResponse = new TokenResponse();
     * tokenResponse.setEntity(entity); tokenResponse.injectDataFromJson(entity);
     * 
     * return tokenResponse; }
     * 
     * return null;
     * 
     * }
     * 
     * public static TokenResponse patRequest(final String tokenUrl, final String
     * clientId, final String clientSecret, final String scope) {
     * 
     * Builder request =
     * ResteasyClientBuilder.newClient().target(tokenUrl).request(); TokenRequest
     * tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
     * tokenRequest.setScope(scope); tokenRequest.setAuthUsername(clientId);
     * tokenRequest.setAuthPassword(clientSecret);
     * tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC
     * );
     * 
     * request.header("Authorization", "Basic " +
     * tokenRequest.getEncodedCredentials()); request.header("Content-Type",
     * MediaType.APPLICATION_FORM_URLENCODED);
     * 
     * ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
     * RestClientBuilder restClient =
     * RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(tokenUrl).build())
     * .register(engine); restClient.property("Authorization", "Basic " +
     * tokenRequest.getEncodedCredentials()); restClient.property("Content-Type",
     * MediaType.APPLICATION_FORM_URLENCODED);
     * 
     * ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration())
     * .target(tokenUrl);
     * 
     * Response response = request .post(Entity.form(new MultivaluedHashMap<String,
     * String>(tokenRequest.getParameters())));
     * 
     * if (response.getStatus() == 200) { String entity =
     * response.readEntity(String.class);
     * 
     * TokenResponse tokenResponse = new TokenResponse();
     * tokenResponse.setEntity(entity); tokenResponse.injectDataFromJson(entity);
     * 
     * return tokenResponse; } return null;
     * 
     * }
     * 
     * public static TokenResponse requestRpt(final String tokenUrl, final String
     * clientId, final String clientSecret, final List<String> scopes, final String
     * ticket, GrantType grantType, AuthenticationMethod authMethod) {
     * 
     * String scope = null; if (scopes != null && scopes.size() > 0) { for (String s
     * : scopes) { scope = scope + " " + s; } }
     * 
     * Builder request =
     * ResteasyClientBuilder.newClient().target(tokenUrl).request(); TokenRequest
     * tokenRequest = new TokenRequest(grantType);
     * tokenRequest.setAuthUsername(clientId);
     * tokenRequest.setAuthPassword(clientSecret); tokenRequest.setScope(scope);
     * tokenRequest.setAuthenticationMethod(authMethod);
     * 
     * final MultivaluedHashMap<String, String> multivaluedHashMap = new
     * MultivaluedHashMap( tokenRequest.getParameters());
     * multivaluedHashMap.add("ticket", ticket); request.header("Authorization",
     * "Basic " + tokenRequest.getEncodedCredentials());
     * request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
     * 
     * ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
     * RestClientBuilder restClient =
     * RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(tokenUrl).build())
     * .register(engine); restClient.property("Authorization", "Basic " +
     * tokenRequest.getEncodedCredentials()); restClient.property("Content-Type",
     * MediaType.APPLICATION_FORM_URLENCODED);
     * 
     * ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration())
     * .target(tokenUrl); Response response =
     * request.post(Entity.form(multivaluedHashMap));
     * 
     * if (response.getStatus() == 200) { String entity =
     * response.readEntity(String.class); TokenResponse tokenResponse = new
     * TokenResponse(); tokenResponse.setEntity(entity);
     * tokenResponse.injectDataFromJson(entity); return tokenResponse; } return
     * null;
     * 
     * }
     * 
     * public static String getIntrospectionEndpoint(String issuer) throws Exception
     * { log.trace("\n\n AuthClientService::getIntrospectionEndpoint() - issuer = "
     * + issuer); String configurationEndpoint = issuer +
     * "/.well-known/openid-configuration"; log.
     * trace("\n\n AuthClientService::getIntrospectionEndpoint() - configurationEndpoint = "
     * + configurationEndpoint);
     * 
     * Builder request =
     * ResteasyClientBuilder.newClient().target(configurationEndpoint).request();
     * request.header("Content-Type", MediaType.APPLICATION_JSON); Response response
     * = request.get();
     * log.trace("\n\n AuthClientService::getIntrospectionEndpoint() - response = "
     * + response);
     * 
     * if (response.getStatus() == 200) { String entity =
     * response.readEntity(String.class);
     * log.trace("\n\n AuthClientService::getIntrospectionEndpoint() - entity = " +
     * entity); return Jackson.getElement(entity, "introspection_endpoint"); }
     * return null; }
     * 
     * private static IntrospectionService createIntrospectionService(String url,
     * boolean followRedirects) { ApacheHttpClient43Engine engine =
     * ClientFactory.createEngine(followRedirects); RestClientBuilder restClient =
     * RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
     * .register(engine); ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration()) .target(url);
     * IntrospectionService proxy = target.proxy(IntrospectionService.class); return
     * proxy;
     * 
     * }
     * 
     * private static UmaMetadataService createUmaMetadataService(String url,
     * boolean followRedirects) { ApacheHttpClient43Engine engine =
     * ClientFactory.createEngine(followRedirects); RestClientBuilder restClient =
     * RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
     * .property("Content-Type", MediaType.APPLICATION_JSON).register(engine);
     * ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration())
     * .property("Content-Type", MediaType.APPLICATION_JSON).target(url);
     * UmaMetadataService proxy = target.proxy(UmaMetadataService.class); return
     * proxy; }
     * 
     * private static UmaPermissionService createUmaPermissionService(UmaMetadata
     * umaMetadata) { ApacheHttpClient43Engine engine =
     * ClientFactory.createEngine(false); RestClientBuilder restClient =
     * RestClientBuilder.newBuilder()
     * .baseUri(UriBuilder.fromPath(umaMetadata.getPermissionEndpoint()).build()).
     * register(engine); ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration())
     * .target(umaMetadata.getPermissionEndpoint()); UmaPermissionService proxy =
     * target.proxy(UmaPermissionService.class); return proxy; }
     * 
     * private static UmaRptIntrospectionService
     * createUmaRptIntrospectionService(UmaMetadata umaMetadata) {
     * ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
     * RestClientBuilder restClient = RestClientBuilder.newBuilder()
     * .baseUri(UriBuilder.fromPath(umaMetadata.getIntrospectionEndpoint()).build())
     * .register(engine); ResteasyWebTarget target = (ResteasyWebTarget)
     * ResteasyClientBuilder.newClient(restClient.getConfiguration())
     * .target(umaMetadata.getPermissionEndpoint()); UmaRptIntrospectionService
     * proxy = target.proxy(UmaRptIntrospectionService.class); return proxy; }
     * 
     * public static String getJwksUri(String issuer) throws Exception {
     * log.trace("\n\n AuthClientService::getJwksUri() - issuer = " + issuer);
     * String configurationEndpoint = issuer + "/.well-known/openid-configuration";
     * log.trace("\n\n AuthClientService::getJwksUri() - configurationEndpoint = " +
     * configurationEndpoint);
     * 
     * Builder request =
     * ResteasyClientBuilder.newClient().target(configurationEndpoint).request();
     * request.header("Content-Type", MediaType.APPLICATION_JSON); Response response
     * = request.get();
     * log.trace("\n\n AuthClientService::getJwksUri() - response = " + response);
     * 
     * if (response.getStatus() == 200) { String entity =
     * response.readEntity(String.class);
     * log.trace("\n\n AuthClientService::getJwksUri() - entity = " + entity);
     * return Jackson.getElement(entity, "jwks_uri"); } return null; }
     * 
     * public static JSONWebKeySet getJSONWebKeys(String jwksUri) throws Exception {
     * log.trace("\n\n AuthClientService::getJSONWebKeys() - jwksUri = " + jwksUri);
     * 
     * Builder request =
     * ResteasyClientBuilder.newClient().target(jwksUri).request();
     * request.header("Content-Type", MediaType.APPLICATION_JSON); Response response
     * = request.get();
     * log.trace("\n\n AuthClientService::getJSONWebKeys() - response = " +
     * response);
     * 
     * if (response.getStatus() == 200) { String entity =
     * response.readEntity(String.class);
     * log.trace("\n\n AuthClientService::getJSONWebKeys() - entity = " + entity);
     * JwkResponse jwkResponse = new JwkResponse(200); JSONWebKeySet jwks = null; if
     * (StringUtils.isNotBlank(entity)) { JSONObject jsonObj = new
     * JSONObject(entity); if (jsonObj.has(JSON_WEB_KEY_SET)) { jwks =
     * JSONWebKeySet.fromJSONObject(jsonObj); jwkResponse.setJwks(jwks); }
     * log.trace("\n\n AuthClientService::getJSONWebKeys() - jwkResponse = " +
     * jwkResponse + " , jwks = " + jwks); return jwks; }
     * 
     * } return null; }
     */
}
