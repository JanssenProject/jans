/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorHandlingMethod;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.error.IErrorType;
import io.jans.jsf2.message.FacesMessages;
import io.jans.jsf2.service.FacesService;
import io.jans.util.StringHelper;
import org.python.jline.internal.Log;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.FacesMessage.Severity;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Helper service to generate either error response or local error based on application settings
 *
 * @author Yuriy Movchan Date: 12/07/2018
 */
@ApplicationScoped
@Named
public class ErrorHandlerService {

    @Inject
    private Logger log;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private CookieService cookieService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private FacesService facesService;

    @Inject
    private FacesMessages facesMessages;

    public void handleError(String facesMessageId, IErrorType errorType, String hint) {
        if (ErrorHandlingMethod.REMOTE == appConfiguration.getErrorHandlingMethod()) {
            handleRemoteError(facesMessageId, errorType, hint);
        } else {
            handleLocalError(facesMessageId);
        }
    }

    private void addMessage(Severity severity, String facesMessageId) {
        if (StringHelper.isNotEmpty(facesMessageId)) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR, String.format("#{msgs['%s']}", facesMessageId));
        }
    }

    private void handleLocalError(String facesMessageId) {
        addMessage(FacesMessage.SEVERITY_ERROR, facesMessageId);
        facesService.redirect("/error.xhtml");
    }

    private void handleRemoteError(String facesMessageId, IErrorType errorType, String hint) {
        String redirectUri = cookieService.getRpOriginIdCookie();

        if (StringHelper.isEmpty(redirectUri)) {
            Log.error("Failed to get redirect_uri from cookie");
            handleLocalError(facesMessageId);
            return;
        }

        RedirectUri redirectUriResponse = new RedirectUri(redirectUri, null, null);
        redirectUriResponse.parseQueryString(errorResponseFactory.getErrorAsQueryString(
                errorType, null));
        if (StringHelper.isNotEmpty(hint)) {
            redirectUriResponse.addResponseParameter("hint", "Create authorization request to start new authentication session.");
        }
        facesService.redirectToExternalURL(redirectUriResponse.toString());

    }

}
