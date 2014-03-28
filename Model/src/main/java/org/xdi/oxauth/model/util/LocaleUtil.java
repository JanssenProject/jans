package org.xdi.oxauth.model.util;

import org.apache.commons.lang.LocaleUtils;

import java.util.List;
import java.util.Locale;

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