/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.i18n;

import io.jans.jsf2.customization.FacesLocalizationConfigPopulator;

public class ApplicationFacesLocalizationConfigPopulator extends FacesLocalizationConfigPopulator {
	private static final String LANGUAGE_FILE_PATTERN = "^oxauth_(.*)\\.properties$";

	@Override
	public String getLanguageFilePattern() {
		return LANGUAGE_FILE_PATTERN;
	}

}
