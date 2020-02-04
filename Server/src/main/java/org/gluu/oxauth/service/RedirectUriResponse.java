package org.gluu.oxauth.service;

import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.error.IErrorType;
import org.gluu.oxauth.util.RedirectUri;
import org.gluu.oxauth.util.RedirectUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RedirectUriResponse {

    private RedirectUri redirectUri;
    private String state;
    private HttpServletRequest httpRequest;
    private ErrorResponseFactory errorFactory;

    public RedirectUriResponse(RedirectUri redirectUri, String state, HttpServletRequest httpRequest, ErrorResponseFactory errorFactory) {
        this.redirectUri = redirectUri;
        this.state = state;
        this.httpRequest = httpRequest;
        this.errorFactory = errorFactory;
    }

    public WebApplicationException createWebException(IErrorType errorType) {
        return createWebException(errorType, null);
    }

    public WebApplicationException createWebException(IErrorType errorType, String reason) {
        redirectUri.parseQueryString(errorFactory.getErrorAsQueryString(errorType, state, reason));
        return new WebApplicationException(RedirectUtil.getRedirectResponseBuilder(redirectUri, httpRequest).build());
    }

    public void setState(String state) {
        this.state = state;
    }

    public Response.ResponseBuilder createErrorBuilder(IErrorType errorType) {
        redirectUri.parseQueryString(errorFactory.getErrorAsQueryString(errorType, state));
        return RedirectUtil.getRedirectResponseBuilder(redirectUri, httpRequest);
    }

    public RedirectUri getRedirectUri() {
        return redirectUri;
    }
}
