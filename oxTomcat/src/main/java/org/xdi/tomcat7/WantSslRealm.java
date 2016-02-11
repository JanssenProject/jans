/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.tomcat7;

import java.security.Principal;
import java.security.cert.X509Certificate;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.NullRealm;

/**
 * Return empty user roles for any certificate
 * 
 * @author Yuriy Movchan Date: 02/11/2016
 */
public class WantSslRealm extends NullRealm {

	@Override
	protected Principal getPrincipal(X509Certificate usercert) {
		if (this.x509UsernameRetriever == null) {
			return new GenericPrincipal(null, null, null);
		}
		
		String username = this.x509UsernameRetriever.getUsername(usercert);
		
		Principal principal = new GenericPrincipal(username, null, null);

		return principal;
	}

	@Override
	public Principal authenticate(X509Certificate certs[]) {
		if ((certs == null) || (certs.length == 0)) {
			return new GenericPrincipal(null, null, null);
		}
		
		return super.authenticate(certs);
	}

}
