/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.oxtrust.util;

import java.io.File;

/**
 * Action class for updating and adding the SAML IDP to Asimba.
 * 
 * @author Dmitry Ognyannikov
 */
public class ProductInstallationChecker {

	public static final String GLUU_CE_PATH = "/install/community-edition-setup";

	public static final String OXASIMBA_PATH = "/opt/gluu/jetty/asimba/webapps/asimba.war";
	public static final String OXAUTH_PATH = "/opt/gluu/jetty/oxauth/webapps/oxauth.war";
	public static final String OXTRUST_PATH = "/opt/gluu/jetty/identity/webapps/identity.war";
	public static final String SHIBBOLETH_IDP2_PATH = "/opt/idp/war/idp.war";
	public static final String SHIBBOLETH_IDP3_PATH = "/opt/gluu/jetty/idp/webapps/idp.war";
	public static final String CAS_PATH = "/opt/gluu/jetty/cas/webapps/cas.war";
	public static final String PASSPORT_PATH = "/opt/gluu/node/passport/server";
	public static final String GLUU_RADIUS_PATH = "/opt/gluu/radius/super-gluu-radius-server.jar";
	public static final String GLUU_CASA_PATH = "/opt/gluu/jetty/casa/.administrable";

	public static boolean isGluuCE() {
		return new File(GLUU_CE_PATH).exists();
	}

	public static boolean isOxAsimbaInstalled() {
		return new File(OXASIMBA_PATH).exists();
	}

	public static boolean isOxAuthInstalled() {
		return new File(OXAUTH_PATH).exists();
	}

	public static boolean isOxTrustInstalled() {
		return new File(OXTRUST_PATH).exists();
	}

	@Deprecated
	public static boolean isShibbolethIDP2Installed() {
		return new File(SHIBBOLETH_IDP2_PATH).exists();
	}

	public static boolean isShibbolethIDP3Installed() {
		return new File(SHIBBOLETH_IDP3_PATH).exists();
	}

	@Deprecated
	public static boolean isCASInstalled() {
		return new File(CAS_PATH).exists();
	}

	public static boolean isPassportInstalled() {
		return new File(PASSPORT_PATH).exists();
	}

	public static boolean isGluuRadiusInstalled() {
		return new File(GLUU_RADIUS_PATH).exists();
	}

	public static boolean isCasaInstalled() {
		return new File(GLUU_CASA_PATH).exists();
	}
}
