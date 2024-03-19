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
import io.jans.service.document.store.service.LocalDocumentStoreService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
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

    public String getDnForAsset(String inum) throws Exception {
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
        log.info("Search asset activeFilter:{}", activeFilter);

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

        log.info("Asset pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("asset dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.debug("Asset pattern and field searchFilter:{}", searchFilter);

        if (activeFilter != null) {
            searchFilter = Filter.createANDFilter(searchFilter, activeFilter);
        }

        log.info("Asset final searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForAsset(null), Document.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public Document getAssetByInum(String inum) throws Exception {
        log.info("Get asset by inum:{}", inum);
        Document document = dbDocumentService.getDocumentByInum(inum);
        log.info("Asset by inum:{} is document:{}", inum, document);
        return document;
    }

    public List<Document> getAssetByName(String name) throws Exception {
        log.info("Get asset by name:{}", name);
        Filter nameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, name);
        List<Document> documents = persistenceEntryManager.findEntries(getDnForAsset(null), Document.class, nameFilter);
        log.info("Asset by name:{} are documents:{}", name, documents);
        return documents;
    }

    public Document saveAsset(Document asset, InputStream documentStream) throws Exception {
        log.info("Save new asset - asset:{}, documentStream:{}", asset, documentStream);

        if (asset == null) {
            throw new InvalidAttributeException("Asset object is null!!!");
        }

        if (documentStream == null) {
            throw new InvalidAttributeException(" Document data stream object is null!!!");
        }

        ByteArrayOutputStream bos = getByteArrayOutputStream(documentStream);
        log.trace("Asset ByteArrayOutputStream :{}", bos);

        if (StringUtils.isBlank(asset.getInum())) {
            log.info("As inum is blank create new asset :{}", asset);
            updateRevision(asset);
            asset = saveNewAsset(asset, getInputStream(bos));
        } else {
            log.info("Inum is not blank hence update existing asset :{}", asset);
            asset = updateAsset(asset, getInputStream(bos));
        }
        log.debug("Saved  asset is :{}", asset);

        // copyAsset on jans-server
        String result = copyAssetOnServer(asset, getInputStream(bos));
        log.info("Result of asset saved on server :{}", result);

        // Get final asset
        asset = dbDocumentService.getDocumentByInum(asset.getInum());

        log.info("Asset saved :{}", asset);
        return asset;
    }

    public Document updateAsset(Document asset, InputStream documentStream) throws Exception {
        log.info("Update new asset - asset:{}, documentStream:{}", asset, documentStream);
        if (asset == null) {
            throw new InvalidAttributeException(" Asset object is null!!!");
        }

        if (documentStream == null) {
            throw new InvalidAttributeException(" Asset data stream object is null!!!");
        }

        String documentContent = new String(documentStream.readAllBytes(), StandardCharsets.UTF_8);
        asset.setDocument(documentContent);
        updateRevision(asset);
        dbDocumentService.updateDocument(asset);

        log.info("Successfully updated asset:{}", asset);
        return asset;
    }

    public InputStream readAssetStream(String name) {
        log.info("Read asset as stream identified by name:{}", name);
        InputStream inputStream = dBDocumentStoreProvider.readDocumentAsStream(name);
        log.info("Asset as stream identified by name:{}, inputStream:{}", name, inputStream);
        return inputStream;
    }

    public boolean removeAsset(String inum) throws Exception {
        log.info("Remove asset - inum:{}", inum);
        
        Document asset = this.getAssetByInum(inum);
        log.info("asset{} identified by inum:{}", asset, inum);
        
        if(asset==null) {
            throw new NotFoundException("Cannot find asset identified by - "+inum);
        }
        //remove from store
        boolean status = dBDocumentStoreProvider.removeDocument(inum);
        log.info("Status on removing a asset identified by inum is:{}", status);
        
        //remove from server
        status =  deleteAssetFromServer(asset);
        log.info("Status on deleting asset from server is:{}", status);
        
        return status;
    }
    
    
    private boolean deleteAssetFromServer(Document asset) {
        log.info("Delete asset - asset:{}", asset);
        boolean deleteStatus = false;
        if(asset==null) {
            return deleteStatus;
        }
        
        String path = asset.getDescription();
        String fileName = asset.getDisplayName();
        String documentStoreModuleName = fileName;
        log.info("path:{}, fileName:{}, documentStoreModuleName:{}", path, fileName, documentStoreModuleName);

        if (StringUtils.isBlank(path)) {
            throw new InvalidConfigurationException("Path to delete the asset is null!");
        }

        if (StringUtils.isBlank(fileName)) {
            throw new InvalidConfigurationException("Name of asset to be deleted is null!");
        }
        
        if(documentStoreService==null) {
            throw new InvalidConfigurationException("document Store Service is null!");
        }

        String filePath = path + File.separator + fileName;
        log.info("documentStoreService:{}, filePath:{} ", documentStoreService, filePath, localDocumentStoreService);
        deleteStatus = documentStoreService.removeDocument(filePath);
        log.info("Asset deletion deleteStatus:{}", deleteStatus); 
        return deleteStatus;
    }

    private Document updateRevision(Document asset) {
        log.info("Update asset revision - asset:{}", asset);
        try {
            if (asset == null) {
                return asset;
            }

            String revision = asset.getJansRevision();
            log.debug(" Current asset revision is:{}", revision);
            int intRevision = 1;
            if (revision != null && revision.trim().length() > 0) {
                intRevision = Integer.parseInt(revision);
                intRevision = intRevision + 1;
            }
            revision = String.valueOf(intRevision);
            log.debug("Current asset intRevision:{}", intRevision);
            asset.setJansRevision(revision);

            log.info("Updated asset revision - asset:{}", asset);
        } catch (Exception ex) {
            log.error("Exception while updating asset revision is:{}", ex);
            return asset;
        }
        return asset;
    }

    private Document saveNewAsset(Document asset, InputStream stream) {
        log.info("Saving new asset in DB DocumentStore - asset:{}, stream:{}", asset, stream);
        String path = dBDocumentStoreProvider.saveDocumentStream(asset.getDisplayName(), asset.getDescription(), stream,
                asset.getJansModuleProperty());
        log.info("Successfully stored asset - Path of saved new asset is :{}", path);
        return asset;
    }

    private String copyAssetOnServer(Document asset, InputStream stream) {
        log.info("Copy asset on server - asset:{}, stream:{}", asset, stream);
        String result = null;

        if (asset == null) {
            throw new InvalidConfigurationException("Asset is null!");
        }

        if (stream == null) {
            throw new InvalidConfigurationException("Asset stream is null!");
        }

        String path = asset.getDescription();
        String fileName = asset.getDisplayName();
        String documentStoreModuleName = fileName;
        log.info("path:{}, fileName:{}, documentStoreModuleName:{}", path, fileName, documentStoreModuleName);

        if (StringUtils.isBlank(path)) {
            throw new InvalidConfigurationException("Path to copy the asset is null!");
        }

        if (StringUtils.isBlank(fileName)) {
            throw new InvalidConfigurationException("Asset name is null!");
        }

        String filePath = path + File.separator + fileName;
        log.info("documentStoreService:{}, filePath:{}, localDocumentStoreService:{} ", documentStoreService, filePath,
                localDocumentStoreService);
        result = documentStoreService.saveDocumentStream(filePath, null, stream, List.of(documentStoreModuleName));
        log.info("Asset saving result:{}", result);

        InputStream newFile = documentStoreService.readDocumentAsStream(filePath);
        log.info("Reading asset file newFile:{}", newFile);

        return result;

    }

    private ByteArrayOutputStream getByteArrayOutputStream(InputStream input) throws IOException {
        return authUtil.getByteArrayOutputStream(input);
    }

    private InputStream getInputStream(ByteArrayOutputStream bos) throws IOException {
        return authUtil.getInputStream(bos);
    }

    public String readAsset(String assetName, String assetPath) {
        log.info("Read asset from server - assetName:{}, assetPath:{}", assetName, assetPath);
        String filePath = null;

        if (StringUtils.isBlank(assetName)) {
            throw new InvalidConfigurationException("Asset name is null!");
        }

        if (StringUtils.isBlank(assetPath)) {
            throw new InvalidConfigurationException("Path to read the asset from is null");
        }

        filePath = assetPath + File.separator + assetName;
        log.info("documentStoreService:{}, filePath:{}, localDocumentStoreService:{} ", documentStoreService, filePath,
                localDocumentStoreService);

        InputStream newFile = documentStoreService.readDocumentAsStream(filePath);
        log.info("Reading asset file newFile:{}", newFile);

        filePath = assetPath + File.separator + assetName + "_puja.new";
        String result = documentStoreService.saveDocumentStream(filePath, null, newFile, List.of("test"));
        log.info("Asset saving result:{}", result);

        return filePath;

    }
}
