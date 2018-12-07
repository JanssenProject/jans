/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.jsf2.message.FacesMessages;
import org.gluu.jsf2.service.FacesService;
import org.python.jline.internal.Log;
import org.slf4j.Logger;
import org.xdi.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorHandlingMethod;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.error.IErrorType;
import org.xdi.oxauth.util.RedirectUri;
import org.xdi.util.StringHelper;

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
            facesMessages.add(FacesMessage.SEVERITY_ERROR, String.format("#{msg['%s']}", facesMessageId));
        }
    }

    private void handleLocalError(String facesMessageId) {
        addMessage(FacesMessage.SEVERITY_ERROR, facesMessageId);
        facesService.redirect("/error.xhtml");
    }
    
    private void handleRemoteError(String facesMessageId, IErrorType errorType, String hint) {
        String redirectUri = sessionIdService.getRpOriginIdCookie();
        
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
