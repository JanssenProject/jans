/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.converter;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

import org.apache.commons.lang.time.DateUtils;

/**
 * @author: Yuriy Movchan Date: 11.24.2010
 */
@FacesConverter("uptimeConverter")
public class UptimeConverter implements Converter {

	private static final String[] dateFormats = { "D 'd' HH 'h' mm 'm' ss 's'" };

	public Object getAsObject(FacesContext context, UIComponent comp, String value) throws ConverterException {
		if ((value == null) || value.trim().length() == 0) {
			return null;
		}

		try {
			return DateUtils.parseDate(value, dateFormats);
		} catch (ParseException e) {
			throw new ConverterException("Unable to convert " + value + " to seconds!");
		}
	}

	public String getAsString(FacesContext context, UIComponent component, Object object) throws ConverterException {
		if (object instanceof String) {
			try {
				return getSecondsAsString(Long.valueOf((String) object));
			} catch (NumberFormatException ex) {
				throw new ConverterException("Unable to convert " + object + " to date!");
			}
		}

		return null;
	}
	
	private String getSecondsAsString(long seconds) {
		int days = (int) TimeUnit.SECONDS.toDays(seconds);
		long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
		long mins = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
		long secondsInMinute = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

		return days + " d " + hours + " h " + mins + " m " + secondsInMinute + " s";
	}

}