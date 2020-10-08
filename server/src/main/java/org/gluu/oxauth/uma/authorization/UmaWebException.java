package org.gluu.oxauth.uma.authorization;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.error.DefaultErrorResponse;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;

import static javax.ws.rs.core.Response.Status.FOUND;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author yuriyz on 06/06/2017.
 */
public class UmaWebException extends WebApplicationException {

    private final static Logger LOGGER = LoggerFactory.getLogger(UmaWebException.class);

    private UmaWebException() {
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
            DefaultErrorResponse error = factory.getErrorResponse(errorType);
            if (redirectUri.contains("?")) {
                redirectUri += "&";
            } else {
                redirectUri += "?";
            }

            redirectUri += "error=" + error.getErrorCode();
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
            throw factory.createWebApplicationException(INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, "Failed to construct uri");
        }
    }
}
