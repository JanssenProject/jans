/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.client;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
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