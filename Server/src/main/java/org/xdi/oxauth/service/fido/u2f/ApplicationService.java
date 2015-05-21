/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.xdi.net.InetAddressUtility;
import org.xdi.oxauth.exception.fido.u2f.BadConfigurationException;

/**
 * Provides operations with U2F applications
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@Scope(ScopeType.STATELESS)
@Name("applicationService")
@AutoCreate
public class ApplicationService {

	public static final String DISABLE_INSTRUCTIONS = "To disable this check, instantiate the U2F object using U2F.withoutAppIdValidation()";

	private boolean validateApplication = true;

	public boolean isValidateApplication() {
		return validateApplication;
	}

	/**
	 * Throws {@link BadConfigurationException} if the given App ID is found to
	 * be incompatible with the U2F specification or any major U2F Client
	 * implementation.
	 *
	 * @param appId
	 *            the App ID to be validated
	 */
	public void checkIsValid(String appId) {
		if (!appId.contains(":")) {
			throw new BadConfigurationException("App ID does not look like a valid facet or URL. Web facets must start with 'https://'. "
					+ DISABLE_INSTRUCTIONS);
		}
		if (appId.startsWith("http:")) {
			throw new BadConfigurationException("HTTP is not supported for App IDs (by Chrome). Use HTTPS instead. " + DISABLE_INSTRUCTIONS);
		}
		if (appId.startsWith("https://")) {
			URI url = checkValidUrl(appId);
			checkPathIsNotSlash(url);
			checkNotIpAddress(url);
		}
	}

	private void checkPathIsNotSlash(URI url) {
		if ("/".equals(url.getPath())) {
			throw new BadConfigurationException(
					"The path of the URL set as App ID is '/'. This is probably not what you want -- remove the trailing slash of the App ID URL. "
							+ DISABLE_INSTRUCTIONS);
		}
	}

	private URI checkValidUrl(String appId) {
		URI url = null;
		try {
			url = new URI(appId);
		} catch (URISyntaxException e) {
			throw new BadConfigurationException("App ID looks like a HTTPS URL, but has syntax errors.", e);
		}
		return url;
	}

	private void checkNotIpAddress(URI url) {
		if (InetAddressUtility.isIpAddress(url.getAuthority()) || (url.getHost() != null && InetAddressUtility.isIpAddress(url.getHost()))) {
			throw new BadConfigurationException("App ID must not be an IP-address, since it is not supported (by Chrome). Use a host name instead. "
					+ DISABLE_INSTRUCTIONS);
		}
	}
}
