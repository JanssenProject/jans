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
import io.jans.configapi.core.util.DataUtil;
import io.jans.model.FieldFilterData;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class SessionService {

    private static final String SID_MSG = "Get Session by sid:{}";
    private static final String SID_ERROR = "Failed to load session entry with sid ";
    private static final List<String> SESSION_ATTR = Arrays.asList("acr", "scope", "auth_user", "client_id",
            "acr_values", "redirect_uri", "response_type");

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

    public String getDnForSession(String sessionId) {
        if (StringHelper.isEmpty(sessionId)) {
            return staticConfiguration.getBaseDn().getSessions();
        }
        return String.format("jansId=%s,%s", sessionId, staticConfiguration.getBaseDn().getSessions());
    }

    public String getDnForUser(String userInum) {
        String peopleDn = staticConfiguration.getBaseDn().getPeople();
        if (StringHelper.isEmpty(userInum)) {
            return peopleDn;
        }

        return String.format("inum=%s,%s", userInum, peopleDn);
    }

    public SessionId getSessionBySid(String sid) {
        if (logger.isInfoEnabled()) {
            logger.info(SID_MSG, escapeLog(sid));
        }
        SessionId sessionId = null;
        try {
            sessionId = this.getSession(sid);
            this.modifySession(sessionId);
        } catch (Exception ex) {
            logger.error(SID_ERROR + sid, ex);
        }
        return sessionId;
    }

    public List<SessionId> getAllSessions(int sizeLimit) {
        logger.debug("Get All Session sizeLimit:{}", sizeLimit);
        List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class, null,
                sizeLimit);
        this.modifySessionList(sessionList);
        return sessionList;
    }

    public List<SessionId> getAllSessions() {
        List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class, null);
        this.modifySessionList(sessionList);
        return sessionList;
    }

    public List<SessionId> getSessions() {
        List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class,
                Filter.createGreaterOrEqualFilter("exp", persistenceEntryManager.encodeTime(getDnForSession(null),
                        new Date(System.currentTimeMillis()))),
                0);
        logger.debug("All sessionList:{}", sessionList);

        sessionList.sort((SessionId s1, SessionId s2) -> s2.getCreationDate().compareTo(s1.getCreationDate()));
        logger.debug("Sorted Session sessionList:{}", sessionList);

        this.modifySessionList(sessionList);

        return sessionList;
    }

    public PagedResult<SessionId> searchSession(SearchRequest searchRequest) {
        logger.info("Search Session with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                logger.info("Session Search with assertionValue:{}", assertionValue);

                String[] targetArray = new String[] { assertionValue };
                Filter userFilter = Filter.createSubstringFilter(ApiConstants.JANS_USR_DN, null, targetArray, null);
                Filter sidFilter = Filter.createSubstringFilter(ApiConstants.SID, null, targetArray, null);
                Filter sessAttrFilter = Filter.createSubstringFilter(ApiConstants.JANS_SESS_ATTR, null, targetArray,
                        null);
                Filter permissionFilter = Filter.createSubstringFilter("jansPermissionGrantedMap", null, targetArray,
                        null);
                Filter idFilter = Filter.createSubstringFilter(ApiConstants.JANSID, null, targetArray, null);
                filters.add(Filter.createORFilter(userFilter, sidFilter, sessAttrFilter, permissionFilter, idFilter));

            }
            searchFilter = Filter.createORFilter(filters);
        }

        logger.debug("Session pattern searchFilter:{}", searchFilter);

        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldFilterData() != null && !searchRequest.getFieldFilterData().isEmpty()) {
            List<FieldFilterData> fieldFilterDataList = this.modifyFilter(searchRequest.getFieldFilterData());
            fieldValueFilters = DataUtil.createFilter(fieldFilterDataList, getDnForSession(null),
                    persistenceEntryManager);
        }

        searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                Filter.createANDFilter(fieldValueFilters));

        logger.info("Session searchFilter:{}", searchFilter);

        PagedResult<SessionId> pagedSessionList = persistenceEntryManager.findPagedEntries(getDnForSession(null),
                SessionId.class, searchFilter, null, searchRequest.getSortBy(),
                SortOrder.getByValue(searchRequest.getSortOrder()), searchRequest.getStartIndex(),
                searchRequest.getCount(), searchRequest.getMaxCount());

        if (pagedSessionList != null) {
            List<SessionId> sessionList = this.modifySessionList(pagedSessionList.getEntries());
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
            SessionId sessionToDelete = this.getSession(sid);
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

    private SessionId modifySession(SessionId session) {
        logger.debug("Modify session:{}", session);
        if (session == null) {
            return session;
        }
        List<SessionId> sessionList = new ArrayList<>();
        sessionList.add(session);
        this.modifySessionList(sessionList);
        logger.debug("After modify session:{}", session);
        return session;

    }

    private List<SessionId> modifySessionList(List<SessionId> sessionList) {
        logger.debug("Modify sessionList:{}", sessionList);

        if (sessionList == null || sessionList.isEmpty()) {
            return sessionList;
        }

        for (SessionId session : sessionList) {
            excludeAttribute(session);
        }

        logger.debug("After modification sessionList:{}", sessionList);
        return sessionList;
    }

    private SessionId getSession(String sid) {
        if (logger.isInfoEnabled()) {
            logger.info(SID_MSG, escapeLog(sid));
        }

        SessionId sessionId = null;
        try {

            List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class,
                    Filter.createEqualityFilter(ApiConstants.SID, sid));
            if (sessionList != null && !sessionList.isEmpty()) {
                sessionId = sessionList.get(0);
            }

        } catch (Exception ex) {
            logger.error(SID_ERROR + sid, ex);
        }
        return sessionId;
    }

    private SessionId excludeAttribute(SessionId session) {
        if (session == null) {
            return session;
        }
        session.setId(null);
        session.setDn(null);
        session.getSessionAttributes().put("session_id", null);
        session.getSessionAttributes().put("old_session_id", null);
        return session;
    }

    private List<FieldFilterData> modifyFilter(List<FieldFilterData> fieldFilterDataList) {

        logger.debug("Modify filter - fieldFilterDataList:{}", fieldFilterDataList);
        if (fieldFilterDataList == null || fieldFilterDataList.isEmpty()) {
            return fieldFilterDataList;
        }

        for (FieldFilterData fieldFilterData : fieldFilterDataList) {
            if (fieldFilterData != null && StringUtils.isNotBlank(fieldFilterData.getField())) {
                String field = fieldFilterData.getField();
                if (StringUtils.isBlank(field)) {
                    continue;
                }
                if ("jansUsrDN".equalsIgnoreCase(field)) {
                    // get Dn
                    fieldFilterData.setValue(getDnForUser(fieldFilterData.getValue()));
                } else if (SESSION_ATTR.contains(field)) {
                    fieldFilterData.setField("jansSessAttr." + field);
                }
            }
        }
        logger.info("After modification of session filter - fieldFilterDataList:{}", fieldFilterDataList);
        return fieldFilterDataList;
    }

}
