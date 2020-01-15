package org.gluu.oxauth.authorize.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.persist.exception.EntryPersistenceException;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * @author Yuriy Zabrovarnyy
 */
@Named
@Stateless
public class AuthorizeRestWebServiceValidator {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ClientService clientService;

    public Client validateClient(String clientId, String state) {
        if (StringUtils.isBlank(clientId)) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, "client_id is empty or blank."))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }

        try {
            final Client client = clientService.getClient(clientId);
            if (client == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.UNAUTHORIZED)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, "Unable to find client."))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
            }
            if (client.isDisabled()) {
                throw new WebApplicationException(Response
                        .status(Response.Status.UNAUTHORIZED)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.DISABLED_CLIENT, state, "Client is disabled."))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
            }

            return client;
        } catch (EntryPersistenceException e) { // Invalid clientId
            throw new WebApplicationException(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, "Unable to find client on AS."))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }
    }

    public boolean validateAuthnMaxAge(Integer maxAge, SessionId sessionUser, Client client) {
        if (maxAge == null) {
            maxAge = client.getDefaultMaxAge();
        }

        GregorianCalendar userAuthnTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        if (sessionUser.getAuthenticationTime() != null) {
            userAuthnTime.setTime(sessionUser.getAuthenticationTime());
        }
        if (maxAge != null) {
            userAuthnTime.add(Calendar.SECOND, maxAge);
            return userAuthnTime.after(ServerUtil.now());
        }
        return true;
    }

    public void validateRequestJwt(String request, String requestUri, String state) {
        if (StringUtils.isNotBlank(request) && StringUtils.isNotBlank(requestUri)) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "Both request and request_uri are specified which is not allowed."))
                    .build());
        }
    }
}
