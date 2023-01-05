/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.token.ClientAssertionType;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Form;
import org.apache.commons.lang.StringUtils;


/**
 * @author Yuriy Zabrovarnyy
 */
public class ClientAuthnEnabler {

    private final Builder clientRequest;
    private final Form requestForm;

    public ClientAuthnEnabler(Builder clientRequest, Form requestForm) {
        this.clientRequest = clientRequest;
        this.requestForm = requestForm;
    }

    public void exec(ClientAuthnRequest request) {
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_BASIC
                && request.hasCredentials()) {
            clientRequest.header("Authorization", "Basic " + request.getEncodedCredentials());
            return;
        }

        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_POST) {
            if (request.getAuthUsername() != null && !request.getAuthUsername().isEmpty()) {
                requestForm.param("client_id", request.getAuthUsername());
            }
            if (request.getAuthPassword() != null && !request.getAuthPassword().isEmpty()) {
                requestForm.param("client_secret", request.getAuthPassword());
            }
            return;
        }
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_JWT ||
                request.getAuthenticationMethod() == AuthenticationMethod.PRIVATE_KEY_JWT) {
            requestForm.param("client_assertion_type", ClientAssertionType.JWT_BEARER.toString());

            final String clientAssertion = request.getClientAssertion();
            if (clientAssertion != null) {
                requestForm.param("client_assertion", clientAssertion);
            }
            if (StringUtils.isNotBlank(request.getAuthUsername())) {
                requestForm.param("client_id", request.getAuthUsername());
            }
        }
    }
}
