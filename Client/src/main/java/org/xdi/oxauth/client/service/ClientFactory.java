/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.service;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */

public class ClientFactory {

    private final static ClientFactory INSTANCE = new ClientFactory();

    private ClientFactory() {
    }

    public static ClientFactory instance() {
        return INSTANCE;
    }

    public IntrospectionService createIntrospectionService(String p_url) {
        return ProxyFactory.create(IntrospectionService.class, p_url);
    }

    public IntrospectionService createIntrospectionService(String p_url, ClientExecutor clientExecutor) {
        return ProxyFactory.create(IntrospectionService.class, p_url, clientExecutor);
    }
}
