package io.jans.net;

import org.apache.commons.lang3.StringUtils;

/**
 * Proxy utilities
 *
 * @author Yuriy Movchan
 * @version 1.0, 08/27/2021
 */
public class ProxyUtil {

	public static boolean isProxyRequied() {
		return (StringUtils.isNotBlank(System.getProperty("http.proxyHost")) && StringUtils.isNotBlank(System.getProperty("http.proxyPort")))
				|| (StringUtils.isNotBlank(System.getProperty("https.proxyHost")) && StringUtils.isNotBlank(System.getProperty("https.proxyPort")));
	}
}
