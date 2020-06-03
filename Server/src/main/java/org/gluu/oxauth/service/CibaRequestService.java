/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.apache.commons.lang.time.DateUtils;
import org.gluu.oxauth.model.common.CIBARequestStatus;
import org.gluu.oxauth.model.common.CibaCacheRequest;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.ldap.CIBARequest;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.service.CacheService;
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

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CacheService cacheService;

    private String cibaBaseDn() {
        return staticConfiguration.getBaseDn().getCiba();  // ou=ciba,o=gluu
    }

    /**
     * Uses request data and expiration sent by the client and save request data in database.
     * @param request Object containing information related to the request.
     * @param expiresIn Expiration time that end user has to answer.
     */
    public void persistRequest(CibaCacheRequest request, int expiresIn) {
        Date expirationDate = DateUtils.addSeconds(new Date(), expiresIn);

        String authReqId = request.getCibaAuthenticationRequestId().getCode();
        CIBARequest cibaRequest = new CIBARequest();
        cibaRequest.setDn("authReqId=" + authReqId + "," + this.cibaBaseDn());
        cibaRequest.setAuthReqId(authReqId);
        cibaRequest.setClientId(request.getClient().getClientId());
        cibaRequest.setExpirationDate(expirationDate);
        cibaRequest.setCreationDate(new Date());
        cibaRequest.setStatus(CIBARequestStatus.AUTHORIZATION_PENDING.getValue());
        cibaRequest.setUserId(request.getUser().getUserId());
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
     * @param maxRequestsToGet Limit of requests that would be returned.
     */
    public List<CIBARequest> loadExpiredByStatus(CIBARequestStatus authorizationStatus,
                                                 int maxRequestsToGet) {
        try {
            Date now = new Date();
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("oxStatus", authorizationStatus.getValue()),
                    Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(this.cibaBaseDn(), now)));
            return entryManager.findEntries(this.cibaBaseDn(), CIBARequest.class, filter, maxRequestsToGet);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Change the status field in database for a specific request.
     * @param cibaRequest Entry containing information of the CIBA request.
     * @param authorizationStatus New status.
     */
    public void updateStatus(CIBARequest cibaRequest, CIBARequestStatus authorizationStatus) {
        try {
            cibaRequest.setStatus(authorizationStatus.getValue());
            entryManager.merge(cibaRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Removes a CibaRequest object from the database.
     * @param cibaRequest Object to be removed.
     */
    public void removeCibaRequest(CIBARequest cibaRequest) {
        try {
            entryManager.remove(cibaRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Removes a CibaRequest from the database.
     * @param authReqId Identifier of the CibaRequest.
     */
    public void removeCibaRequest(String authReqId) {
        try {
            String requestDn = String.format("authReqId=%s,%s", authReqId, this.cibaBaseDn());
            entryManager.remove(requestDn);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Register a new CibaCacheRequest instance in Cache and in the database.
     * @param request New instance to be saved.
     * @param expiresIn Expiration time of the request in Cache and memory.
     */
    public void save(CibaCacheRequest request, int expiresIn) {
        int expiresInCache = expiresIn;
        if (appConfiguration.getCibaGrantLifeExtraTimeSec() > 0) {
            expiresInCache += appConfiguration.getCibaGrantLifeExtraTimeSec();
        }

        cacheService.put(expiresInCache, request.cacheKey(), request);
        this.persistRequest(request, expiresIn);
        log.trace("Ciba request saved in cache, authReqId: {} clientId: {}", request.getCibaAuthenticationRequestId().getCode(), request.getClient().getClientId());
    }

    /**
     * Put in cache a CibaCacheRequest object, it uses same expiration time that it has.
     * @param request Object to be updated, replaced or created.
     */
    public void update(CibaCacheRequest request) {
        int expiresInCache = request.getExpiresIn();
        if (appConfiguration.getCibaGrantLifeExtraTimeSec() > 0) {
            expiresInCache += appConfiguration.getCibaGrantLifeExtraTimeSec();
        }

        cacheService.put(expiresInCache, request.cacheKey(), request);
    }

    /**
     * Get a CibaCacheRequest object from Cache service.
     * @param authReqId Identifier of the object to be gotten.
     */
    public CibaCacheRequest getCibaRequest(String authReqId) {
        Object cachedObject = cacheService.get(authReqId);
        if (cachedObject == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedObject = cacheService.get(authReqId);
            log.trace("Failed to fetch CIBA request from cache, authenticationRequestId: " + authReqId);
        }
        return cachedObject instanceof CibaCacheRequest ? (CibaCacheRequest) cachedObject : null;
    }

    /**
     * Removes from cache a request.
     * @param cacheKey Object to be removed from Cache.
     */
    public void removeCibaCacheRequest(String cacheKey) {
        try {
            cacheService.remove(cacheKey);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}