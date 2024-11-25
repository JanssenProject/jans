/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.ilocale;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Javier Rojas Blum Date: 11.27.2013
 * @author Yuriy Movchan Date: 02/28/2020
 */
public class LocaleUtil {

    public static Locale localeMatch(List<String> requestedLocales, List<Locale> availableLocales) {
        if (requestedLocales == null || availableLocales == null) {
            return null;
        }

        for (String requestedLocale : requestedLocales) {
        	Pair<Locale, List<Locale>> locales = toLocaleList(requestedLocale);
            Locale reqInQuestion = locales.getLeft();
            List<Locale> lookupList = locales.getRight();

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
    
    public static Pair<Locale, List<Locale>> toLocaleList(String locale) {
        // LocaleUtils uses an underscore format (e.g. en_US), but the new Java standard
        // is a hyphenated format (e.g. en-US). This allows us to use LocaleUtils' validation.
        Locale localeLanguage = LocaleUtils.toLocale(locale.replace('-', '_'));
        List<Locale> localeList = LocaleUtils.localeLookupList(localeLanguage);
        
        return Pair.of(localeLanguage, localeList);
    }

}