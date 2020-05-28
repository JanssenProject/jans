/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.gluu.oxauth.model.common.CIBAGrant;
import org.gluu.oxauth.model.common.CIBAGrantUserAuthorization;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.ldap.CIBARequest;
import org.gluu.oxauth.model.ldap.TokenLdap;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.DeletableEntity;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;

/**
 * @author Milton BO
 * @version May 28, 2020
 */
@Stateless
@Named
public class CibaRequestService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    private String cibaBaseDn() {
        return staticConfiguration.getBaseDn().getCiba();  // ou=ciba,o=gluu
    }

    public void merge(CIBARequest cibaRequest) {
        ldapEntryManager.merge(cibaRequest);
    }

    public void persistRequest(CIBAGrant grant) {
        String authReqId = grant.getCIBAAuthenticationRequestId().getCode();
        CIBARequest cibaRequest = new CIBARequest();
        cibaRequest.setDn("authReqId=" + authReqId + "," + this.cibaBaseDn());
        cibaRequest.setAuthReqId(authReqId);
        cibaRequest.setClientId(grant.getClientId());
        cibaRequest.setExpirationDate(grant.getExpirationDate());
        cibaRequest.setRequestDate(new Date());
        cibaRequest.setStatus(CIBAGrantUserAuthorization.AUTHORIZATION_PENDING.getValue());
        cibaRequest.setUserId(grant.getUserId());
        ldapEntryManager.persist(cibaRequest);
    }

    public CIBARequest load(String authReqId) {
        try {
            return ldapEntryManager.find(CIBARequest.class, authReqId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public List<CIBARequest> loadExpiredByStatus(CIBAGrantUserAuthorization authorizationStatus) {
        try {
            Date now = new Date();
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("status", authorizationStatus.getValue()),
                    Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(this.cibaBaseDn(), now)));
            return ldapEntryManager.findEntries(this.cibaBaseDn(), CIBARequest.class, filter);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}