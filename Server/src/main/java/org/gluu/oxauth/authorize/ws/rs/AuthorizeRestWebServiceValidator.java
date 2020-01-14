package org.gluu.oxauth.authorize.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.gluu.oxauth.model.authorize.JwtAuthorizationRequest;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.ClientService;
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

    public Client validate(String clientId, String state) {
        if (StringUtils.isBlank(clientId)) {
            throw new WebApplicationException(Response
                    .status(Response.Status.UNAUTHORIZED)
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

    public boolean validateAuthenticationMaxAge(Integer maxAge, SessionId sessionUser, Client client, JwtAuthorizationRequest jwtAuthorizationRequest, boolean invalidOpenidRequestObject) {
        Integer authenticationMaxAge = null;
        if (maxAge != null) {
            authenticationMaxAge = maxAge;
        } else if (!invalidOpenidRequestObject && jwtAuthorizationRequest != null
                && jwtAuthorizationRequest.getIdTokenMember() != null
                && jwtAuthorizationRequest.getIdTokenMember().getMaxAge() != null) {
            authenticationMaxAge = jwtAuthorizationRequest.getIdTokenMember().getMaxAge();
        }
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        GregorianCalendar userAuthenticationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        if (sessionUser.getAuthenticationTime() != null) {
            userAuthenticationTime.setTime(sessionUser.getAuthenticationTime());
        }
        if (authenticationMaxAge != null) {
            userAuthenticationTime.add(Calendar.SECOND, authenticationMaxAge);
            return userAuthenticationTime.after(now);
        } else if (client.getDefaultMaxAge() != null) {
            userAuthenticationTime.add(Calendar.SECOND, client.getDefaultMaxAge());
            return userAuthenticationTime.after(now);
        }
        return true;
    }
}
