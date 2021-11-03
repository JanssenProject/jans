/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import org.jboss.resteasy.client.ClientRequest;

import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.token.ClientAssertionType;


/**
 * @author Yuriy Zabrovarnyy
 */
public class ClientAuthnEnabler {

    @SuppressWarnings("java:S1874")
    private final ClientRequest clientRequest;

    @SuppressWarnings("java:S1874")
    public ClientAuthnEnabler(ClientRequest clientRequest) {
        this.clientRequest = clientRequest;
    }

    public void exec(ClientAuthnRequest request){
        if (addBasic(request)) {
            return;
        }

        if (addSecretPost(request)) {
            return;
        }

        addJwt(request);
    }

    private void addJwt(ClientAuthnRequest request) {
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_JWT ||
                request.getAuthenticationMethod() == AuthenticationMethod.PRIVATE_KEY_JWT) {
            clientRequest.formParameter("client_assertion_type", ClientAssertionType.JWT_BEARER);
            if (request.getClientAssertion() != null) {
                clientRequest.formParameter("client_assertion", request.getClientAssertion());
            }
            if (request.getAuthUsername() != null && !request.getAuthUsername().isEmpty()) {
                clientRequest.formParameter("client_id", request.getAuthUsername());
            }
        }
    }

    private boolean addSecretPost(ClientAuthnRequest request) {
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_POST) {
            if (request.getAuthUsername() != null && !request.getAuthUsername().isEmpty()) {
                clientRequest.formParameter("client_id", request.getAuthUsername());
            }
            if (request.getAuthPassword() != null && !request.getAuthPassword().isEmpty()) {
                clientRequest.formParameter("client_secret", request.getAuthPassword());
            }
            return true;
        }
        return false;
    }

    private boolean addBasic(ClientAuthnRequest request) {
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_BASIC
                && request.hasCredentials()) {
            clientRequest.header("Authorization", "Basic " + request.getEncodedCredentials());
            return true;
        }
        return false;
    }
}
