/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.client;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.jans.util.exception.InvalidAttributeException;
import io.jans.util.exception.ConfigurationException;

import io.jans.configapi.core.util.Jackson;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public String getAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String grantType, final String scope, final String username, final String password,
            final String serverUrl) {
        String token = null;
        try {
            logger.error(
                    "Get  tokenUrl:{}, clientId:{}, clientSecret:{}, grantType:{}, scope:{}, username:{}, password:{}, serverUrl:{}",
                    tokenUrl, clientId, clientSecret, grantType, scope, username, password, serverUrl);

            Response response = requestAccessToken(tokenUrl, clientId, clientSecret, grantType, scope, username,
                    password, serverUrl);
            logger.error("Access Token -  response:{}", response);

            if (response != null) {
                token = getElement(response.getEntity().toString(), "access_token");
                logger.error("Access Token -  token:{}", token);
            }
        } catch (Exception ex) {
            logger.error(" Error while getting access token - ex:{}", ex);
            throw new InvalidAttributeException("Error while getting access token - ex:{}", ex);
        }

        return token;
    }

    public static Response requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String grantType, final String scope, final String username, final String password,
            final String serverUrl) {
        logger.error(
                "Get  tokenUrl:{}, clientId:{}, clientSecret:{}, grantType:{}, scope:{}, username:{}, password:{}, serverUrl:{}",
                tokenUrl, clientId, clientSecret, grantType, scope, username, password, serverUrl);
        Response response = null;
        try {
            Builder request = getClientBuilder(tokenUrl);
            logger.error("request:{}", request);
            request.header("Authorization", "Basic " + clientId + ":" + clientSecret);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

            final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>();
            multivaluedHashMap.add("client_id", clientId);
            multivaluedHashMap.add("client_secret", clientSecret);
            multivaluedHashMap.add("grant_type", grantType);
            multivaluedHashMap.add("scope", scope);
            multivaluedHashMap.add("username", username);
            multivaluedHashMap.add("password", password);
            multivaluedHashMap.add("redirect_uri", serverUrl);
            logger.error("Request for Access Token -  multivaluedHashMap:{}", multivaluedHashMap);

            response = request.post(Entity.form(multivaluedHashMap));
            logger.error("Response for Access Token -  response:{}", response);
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                logger.error("Access Token -  entity:{}", entity);
            }
        } catch (Exception ex) {
            logger.error("IdpClientFactory Exception  requestAccessToken is - ", ex);
        } finally {

            /*
             * if (response != null) { response.close(); }
             */
        }
        return response;
    }

    public Response importSamlMetadata(final String idpMetadataConfigUrl, final String token, final String providerId,
            String realmName, InputStream idpMetadataStream) {
        Response response = null;
        try {
            logger.info(
                    "Import Saml Idp Metadata idpMetadataConfigUrl:{}, token:{}, providerId:{}, realmName:{}, idpMetadataStream:{}",
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
            logger.debug("SAML idpMetadataStream.available():{}", idpMetadataStream.available());

            byte[] content = idpMetadataStream.readAllBytes();
            logger.debug("content:{}", content);
            String body = new String(content, Charset.forName("utf-8"));
            formData.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

            logger.error("Request for SAML metadata import - formData:{}", formData);
            Entity<MultipartFormDataOutput> formDataEntity = Entity.entity(formData, MediaType.MULTIPART_FORM_DATA);
            logger.error("Request for SAML metadata import - formDataEntity:{}", formDataEntity);
            response = request.post(formDataEntity);
            logger.error("Response for SAML metadata  import-  response:{}", response);
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                logger.error("Access Token -  entity:{}", entity);
            }

        } catch (Exception ex) {
            throw new ConfigurationException("Error while validating SAML IDP Metadata", ex);
        }

        return response;
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

    public Response getAllIdp(String idpUrl, String token) {
        logger.info(" All IDP - idpUrl:{}", idpUrl);
        Builder client = getClientBuilder(idpUrl);
        client.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        client.header(AUTHORIZATION, token);
        Response response = client.get();
        logger.debug("SpMetadata- response:{}", response);

        if (response != null) {
            logger.trace(
                    "SP metadata response.getStatusInfo():{}, response.getEntity():{}, response.getEntity().getClass():{}",
                    response.getStatusInfo(), response.getEntity(), response.getEntity().getClass());
        }

        return response;
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
