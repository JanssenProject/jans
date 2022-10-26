/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.i18n;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.util.StringHelper;
import io.jans.util.ilocale.LocaleUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @version August 9, 2017
 */
@Named("language")
@ApplicationScoped
public class LanguageBean implements Serializable {

    private static final long serialVersionUID = -6723715664277907737L;

    private static final String COOKIE_NAME = "org.gluu.i18n.Locale";
    private static final int DEFAULT_MAX_AGE = 31536000; // 1 year in seconds
    private static final String COOKIE_PATH = "/";

    private static final Locale defaultLocale = Locale.ENGLISH;

    @Inject
    private Logger log;

    private List<Locale> supportedLocales;

    public void initSupportedLocales(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
        this.supportedLocales = buildSupportedLocales(appConfiguration);
    }

    @Deprecated
    // We need to keep it till 5.0 for compatibility with old xhtml files
    public String getLocaleCode() {
        try {
            Locale locale = getCookieLocale();
            if (locale != null) {
                setLocale(locale);
                return locale.toLanguageTag();
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultLocale.getLanguage();
    }

    public void setLocaleCode(String requestedLocaleCode) {
        for (Locale supportedLocale : supportedLocales) {
            if (!Strings.isEmpty(supportedLocale.getLanguage()) && supportedLocale.getLanguage().equals(requestedLocaleCode)) {
                Locale locale = new Locale(requestedLocaleCode);
                FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);
                setCookieValue(locale.toLanguageTag());
                break;
            }
        }
    }

    public Locale getLocale() {
        try {
            Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
            if (locale != null) {
                return locale;
            }
        } catch (Exception ex) {
            log.trace("Failed to get locale from cookie", ex);
        }

        return defaultLocale;
    }

    public void setLocale(Locale requestedLocale) {
        for (Locale supportedLocale : supportedLocales) {
            if (supportedLocale.equals(requestedLocale)) {
                FacesContext.getCurrentInstance().getViewRoot().setLocale(supportedLocale);
                setCookieValue(supportedLocale.toLanguageTag());
                break;
            }
        }

        // If there is no supported locale attempt to find it by language
        setLocaleCode(requestedLocale.getLanguage());
    }

    public List<Locale> getSupportedLocales() {
        return supportedLocales;
    }

    private List<Locale> buildSupportedLocales(AppConfiguration appConfiguration) {
        List<String> uiLocales = appConfiguration.getUiLocalesSupported();

        List<Locale> supportedLocales = new LinkedList<Locale>();
        for (String uiLocale : uiLocales) {
            Pair<Locale, List<Locale>> locales = LocaleUtil.toLocaleList(uiLocale);

            supportedLocales.addAll(locales.getRight());
        }

        return supportedLocales;
    }

    public String getMessage(String key) {
        FacesContext context = FacesContext.getCurrentInstance();
        ResourceBundle bundle = context.getApplication().getResourceBundle(context, "msgs");
        String result;
        try {
            result = bundle.getString(key);
        } catch (MissingResourceException e) {
            result = "???" + key + "??? not found";
        }
        return result;
    }

    private String getCookieValue() {
        Cookie cookie = getCookie();
        return cookie == null ? null : cookie.getValue();
    }

    private void setCookieValue(String value) {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (ctx == null)
            return;
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setMaxAge(DEFAULT_MAX_AGE);
        cookie.setPath(COOKIE_PATH);
        cookie.setSecure(true);
        cookie.setVersion(1);
        response.addCookie(cookie);
    }

    @Deprecated
    // Cookie is not storing value first time. This is causing default language setting
    private Locale getCookieLocale() {
        String cookieValue = getCookieValue();
        if (StringHelper.isEmpty(cookieValue)) {
            return null;
        }

        return Locale.forLanguageTag(cookieValue);
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