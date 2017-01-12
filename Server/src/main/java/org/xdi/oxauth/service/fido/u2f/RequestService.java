/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import com.unboundid.ldap.sdk.Filter;
import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.SearchScope;
import org.xdi.oxauth.model.config.StaticConf;
import org.xdi.oxauth.model.fido.u2f.RequestMessageLdap;
import org.xdi.oxauth.service.CleanerTimer;

import java.util.Date;
import java.util.List;

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

	@In
	private StaticConf staticConfiguration;

	public List<RequestMessageLdap> getExpiredRequestMessages(BatchOperation<RequestMessageLdap> batchOperation, Date expirationDate) {
		final String u2fBaseDn = staticConfiguration.getBaseDn().getU2fBase(); // ou=u2f,o=@!1111,o=gluu
		Filter expirationFilter = Filter.createLessOrEqualFilter("creationDate", ldapEntryManager.encodeGeneralizedTime(expirationDate));

		List<RequestMessageLdap> requestMessageLdap = ldapEntryManager.findEntries(u2fBaseDn, RequestMessageLdap.class, expirationFilter, SearchScope.SUB, null, batchOperation, 0, CleanerTimer.BATCH_SIZE, CleanerTimer.BATCH_SIZE);

		return requestMessageLdap;
	}

	public void removeRequestMessage(RequestMessageLdap requestMessageLdap) {
		ldapEntryManager.remove(requestMessageLdap);
	}

}
