/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.configuration.BaseFilter;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapFilterConverter;
import io.jans.orm.model.base.BaseEntry;
import io.jans.orm.search.filter.Filter;
import io.jans.util.ArrayHelper;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version March 4, 2016
 */

public abstract class BaseAuthFilterService {

    @Inject
    protected Logger log;

    @Inject
    protected LdapFilterConverter ldapFilterConverter;

    public static final Pattern PARAM_VALUE_PATTERN = Pattern.compile("([\\w]+)[\\s]*\\=[\\*\\s]*(\\{[\\s]*[\\d]+[\\s]*\\})[\\*\\s]*");

    private boolean enabled;
    private boolean filterAttributes = true;

    private List<AuthenticationFilterWithParameters> filterWithParameters;

    public static class AuthenticationFilterWithParameters {

        private BaseFilter authenticationFilter;
        private List<String> variableNames;
        private List<IndexedParameter> indexedVariables;

        public AuthenticationFilterWithParameters(BaseFilter authenticationFilter, List<String> variableNames, List<IndexedParameter> indexedVariables) {
            this.authenticationFilter = authenticationFilter;
            this.variableNames = variableNames;
            this.indexedVariables = indexedVariables;
        }

        public BaseFilter getAuthenticationFilter() {
            return authenticationFilter;
        }

        public void setAuthenticationFilter(BaseFilter authenticationFilter) {
            this.authenticationFilter = authenticationFilter;
        }

        public List<String> getVariableNames() {
            return variableNames;
        }

        public void setVariableNames(List<String> variableNames) {
            this.variableNames = variableNames;
        }

        public List<IndexedParameter> getIndexedVariables() {
            return indexedVariables;
        }

        public void setIndexedVariables(List<IndexedParameter> indexedVariables) {
            this.indexedVariables = indexedVariables;
        }

        public String toString() {
            return String.format("AutheticationFilterWithParameters [authenticationFilter=%s, variableNames=%s, indexedVariables=%s]",
                    authenticationFilter, variableNames, indexedVariables);
        }

    }

    public static class IndexedParameter {

        private String paramName;
        private String paramIndex;

        public IndexedParameter(String paramName, String paramIndex) {
            this.paramName = paramName;
            this.paramIndex = paramIndex;
        }

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public String getParamIndex() {
            return paramIndex;
        }

        public void setParamIndex(String paramIndex) {
            this.paramIndex = paramIndex;
        }

        public String toString() {
            return String.format("IndexedParameter [paramName=%s, paramIndex=%s]", paramName, paramIndex);
        }
    }

    public void init(List<? extends BaseFilter> filterList, boolean enabled, boolean filterAttributes) {
        this.enabled = enabled;
        this.filterWithParameters = prepareAuthenticationFilterWithParameters(filterList);
        this.filterAttributes = filterAttributes;
    }

    private List<AuthenticationFilterWithParameters> prepareAuthenticationFilterWithParameters(List<? extends BaseFilter> filterList) {
        final List<AuthenticationFilterWithParameters> tmpAuthenticationFilterWithParameters = new ArrayList<>();

        if (!this.enabled || filterList == null) {
            return tmpAuthenticationFilterWithParameters;
        }

        for (BaseFilter authenticationFilter : filterList) {
            if (Boolean.TRUE.equals(authenticationFilter.getBind()) && StringHelper.isEmpty(authenticationFilter.getBindPasswordAttribute())) {
                log.error("Skipping authentication filter:\n '{}'\n. It should contains not empty bind-password-attribute attribute. ", authenticationFilter);
                continue;
            }

            List<String> variableNames = new ArrayList<>();
            List<BaseAuthFilterService.IndexedParameter> indexedParameters = new ArrayList<>();

            Matcher matcher = BaseAuthFilterService.PARAM_VALUE_PATTERN.matcher(authenticationFilter.getFilter());
            while (matcher.find()) {
                String paramName = normalizeAttributeName(matcher.group(1));
                String paramIndex = matcher.group(2);

                variableNames.add(paramName);
                indexedParameters.add(new BaseAuthFilterService.IndexedParameter(paramName, paramIndex));
            }

            AuthenticationFilterWithParameters tmpAutheticationFilterWithParameter = new AuthenticationFilterWithParameters(authenticationFilter, variableNames, indexedParameters);
            tmpAuthenticationFilterWithParameters.add(tmpAutheticationFilterWithParameter);

            log.debug("Authentication filter with parameters: '{}'. ", tmpAutheticationFilterWithParameter);
        }

        return tmpAuthenticationFilterWithParameters;
    }

