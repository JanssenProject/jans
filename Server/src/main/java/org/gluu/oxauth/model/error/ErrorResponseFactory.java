/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.error;

import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.gluu.oxauth.model.clientinfo.ClientInfoErrorResponseType;
import org.gluu.oxauth.model.configuration.Configuration;
import org.gluu.oxauth.model.error.IErrorType;
import org.gluu.oxauth.model.fido.u2f.U2fErrorResponseType;
import org.gluu.oxauth.model.register.RegisterErrorResponseType;
import org.gluu.oxauth.model.session.EndSessionErrorResponseType;
import org.gluu.oxauth.model.token.TokenErrorResponseType;
import org.gluu.oxauth.model.token.TokenRevocationErrorResponseType;
import org.gluu.oxauth.model.uma.UmaErrorResponse;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.model.userinfo.UserInfoErrorResponseType;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Vetoed;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * Provides an easy way to get Error responses based in an error response type
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version January 16, 2019
 */
@Vetoed
public class ErrorResponseFactory implements Configuration {

    private static Logger log = LoggerFactory.getLogger(ErrorResponseFactory.class);

    private ErrorMessages messages;

    public ErrorResponseFactory() {
    }

    public ErrorResponseFactory(ErrorMessages messages) {
        this.messages = messages;
    }

    public ErrorMessages getMessages() {
        return messages;
    }

    public void setMessages(ErrorMessages p_messages) {
        messages = p_messages;
    }

    /**
     * Looks for an error message.
     *
     * @param p_list error list
     * @param type   The type of the error.
     * @return Error message or <code>null</code> if not found.
     */
    private ErrorMessage getError(List<ErrorMessage> p_list, IErrorType type) {
        log.debug("Looking for the error with id: {}", type);

        if (p_list != null) {
            for (ErrorMessage error : p_list) {
                if (error.getId().equals(type.getParameter())) {
                    log.debug("Found error, id: {}", type);
                    return error;
                }
            }
        }

        log.debug("Error not found, id: {}", type);
        return null;
    }

    public String getErrorAsJson(IErrorType p_type) {
        return getErrorResponse(p_type).toJSonString();
    }

    public String errorAsJson(IErrorType p_type, String reason) {
        final DefaultErrorResponse error = getErrorResponse(p_type);
        error.setReason(reason);
        return error.toJSonString();
    }

    public void throwUnauthorizedException(IErrorType type) throws WebApplicationException {
        throwWebApplicationException(Response.Status.UNAUTHORIZED, type);
    }

    public void throwBadRequestException(IErrorType type) throws WebApplicationException {
        throwWebApplicationException(Response.Status.BAD_REQUEST, type);
    }

    public void throwWebApplicationException(Response.Status status, IErrorType type) throws WebApplicationException {
        final Response response = Response.status(status).entity(getErrorAsJson(type)).build();
        throw new WebApplicationException(response);
    }

    public void throwUmaNotFoundException() throws WebApplicationException {
        throwUmaWebApplicationException(Response.Status.NOT_FOUND, UmaErrorResponseType.NOT_FOUND);
    }

    public void throwUmaInternalErrorException() throws WebApplicationException {
        throwUmaWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR);
    }

    public <T> T throwUmaWebApplicationException(Response.Status status, IErrorType type) throws WebApplicationException {
        final Response response = Response.status(status).entity(getUmaJsonErrorResponse(type)).build();
        throw new WebApplicationException(response);
    }

    public String getErrorAsJson(IErrorType p_type, String p_state) {
        return getErrorResponse(p_type, p_state).toJSonString();
    }

    public String getErrorAsQueryString(IErrorType p_type, String p_state) {
        return getErrorResponse(p_type, p_state).toQueryString();
    }

    public DefaultErrorResponse getErrorResponse(IErrorType type, String p_state) {
        final DefaultErrorResponse response = getErrorResponse(type);
        response.setState(p_state);
        return response;
    }

    public DefaultErrorResponse getErrorResponse(IErrorType type) {
        final DefaultErrorResponse response = new DefaultErrorResponse();
        response.setType(type);

        if (type != null && messages != null) {
            List<ErrorMessage> list = null;
            if (type instanceof AuthorizeErrorResponseType) {
                list = messages.getAuthorize();
            } else if (type instanceof ClientInfoErrorResponseType) {
                list = messages.getClientInfo();
            } else if (type instanceof EndSessionErrorResponseType) {
                list = messages.getEndSession();
            } else if (type instanceof RegisterErrorResponseType) {
                list = messages.getRegister();
            } else if (type instanceof TokenErrorResponseType) {
                list = messages.getToken();
            } else if (type instanceof TokenRevocationErrorResponseType) {
                list = messages.getRevoke();
            } else if (type instanceof UmaErrorResponseType) {
                list = messages.getUma();
            } else if (type instanceof UserInfoErrorResponseType) {
                list = messages.getUserInfo();
            } else if (type instanceof U2fErrorResponseType) {
                list = messages.getFido();
            }

            if (list != null) {
                final ErrorMessage m = getError(list, type);
                response.setErrorDescription(m.getDescription());
                response.setErrorUri(m.getUri());
            }
        }

        return response;
    }

    public UmaErrorResponse getUmaErrorResponse(IErrorType type) {
        final UmaErrorResponse response = new UmaErrorResponse();

        final ErrorMessage errorMessage = getError(messages.getUma(), type);
        response.setError(errorMessage.getId());
        response.setErrorDescription(errorMessage.getDescription());
        response.setErrorUri(errorMessage.getUri());

        return response;
    }

    public String getUmaJsonErrorResponse(IErrorType type) {
        final UmaErrorResponse response = getUmaErrorResponse(type);

        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put("error", response.getError());

            if (StringHelper.isNotEmpty(response.getStatus())) {
                jsonObj.put("status", response.getStatus());
            }

            if (StringHelper.isNotEmpty(response.getErrorDescription())) {
                jsonObj.put("error_description", response.getErrorDescription());
            }


            if (StringHelper.isNotEmpty(response.getErrorUri())) {
                jsonObj.put("error_uri", response.getErrorUri());
            }
        } catch (JSONException ex) {
            log.error("Failed to generate error response", ex);
            return null;
        }

        return jsonObj.toString();
    }


    public String getJsonErrorResponse(IErrorType type) {
        final DefaultErrorResponse response = getErrorResponse(type);

        JsonErrorResponse jsonErrorResponse = new JsonErrorResponse(response);

        try {
            return ServerUtil.asJson(jsonErrorResponse);
        } catch (IOException ex) {
            log.error("Failed to generate error response", ex);
            return null;
        }
    }

}