/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.util;

/**
 * Neutralizes caller-controlled values before they reach the log.
 */
public final class LogEscaper {

	private LogEscaper() {
	}

	/**
	 * Replaces every control character (CR, LF, TAB, ESC and the rest of the C0/C1 ranges, DEL) with
	 * an underscore, so a value can neither forge log lines nor drive a terminal that displays the log.
	 *
	 * @param param value to log, may be null
	 * @return the printable form, or an empty string for null
	 */
	public static String escapeLog(Object param) {
		if (param == null) {
			return "";
		}
		return param.toString().replaceAll("\\p{Cntrl}", "_");
	}

}
