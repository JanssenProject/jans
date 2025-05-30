/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.util;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

import io.jans.model.SimpleProperty;
import io.jans.service.EncryptionService;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter;
import jakarta.inject.Inject;

/**
 * Utility class with helpers methods to generate configuration files
 * 
 * @author Yuriy Movchan Date: 08.02.2011
 */
public class PropertyUtil {

	private static final Logger log = Logger.getLogger(PropertyUtil.class);

	@Inject
	private EncryptionService encryptionService;
	
	public String encryptString(String value) {
		try {
			return encryptionService.encrypt(value);
		} catch (StringEncrypter.EncryptionException ex) {
			log.error("Failed to encrypt string: " + value, ex);
		}

		return null;
	}

	public static String stringsToCommaSeparatedList(List<String> values) {
		StringBuilder sb = new StringBuilder();

		int count = values.size();
		for (int i = 0; i < count; i++) {
			sb.append(escapeString(values.get(i)));
			if (i < count - 1) {
				sb.append(", ");
			}
		}

		return sb.toString();
	}

	public static String simplePropertiesToCommaSeparatedList(List<SimpleProperty> values) {
		StringBuilder sb = new StringBuilder();

		int count = values.size();
		for (int i = 0; i < count; i++) {
			sb.append(escapeString(values.get(i).getValue()));
			if (i < count - 1) {
				sb.append(", ");
			}
		}

		return sb.toString();
	}

	public static String escapeString(String value) {
		if (StringHelper.isEmpty(value)) {
			return "";
		}

		return escapeComma(StringEscapeUtils.escapeJava(value));
	}

	/**
	 * Inserts a backslash before every comma
	 */
	private static String escapeComma(String s) {
		StringBuffer buf = new StringBuffer(s);
		for (int i = 0; i < buf.length(); i++) {
			char c = buf.charAt(i);
			if (c == ',') {
				buf.insert(i, '\\');
				i++;
			}
		}
		return buf.toString();
	}
	
	public static boolean isEmptyString(String string) {
		return StringHelper.isEmpty(string);
	}

}
