/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.i18n;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.util.Strings;

/**
 * @version August 9, 2017
 */
@Named("language")
@RequestScoped
public class LanguageBean implements Serializable {

	private static final long serialVersionUID = -6723715664277907737L;

	private static final String COOKIE_NAME = "org.gluu.i18n.Locale";
	private static final int DEFAULT_MAX_AGE = 31536000; // 1 year in seconds
	private static final String COOKIE_PATH = "/";

	private String localeCode = Locale.ENGLISH.getLanguage();

	public String getLocaleCode() {
		try {
			String localeCode = getCookieValue();
			if (localeCode != null)
				setLocaleCode(localeCode);
			return this.localeCode;
		} catch (Exception e) {
			return localeCode;
		}
	}

	public void setLocaleCode(String localeCode) {
		Iterator<Locale> locales = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
		while (locales.hasNext()) {
			Locale locale = locales.next();
			if (!Strings.isEmpty(locale.getLanguage()) && locale.getLanguage().equals(localeCode)) {
				this.localeCode = localeCode;
				FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(localeCode));
				setCookieValue(localeCode);
				break;
			}
		}
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