/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model;

import io.jans.as.server.register.ws.rs.RegisterRestWebService;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import jakarta.ws.rs.core.MediaType;

/**
 * @author Yuriy Zabrovarnyy
 */
public class WebServiceFactory {

    public static final String RESTV_1 = "restv1";

    private WebServiceFactory() {
    }

    public static WebServiceFactory instance() {
        return new WebServiceFactory();
    }

    public RegisterRestWebService createRegisterWs(String url) {
        return createRegisterWs(RegisterRestWebService.class, url, MediaType.APPLICATION_JSON_TYPE);
    }

    public <T> T createRegisterWs(Class<T> clazz, String url, MediaType consumeMediaType) {
        final ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient().target(url + RESTV_1);
        return target.proxyBuilder(clazz).defaultConsumes(consumeMediaType).build();
    }
}
