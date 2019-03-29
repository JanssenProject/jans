/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.session;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.inject.Named;

import org.xdi.oxauth.model.registration.Client;

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