/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.gluu.oxauth.model.util.LocaleUtil;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

/**
 * @author Javier Rojas Blum Date: 11.27.2013
 */
public class LocaleTest extends BaseTest {

	private List<Locale> availableLocales = Arrays.asList(new Locale("fr", "CA"), new Locale("fr"),
			new Locale("en", "CA"), new Locale("en", "US"), new Locale("en", "GB"), new Locale("es", "BO"),
			new Locale("en"), new Locale("zh", "HK"), new Locale("ja"));

	@Test
	public void localeMatch1() throws Exception {
		List<String> requestedLocales = Arrays.asList("es_BO");

		Locale matchingLocale = LocaleUtil.localeMatch(requestedLocales, availableLocales);
		assertEquals(matchingLocale, new Locale("es", "BO"));
	}

	@Test
	public void localeMatch2() throws Exception {
		List<String> requestedLocales = Arrays.asList("es");

		Locale matchingLocale = LocaleUtil.localeMatch(requestedLocales, availableLocales);
		assertEquals(matchingLocale, new Locale("es", "BO"));
	}

	@Test
	public void localeMatch3() throws Exception {
		List<String> requestedLocales = Arrays.asList("en");

		Locale matchingLocale = LocaleUtil.localeMatch(requestedLocales, availableLocales);
		assertEquals(matchingLocale, new Locale("en"));
	}

	@Test
	public void localeMatch4() throws Exception {
		List<String> requestedLocales = Arrays.asList("hr");

		Locale matchingLocale = LocaleUtil.localeMatch(requestedLocales, availableLocales);
		assertNull(matchingLocale);
	}

	@Test
	public void localeMatch5() throws Exception {
		List<String> requestedLocales = Arrays.asList("zh", "TW");

		Locale matchingLocale = LocaleUtil.localeMatch(requestedLocales, availableLocales);
		assertEquals(matchingLocale, new Locale("zh", "HK"));
	}

	@Test
	public void localeMatch6() throws Exception {
		List<String> requestedLocales = Arrays.asList("en_AU");

		Locale matchingLocale = LocaleUtil.localeMatch(requestedLocales, availableLocales);
		assertEquals(matchingLocale, new Locale("en"));
	}
}