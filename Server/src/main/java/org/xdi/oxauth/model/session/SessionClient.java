package org.xdi.oxauth.model.session;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.xdi.oxauth.model.registration.Client;

import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * @author Javier Rojas Blum Date: 03.20.2012
 */
@Name("sessionClient")
@AutoCreate
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