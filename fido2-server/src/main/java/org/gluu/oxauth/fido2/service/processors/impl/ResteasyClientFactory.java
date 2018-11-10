/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.service.processors.impl;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

@ApplicationScoped
public class ResteasyClientFactory {

    private ResteasyClientBuilder resteasyClientBuilder;

    @PostConstruct
    public void init() {
        this.resteasyClientBuilder = new ResteasyClientBuilder();
    }

    public ResteasyClient buildResteasyClient() {
        return resteasyClientBuilder.build();
    }

}