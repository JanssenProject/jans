/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.document.store.provider.DBDocumentStoreProvider;
import io.jans.service.document.store.service.DBDocumentService;
import io.jans.service.document.store.service.Document;

import java.io.InputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;

@ApplicationScoped
public class AssetService {

    @Inject
    Logger log;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    DBDocumentStoreProvider dBDocumentStoreProvider;

    @Inject
    DBDocumentService dbDocumentService;

    public String getDnForDocument(String inum) throws Exception {
        return dbDocumentService.getDnForDocument(inum);
    }

    public PagedResult<Document> searchAsset(SearchRequest searchRequest, String status) throws Exception {
        log.info("Search Documents with searchRequest:{}, status:{}", searchRequest, status);

        Filter activeFilter = null;
        if (ApiConstants.ACTIVE.equalsIgnoreCase(status)) {
            activeFilter = Filter.createEqualityFilter("jansEnabled", true);
        } else if (ApiConstants.INACTIVE.equalsIgnoreCase(status)) {
            activeFilter = Filter.createEqualityFilter("jansEnabled", false);
        }
        log.info(" Documents activeFilter:{}", activeFilter);

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

        log.info("Document pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("Document dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.debug("Document pattern and field searchFilter:{}", searchFilter);

        if (activeFilter != null) {
            searchFilter = Filter.createANDFilter(searchFilter, activeFilter);
        }

        log.info("Document final searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForDocument(null), Document.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public Document getDocumentByInum(String inum) throws Exception {
        log.info("Get Document by inum:{}", inum);
        Document document = dbDocumentService.getDocumentByInum(inum);
        log.info("Document by inum:{} is document:{}", inum, document);
        return document;
    }

    public List<Document> getDocumentByName(String name) throws Exception {
        log.info("Get Document by name:{}", name);
        Filter nameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, name);
        List<Document> documents = persistenceEntryManager.findEntries(getDnForDocument(null), Document.class,
                nameFilter);
        log.info("Document by name:{} are documents:{}", name, documents);
        return documents;
    }

    public boolean saveAsset(Document document, InputStream documentStream) {
        log.info("Save new asset - document:{}, documentStream:{}", document, documentStream);
        return dBDocumentStoreProvider.saveDocumentStream(document.getDisplayName(), documentStream,
                document.getJansModuleProperty());
    }

    /*
     * public void updateAsset(Document document, InputStream documentStream) {
     * log.info("Update new asset - document:{}, documentStream:{}", document,
     * documentStream); return
     * dBDocumentStoreProvider.updateDocumentStream(document.getDisplayName(),
     * documentStream, document.getJansModuleProperty()); }
     */

    public InputStream readAssetStream(String name) {
        log.info("Read asset as stream identified by name:{}", name);

        InputStream inputStream = dBDocumentStoreProvider.readDocumentAsStream(name);
        log.info("Asset as stream identified by name:{}, inputStream:{}", name, inputStream);
        return inputStream;
    }

    public boolean removeAsset(String inum) {
        log.info("Remove new asset - inum:{}", inum);
        return dBDocumentStoreProvider.removeDocument(inum);
    }

}
