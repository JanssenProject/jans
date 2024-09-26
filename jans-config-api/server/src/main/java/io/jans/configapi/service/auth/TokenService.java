package io.jans.configapi.service.auth;

import io.jans.as.common.service.OrganizationService;
import io.jans.model.SearchRequest;
import io.jans.model.token.TokenEntity;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import static io.jans.as.model.util.Util.escapeLog;

import java.util.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;

@ApplicationScoped
public class TokenService {

    @Inject
    private Logger logger;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private OrganizationService organizationService;

    public String getDnForTokenEntity(String tknCde) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(tknCde)) {
            return String.format("ou=tokens,%s", orgDn);
        }
        return String.format("tknCde=%s,ou=tokens,%s", tknCde, orgDn);
    }

    public TokenEntity getTokenEntityByCode(String tknCde) {
        TokenEntity tokenEntity = null;
        try {
            tokenEntity = persistenceEntryManager.find(TokenEntity.class, getDnForTokenEntity(tknCde));
        } catch (Exception ex) {
            logger.error("Failed to get Token identified by tknCde:{" + tknCde + "}", ex);
        }
        return tokenEntity;
    }

    public PagedResult<TokenEntity> searchToken(SearchRequest searchRequest) {
        logger.info("Search Token with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {
            logger.trace("Search Token searchRequest.getFilterAssertionValue() :{}",
                    searchRequest.getFilterAssertionValue());
            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter grantIdFilter = Filter.createSubstringFilter("grtId", null, targetArray, null);
                Filter userIdFilter = Filter.createSubstringFilter("usrId", null, targetArray, null);
                Filter userDnFilter = Filter.createSubstringFilter("jansUsrDN", null, targetArray, null);
                Filter clientIdFilter = Filter.createSubstringFilter("clnId", null, targetArray, null);
                Filter scopeFilter = Filter.createSubstringFilter("scp", null, targetArray, null);
                Filter tokenTypeFilter = Filter.createSubstringFilter("tknTyp", null, targetArray, null);
                Filter grantTypeFilter = Filter.createSubstringFilter("grtTyp", null, targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter("jansId", null, targetArray, null);
                filters.add(Filter.createORFilter(grantIdFilter, userIdFilter, userDnFilter, clientIdFilter,
                        scopeFilter, tokenTypeFilter, grantTypeFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
            logger.trace("Search Token searchFilter :{}", searchFilter);
        }

        logger.debug("Token pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createSubstringFilter(entry.getKey(), null,
                        new String[] { entry.getValue() }, null);
                logger.trace("Token dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        logger.info("Token final searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForTokenEntity(null), TokenEntity.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public void revokeTokenEntity(String tknCde) {
        if (logger.isInfoEnabled()) {
            logger.info(" Revoke token - tknCde:{}", escapeLog(tknCde));
        }

        TokenEntity tokenEntity = this.getTokenEntityByCode(tknCde);
        logger.debug("Token to be revoked identified by tknCde:{} is:{}", tokenEntity, tknCde);

        if (tokenEntity == null) {
            throw new NotFoundException("Could not find Token identified by - " + tknCde);
        }

        persistenceEntryManager.removeRecursively(tokenEntity.getDn(), TokenEntity.class);
    }

}
