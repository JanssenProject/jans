/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.util.exception.InvalidAttributeException;
import io.jans.configapi.plugin.saml.util.Constants;

import io.jans.configapi.core.util.Jackson;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IdpClientFactory {

    private static Logger logger = LoggerFactory.getLogger(IdpClientFactory.class);
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer  ";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String ACCESS_TOKEN_NULL = "Access Token is null!!!";
    public static final String IDP_URL_NULL = "IDP URL is null!!!";

    public static String getAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String grantType, final String scope, final String username, final String password,
            final String serverUrl) throws JsonProcessingException {
        logger.info("Get  tokenUrl:{}, clientId:{}, grantType:{}, scope:{}, username:{}, serverUrl:{}", tokenUrl,
                clientId, grantType, scope, username, serverUrl);

        Builder request = getClientBuilder(tokenUrl);
        request.header(AUTHORIZATION, "Basic " + clientId + ":" + clientSecret);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>();
        multivaluedHashMap.add("client_id", clientId);
        multivaluedHashMap.add("client_secret", clientSecret);
        multivaluedHashMap.add("grant_type",
                (StringUtils.isNotBlank(grantType) ? grantType.toLowerCase() : "password"));
        multivaluedHashMap.add("scope", scope);
        multivaluedHashMap.add("username", username);
        multivaluedHashMap.add("password", password);
        multivaluedHashMap.add("redirect_uri", serverUrl);
        Response response = request.post(Entity.form(multivaluedHashMap));

        String token = null;
        if (response != null) {
            logger.trace(
                    "Response for Access Token -  response.getStatus():{}, response.getStatusInfo():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo(), response.getEntity().getClass());
            String entity = response.readEntity(String.class);
            if (response.getStatusInfo().equals(Status.OK)) {
                token = Jackson.getElement(entity, Constants.ACCESS_TOKEN);
            } else {
                throw new WebApplicationException(
                        "Error while Access Token is " + response.getStatusInfo() + " - " + entity, response);
            }
        }

        return token;
    }

    public String getAllIdp(String idpUrl, String token) {
        logger.info(" All IDP - idpUrl:{}", idpUrl);

        Builder client = getClientBuilder(idpUrl);
        client.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        client.header(AUTHORIZATION, BEARER + token);
        Response response = client.get();
        logger.debug("All IDP - response:{}", response);

        String identityProviderJsonList = null;
        if (response != null) {
            logger.trace(
                    "Fetch all IDP response.getStatus():{}, response.getStatusInfo():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo(), response.getEntity().getClass());
            String entity = response.readEntity(String.class);
            logger.trace("Get All IDP entity:{}", entity);
            if (response.getStatusInfo().equals(Status.OK)) {

                identityProviderJsonList = entity;
            } else {
                throw new WebApplicationException(
                        "Error while fetching All IDP is " + response.getStatusInfo() + " - " + entity, response);
            }
        }

        return identityProviderJsonList;
    }

    public String getIdp(String idpUrl, String token) {
        logger.info(" Fetch IDP - idpUrl:{}", idpUrl);
        Builder client = getClientBuilder(idpUrl);
        client.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        client.header(AUTHORIZATION, BEARER + token);
        Response response = client.get();
        logger.debug("Fetch IDP - response:{}", response);
        String identityProviderJson = null;
        if (response != null) {
            logger.trace(
                    "IDP -  response.getStatus():{}, response.getStatusInfo():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo(), response.getEntity().getClass());
            String entity = response.readEntity(String.class);
            if (response.getStatusInfo().equals(Status.OK)) {
                identityProviderJson = entity;

            } else {
                throw new WebApplicationException(
                        "Error while fetching IDP is " + response.getStatusInfo() + " - " + entity, response);
            }
        }

        return identityProviderJson;
    }

    public Map<String, String> extractSamlMetadata(final String idpMetadataConfigUrl, final String token,
            final String providerId, String realmName, InputStream idpMetadataStream) throws IOException {
        Map<String, String> config = null;
       
            logger.info("Saml Idp Metadata idpMetadataConfigUrl:{}, providerId:{}, realmName:{}, idpMetadataStream:{}",
                    idpMetadataConfigUrl, providerId, realmName, idpMetadataStream);

            if (StringUtils.isBlank(token)) {
                throw new InvalidAttributeException(ACCESS_TOKEN_NULL);
            }
            if (idpMetadataStream == null) {
                throw new InvalidAttributeException("Idp Metedata file is null!!!");
            }

            Builder request = getClientBuilder(idpMetadataConfigUrl);
            request.header(AUTHORIZATION, BEARER + token);
            MultipartFormDataOutput formData = new MultipartFormDataOutput();
            formData.addFormData("providerId", providerId, MediaType.TEXT_PLAIN_TYPE);
            logger.debug("SAML idpMetadataStream.available():{}", idpMetadataStream.available());

            byte[] content = idpMetadataStream.readAllBytes();
            logger.debug("content:{}", content);
            String body = new String(content, StandardCharsets.UTF_8);
            formData.addFormData("file", body, MediaType.APPLICATION_OCTET_STREAM_TYPE);

            logger.info("Request for SAML metadata import - formData:{}", formData);
            Entity<MultipartFormDataOutput> formDataEntity = Entity.entity(formData, MediaType.MULTIPART_FORM_DATA);
            Response response = request.post(formDataEntity);
            logger.trace("Response for SAML metadata  import-  response:{}", response);

            if (response != null) {
                logger.trace(
                        "extract Saml Metadata -  response.getStatus():{}, response.getStatusInfo():{}, response.getEntity().getClass():{}",
                        response.getStatus(), response.getStatusInfo(), response.getEntity().getClass());
                String entity = response.readEntity(String.class);

                if (response.getStatusInfo().equals(Status.OK)) {
                    ObjectMapper mapper = Jackson.createJsonMapper();
                    config = mapper.readValue(entity, Map.class);

                } else {
                    throw new WebApplicationException(
                            "Error while validating SAML IDP Metadata " + response.getStatusInfo() + " - " + entity, response);
                }
            }

       

        return config;
    }

    public String createUpdateIdp(final String idpUrl, final String token, boolean isUpdate,
            JSONObject identityProviderJson) {
        String idpJson = null;
       
            logger.info("Add/modify IDP idpUrl:{}, isUpdate:{}, identityProviderJson:{}", idpUrl, isUpdate,
                    identityProviderJson);

            if (StringUtils.isBlank(idpUrl)) {
                throw new InvalidAttributeException(IDP_URL_NULL);
            }
            if (StringUtils.isBlank(token)) {
                throw new InvalidAttributeException(ACCESS_TOKEN_NULL);
            }
            if (identityProviderJson == null) {
                throw new InvalidAttributeException("IDP Json object is null!!!");
            }

            Builder request = getClientBuilder(idpUrl);
            request.header(AUTHORIZATION, BEARER + token);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
            Response response = null;

            if (isUpdate) {
                logger.debug(" Update SAML IDP in KC server -  identityProviderJson.toMap():{}",
                        identityProviderJson.toMap());
                response = request.put(Entity.json(identityProviderJson.toMap()));
            } else {
                logger.debug(" Create SAML IDP in KC server -  identityProviderJson.toMap():{}",
                        identityProviderJson.toMap());
                response = request.post(Entity.json(identityProviderJson.toMap()));
            }

            logger.debug("Response for SAML IDP -  response:{}", response);
            String url = idpUrl;
            String name = identityProviderJson.getString(Constants.ALIAS);
            logger.debug("Add/Update IDP Id -  name:{}", name);

            if (response != null) {
                logger.debug(
                        "IDP Add/Update - isUpdate:{}, response.getStatus():{}, response.getStatusInfo():{}, response.getEntity():{},response.getStatusInfo().equals(Status.OK):{},  response.getStatusInfo().equals(Status.CREATED):{}, , response.getStatusInfo().equals(Status.NO_CONTENT):{}",
                        isUpdate, response.getStatus(), response.getStatusInfo(), response.getEntity(),
                        response.getStatusInfo().equals(Status.OK), response.getStatusInfo().equals(Status.CREATED),
                        response.getStatusInfo().equals(Status.NO_CONTENT));

                String entity = response.readEntity(String.class);
                logger.debug("Add/Update IDP entity:{}", entity);

                if (isUpdate && (response.getStatusInfo().equals(Status.OK)
                        || response.getStatusInfo().equals(Status.NO_CONTENT))) {
                    logger.debug(
                            "Successful response for Update IDP request - identityProviderJson:{}, status:{}, entity:{}",
                            identityProviderJson, response.getStatusInfo(), entity);

                } else if (!isUpdate && (response.getStatusInfo().equals(Status.OK)
                        || response.getStatusInfo().equals(Status.CREATED))) {
                    url = idpUrl + "/" + name;
                    logger.debug(
                            "Successful response for Add IDP request - identityProviderJson:{}, status:{}, entity:{}, url:{}",
                            identityProviderJson, response.getStatusInfo(), entity, url);

                } else {
                    logger.error("Error while creating/updating IDP - identityProviderJson:{}, status:{}, entity:{}",
                            identityProviderJson, response.getStatusInfo(), entity);
                    throw new WebApplicationException("Error while creating/updating IDP" + identityProviderJson
                            + ", Status is " + response.getStatusInfo() + " - " + entity, response);
                }

                idpJson = getIdp(url, token);
                logger.debug("Added/Updated IDP -  idpJson:{}", idpJson);
            }

       

        return idpJson;
    }

    public boolean deleteIdp(final String idpUrl, final String token) {
        boolean isDeleted = false;
       
            logger.info("Delete IDP idpUrl:{}", idpUrl);

            if (StringUtils.isBlank(idpUrl)) {
                throw new InvalidAttributeException(IDP_URL_NULL);
            }
            if (StringUtils.isBlank(token)) {
                throw new InvalidAttributeException(ACCESS_TOKEN_NULL);
            }

            Builder request = getClientBuilder(idpUrl);
            request.header(AUTHORIZATION, BEARER + token);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
            Response response = request.delete();
            logger.debug("Response for SAML IDP deletion -  response:{}", response);

            if (response != null) {
                logger.debug(
                        "Delete IDP  -  response.getStatus():{}, response.getStatusInfo():{}, response.getEntity():{}",
                        response.getStatus(), response.getStatusInfo(), response.getEntity());
                String entity = response.readEntity(String.class);
                logger.trace("Delete IDP entity:{}", entity);
                if (response.getStatusInfo().equals(Status.NO_CONTENT)) {
                    isDeleted = true;
                } else {
                    throw new WebApplicationException(
                            "Error while deleting IDP " + response.getStatusInfo() + " - " + entity, response);
                }
            }

      

        return isDeleted;
    }

    public String getSpMetadata(String metadataEndpoint, final String token) {
        logger.info(" SP Metadata - metadataEndpoint:{}", metadataEndpoint);
        String jsonStrn = null;

        Builder request = getClientBuilder(metadataEndpoint);
        request.header(AUTHORIZATION, BEARER + token);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        logger.debug("SpMetadata- response:{}", response);

        if (response != null) {
            logger.trace(
                    "IDP Add/Update - response.getStatus():{}, response.getStatusInfo():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo(), response.getEntity().getClass());
            if (response.getStatusInfo().equals(Status.OK)) {
                jsonStrn = response.readEntity(String.class);
            } else {
                throw new WebApplicationException(
                        "Error while fetching SP Metadata " + response.getStatusInfo() + " - " + jsonStrn, response);
            }
        }
        return jsonStrn;
    }

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

}
