/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.i18n;

import io.jans.jsf2.customization.FacesLocalizationConfigPopulator;

public class ApplicationFacesLocalizationConfigPopulator extends FacesLocalizationConfigPopulator {
    private static final String LANGUAGE_FILE_PATTERN = "^jans-auth_(.*)\\.properties$";

    @Override
    public String getLanguageFilePattern() {
        return LANGUAGE_FILE_PATTERN;
    }

}
