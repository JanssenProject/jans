/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service;

import io.jans.as.model.configuration.AppConfiguration;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

/**
 * @author Yuriy Zabrovarnyy
 */
@RequestScoped
public class CookieService {

    public static final String SESSION_ID_COOKIE_NAME = "session_id";

    @Inject
    private Logger log;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private AppConfiguration appConfiguration;

    public String getSessionIdFromCookie(HttpServletRequest request) {
        return getValueFromCookie(request, SESSION_ID_COOKIE_NAME);
    }

    public String getValueFromCookie(HttpServletRequest request, String cookieName) {
        try {
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(cookieName) /*&& cookie.getSecure()*/) {
                        log.trace("Found cookie: '{}'", cookie.getValue());
                        return cookie.getValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public String getSessionIdFromCookie() {
        try {
            if (facesContext == null) {
                return null;
            }
            final HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            if (request != null) {
                return getSessionIdFromCookie(request);
            } else {
                log.trace("Faces context returns null for http request object.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

}