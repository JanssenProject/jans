package org.xdi.net;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yuriy Movchan Date: 05/20/2015
 */
public class InetAddressUtility {

	private static Pattern VALID_IPV4_PATTERN = null;
	private static Pattern VALID_IPV6_PATTERN = null;
	private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
	private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

	static {
		VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
		VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
	}

	/**
	 * Determine if the given string is a valid IPv4 or IPv6 address
	 */
	public static boolean isIpAddress(String ipAddress) {

		Matcher m1 = VALID_IPV4_PATTERN.matcher(ipAddress);
		if (m1.matches()) {
			return true;
		}

		Matcher m2 = VALID_IPV6_PATTERN.matcher(ipAddress);

		return m2.matches();
	}

}