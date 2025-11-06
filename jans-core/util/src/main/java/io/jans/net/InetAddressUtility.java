/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Yuriy Movchan Date: 05/20/2015
 */
public final class InetAddressUtility {

	private static final String[] HEADERS_TO_TRY = new String[]{
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    private static Pattern VALID_IPV4_PATTERN = null;
    private static Pattern VALID_IPV6_PATTERN = null;
    private static final String IPV4_PATTERN = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final String IPV6_PATTERN = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

    static {
        VALID_IPV4_PATTERN = Pattern.compile(IPV4_PATTERN, Pattern.CASE_INSENSITIVE);
        VALID_IPV6_PATTERN = Pattern.compile(IPV6_PATTERN, Pattern.CASE_INSENSITIVE);
    }

    private static String MAC_ADDRESS;
    private static boolean MAC_ADDRESS_SET = false;

    private InetAddressUtility() { }

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

    public static String getMACAddressOrNull() {
        if (!MAC_ADDRESS_SET) {
            MAC_ADDRESS = getMACAddressOrNullImpl();
            MAC_ADDRESS_SET = true;
        }

        return MAC_ADDRESS;
    }

    private static synchronized String getMACAddressOrNullImpl() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                return null;
            }

            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (UnknownHostException e) {
            return null;
        } catch (SocketException e) {
            return null;
        }
    }

    /**
     * @param httpRequest interface to provide request information for HTTP servlets.
     * @return IP address of client
     * @see <a href="http://stackoverflow.com/a/21884642/5202500">Getting IP address of client</a>
     */
	public static String getIpAddress(HttpServletRequest httpRequest) {
		for (String header : HEADERS_TO_TRY) {
			String ip = httpRequest.getHeader(header);
			if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
				// X-Forwarded-For can contain multiple IPs; take the first one
				int commaIndex = ip.indexOf(',');
				if (commaIndex > 0) {
					ip = ip.substring(0, commaIndex).trim();
				}
				return ip;
			}
		}
		return httpRequest.getRemoteAddr();
	}

}
