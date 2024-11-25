/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.test.service;

import java.io.Serializable;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.client.ClientBuilder;

import jakarta.ws.rs.client.Invocation.Builder;




public class ResteasyService implements Serializable {

    private static final long serialVersionUID = 1L;
    protected Logger logger = LogManager.getLogger(getClass());


    public Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

   

}
