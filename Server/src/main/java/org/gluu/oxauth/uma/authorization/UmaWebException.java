package org.gluu.oxauth.uma.authorization;

import static javax.ws.rs.core.Response.Status.FOUND;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.net.URI;
import java.net.URLEncoder;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.UmaErrorResponse;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuriyz on 06/06/2017.
 */
public class UmaWebException extends WebApplicationException {

    private final static Logger LOGGER = LoggerFactory.getLogger(UmaWebException.class);

    public UmaWebException(Response.Status status, ErrorResponseFactory factory, UmaErrorResponseType error) {
        super(Response.status(status).entity(factory.getUmaJsonErrorResponse(error)).build());
    }

    public UmaWebException(String redirectUri, ErrorResponseFactory factory, UmaErrorResponseType error, String state) {
        super(createRedirectErrorResponse(redirectUri, factory, error, state));
    }

    public static Response createRedirectErrorResponse(String redirectUri, ErrorResponseFactory factory, UmaErrorResponseType errorType, String state) {
        return Response
                .status(FOUND)
                .location(createErrorUri(redirectUri, factory, errorType, state))
                .build();
    }

    public static URI createErrorUri(String redirectUri, ErrorResponseFactory factory, UmaErrorResponseType errorType, String state) {
        try {
            UmaErrorResponse error = factory.getUmaErrorResponse(errorType);
            if (redirectUri.contains("?")) {
                redirectUri += "&";
            } else {
                redirectUri += "?";
            }

            redirectUri += "error=" + error.getError();
            redirectUri += "&error_description=" + URLEncoder.encode(error.getErrorDescription(), "UTF-8");
            if (StringUtils.isNotBlank(error.getErrorUri())) {
                redirectUri += "&error_uri=" + URLEncoder.encode(error.getErrorUri(), "UTF-8");
            }
            if (StringUtils.isNotBlank(state)) {
                redirectUri += "&state=" + state;
            }

            return new URI(redirectUri);
        } catch (Exception e) {
            LOGGER.error("Failed to construct uri: " + redirectUri, e);
            throw new UmaWebException(INTERNAL_SERVER_ERROR, factory, UmaErrorResponseType.SERVER_ERROR);
        }
    }
}
