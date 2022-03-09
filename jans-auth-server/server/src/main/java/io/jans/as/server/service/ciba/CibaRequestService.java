/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.ciba;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.common.CibaRequestCacheControl;
import io.jans.as.server.model.common.CibaRequestStatus;
import io.jans.as.server.model.ldap.CIBARequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.CacheService;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
        return staticConfiguration.getBaseDn().getCiba();  // ou=ciba,o=jans
    }

    /**
     * Uses request data and expiration sent by the client and save request data in database.
     *
     * @param request   Object containing information related to the request.
     * @param expiresIn Expiration time that end user has to answer.
     */
    public void persistRequest(CibaRequestCacheControl request, int expiresIn) {
        Date expirationDate = DateUtils.addSeconds(new Date(), expiresIn);

        String authReqId = request.getAuthReqId();
        CIBARequest cibaRequest = new CIBARequest();
        cibaRequest.setDn("authReqId=" + authReqId + "," + this.cibaBaseDn());
        cibaRequest.setAuthReqId(authReqId);
        cibaRequest.setClientId(request.getClient().getClientId());
        cibaRequest.setExpirationDate(expirationDate);
        cibaRequest.setCreationDate(new Date());
        cibaRequest.setStatus(CibaRequestStatus.PENDING.getValue());
        cibaRequest.setUserId(request.getUser().getUserId());
        entryManager.persist(cibaRequest);
    }

    /**
     * Load a CIBARequest entry from database.
     *
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
     *
     * @param authorizationStatus Status used to filter entries.
     * @param maxRequestsToGet    Limit of requests that would be returned.
     */
    public List<CIBARequest> loadExpiredByStatus(CibaRequestStatus authorizationStatus,
                                                 int maxRequestsToGet) {
        try {
            Date now = new Date();
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("jansStatus", authorizationStatus.getValue()),
                    Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(this.cibaBaseDn(), now)));
            return entryManager.findEntries(this.cibaBaseDn(), CIBARequest.class, filter, maxRequestsToGet);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Change the status field in database for a specific request.
     *
     * @param cibaRequest         Entry containing information of the CIBA request.
     * @param authorizationStatus New status.
     */
    public void updateStatus(CIBARequest cibaRequest, CibaRequestStatus authorizationStatus) {
        try {
            cibaRequest.setStatus(authorizationStatus.getValue());
            entryManager.merge(cibaRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Removes a CibaRequest object from the database.
     *
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
     *
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
     * Register a new CibaRequestCacheControl instance in Cache and in the database.
     *
     * @param request   New instance to be saved.
     * @param expiresIn Expiration time of the request in Cache and memory.
     */
    public void save(CibaRequestCacheControl request, int expiresIn) {
        int expiresInCache = expiresIn;
        if (appConfiguration.getCibaGrantLifeExtraTimeSec() > 0) {
            expiresInCache += appConfiguration.getCibaGrantLifeExtraTimeSec();
        }

        cacheService.put(expiresInCache, request.cacheKey(), request);
        this.persistRequest(request, expiresIn);
        log.trace("Ciba request saved in cache, authReqId: {} clientId: {}", request.getAuthReqId(), request.getClient().getClientId());
    }

    /**
     * Put in cache a CibaRequestCacheControl object, it uses same expiration time that it has.
     *
     * @param request Object to be updated, replaced or created.
     */
    public void update(CibaRequestCacheControl request) {
        int expiresInCache = request.getExpiresIn();
        if (appConfiguration.getCibaGrantLifeExtraTimeSec() > 0) {
            expiresInCache += appConfiguration.getCibaGrantLifeExtraTimeSec();
        }

        cacheService.put(expiresInCache, request.cacheKey(), request);
    }

    /**
     * Get a CibaRequestCacheControl object from Cache service.
     *
     * @param authReqId Identifier of the object to be gotten.
     */
    public CibaRequestCacheControl getCibaRequest(String authReqId) {
        Object cachedObject = cacheService.get(authReqId);
        if (cachedObject == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedObject = cacheService.get(authReqId);
            log.trace("Failed to fetch CIBA request from cache, authReqId: {}", authReqId);
        }
        return cachedObject instanceof CibaRequestCacheControl ? (CibaRequestCacheControl) cachedObject : null;
    }

    /**
     * Removes from cache a request.
     *
     * @param cacheKey Object to be removed from Cache.
     */
    public void removeCibaCacheRequest(String cacheKey) {
        try {
            cacheService.remove(cacheKey);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Verifies whether a specific client has CIBA compatibility.
     *
     * @param client Client to check.
     */
    public boolean hasCibaCompatibility(Client client) {
        if (client.getBackchannelTokenDeliveryMode() == null) {
            return false;
        }
        for (GrantType gt : client.getGrantTypes()) {
            if (gt.getValue().equals(GrantType.CIBA.getValue())) {
                return true;
            }
        }
        return false;
    }
}