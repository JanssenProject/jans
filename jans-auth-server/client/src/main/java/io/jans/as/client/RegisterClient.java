/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.client.builder.RegistrationBuilder;
import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.register.ApplicationType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Encapsulates functionality to make Register request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version August 20, 2019
 */
public class RegisterClient extends BaseClient<RegisterRequest, RegisterResponse> {

    private static final Logger LOG = Logger.getLogger(RegisterClient.class);

    /**
     * Construct a register client by providing an URL where the REST service is located.
     *
     * @param url The REST service location.
     */
    public RegisterClient(String url) {
        super(url);
    }

    public static RegistrationBuilder builder() {
        return new RegistrationBuilder();
    }

    @Override
    public String getHttpMethod() {
        if (getRequest() != null) {
            if (StringUtils.isNotBlank(getRequest().getHttpMethod())) {
                return getRequest().getHttpMethod();
            }
            if (getRequest().getRegistrationAccessToken() != null) {
                return HttpMethod.GET;
            }
        }

        return HttpMethod.POST;
    }

    /**
     * Executes the call to the REST service requesting to register and process the response.
     *
     * @param applicationType The application type.
     * @param clientName      The client name.
     * @param redirectUri     A list of space-delimited redirection URIs.
     * @return The service response.
     */
    public RegisterResponse execRegister(ApplicationType applicationType,
                                         String clientName, List<String> redirectUri) {
        setRequest(new RegisterRequest(applicationType, clientName, redirectUri));

        return exec();
    }

    public RegisterResponse exec() {
        initClient();
        return _exec();
    }

    /**
     * @deprecated Engine should be shared between clients
     */
    @SuppressWarnings("java:S1133")
    @Deprecated
    public RegisterResponse exec(ClientHttpEngine engine) {
        resteasyClient = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        webTarget = resteasyClient.target(getUrl());

        return _exec();
    }

    private RegisterResponse _exec() {
        try {
            // Prepare request parameters
            Entity<?> requestEntity = null;
            Builder clientRequest = webTarget.request();
            applyCookies(clientRequest);

            // POST - Client Register, PUT - update client
            if (getHttpMethod().equals(HttpMethod.POST) || getHttpMethod().equals(HttpMethod.PUT)) {
                clientRequest.header("Content-Type", getRequest().getContentType());
                clientRequest.accept(getRequest().getMediaType());

                if (StringUtils.isNotBlank(getRequest().getRegistrationAccessToken())) {
                    clientRequest.header("Authorization", "Bearer " + getRequest().getRegistrationAccessToken());
                }

                String bodyString = getRequest().hasJwtRequestAsString() ? getRequest().getJwtRequestAsString() : ClientUtil.toPrettyJson(getRequest().getJSONParameters());

                requestEntity = Entity.json(bodyString);
            } else { // GET, Client Read
                clientRequest.accept(MediaType.APPLICATION_JSON);

                if (StringUtils.isNotBlank(getRequest().getRegistrationAccessToken())) {
                    clientRequest.header("Authorization", "Bearer " + getRequest().getRegistrationAccessToken());
                }
            }

            // Call REST Service and handle response

            if (getHttpMethod().equals(HttpMethod.POST)) {
                clientResponse = clientRequest.buildPost(requestEntity).invoke();
            } else if (getHttpMethod().equals(HttpMethod.PUT)) {
                clientResponse = clientRequest.buildPut(requestEntity).invoke();
            } else if (getHttpMethod().equals(HttpMethod.DELETE)) {
                clientResponse = clientRequest.buildDelete().invoke();
            } else { // GET
                clientResponse = clientRequest.buildGet().invoke();
            }
            setResponse(new RegisterResponse(clientResponse));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}