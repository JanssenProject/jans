/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.util.exception.InvalidAttributeException;
import io.jans.util.exception.ConfigurationException;
import io.jans.configapi.plugin.saml.model.IdentityProvider;
import io.jans.configapi.plugin.saml.util.Constants;

import io.jans.configapi.core.util.Jackson;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IdpClientFactory {

    private static Logger logger = LoggerFactory.getLogger(IdpClientFactory.class);
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    public static String getAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String grantType, final String scope, final String username, final String password,
            final String serverUrl) throws JsonProcessingException {
        logger.error(
                "Get  tokenUrl:{}, clientId:{}, clientSecret:{}, grantType:{}, scope:{}, username:{}, password:{}, serverUrl:{}",
                tokenUrl, clientId, clientSecret, grantType, scope, username, password, serverUrl);

        Builder request = getClientBuilder(tokenUrl);
        logger.error("request:{}", request);
        request.header("Authorization", "Basic " + clientId + ":" + clientSecret);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>();
        multivaluedHashMap.add("client_id", clientId);
        multivaluedHashMap.add("client_secret", clientSecret);
        multivaluedHashMap.add("grant_type", "password");
        multivaluedHashMap.add("scope", scope);
        multivaluedHashMap.add("username", username);
        multivaluedHashMap.add("password", password);
        multivaluedHashMap.add("redirect_uri", serverUrl);
        logger.error("Request for Access Token -  multivaluedHashMap:{}", multivaluedHashMap);

        Response response = request.post(Entity.form(multivaluedHashMap));
        logger.error("Response for Access Token -  response:{}", response);
        String token = null;
        if (response != null) {
            logger.error(
                    "Response for Access Token -  response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
            String entity = response.readEntity(String.class);
            logger.error("Access Token -  entity:{}", entity);
            token = Jackson.getElement(entity, Constants.ACCESS_TOKEN);
            logger.error("Access Token -  token:{}", token);
        }

        return token;
    }

    public Map<String, String> extractSamlMetadata(final String idpMetadataConfigUrl, final String token,
            final String providerId, String realmName, InputStream idpMetadataStream) throws JsonProcessingException {
        Map<String, String> config = null;
        try {
            logger.error(
                    "Saml Idp Metadata idpMetadataConfigUrl:{}, token:{}, providerId:{}, realmName:{}, idpMetadataStream:{}",
                    idpMetadataConfigUrl, token, providerId, realmName, idpMetadataStream);

            if (StringUtils.isBlank(token)) {
                throw new InvalidAttributeException("Access Token is null!!!");
            }
            if (idpMetadataStream == null) {
                throw new InvalidAttributeException("Idp Metedata file is null!!!");
            }

            Builder request = getClientBuilder(idpMetadataConfigUrl);
            logger.error("request:{}", request);
            request.header("Authorization", "Bearer  " + token);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

            MultipartFormDataOutput formData = new MultipartFormDataOutput();
            formData.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);
            logger.error("SAML idpMetadataStream.available():{}", idpMetadataStream.available());

            byte[] content = idpMetadataStream.readAllBytes();
            logger.error("content:{}", content);
            String body = new String(content, Charset.forName("utf-8"));
            formData.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

            logger.error("Request for SAML metadata import - formData:{}", formData);
            Entity<MultipartFormDataOutput> formDataEntity = Entity.entity(formData, MediaType.MULTIPART_FORM_DATA);
            logger.error("Request for SAML metadata import - formDataEntity:{}", formDataEntity);
            Response response = request.post(formDataEntity);
            logger.error("Response for SAML metadata  import-  response:{}", response);

            if (response != null) {
                logger.error(
                        "extract Saml Metadata -  response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                        response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
                if (response.getStatusInfo().equals(Status.OK)) {
                    String entity = response.readEntity(String.class);
                    logger.error("entity:{}", entity);
                    ObjectMapper mapper = Jackson.createJsonMapper();
                    config = mapper.readValue(entity, Map.class);
                    logger.error("config:{}", config);

                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ConfigurationException("Error while validating SAML IDP Metadata", ex);
        }

        return config;
    }

    public Response getSpMetadata(String metadataEndpoint) {
        logger.info(" SP Metadata - metadataEndpoint:{}", metadataEndpoint);
        Builder metadataClient = getClientBuilder(metadataEndpoint);
        metadataClient.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = metadataClient.get();
        logger.debug("SpMetadata- response:{}", response);

        if (response != null) {
            logger.trace(
                    "SP metadata response.getStatusInfo():{}, response.getEntity():{}, response.getEntity().getClass():{}",
                    response.getStatusInfo(), response.getEntity(), response.getEntity().getClass());
        }

        return response;
    }

    public List<IdentityProvider> getAllIdp(String idpUrl, String token) throws JsonProcessingException, JsonMappingException {
        logger.error(" All IDP - idpUrl:{}", idpUrl);
        Builder client = getClientBuilder(idpUrl);
        client.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        client.header(AUTHORIZATION, token);
        Response response = client.get();
        logger.debug("SpMetadata- response:{}", response);
        List<IdentityProvider> identityProviderList = null;
        if (response != null) {
            logger.error(
                    "extract Saml Metadata -  response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
            if (response.getStatusInfo().equals(Status.OK)) {
                String entity = response.readEntity(String.class);
                logger.error("entity:{}", entity);
                ObjectMapper mapper = Jackson.createJsonMapper();
                identityProviderList = mapper.readValue(entity, List.class);
                logger.error("identityProviderList:{}", identityProviderList);

            }
        }

        return identityProviderList;
    }

    private String getElement(String jsonString, String name) throws JsonProcessingException {
        logger.error("Json element -  jsonString:{}, name:{}", jsonString, name);
        if (StringUtils.isBlank(jsonString) || StringUtils.isBlank(name)) {
            return null;
        }
        return Jackson.getElement(name, name);
    }

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

}
