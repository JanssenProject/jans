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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.json.JSONObject;
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
        multivaluedHashMap.add("grant_type", (StringUtils.isNotBlank(grantType)?grantType.toLowerCase():"password"));
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
            if (response.getStatusInfo().equals(Status.OK)) {
            token = Jackson.getElement(entity, Constants.ACCESS_TOKEN);
            logger.error("Access Token -  token:{}", token);
            }else {
                throw new WebApplicationException("Error while Access Token is "+response.getStatusInfo()+" - "+entity);
            }
        }

        return token;
    }

    public String getAllIdp(String idpUrl, String token) throws JsonProcessingException, JsonMappingException {
        logger.error(" All IDP - idpUrl:{}, token:{}", idpUrl, token);
        
        Builder client = getClientBuilder(idpUrl);
        client.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        client.header(AUTHORIZATION,"Bearer  " + token);
        Response response = client.get();
        logger.debug("All IDP - response:{}", response);
        String identityProviderJsonList = null;
        // List<IdentityProvider> identityProviderList = null;
        if (response != null) {
            logger.error(
                    "Fetch all IDP response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
            String entity = response.readEntity(String.class);
            logger.error("entity:{}", entity);
            if (response.getStatusInfo().equals(Status.OK)) {

                identityProviderJsonList = entity;
                // ObjectMapper mapper = Jackson.createJsonMapper();
                // identityProviderList = mapper.readValue(entity, List.class);
                // logger.error("identityProviderList:{}", identityProviderList);

            }else {
                throw new WebApplicationException("Error while fetching All IDP is "+response.getStatusInfo()+" - "+entity);
            }
        }

        return identityProviderJsonList;
    }

    public String getIdp(String idpUrl, String token) throws JsonProcessingException, JsonMappingException {
        logger.error(" Fetch IDP - idpUrl:{}", idpUrl);
        Builder client = getClientBuilder(idpUrl);
        client.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        client.header(AUTHORIZATION,"Bearer  " + token);
        Response response = client.get();
        logger.debug("Fetch IDP - response:{}", response);
        String identityProviderJson = null;
        if (response != null) {
            logger.error(
                    "IDP -  response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
            String entity = response.readEntity(String.class);
            logger.error("entity:{}", entity);
            if (response.getStatusInfo().equals(Status.OK)) {          
                identityProviderJson = entity;
                // ObjectMapper mapper = Jackson.createJsonMapper();
                // identityProvider = mapper.readValue(entity, IdentityProvider.class);
                // logger.error("identityProvider:{}", identityProvider);

            }else {
                throw new WebApplicationException("Error while fetching IDP is "+response.getStatusInfo()+" - "+entity);
            }
        }

        return identityProviderJson;
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
            //request.header(CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA);

            MultipartFormDataOutput formData = new MultipartFormDataOutput();
            formData.addFormData("providerId", providerId, MediaType.TEXT_PLAIN_TYPE);
            logger.debug("SAML idpMetadataStream.available():{}", idpMetadataStream.available());

            byte[] content = idpMetadataStream.readAllBytes();
            logger.debug("content:{}", content);
            String body = new String(content, Charset.forName("utf-8"));
            formData.addFormData("file", body, MediaType.APPLICATION_OCTET_STREAM_TYPE);

            logger.error("Request for SAML metadata import - formData:{}", formData);
            Entity<MultipartFormDataOutput> formDataEntity = Entity.entity(formData, MediaType.MULTIPART_FORM_DATA);
            logger.error("Request for SAML metadata import - formDataEntity:{}", formDataEntity.toString());
            Response response = request.post(formDataEntity);
            logger.error("Response for SAML metadata  import-  response:{}", response);

            if (response != null) {
                logger.error(
                        "extract Saml Metadata -  response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                        response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
                String entity = response.readEntity(String.class);
                logger.error("entity:{}", entity);
                if (response.getStatusInfo().equals(Status.OK)) {                    
                    ObjectMapper mapper = Jackson.createJsonMapper();
                    config = mapper.readValue(entity, Map.class);
                    logger.error("config:{}", config);

                }else {
                          throw new WebApplicationException("Error while validating SAML IDP Metadata "+response.getStatusInfo()+" - "+entity);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new WebApplicationException("Error while validating SAML IDP Metadata", ex);
        }

        return config;
    }

    public String createUpdateIdp(final String idpUrl, final String token, boolean isUpdate,
            String identityProviderJson) {
        String idpJson = null;
        try {
            logger.error("Add/modify IDP idpUrl:{}, token:{}, isUpdate:{}, identityProviderJson:{}", idpUrl, token,
                    isUpdate, identityProviderJson);

            if (StringUtils.isBlank(idpUrl)) {
                throw new InvalidAttributeException("IDP URL is null!!!");
            }
            if (StringUtils.isBlank(token)) {
                throw new InvalidAttributeException("Access Token is null!!!");
            }
            if (StringUtils.isBlank(identityProviderJson)) {
                throw new InvalidAttributeException("Idp json is null!!!");
            }

            Builder request = getClientBuilder(idpUrl);
            logger.error("request:{}", request);
            request.header("Authorization", "Bearer  " + token);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

            String objectJson = Jackson.getJsonString(identityProviderJson);
            logger.error(" SAML IDP JSON - objectJson:{}", objectJson);

            Response response = null;
            if (isUpdate) {
                response = request.put(Entity.json(objectJson));
            } else {
                response = request.post(Entity.json(objectJson));
            }

            logger.error("Response for SAML IDP -  response:{}", response);

            if (response != null) {
                logger.error(
                        "IDP Add/Update - response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                        response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
                String entity = response.readEntity(String.class);
                logger.error("Add/Update IDP entity:{}", entity);
                if (response.getStatusInfo().equals(Status.OK)) {

                    String name = Jackson.getElement(identityProviderJson, Constants.ALIAS);
                    logger.error("Add/Update IDP Id -  name:{}", name);
                    idpJson = getIdp(idpUrl + "/" + name, token);
                }else {
                    throw new WebApplicationException("Error while Adding/Updating IDP "+response.getStatusInfo()+" - "+entity);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ConfigurationException("Error while add/updating SAML IDP", ex);
        }

        return idpJson;
    }
    
    
    public String createUpdateIdp(final String idpUrl, final String token, boolean isUpdate,
            JSONObject identityProviderJson) {
        String idpJson = null;
        try {
            logger.error("Add/modify IDP idpUrl:{}, token:{}, isUpdate:{}, identityProviderJson:{}", idpUrl, token,
                    isUpdate, identityProviderJson);

            if (StringUtils.isBlank(idpUrl)) {
                throw new InvalidAttributeException("IDP URL is null!!!");
            }
            if (StringUtils.isBlank(token)) {
                throw new InvalidAttributeException("Access Token is null!!!");
            }
            if (identityProviderJson == null) {
                throw new InvalidAttributeException("IDP Json object is null!!!");
            }

            Builder request = getClientBuilder(idpUrl);
            logger.error("request:{}", request);
            request.header("Authorization", "Bearer  " + token);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

            String objectJson = Jackson.getJsonString(identityProviderJson);
            logger.error(" SAML IDP JSON - objectJson:{}", objectJson);

            Response response = null;
            if (isUpdate) {
                response = request.put(Entity.json(objectJson));
            } else {
                response = request.post(Entity.json(objectJson));
            }

            logger.error("Response for SAML IDP -  response:{}", response);

            if (response != null) {
                logger.error(
                        "IDP Add/Update - response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                        response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
                String entity = response.readEntity(String.class);
                logger.error("Add/Update IDP entity:{}", entity);
                if (response.getStatusInfo().equals(Status.OK)) {

                    String name = identityProviderJson.getString(Constants.ALIAS);
                    logger.error("Add/Update IDP Id -  name:{}", name);
                    idpJson = getIdp(idpUrl + "/" + name, token);
                }else {
                    throw new WebApplicationException("Error while Adding/Updating IDP "+response.getStatusInfo()+" - "+entity);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ConfigurationException("Error while add/updating SAML IDP", ex);
        }

        return idpJson;
    }

    public boolean deleteIdp(final String idpUrl, final String token) {
        boolean isDeleted = false;
        try {
            logger.error("Delete IDP idpUrl:{}, token:{}", idpUrl, token);

            if (StringUtils.isBlank(idpUrl)) {
                throw new InvalidAttributeException("IDP URL is null!!!");
            }
            if (StringUtils.isBlank(token)) {
                throw new InvalidAttributeException("Access Token is null!!!");
            }

            Builder request = getClientBuilder(idpUrl);
            logger.error("request:{}", request);
            request.header("Authorization", "Bearer  " + token);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

            Response response = request.delete();
            logger.error("Response for SAML IDP deletion -  response:{}", response);

            if (response != null) {
                logger.error(
                        "Delete IDP  -  response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                        response.getStatus(), response.getStatusInfo(), response.getEntity().getClass());
                String entity = response.readEntity(String.class);
                logger.error("Delete IDP entity:{}", entity);
                if (response.getStatusInfo().equals(Status.OK)) {                   
                    isDeleted = true;
                }else {
                    throw new WebApplicationException("Error while deleting SP Metadata "+response.getStatusInfo()+" - "+entity);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ConfigurationException("Error while deleting SAML IDP", ex);
        }

        return isDeleted;
    }

    public String getSpMetadata(String metadataEndpoint, final String token) {
        logger.info(" SP Metadata - metadataEndpoint:{}, token:{}", metadataEndpoint, token);
        String jsonStrn = null;

        Builder request = getClientBuilder(metadataEndpoint);
        logger.error("request:{}", request);
        request.header("Authorization", "Bearer  " + token);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        logger.debug("SpMetadata- response:{}", response);

        if (response != null) {
            logger.error(
                    "IDP Add/Update - response.getStatus():{}, response.getStatusInfo().toString():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo().toString(), response.getEntity().getClass());
            if (response.getStatusInfo().equals(Status.OK)) {
                jsonStrn = response.readEntity(String.class);
                logger.error("Add/Update IDP jsonStrn:{}", jsonStrn);
            }else {
                throw new WebApplicationException("Error while fetching SP Metadata "+response.getStatusInfo()+" - "+jsonStrn);
            }
        }
        return jsonStrn;
    }

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

}
