/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.model.token.TokenEntity;
import io.jans.model.token.TokenType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.service.CacheService;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import static io.jans.as.model.util.Util.escapeLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class SessionService {

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    StaticConfiguration staticConfiguration;

    @Inject
    CacheService cacheService;

    @Inject
    TokenService tokenService;

    @Inject
    private Logger logger;

    public String getDnForSession(String sessionId) {
        if (StringHelper.isEmpty(sessionId)) {
            return staticConfiguration.getBaseDn().getSessions();
        }
        return String.format("jansId=%s,%s", sessionId, staticConfiguration.getBaseDn().getSessions());
    }

    public SessionId getSessionById(String id) {
        logger.debug("Get Session by id:{}", id);
        SessionId sessionId = null;
        try {
            sessionId = persistenceEntryManager.find(SessionId.class, getDnForSession(id));
        } catch (Exception ex) {
            logger.error("Failed to load session entry with id " + id, ex);
        }
        return sessionId;
    }

    public List<SessionId> getAllSessions(int sizeLimit) {
        logger.debug("Get All Session sizeLimit:{}", sizeLimit);
        return persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class, null, sizeLimit);
    }

    public List<SessionId> getAllSessions() {
        return persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class, null);
    }

    public List<SessionId> getSessions() {
        List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class,
                Filter.createGreaterOrEqualFilter("exp", persistenceEntryManager.encodeTime(getDnForSession(null),
                        new Date(System.currentTimeMillis()))),
                0);
        logger.debug("All sessionList:{}", sessionList);

        sessionList.sort((SessionId s1, SessionId s2) -> s2.getCreationDate().compareTo(s1.getCreationDate()));
        logger.debug("Sorted Session sessionList:{}", sessionList);
        return sessionList;
    }

    public PagedResult<SessionId> searchSession(SearchRequest searchRequest) {
        logger.info("Search Session with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter userFilter = Filter.createSubstringFilter(ApiConstants.JANS_USR_DN, null, targetArray, null);
                Filter sidFilter = Filter.createSubstringFilter(ApiConstants.OUTSIDE_SID, null, targetArray, null);
                Filter sessAttrFilter = Filter.createSubstringFilter(ApiConstants.JANS_SESS_ATTR, null, targetArray,
                        null);
                Filter permissionFilter = Filter.createSubstringFilter("jansPermissionGrantedMap", null, targetArray,
                        null);
                Filter idFilter = Filter.createSubstringFilter(ApiConstants.JANSID, null, targetArray, null);
                filters.add(Filter.createORFilter(userFilter, sidFilter, sessAttrFilter, permissionFilter, idFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        logger.trace("Session pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                logger.trace("Session dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        logger.info("Session searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForSession(null), SessionId.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public void revokeSessionTokens(String id, String sessionDn) {
        logger.info("Revoke session tokens for id:{}, sessionDn:{}", id, sessionDn);
        try {
            String[] tokenTypeList = { TokenType.ACCESS_TOKEN.getValue(), TokenType.ID_TOKEN.getValue() };
            List<TokenEntity> tokenList = tokenService.getTokenEntityBySessionDn(sessionDn, tokenTypeList);
            logger.info("Revoke tokens for id:{}, sessionDn:{}, tokenList:{}", id, sessionDn, tokenList);
            for (TokenEntity token : tokenList) {
                tokenService.revokeTokenEntity(token.getTokenCode());
            }
        } catch (Exception ex) {
            logger.error(" Error while revoking session token is - ", ex);
        }
    }

    public void revokeSessionById(String id) {
        if (logger.isInfoEnabled()) {
            logger.info("Delete session by id:{}", escapeLog(id));
        }

        if (StringUtils.isNotBlank(id)) {
            Filter filter = Filter.createANDFilter(Filter.createEqualityFilter(ApiConstants.JANSID, id),
                    Filter.createEqualityFilter("jansState", SessionIdState.AUTHENTICATED));

            SessionId sessionToDelete = getSessionById(id);
            logger.debug("User sessionToDelete:{}", sessionToDelete);

            if (sessionToDelete == null) {
                throw new NotFoundException(
                        "No " + SessionIdState.AUTHENTICATED + " session exists for id '" + id + "'!!!");
            }

            persistenceEntryManager.remove(sessionToDelete.getDn(), SessionId.class);
            cacheService.remove(sessionToDelete.getDn());
            revokeSessionTokens(id, sessionToDelete.getDn());

        }
    }

    public void revokeUserSession(String userDn) {
        if (logger.isInfoEnabled()) {
            logger.info("Delete session of userDn:{}", escapeLog(userDn));
        }
        logger.info("Revoke session userDn:{}, cacheService:{}", userDn, cacheService);

        if (StringUtils.isNotBlank(userDn)) {
            Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("jansUsrDN", userDn),
                    Filter.createEqualityFilter("jansState", SessionIdState.AUTHENTICATED));

            List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class,
                    filter);
            logger.debug("User sessionList:{}", sessionList);

            if (sessionList == null || sessionList.isEmpty()) {
                throw new NotFoundException(
                        "No " + SessionIdState.AUTHENTICATED + " session exists for user '" + userDn + "'!!!");
            }

            sessionList.stream().forEach(session -> {
                persistenceEntryManager.remove(session.getDn(), SessionId.class);
                cacheService.remove(session.getDn());
                revokeSessionTokens(userDn, session.getDn());
            });
        }
    }

}
