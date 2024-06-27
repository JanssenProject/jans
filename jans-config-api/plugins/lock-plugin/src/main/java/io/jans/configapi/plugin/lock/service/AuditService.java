package io.jans.configapi.plugin.lock.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.Conf;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.configapi.plugin.lock.model.stat.TelemetryEntry;

@ApplicationScoped
public class AuditService {

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    ConfigurationFactory configurationFactory;
          
    public void addClient(Client client) {
        persistenceEntryManager.persist(client);
    }

    public void removeClient(Client client) {
        persistenceEntryManager.removeRecursively(client.getDn(), Client.class);
    }

    public void updateClient(Client client) {
        persistenceEntryManager.merge(client);
    }

    public Client getClientByInum(String inum) {
        Client result = null;
        try {
            result = persistenceEntryManager.find(Client.class, getDnForClient(inum));
        } catch (Exception ex) {
            logger.error("Failed to load client entry", ex);
        }
        return result;
    }

    public List<Client> searchClients(String pattern, int sizeLimit) {

        logger.debug("Search Clients with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        logger.debug("Search Clients with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, searchFilter, sizeLimit);
    }

    public List<Client> getAllClients(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, null, sizeLimit);
    }

    public List<Client> getAllClients() {
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, null);
    }

    public PagedResult<Client> getClients(SearchRequest searchRequest) {
        logger.debug("Search Clients with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                        targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null,
                        targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        logger.trace("Clients pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                logger.trace("Clients dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        logger.debug("Clients searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForClient(null), Client.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public Client getClientByDn(String dn) {
        try {
            return persistenceEntryManager.find(Client.class, dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }

    
}
