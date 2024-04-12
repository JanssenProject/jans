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
        log.info("Search asset with searchRequest:{}, status:{}", searchRequest, status);

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
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter));
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
        Document asset = dbDocumentService.getDocumentByInum(inum);
        log.info("Asset by inum:{} is asset:{}", inum, asset);
        return asset;
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

        // update asset revision
        updateRevision(asset);

        // save asset in DB store
        if (StringUtils.isBlank(asset.getInum())) {
            log.info("As inum is blank create new asset :{}", asset);
            saveNewAsset(asset, getInputStream(bos));
        } else {
            log.info("Inum is not blank hence update existing asset :{}", asset);
            asset = updateAsset(asset, getInputStream(bos));
        }
        log.debug("Saved  asset is :{}", asset);

        // copy asset on jans-server
        String result = copyAssetOnServer(asset, getInputStream(bos));
        log.info("Result of asset saved on server :{}", result);

        // Get final asset
        List<Document >assets = this.getAssetByName(asset.getDisplayName());
        if(assets==null) {
            throw new WebApplicationException(" Could not  save asset");
        }
        asset = assets.get(0);
        log.info("\n * Asset saved :{}", asset);
        return asset;
    }

    public boolean removeAsset(String inum) throws Exception {
        log.info("Remove asset - inum:{}", inum);

        Document asset = this.getAssetByInum(inum);
        log.info("asset{} identified by inum:{}", asset, inum);

        if (asset == null) {
            throw new NotFoundException("Cannot find asset identified by - " + inum);
        }

        // remove asset from DB store
        dbDocumentService.removeDocument(asset);
        log.info("Deleted asset identified by inum {}", inum);
        
        // remove asset from server
        boolean status = deleteAssetFromServer(asset);
        log.info("Status on deleting asset from server is:{}", status);
        if(!status) {
            log.error("Could not remove asset from server identified by inum:{}",inum);
            throw new WebApplicationException("Could not delete asset identified by inum - "+inum);
        }
        
        return status;
    }

    public InputStream readAssetStream(String assetName) throws Exception {
        log.info("Read asset stream from server - assetName:{}", assetName);
        String filePath = null;

        if (StringUtils.isBlank(assetName)) {
            throw new InvalidConfigurationException("Asset name is null!");
        }

        List<Document> assets = this.getAssetByName(assetName);
        log.info("assets{} identified by assetName:{}", assets, assetName);

        if (assets == null || assets.isEmpty()) {
            throw new NotFoundException("Cannot find asset identified by - " + assetName);
        }

        Document asset = assets.get(0);
        String assetPath = asset.getDescription();
        filePath = assetPath + File.separator + assetName;
        log.info("documentStoreService:{}, filePath:{}, localDocumentStoreService:{} ", documentStoreService, filePath,
                localDocumentStoreService);

        InputStream stream = dBDocumentStoreProvider.readDocumentAsStream(filePath);
        log.info("Read asset stream:{}", stream);

        return stream;

    }

    private Document updateAsset(Document asset, InputStream documentStream) throws Exception {
        log.info("Update an asset - asset:{}, documentStream:{}", asset, documentStream);
        if (asset == null) {
            throw new InvalidAttributeException(" Asset object is null!!!");
        }

        if (documentStream == null) {
            throw new InvalidAttributeException(" Asset data stream object is null!!!");
        }

        String documentContent = new String(documentStream.readAllBytes(), StandardCharsets.UTF_8);
        asset.setDocument(documentContent);
        dbDocumentService.updateDocument(asset);

        // Get final asset
        asset = dbDocumentService.getDocumentByInum(asset.getInum());

        log.info("\n * Successfully updated asset:{}", asset);
        return asset;
    }

    private boolean deleteAssetFromServer(Document asset) {
        log.info("Delete asset - asset:{}", asset);
        boolean deleteStatus = false;
        if (asset == null) {
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

        if (documentStoreService == null) {
            throw new InvalidConfigurationException("document Store Service is null!");
        }

        String filePath = path + File.separator + fileName;
        log.info("documentStoreService:{}, localDocumentStoreService:{}, filePath:{} ", documentStoreService,
                localDocumentStoreService, filePath);
        deleteStatus = documentStoreService.removeDocument(filePath);
        log.info("Asset deletion status:{}", deleteStatus);
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
            asset.setJansRevision(revision);
            log.info("Updated asset revision - asset:{}", asset);
        } catch (Exception ex) {
            log.error("Exception while updating asset revision is - ", ex);
            return asset;
        }
        return asset;
    }

    private Document saveNewAsset(Document asset, InputStream stream) {
        log.info("Saving new asset in DB DocumentStore - asset:{}, stream:{}", asset, stream);
        String path = dBDocumentStoreProvider.saveBinaryDocumentStream(asset.getDisplayName(), asset.getDescription(), stream,
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

    private InputStream getInputStream(ByteArrayOutputStream bos) {
        return authUtil.getInputStream(bos);
    }

}
