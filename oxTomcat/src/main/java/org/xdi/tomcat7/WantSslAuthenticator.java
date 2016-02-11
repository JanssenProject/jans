/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.tomcat7;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.authenticator.SSLAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
/**
 * Allow to proceed container authentication with client certificate or without it
 * 
 * @author Yuriy Movchan Date: 02/11/2016
 */
public class WantSslAuthenticator extends SSLAuthenticator {
    
	private String infoStr = null;
	
    public WantSslAuthenticator() {
		this.infoStr = this.getClass().getName();
	}

	@Override
    public boolean authenticate(Request request,
                                HttpServletResponse response,
                                LoginConfig config)
        throws IOException {

        // NOTE: We don't try to reauthenticate using any existing SSO session,
        // because that will only work if the original authentication was
        // BASIC or FORM, which are less secure than the CLIENT-CERT auth-type
        // specified for this webapp
        //
        // Change to true below to allow previous FORM or BASIC authentications
        // to authenticate users for this webapp
        // TODO make this a configurable attribute (in SingleSignOn??)
        if (checkForCachedAuthentication(request, response, false)) {
            return true;
        }

        // Retrieve the certificate chain for this client
        if (containerLog.isDebugEnabled())
            containerLog.debug(" Looking up certificates");

        X509Certificate certs[] = getRequestCertificates(request);

        if ((certs == null) || (certs.length < 1)) {
            if (containerLog.isDebugEnabled())
                containerLog.debug(" There is no user certificate");
        }


        // Authenticate the specified certificate chain
        Principal principal = context.getRealm().authenticate(certs);
        if (principal == null) {
            if (containerLog.isDebugEnabled())
                containerLog.debug("  Realm.authenticate() returned false");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                               sm.getString("authenticator.unauthorized"));
            return false;
        }

        // Cache the principal (if requested) and record this authentication
        register(request, response, principal,
                HttpServletRequest.CLIENT_CERT_AUTH, null, null);

        return true;
    }

	@Override
	public String getInfo() {
		return infoStr;
	}

}