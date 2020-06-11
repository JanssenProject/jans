package org.gluu.oxauth.i18n;

import org.gluu.jsf2.customization.FacesLocalizationConfigPopulator;

public class ApplicationFacesLocalizationConfigPopulator extends FacesLocalizationConfigPopulator {
	private static final String LANGUAGE_FILE_PATTERN = "^oxauth_(.*)\\.properties$";

	@Override
	public String getLanguageFilePattern() {
		return LANGUAGE_FILE_PATTERN;
	}

}
