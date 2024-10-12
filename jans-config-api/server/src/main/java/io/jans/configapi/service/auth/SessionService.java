/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.ApiEndpointMgt;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.DataUtil;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
public class SessionService {

    @Inject
    private Logger logger;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    StaticConfiguration staticConfiguration;

    @Inject
    CacheService cacheService;

    @Inject
    TokenService tokenService;

    @Inject
    private ApiAppConfiguration appConfiguration;

    public String getDnForSession(String sessionId) {
        if (StringHelper.isEmpty(sessionId)) {
            return staticConfiguration.getBaseDn().getSessions();
        }
        return String.format("jansId=%s,%s", sessionId, staticConfiguration.getBaseDn().getSessions());
    }

    public SessionId getSessionBySid(String sid, boolean excludeAttributes) {
        logger.debug("Get Session by sid:{}, excludeAttributes:{}", sid, excludeAttributes);
        SessionId sessionId = null;
        try {

            List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class,
                    Filter.createEqualityFilter(ApiConstants.SID, sid));
            if (sessionList != null && !sessionList.isEmpty()) {
                sessionId = sessionList.get(0);
            }
            this.modifySession(sessionId, excludeAttributes);
        } catch (Exception ex) {
            logger.error("Failed to load session entry with sid " + sid, ex);
        }
        return sessionId;
    }

    public List<SessionId> getAllSessions(int sizeLimit, boolean excludeAttributes) {
        logger.debug("Get All Session sizeLimit:{}, excludeAttributes:{}", sizeLimit, excludeAttributes);
        List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class, null,
                sizeLimit);
        this.modifySessionList(sessionList, excludeAttributes);
        return sessionList;
    }

    public List<SessionId> getAllSessions(boolean excludeAttributes) {
        List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class, null);
        this.modifySessionList(sessionList, excludeAttributes);
        return sessionList;
    }

    public List<SessionId> getSessions(boolean excludeAttributes) {
        List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class,
                Filter.createGreaterOrEqualFilter("exp", persistenceEntryManager.encodeTime(getDnForSession(null),
                        new Date(System.currentTimeMillis()))),
                0);
        logger.debug("All sessionList:{}, excludeAttributes:{}", sessionList, excludeAttributes);

        sessionList.sort((SessionId s1, SessionId s2) -> s2.getCreationDate().compareTo(s1.getCreationDate()));
        logger.debug("Sorted Session sessionList:{}", sessionList);

        this.modifySessionList(sessionList, excludeAttributes);

        return sessionList;
    }

    public PagedResult<SessionId> searchSession(SearchRequest searchRequest, boolean excludeAttributes) {
        logger.info("Search Session with searchRequest:{}, excludeAttributes:{}", searchRequest, excludeAttributes);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                logger.debug("Session Search with assertionValue:{}", assertionValue);
                if (StringUtils.isNotBlank(assertionValue)) {
                    String[] targetArray = new String[] { assertionValue };
                    Filter userFilter = Filter.createSubstringFilter(ApiConstants.JANS_USR_DN, null, targetArray, null);
                    Filter sidFilter = Filter.createSubstringFilter(ApiConstants.SID, null, targetArray, null);
                    Filter sessAttrFilter = Filter.createSubstringFilter(ApiConstants.JANS_SESS_ATTR, null, targetArray,
                            null);
                    Filter permissionFilter = Filter.createSubstringFilter("jansPermissionGrantedMap", null,
                            targetArray, null);
                    Filter idFilter = Filter.createSubstringFilter(ApiConstants.JANSID, null, targetArray, null);
                    filters.add(
                            Filter.createORFilter(userFilter, sidFilter, sessAttrFilter, permissionFilter, idFilter));
                }
            }
            searchFilter = Filter.createORFilter(filters);
        }

        logger.debug("Session pattern searchFilter:{}", searchFilter);
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

        logger.debug("Session searchFilter:{}", searchFilter);

        PagedResult<SessionId> pagedSessionList = persistenceEntryManager.findPagedEntries(getDnForSession(null),
                SessionId.class, searchFilter, null, searchRequest.getSortBy(),
                SortOrder.getByValue(searchRequest.getSortOrder()), searchRequest.getStartIndex(),
                searchRequest.getCount(), searchRequest.getMaxCount());

        if (pagedSessionList != null) {
            List<SessionId> sessionList = this.modifySessionList(pagedSessionList.getEntries(), excludeAttributes);
            pagedSessionList.setEntries(sessionList);
        }

        return pagedSessionList;

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

    public void revokeSessionBySid(String sid) {
        if (logger.isInfoEnabled()) {
            logger.info("Delete session by sid:{}", escapeLog(sid));
        }

        if (StringUtils.isNotBlank(sid)) {
            SessionId sessionToDelete = this.getSessionBySid(sid, false);
            logger.debug("User sessionToDelete:{}", sessionToDelete);

            if (sessionToDelete == null) {
                throw new NotFoundException(
                        "No " + SessionIdState.AUTHENTICATED + " session exists for sid '" + sid + "'!!!");
            }

            persistenceEntryManager.remove(sessionToDelete.getDn(), SessionId.class);
            cacheService.remove(sessionToDelete.getDn());
            revokeSessionTokens(sessionToDelete.getId(), sessionToDelete.getDn());

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

    private ApiEndpointMgt getSessionApiEndpointMgt() {
        ApiEndpointMgt apiEndpointMgt = null;
        if (this.appConfiguration.getApiEndpointMgt() != null && !this.appConfiguration.getApiEndpointMgt().isEmpty()) {
            apiEndpointMgt = this.appConfiguration.getApiEndpointMgt().stream()
                    .filter(e -> e.getName().equalsIgnoreCase("Session")).findFirst().orElse(null);
        }
        return apiEndpointMgt;
    }

    private SessionId modifySession(SessionId session, boolean excludeAttributes) {
        logger.debug("Modify session:{}, excludeAttributes:{}", session, excludeAttributes);
        if (session == null) {
            return session;
        }
        List<SessionId> sessionList = new ArrayList<>();
        sessionList.add(session);
        this.modifySessionList(sessionList, excludeAttributes);
        logger.debug("After modify session:{}", session);
        return session;

    }

    private List<SessionId> modifySessionList(List<SessionId> sessionList, boolean excludeAttributes) {
        logger.debug("Modify sessionList:{}, excludeAttributes:{}", sessionList, excludeAttributes);

        if (sessionList == null || sessionList.isEmpty() || !excludeAttributes) {
            return sessionList;
        }

        ApiEndpointMgt sessionApiEndpointMgt = this.getSessionApiEndpointMgt();
        logger.debug("sessionApiEndpointMgt:{}", sessionApiEndpointMgt);
        if (sessionApiEndpointMgt == null) {
            return sessionList;
        }

        for (SessionId session : sessionList) {
            this.excludeAttribute(session, sessionApiEndpointMgt.getExclusionAttributes());
        }

        logger.debug("After modification sessionList:{}", sessionList);
        return sessionList;
    }

    private SessionId excludeAttribute(SessionId session, List<String> exclusionAttributes) {
        logger.debug("Exclude attribute - session:{}, exclusionAttributes:{}", session, exclusionAttributes);
        try {
            if (session == null || exclusionAttributes == null || exclusionAttributes.isEmpty()) {
                return session;
            }
            
            for (String attribute : exclusionAttributes) {
                session = DataUtil.setField(session, attribute, null);
            }
            logger.info("After exclude attribute - exclusionAttributes:{}, session:{}", exclusionAttributes, session);
        } catch (Exception ex) {
            logger.error("Error while nullifying attribute[" + exclusionAttributes + "] value is - ", ex);
        }

        return session;
    }

  

}
