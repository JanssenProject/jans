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
import io.jans.configapi.util.AuthUtil;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.document.store.provider.DBDocumentStoreProvider;
import io.jans.service.document.store.service.DBDocumentService;
import io.jans.service.document.store.service.Document;
import io.jans.util.exception.InvalidAttributeException;
import io.jans.util.exception.InvalidConfigurationException;
import io.jans.service.document.store.service.DocumentStoreService;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.service.LocalDocumentStoreService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class AssetService {

    @Inject
    Logger log;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    AuthUtil authUtil;

    @Inject
    DocumentStoreService documentStoreService;

    @Inject
    DBDocumentStoreProvider dBDocumentStoreProvider;

    @Inject
    DBDocumentService dbDocumentService;

    @Inject
    private LocalDocumentStoreService localDocumentStoreService;

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
                Filter aliasFilter = Filter.createSubstringFilter("jansAlias", null, targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, aliasFilter, inumFilter));
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

    public Document saveAsset(Document document, InputStream documentStream) throws Exception {
        log.info("Save new asset - document:{}, documentStream:{}", document, documentStream);

        if (document == null) {
            throw new InvalidAttributeException(" Document object is null!!!");
        }

        if (documentStream == null) {
            throw new InvalidAttributeException(" Document data stream object is null!!!");
        }

        // common code
        ByteArrayOutputStream bos = getByteArrayOutputStream(documentStream);
        log.trace("Asset ByteArrayOutputStream :{}", bos);

        document = saveDocument(document, getInputStream(bos));
        log.info("Saved  document is :{}", document);

        // copyAsset on jans-server
        String result = copyAsset(document, getInputStream(bos));
        log.info("Result of asset saved on server :{}", result);

        // Get final document
        document = dbDocumentService.getDocumentByDisplayName(document.getDisplayName());

        log.info("New document saved :{}", document);
        return document;
    }

    public Document updateAsset(Document document, InputStream documentStream) throws Exception {
        log.info("Update new asset - document:{}, documentStream:{}", document, documentStream);
        if (document == null) {
            throw new InvalidAttributeException(" Document object is null!!!");
        }

        if (documentStream == null) {
            throw new InvalidAttributeException(" Document data stream object is null!!!");
        }

        String documentContent = new String(documentStream.readAllBytes(), StandardCharsets.UTF_8);
        document.setDocument(documentContent);
        dbDocumentService.updateDocument(document);
        document = dbDocumentService.getDocumentByDisplayName(document.getDisplayName());
        log.info("Updated document:{}", document);
        return document;
    }

    public InputStream readAssetStream(String name) {
        log.info("Read asset as stream identified by name:{}", name);
        InputStream inputStream = dBDocumentStoreProvider.readDocumentAsStream(name);
        log.info("Asset as stream identified by name:{}, inputStream:{}", name, inputStream);
        return inputStream;
    }

    public boolean removeAsset(String inum) {
        log.info("Remove new asset - inum:{}", inum);
        boolean status = dBDocumentStoreProvider.removeDocument(inum);
        log.info("Status on removing a document identified by inum is:{}", status);
        return status;
    }

    private Document saveDocument(Document document, InputStream stream) {
        log.info("Saving document in DB DocumentStore - document:{}, stream:{}", document, stream);
        try {
            String path = dBDocumentStoreProvider.saveDocumentStream(document.getDisplayName(), document.getDescription(), stream,
                    document.getJansModuleProperty());
            log.info("Successfully stored document - Path of saved new document is :{}", path);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error while saving document in DB DocumentStore is:{}", ex);
            // throw new WebApplicationException(ex);
        }
        return document;
    }

    private String copyAsset(Document document, InputStream stream) {
        log.info("Copy document on server - document:{}, stream:{}", document, stream);
        String result = null;
        try {
            if (document == null) {
                throw new InvalidConfigurationException("Document is null!");
            }

            if (stream == null) {
                throw new InvalidConfigurationException("Asset stream is null!");
            }

            String path = document.getDescription();
            String fileName = document.getDisplayName();
            String documentStoreModuleName = fileName;
            log.info("path:{}, fileName:{}, documentStoreModuleName:{}", path, fileName, documentStoreModuleName);

            if (StringUtils.isBlank(path)) {
                throw new InvalidConfigurationException("Path to copy the asset is null!");
            }

            if (StringUtils.isBlank(fileName)) {
                throw new InvalidConfigurationException("Asset name is null!");
            }

            String filePath = path + File.separator + fileName;
            log.info("documentStoreService:{}, filePath:{}, localDocumentStoreService:{} ", documentStoreService,
                    filePath, localDocumentStoreService);
            result = documentStoreService.saveDocumentStream(filePath, null, stream, List.of(documentStoreModuleName));
            log.info("Asset saving result:{}", result);

            InputStream newFile = documentStoreService.readDocumentAsStream(filePath);
            log.info("Reading asset file newFile:{}", newFile);

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error while copying document on server is:{}", ex);
            // throw new WebApplicationException(ex);
        }
        return result;

    }

    private ByteArrayOutputStream getByteArrayOutputStream(InputStream input) throws IOException {
        return authUtil.getByteArrayOutputStream(input);
    }

    private InputStream getInputStream(ByteArrayOutputStream bos) throws IOException {
        return authUtil.getInputStream(bos);
    }

}
