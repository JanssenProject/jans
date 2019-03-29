/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.LocaleUtils;

/**
 * @author Javier Rojas Blum Date: 11.27.2013
 */
public class LocaleUtil {

    public static Locale localeMatch(List<String> requestedLocales, List<Locale> availableLocales) {
        if (requestedLocales == null || availableLocales == null) {
            return null;
        }

        for (String requestedLocale : requestedLocales) {
            Locale reqInQuestion = LocaleUtils.toLocale(requestedLocale);
            List<Locale> lookupList = LocaleUtils.localeLookupList(reqInQuestion);

            for (Locale localeInQuestion : lookupList) {
                for (Locale availableLocale : availableLocales) {
                    if (localeInQuestion.equals(availableLocale)) {
                        return availableLocale;
                    }
                }
            }

            for (Locale availableLocale : availableLocales) {
                if (reqInQuestion.getLanguage().equals(availableLocale.getLanguage())) {
                    return availableLocale;
                }
            }
        }

        return null;
    }
}