/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.configapi.rest.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationScoped
public class ClientService implements Serializable {

    private static final long serialVersionUID = 7912416439116338984L;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private Logger logger;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private InumService inumService;

    public boolean contains(String clientDn) {
        return persistenceEntryManager.contains(clientDn, Client.class);
    }

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
        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.displayName, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.description, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.inum, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, searchFilter, sizeLimit);
    }

    public List<Client> getAllClients(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, null, sizeLimit);
    }

    public List<Client> getAllClients() {
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, null);
    }
    
    public PagedResult<Client> searchClients(SearchRequest searchRequest) {
        Filter searchFilter = null;
        if (StringUtils.isNotEmpty(searchRequest.getFilter())) {
        String[] targetArray = new String[] { searchRequest.getFilter() };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.displayName, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.description, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.inum, null, targetArray, null);
        searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);
        }
        
        PagedResult<Client> list = persistenceEntryManager.findPagedEntries(getDnForClient(null), Client.class, searchFilter, null, searchRequest.getSortBy(),
                SortOrder.getByValue(searchRequest.getSortOrder()), searchRequest.getStartIndex() - 1, searchRequest.getCount(), searchRequest.getMaxCount());
        
        return list;
    }

    public Client getClientByDn(String Dn) {
        try {
            return persistenceEntryManager.find(Client.class, Dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }

    public ApplicationType[] getApplicationType() {
        return ApplicationType.values();
    }

    public SubjectType[] getSubjectTypes() {
        return SubjectType.values();
    }

    public SignatureAlgorithm[] getSignatureAlgorithms() {
        return SignatureAlgorithm.values();
    }

    public String getDnForClient(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=clients,%s", orgDn);
        }
        return String.format("inum=%s,ou=clients,%s", inum, orgDn);
    }

    public String generateInumForNewClient() {
        String newInum = null;
        String newDn = null;
        int trycount = 0;
        do {
            if (trycount < InumService.MAX_IDGEN_TRY_COUNT) {
                newInum = inumService.generateId("client");
                trycount++;
            } else {
                newInum = inumService.generateDefaultId();
            }
            newDn = getDnForClient(newInum);
        } while (persistenceEntryManager.contains(newDn, Client.class));
        return newInum;
    }
}
