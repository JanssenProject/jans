/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.util.Date;
import java.util.List;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.fido.u2f.RequestMessageLdap;

import com.unboundid.ldap.sdk.Filter;

/**
 * Provides generic operations with U2F requests
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@Scope(ScopeType.STATELESS)
@Name("u2fRequestService")
@AutoCreate
public class RequestService {

	@Logger
	private Log log;

	@In
	private LdapEntryManager ldapEntryManager;

	public List<RequestMessageLdap> getExpiredRequestMessages(Date expirationDate) {
		final String u2fBaseDn = ConfigurationFactory.instance().getBaseDn().getU2fBase(); // ou=u2f,o=@!1111,o=gluu
		Filter expiratioFilter = Filter.createLessOrEqualFilter("creationDate", ldapEntryManager.encodeGeneralizedTime(expirationDate));

		List<RequestMessageLdap> requestMessageLdap = ldapEntryManager.findEntries(u2fBaseDn, RequestMessageLdap.class, expiratioFilter);

		return requestMessageLdap;
	}

	public void removeRequestMessage(RequestMessageLdap requestMessageLdap) {
		ldapEntryManager.remove(requestMessageLdap);
	}

}
