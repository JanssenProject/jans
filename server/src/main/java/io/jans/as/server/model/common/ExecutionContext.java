/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.registration.Client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ExecutionContext {

    private final HttpServletRequest httpRequest;
    private final HttpServletResponse httpResponse;
    private Client client;

    public ExecutionContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
