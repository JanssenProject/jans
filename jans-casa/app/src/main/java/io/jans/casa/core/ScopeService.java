package io.jans.casa.core;

import io.jans.orm.search.filter.Filter;
import io.jans.as.model.common.ScopeType;

import io.jans.casa.core.model.Scope;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ApplicationScoped
public class ScopeService {

    private static final String SCOPE_TYPE_ATTR = "oxScopeType";

    @Inject
    private Logger logger;

    @Inject
    private PersistenceService persistenceService;

    private String scopesDN;

    public List<String> getDNsFromIds(List<String> oxIds) {
        List<Filter> filters = oxIds.stream().map(oxId -> Filter.createEqualityFilter("jansId", oxId)).collect(Collectors.toList());
        List<Scope> scopes = persistenceService.find(Scope.class, scopesDN, Filter.createORFilter(filters.toArray(new Filter[0])));
        return scopes.stream().map(Scope::getDn).collect(Collectors.toList());
    }

    public List<Scope> getNonUMAScopes() {

    	List<Filter> filters = Stream.of(ScopeType.values()).filter(st -> !st.equals(ScopeType.UMA))
    							.map(ScopeType::getValue).map(value -> Filter.createEqualityFilter(SCOPE_TYPE_ATTR, value))
    							.collect(Collectors.toList());
                
        Filter filter = Filter.createORFilter(filters.toArray(new Filter[0]));
        return persistenceService.find(Scope.class, scopesDN, filter);
    }

    @PostConstruct
    private void init() {
        scopesDN = persistenceService.getScopesDn();
    }

}
