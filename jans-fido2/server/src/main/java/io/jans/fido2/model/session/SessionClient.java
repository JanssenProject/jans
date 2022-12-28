/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.session;

import io.jans.as.common.model.registration.Client;
import jakarta.inject.Named;

import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * @author Javier Rojas Blum Date: 03.20.2012
 */
@Named
public class SessionClient {

    private Client client;
    private Long authenticationTime;


    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
        long authTime = -1L;
        if (client != null) {
            GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            authTime = c.getTimeInMillis();
        }
        setAuthenticationTime(authTime);
    }

    public Long getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(Long authenticationTime) {
        this.authenticationTime = authenticationTime;
    }
}