    public static List<AuthenticationFilterWithParameters> getAllowedAuthenticationFilters(Collection<?> attributeNames, List<AuthenticationFilterWithParameters> filterList) {
        List<AuthenticationFilterWithParameters> tmpAuthenticationFilterWithParameters = new ArrayList<>();
        if (attributeNames == null) {
            return tmpAuthenticationFilterWithParameters;
        }

        Set<String> normalizedAttributeNames = new HashSet<>();
        for (Object attributeName : attributeNames) {
            normalizedAttributeNames.add(normalizeAttributeName(attributeName.toString()));
        }

        for (AuthenticationFilterWithParameters autheticationFilterWithParameters : filterList) {
            if (normalizedAttributeNames.containsAll(autheticationFilterWithParameters.getVariableNames())) {
                tmpAuthenticationFilterWithParameters.add(autheticationFilterWithParameters);
            }
        }

        return tmpAuthenticationFilterWithParameters;
    }

    public static Map<String, String> normalizeAttributeMap(Map<?, ?> attributeValues) {
        Map<String, String> normalizedAttributeValues = new HashMap<>();
        for (Map.Entry<?, ?> attributeValueEntry : attributeValues.entrySet()) {
            String attributeValue = null;

            Object attributeValueEntryValue = attributeValueEntry.getValue();
            if (attributeValueEntryValue instanceof String[]) {
                if (ArrayHelper.isNotEmpty((String[]) attributeValueEntryValue)) {
                    attributeValue = ((String[]) attributeValueEntryValue)[0];
                }
            } else if (attributeValueEntryValue instanceof String) {
                attributeValue = (String) attributeValueEntryValue;
            } else if (attributeValueEntryValue != null) {
                attributeValue = attributeValueEntryValue.toString();
            }

            if (attributeValue != null) {
                normalizedAttributeValues.put(normalizeAttributeName(attributeValueEntry.getKey().toString()), attributeValue);
            }
        }
        return normalizedAttributeValues;
    }

    public static String buildFilter(AuthenticationFilterWithParameters authenticationFilterWithParameters, Map<String, String> normalizedAttributeValues) {
        String filter = authenticationFilterWithParameters.getAuthenticationFilter().getFilter();
        for (IndexedParameter indexedParameter : authenticationFilterWithParameters.getIndexedVariables()) {
            String attributeValue = normalizedAttributeValues.get(indexedParameter.getParamName());
            if (attributeValue != null) {
                filter = filter.replace(indexedParameter.getParamIndex(), attributeValue);
            }
        }
        return filter;
    }

    public <T> String loadEntryDN(PersistenceEntryManager manager, Class<T> entryClass, AuthenticationFilterWithParameters authenticationFilterWithParameters, Map<String, String> normalizedAttributeValues) throws SearchException {
        final String filter = buildFilter(authenticationFilterWithParameters, normalizedAttributeValues);

        Filter ldapFilter = ldapFilterConverter.convertRawLdapFilterToFilter(filter).multiValued(false);
        log.debug("Using filter: '{}'", ldapFilter);
        List<T> foundEntries = manager.findEntries(authenticationFilterWithParameters.getAuthenticationFilter().getBaseDn(), entryClass, ldapFilter, new String[0]);

        if (foundEntries.size() > 1) {
            log.error("Found more than one entry by filter: '{}'. Entries: {}\n", ldapFilter, foundEntries);
            return null;
        }

        log.debug("Found entries: {}", foundEntries.size());

        if (foundEntries.size() != 1) {
            return null;
        }

        return ((BaseEntry) foundEntries.get(0)).getDn();
    }

    public String processAuthenticationFilters(Map<?, ?> attributeValues) throws SearchException {
        if (attributeValues == null) {
            return null;
        }

        final List<AuthenticationFilterWithParameters> allowedList = filterAttributes ?
                getAllowedAuthenticationFilters(attributeValues.keySet(), getFilterWithParameters()) :
                getFilterWithParameters();

        for (AuthenticationFilterWithParameters allowed : allowedList) {
            String resultDn = processAuthenticationFilter(allowed, attributeValues);
            if (StringHelper.isNotEmpty(resultDn)) {
                return resultDn;
            }
        }

        return null;
    }

    public abstract String processAuthenticationFilter(AuthenticationFilterWithParameters allowed, Map<?, ?> attributeValues) throws SearchException;

    public List<AuthenticationFilterWithParameters> getFilterWithParameters() {
        return filterWithParameters;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFilterAttributes() {
        return filterAttributes;
    }

    public void setFilterAttributes(boolean filterAttributes) {
        this.filterAttributes = filterAttributes;
    }

    public static String normalizeAttributeName(String attributeName) {
        return StringHelper.toLowerCase(attributeName.trim());
    }
}
