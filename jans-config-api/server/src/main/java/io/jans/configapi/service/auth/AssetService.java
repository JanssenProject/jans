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
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.AssetDirMapping;
import io.jans.configapi.model.configuration.AssetMgtConfiguration;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.FilenameUtils;
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
    DBDocumentStoreProvider dBDocumentStoreProvider;

    @Inject
    DocumentStoreService documentStoreService;

    @Inject
    DBDocumentService dbDocumentService;

    @Inject
    private ApiAppConfiguration appConfiguration;

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
        log.trace("Asset by name:{} are documents:{}", name, documents);
        return documents;
    }

    public Document saveAsset(Document asset, InputStream documentStream) throws Exception {
        log.info("Save asset - asset:{}, documentStream:{}", asset, documentStream);

        if (asset == null) {
            throw new InvalidAttributeException("Asset object is null!!!");
        }

        if (documentStream == null) {
            throw new InvalidAttributeException(" Document data stream object is null!!!");
        }

        ByteArrayOutputStream bos = getByteArrayOutputStream(documentStream);
        log.trace("Asset ByteArrayOutputStream :{}", bos);

        // get asset
        try (InputStream is = new Base64InputStream(getInputStream(bos), true)) {
            asset = setAssetContent(asset, is);
        }
        // save asset in DB store
        String inum = asset.getInum();
        log.trace("inum of asset to be saved is:{}", inum);
        if (StringUtils.isBlank(inum)) {
            inum = dbDocumentService.generateInumForNewDocument();
            asset.setInum(inum);
            String dn = "inum=" + asset.getInum() + ",ou=document,o=jans";
            asset.setDn(dn);
            log.info("As inum is blank create new asset with inum:{}", inum);
            dbDocumentService.addDocument(asset);
        } else {
            log.info("Inum is not blank hence update existing asset with inum :{}", inum);
            dbDocumentService.updateDocument(asset);
        }

        // copy asset on jans-server
        if (isAssetServerUploadEnabled()) {
            String result = copyAssetOnServer(asset, bos);
            log.info("Result of asset saved on server :{}", result);

        }

        // Get final asset
        asset = this.getAssetByInum(asset.getInum());

        log.info("\n * Asset successfully saved :{}", asset);
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
        if (!status) {
            log.error("Could not remove asset from server identified by inum:{}", inum);
            throw new WebApplicationException("Could not delete asset identified by inum - " + inum);
        }

        return status;
    }

    private Document setAssetContent(Document asset, InputStream documentStream) throws IOException {
        log.info(" an asset - asset:{}, documentStream:{}", asset, documentStream);
        if (asset == null) {
            throw new InvalidAttributeException(" Asset object is null!!!");
        }

        if (documentStream == null) {
            throw new InvalidAttributeException(" Asset data stream is null!!!");
        }

        String documentContent = new String(documentStream.readAllBytes(), StandardCharsets.UTF_8);
        asset.setDocument(documentContent);

        // update asset revision
        updateRevision(asset);

        log.info("\n * Successfully updated asset");
        return asset;
    }

    private Document updateRevision(Document asset) {
        log.debug("Update asset revision - asset:{}", asset);
        try {
            if (asset == null) {
                return asset;
            }

            int intRevision = asset.getJansRevision();
            log.debug(" Current asset intRevision is:{}", intRevision);
            asset.setJansRevision(intRevision++);
            log.info("Updated asset revision to asset.getJansRevision():{}", asset.getJansRevision());
        } catch (Exception ex) {
            log.error("Exception while updating asset revision is - ", ex);
            return asset;
        }
        return asset;
    }

    private String copyAssetOnServer(Document asset, ByteArrayOutputStream stream) throws IOException {
        log.info("Copy asset on server - asset:{}, stream:{}", asset, stream);
        String result = null;

        if (asset == null) {
            throw new InvalidConfigurationException("Asset is null!");
        }

        if (stream == null) {
            throw new InvalidConfigurationException("Asset stream is null!");
        }

        List<String> serviceModules = asset.getJansModuleProperty();
        String assetFileName = asset.getDisplayName();
        String documentStoreModuleName = assetFileName;
        log.info("Save asset for - serviceModules:{}, assetFileName:{}", serviceModules, assetFileName);

        if (StringUtils.isBlank(assetFileName)) {
            throw new InvalidConfigurationException("Asset name is null!");
        }

        String assetDir = this.getAssetDir(assetFileName);
        log.info("For saving assetFileName:{} assetDir:{}", assetFileName, assetDir);
        
        if(StringUtils.isBlank(assetFileName)|| StringUtils.isBlank(FilenameUtils.getExtension(assetFileName)) ){
            throw new InvalidConfigurationException("Valid file name not provided!");
        }
        
        if (serviceModules == null || serviceModules.isEmpty()) {
            throw new InvalidConfigurationException("Service module list is null or empty!");
        }
        
        for (String serviceName : serviceModules) {

            String serviceDirectory = this.getServiceDirectory(assetDir, serviceName);
            log.info("Save asset for - serviceName:{} in serviceDirectory:{}", serviceName, serviceDirectory);

            if (StringUtils.isBlank(serviceDirectory)) {
                throw new InvalidConfigurationException("Service directory to save asset is null!");
            }

            String filePath = serviceDirectory + File.separator + assetFileName;
            log.info("To save asset - documentStoreService:{}, filePath:{} ", documentStoreService, filePath);

            try (InputStream ins = getInputStream(stream)) {
                result = documentStoreService.saveDocumentStream(filePath, null, ins, List.of(documentStoreModuleName));
                log.info("Result of asset saved on server :{}", result);
            }

        }
        return result;

    }

    private boolean deleteAssetFromServer(Document asset) {
        log.info("Delete asset - asset:{}", asset);
        boolean deleteStatus = false;
        if (asset == null) {
            return deleteStatus;
        }

        List<String> serviceModules = asset.getJansModuleProperty();
        String assetFileName = asset.getDisplayName();

        log.info("Asset to be deleted for serviceModules:{}, assetFileName:{}", serviceModules, assetFileName);

        if (StringUtils.isBlank(assetFileName)) {
            throw new InvalidConfigurationException("Asset name is null!");
        }

        String assetDir = this.getAssetDir(assetFileName);
        log.info("For removing assetFileName:{} assetDir:{}", assetFileName, assetDir);

        for (String serviceName : serviceModules) {

            String serviceDirectory = this.getServiceDirectory(assetDir, serviceName);
            log.info("Delete asset from - assetDir:{}, serviceDirectory:{}", assetDir, serviceDirectory);

            if (StringUtils.isBlank(serviceDirectory)) {
                throw new InvalidConfigurationException("Service directory to save asset is null!");
            }
            String filePath = serviceDirectory + File.separator + assetFileName;
            try {
                log.info("To delete asset - documentStoreService:{}, filePath:{} ", documentStoreService, filePath);
                deleteStatus = documentStoreService.removeDocument(filePath);
                log.info("Asset deletion status:{}", deleteStatus);
            } catch (Exception ex) {
                log.error("Error while deleting asset:{} with fileName:{} from server is:{}", asset.getInum(),
                        assetFileName, ex);
            }

        }

        return deleteStatus;
    }

    private ByteArrayOutputStream getByteArrayOutputStream(InputStream input) throws IOException {
        return authUtil.getByteArrayOutputStream(input);
    }

    private InputStream getInputStream(ByteArrayOutputStream bos) {
        return authUtil.getInputStream(bos);
    }

    private boolean isAssetServerUploadEnabled() {
        return this.appConfiguration.getAssetMgtConfiguration().isAssetServerUploadEnabled();
    }

    private String getAssetDir(String assetFileName) {
        log.info("Get asset directory assetFileName:{}", assetFileName);
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isBlank(assetFileName) || this.appConfiguration == null
                || this.appConfiguration.getAssetMgtConfiguration() == null) {
            return sb.toString();
        }

        AssetMgtConfiguration assetMgtConfiguration = this.appConfiguration.getAssetMgtConfiguration();
        sb.append(assetMgtConfiguration.getAssetBaseDirectory());
        String assetDir = getAssetDirectory(assetFileName);

        log.info("assetMgtConfiguration:{}, sb:{}, assetDir:{}", assetMgtConfiguration, sb, assetDir);
        if (StringUtils.isNotBlank(assetDir)) {
            sb.append(File.separator);
            sb.append(assetDir);
        }

        return sb.toString();
    }

    private String getServiceDirectory(String assetDir, String serviceName) {

        log.info("Get service directory assetDir:{}, serviceName:{}", assetDir, serviceName);

        String path = null;
        if (StringUtils.isBlank(assetDir) || StringUtils.isBlank(serviceName)) {
            return path;
        }
        path = String.format(assetDir, serviceName);

        return path;

    }

    private String getAssetDirectory(String assetFileName) {
        log.info("Get asset Directory for assetFileName:{}", assetFileName);

        String directory = null;
        if (StringUtils.isBlank(assetFileName) || this.appConfiguration == null
                || this.appConfiguration.getAssetMgtConfiguration() == null) {
            return directory;
        }

        List<AssetDirMapping> dirMapping = this.appConfiguration.getAssetMgtConfiguration().getAssetDirMapping();
        log.info("Get asset Directory - dirMapping:{}", dirMapping);
        if (dirMapping == null || dirMapping.isEmpty()) {
            return directory;
        }
        String fileExtension = FilenameUtils.getExtension(assetFileName);
        log.info("Get asset Directory - fileExtension:{}", fileExtension);

        Optional<AssetDirMapping> assetDirMapping = dirMapping.stream().filter(e -> e.getType().contains(fileExtension))
                .findFirst();
        log.info("Get asset Directory - assetDirMapping.isPresent():{}", assetDirMapping.isPresent());

        if (assetDirMapping.isEmpty()) {
            return directory;
        }

        directory = assetDirMapping.get().getDirectory();
        return directory;
    }

}
