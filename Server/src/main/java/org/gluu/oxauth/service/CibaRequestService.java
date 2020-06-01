/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.apache.commons.lang.time.DateUtils;
import org.gluu.oxauth.model.common.CIBAGrant;
import org.gluu.oxauth.model.common.CIBAGrantUserAuthorization;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.ldap.CIBARequest;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;

/**
 * Service used to access to the database for CibaRequest ObjectClass.
 *
 * @author Milton BO
 * @version May 28, 2020
 */
@Stateless
@Named
public class CibaRequestService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    private String cibaBaseDn() {
        return staticConfiguration.getBaseDn().getCiba();  // ou=ciba,o=gluu
    }

    /**
     * Uses request data and expiration sent by the client and save request data in database.
     * @param grant Object containing information related to the request.
     * @param expiresIn Expiration time that end user has to answer.
     */
    public void persistRequest(CIBAGrant grant, int expiresIn) {
        Date expirationDate = DateUtils.addSeconds(new Date(), expiresIn);

        String authReqId = grant.getCIBAAuthenticationRequestId().getCode();
        CIBARequest cibaRequest = new CIBARequest();
        cibaRequest.setDn("authReqId=" + authReqId + "," + this.cibaBaseDn());
        cibaRequest.setAuthReqId(authReqId);
        cibaRequest.setClientId(grant.getClientId());
        cibaRequest.setExpirationDate(expirationDate);
        cibaRequest.setRequestDate(new Date());
        cibaRequest.setStatus(CIBAGrantUserAuthorization.AUTHORIZATION_PENDING.getValue());
        cibaRequest.setUserId(grant.getUserId());
        entryManager.persist(cibaRequest);
    }

    /**
     * Load a CIBARequest entry from database.
     * @param authReqId Identifier of the entry.
     */
    public CIBARequest load(String authReqId) {
        try {
            return entryManager.find(CIBARequest.class, authReqId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generates a list of requests that are expired and also filter them using a Status.
     * @param authorizationStatus Status used to filter entries.
     */
    public List<CIBARequest> loadExpiredByStatus(CIBAGrantUserAuthorization authorizationStatus) {
        try {
            Date now = new Date();
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("status", authorizationStatus.getValue()),
                    Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(this.cibaBaseDn(), now)));
            return entryManager.findEntries(this.cibaBaseDn(), CIBARequest.class, filter);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Change the status field in database for a specific request.
     * @param authReqId Identificator of the request.
     * @param authorizationStatus New status.
     */
    public void updateStatus(String authReqId, CIBAGrantUserAuthorization authorizationStatus) {
        try {
            String requestDn = String.format("authReqId=%s,%s", authReqId, this.cibaBaseDn());
            CIBARequest cibaRequest = entryManager.find(CIBARequest.class, requestDn);
            cibaRequest.setStatus(authorizationStatus.getValue());
            entryManager.merge(cibaRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Change the status field in database for a specific request.
     * @param cibaRequest Entry containing information of the CIBA request.
     * @param authorizationStatus New status.
     */
    public void updateStatus(CIBARequest cibaRequest, CIBAGrantUserAuthorization authorizationStatus) {
        try {
            cibaRequest.setStatus(authorizationStatus.getValue());
            entryManager.merge(cibaRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}