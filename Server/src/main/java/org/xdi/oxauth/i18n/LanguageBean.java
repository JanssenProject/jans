/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.i18n;

import org.apache.logging.log4j.util.Strings;
import org.xdi.oxauth.service.AuthenticationService;
import org.xdi.oxauth.service.SessionIdService;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @version August 9, 2017
 */
@Named("language")
@ApplicationScoped
public class LanguageBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String BASE_NAME = "messages";
    private static final String COOKIE_NAME = "org.gluu.i18n.Locale";
    private static final int DEFAULT_MAX_AGE = 31536000; // 1 year in seconds
    private static final String COOKIE_PATH = "/";

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private AuthenticationService authenticationService;

    private String localeCode = Locale.ENGLISH.getLanguage();

    public String getLocaleCode() {
        String localeCode = getCookieValue();
        if (localeCode != null) setLocaleCode(localeCode);

        return this.localeCode;
    }

    public void setLocaleCode(String localeCode) {
        Iterator<Locale> locales = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
        while (locales.hasNext()) {
            Locale locale = locales.next();
            if (!Strings.isEmpty(locale.getLanguage()) && locale.getLanguage().equals(localeCode)) {
                this.localeCode = localeCode;
                FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(localeCode));
                setCookieValue(localeCode);
            }
        }
    }

    public String getMessage(String key) {
        String result;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, new Locale(this.localeCode), loader);
            result = bundle.getString(key);
        } catch (MissingResourceException e) {
            result = "???" + key + "??? not found";
        }
        return result;
    }

    private void setCookieValue(String value) {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (ctx == null)
            return;
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setMaxAge(DEFAULT_MAX_AGE);
        cookie.setPath(COOKIE_PATH);
        response.addCookie(cookie);
    }

    private String getCookieValue() {
        Cookie cookie = getCookie();
        return cookie == null ? null : cookie.getValue();
    }

    private Cookie getCookie() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            return (Cookie) ctx.getExternalContext().getRequestCookieMap().get(COOKIE_NAME);
        } else {
            return null;
        }
    }
}