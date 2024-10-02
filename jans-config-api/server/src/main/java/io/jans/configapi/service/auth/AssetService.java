/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;



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
import io.jans.service.document.store.model.Document;
import io.jans.service.document.store.provider.DBDocumentStoreProvider;
import io.jans.service.document.store.service.DBDocumentService;
import io.jans.service.document.store.conf.DocumentStoreType;

import io.jans.util.exception.InvalidAttributeException;
import io.jans.util.exception.InvalidConfigurationException;
import io.jans.service.document.store.service.DocumentStoreService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.stream.*;

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

    public PagedResult<Document> searchAssetByName(SearchRequest searchRequest) throws Exception {
        log.info("Search asset with searchRequest:{}", searchRequest);

        Filter nameFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter(AttributeConstants.DISPLAY_NAME),
                null, new String[] { searchRequest.getFilter() }, null);

        log.debug("Asset Search nameFilter:{}", nameFilter);
        return persistenceEntryManager.findPagedEntries(getDnForAsset(null), Document.class, nameFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());
    }

    public Document saveAsset(Document asset, InputStream documentStream, boolean isUpdate) throws Exception {
        log.info("Save asset - asset:{}, documentStream:{}, isUpdate:{}", asset, documentStream, isUpdate);

        if (asset == null) {
            throw new InvalidAttributeException("Asset object is null!!!");
        }

        // For update request, asset file is optional.
        if (!isUpdate && documentStream == null) {
            throw new InvalidAttributeException(" Document data stream object is null!!!");
        }

        // validation
        validateModules(asset);
        validateAssetMetadata(asset);
        ByteArrayOutputStream bos = null;
        if (documentStream != null) {
            validateFileExtension(asset);

            bos = getByteArrayOutputStream(documentStream);
            log.trace("Asset ByteArrayOutputStream :{}", bos);

            // get asset
            try (InputStream is = new Base64InputStream(getInputStream(bos), true)) {
                asset = setAssetContent(asset, is);
            }
        }

        if (isUpdate && documentStream == null) {
            // update request without asset file, get the existing asset content from DB
            Document existingDoc = getAssetByInum(asset.getInum());
            if (existingDoc == null) {
                throw new InvalidAttributeException("Asset with inum '" + asset.getInum() + "' does not exist!!!");
            } else {
                asset.setDocument(existingDoc.getDocument());
            }
        }

        // update asset revision
        updateRevision(asset, isUpdate);

        // copy asset on jans-server
        if (documentStream != null && isAssetServerUploadEnabled()) {

            try (InputStream is = new Base64InputStream(getInputStream(bos), true)) {
                DocumentStoreType documentStoreProvider = this.getDocumentStoreType();
                log.info("Copy asset on server documentStoreProvider is:{}", documentStoreProvider);
                if (documentStoreProvider == DocumentStoreType.LOCAL) {
                    String result = copyAssetOnServer(asset, is);
                    log.info("Result of asset saved on server :{}", result);
                }
            }
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

        // Get final asset
        asset = this.getAssetByInum(asset.getInum());

        log.info("\n * Asset successfully saved :{}", asset);
        return asset;
    }

    public String loadServiceAsset(String serviceName) throws Exception {
        log.info("Fetch and load asset for serviceName:{}", serviceName);

        StringBuilder sb = new StringBuilder();
        if (StringUtils.isBlank(serviceName)) {
            throw new InvalidAttributeException("Service name is null!!!");
        }
        Filter serviceNameFilter = Filter.createEqualityFilter("jansService", serviceName);
        List<Document> assets = persistenceEntryManager.findEntries(getDnForAsset(null), Document.class,
                serviceNameFilter);
        log.info(" serviceNameFilter:{}, assets:{}", serviceNameFilter, assets);
        if (assets == null || !assets.isEmpty()) {
            sb.append(" No asset found for service{" + serviceName + "}");
            log.info(" No asset found for service:{}", serviceName);
            return sb.toString();
        }

        // copy assets on server
        for (Document asset : assets) {
            InputStream in = this.readDocumentAsStream(asset.getFileName(), asset.getDocument());
            if (in == null) {
                sb.append("Asset file for service{" + serviceName + "} is blank");
            }

            // save on server
            String result = copyAssetOnServer(asset, in);
            log.info("Asset file:{} load result for serviceName:{} is:{}", asset.getFileName(), serviceName, result);
            sb.append("Asset file:{" + asset.getFileName() + "} load result for service:{" + serviceName + "} is:{"
                    + result + "}");

        }
        return sb.toString();
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

        asset = this.getAssetByInum(inum);
        log.info("Checking if asset is deleted properly - asset :{}", asset);
        if (asset != null) {
            log.error("Could not remove asset from server identified by inum:{}", inum);
            throw new WebApplicationException("Could not delete asset identified by inum - " + inum);
        }

        return true;
    }

    public List<String> getValidServiceModuleName() {
        List<String> serviceModules = null;
        AssetMgtConfiguration assetMgtConfiguration =  this.getAssetMgtConfiguration();
        log.info("assetMgtConfiguration:{} ", assetMgtConfiguration);
        if(assetMgtConfiguration == null || assetMgtConfiguration.getAssetDirMapping()==null)
        {
            return serviceModules;
        }
        
        List<AssetDirMapping> assetDirMappingList = this.getAssetDirMapping();
        
        if(assetDirMappingList==null || assetDirMappingList.isEmpty()) {
            return serviceModules;
        }

        HashSet<String> dirSet = new HashSet<>();
        for(AssetDirMapping mapping: assetDirMappingList) {
            dirSet.addAll(mapping.getJansServiceModule());
        }
        
        serviceModules = new ArrayList(dirSet);
        log.info(" ServiceModuleName  serviceModules:{}- ", serviceModules);
        return serviceModules;        
    }

    public List<String> getValidFileExtension() {
        List<String> validFileExtension = new ArrayList<>();

        List<AssetDirMapping> assetDirList = this.getAssetDirMapping();
        
        if (assetDirList == null || assetDirList.isEmpty()) {
            return validFileExtension;
        }

        for (AssetDirMapping dir : assetDirList) {
            validFileExtension.addAll(dir.getType());
        }

        log.info("validFileExtension:{}  - ", validFileExtension);

        return validFileExtension;
    }

    public InputStream readDocumentAsStream(Document asset) {
        log.info(" Asset to fetch file - asset:{}", asset);
        if (asset == null) {
            throw new InvalidAttributeException(" Asset object is null!!!");
        }
        return readDocumentAsStream(asset.getFileName(), asset.getDocument());

    }

    private Document setAssetContent(Document asset, InputStream documentStream) throws IOException {
        log.info(" Set asset content - asset:{}, documentStream:{}", asset, documentStream);
        if (asset == null) {
            throw new InvalidAttributeException(" Asset object is null!!!");
        }

        if (documentStream == null) {
            throw new InvalidAttributeException(" Asset data stream is null!!!");
        }

        String documentContent = new String(documentStream.readAllBytes(), StandardCharsets.UTF_8);
        asset.setDocument(documentContent);

        log.info("Successfully updated asset");
        return asset;
    }

    private Document updateRevision(Document asset, boolean isUpdate) {
        log.debug("Update asset revision - asset:{}, isUpdate:{}", asset, isUpdate);
        try {
            if (asset == null) {
                return asset;
            }

            int intRevision = asset.getRevision();
            log.debug(" Current asset intRevision is:{}", intRevision);

            if (isUpdate) {
                intRevision = intRevision + intRevision;
            }
            asset.setRevision(intRevision);
            log.info("Updated asset revision to asset.getJansRevision():{}", asset.getRevision());
        } catch (Exception ex) {
            log.error("Exception while updating asset revision is - ", ex);
            return asset;
        }
        return asset;
    }

    private Document validateAssetMetadata(Document asset) {
        log.info("Validate Asset File type - asset:{}", asset);
        if (asset == null) {
            throw new InvalidConfigurationException("Asset is null!");
        }
        String assetFileName = asset.getFileName();
        log.info("assetFileName", assetFileName);
        if (StringUtils.isBlank(assetFileName)) {
            throw new InvalidConfigurationException("Asset name is null!");
        }

        String assetDir = this.getAssetDir(asset);
        asset.setFilePath(assetDir);
        log.info("For saving assetFileName:{} assetDir:{}", assetFileName, assetDir, asset.getFileName());

        return asset;
    }
    
    private String copyAssetOnServer(Document asset, InputStream stream) throws IOException {
        log.info("Copy asset on server - asset:{}, stream:{}", asset, stream);
        String result = null;

        if (asset == null) {
            throw new InvalidConfigurationException("Asset is null!");
        }

        if (stream == null) {
            throw new InvalidConfigurationException("Asset stream is null!");
        }

        List<String> serviceModules = asset.getService();
        String assetFileName = asset.getFileName();
        log.info("Copy assetFileName:{} for serviceModules:{}", asset, serviceModules);
        if (StringUtils.isBlank(assetFileName)) {
            throw new InvalidConfigurationException("Asset name is null!");
        }

        String assetDir = asset.getFilePath();
        log.info("For saving assetFileName:{} assetDir:{}", assetFileName, assetDir, asset.getFileName());

        // validate service directory
        validateServiceDirectory(assetFileName, assetDir, serviceModules);

        for (String serviceName : serviceModules) {
            result = copyAsset(assetFileName, assetDir, serviceName, stream);
        }
        return result;
    }

    private String copyAsset(String assetFileName, String assetDir, String serviceName, InputStream stream)
            throws IOException {
        String result = null;

        String serviceDirectory = this.getServiceDirectory(assetDir, serviceName);
        log.info("Save asset for - serviceName:{} in serviceDirectory:{}", serviceName, serviceDirectory);
        String filePath = serviceDirectory + File.separator + assetFileName;
        log.info("To save asset - documentStoreService:{}, filePath:{} ", documentStoreService, filePath);

        try (stream) {
            result = documentStoreService.saveDocumentStream(filePath, null, stream, List.of(assetFileName));
            log.info("Result of asset saved on server :{}", result);
        }

        return result;
    }

    private boolean deleteAssetFromServer(Document asset) {
        log.info("Delete asset - asset:{}", asset);
        boolean deleteStatus = false;
        if (asset == null) {
            return deleteStatus;
        }

        List<String> serviceModules = asset.getService();
        String assetFileName = asset.getFileName();

        log.info("Asset to be deleted for serviceModules:{}, assetFileName:{}", serviceModules, assetFileName);

        if (StringUtils.isBlank(assetFileName)) {
            throw new InvalidConfigurationException("Asset name is null!");
        }

        String assetDir = asset.getFilePath();
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
        return this.getAssetMgtConfiguration().isAssetServerUploadEnabled();
    }

    private String getFileExtension(String fileName) {
        return FilenameUtils.getExtension(fileName);
    }

    private String getAssetDir(Document asset) {
        log.info("Get asset directory asset:{}", asset);
        
        if (asset == null) {
            throw new InvalidConfigurationException("Asset details is empty!");
        }
        
        String assetFileName = asset.getFileName();
        StringBuilder sb = new StringBuilder();
        
        if (StringUtils.isBlank(assetFileName) || this.appConfiguration == null
                || this.getAssetMgtConfiguration() == null) {
            return sb.toString();
        }

        AssetMgtConfiguration assetMgtConfiguration = this.getAssetMgtConfiguration();

    
        //sb.append(assetMgtConfiguration.getAssetBaseDirectory());
        String assetDir = getAssetDirectory(asset);
        log.info("assetMgtConfiguration:{}, sb:{}, assetDir:{}", assetMgtConfiguration, sb, assetDir);

        if (StringUtils.isBlank(assetDir)) {
            throw new InvalidConfigurationException(
                    "Directory to upload asset [" + assetFileName + "] is not defined in config!");
        }

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
        log.info("Service directory assetDir:{}, serviceName:{}, path:{}", assetDir, serviceName, path);

        return path;

    }

    private String getAssetDirectory(Document asset) {
        log.info("Get asset Directory for asset:{}", asset);

        String directory = null;
        String assetFileName = asset.getFileName();
        String filePath = asset.getFilePath();
        List<String> serviceModuleList = asset.getService();
        
        if (StringUtils.isBlank(assetFileName)) {
            throw new InvalidConfigurationException("Asset FileName is null!");
        }
        
        if (serviceModuleList==null || serviceModuleList.isEmpty()) {
            throw new InvalidConfigurationException("Service module for asset to be upload is null!");
        }

        List<AssetDirMapping> dirMapping = this.getAssetDirMapping();
        log.info("Get asset Directory - dirMapping:{}", dirMapping);
        if (dirMapping == null || dirMapping.isEmpty()) {
            return directory;
        }
        String fileExtension = this.getFileExtension(assetFileName);
        log.info("Get asset Directory for fileExtension:{}", fileExtension);
                     
        List<AssetDirMapping> assetDirMappingList = dirMapping.stream().filter(e -> e.getType().contains(fileExtension)).toList();
        log.info(" AssetDirMapping for fileExtension:{} is assetDirMappingList:{}", fileExtension, assetDirMappingList);

        if (assetDirMappingList==null || assetDirMappingList.isEmpty()) {
            throw new InvalidConfigurationException(
                    "AssetDirMapping for [" + fileExtension + "] is not defined in config!");
        }
        
        List<AssetDirMapping> moduleMappingList = assetDirMappingList.stream().filter(e -> e.getJansServiceModule().contains(serviceModuleList)).toList();
        log.info(" AssetDirMapping for specified service:{} is moduleMappingList:{}", serviceModuleList, moduleMappingList);

        
        if (moduleMappingList==null || moduleMappingList.isEmpty()) {
            throw new InvalidConfigurationException(
                    "AssetDirMapping for [" + serviceModuleList + "] is not defined in config!");
        }
        
        HashMap<String, AssetDirMapping> serviceMap = new HashMap<>();
        for(String service : serviceModuleList) {
            log.info("Get mapping for service:{}", service);
               for(AssetDirMapping mapping : assetDirMappingList) {
                   log.info("mapping:{}", mapping);
                   if(mapping.getJansServiceModule()!=null && mapping.getJansServiceModule().contains(service)) {
                       serviceMap.put(service, mapping);
                   }
        }
    }
        
        if (serviceMap==null || serviceMap.isEmpty()) {
            throw new InvalidConfigurationException(
                    "AssetDirMapping for [" + serviceModuleList + "] is not defined in config!");
        }
        
        
        Set<String> keys = serviceMap.keySet();
        List<String> missingService = missingElements(new ArrayList(keys), serviceModuleList);
        if (serviceMap==null || serviceMap.isEmpty()) {
            throw new InvalidConfigurationException(
                    "AssetDirMapping for [" + missingService + "] is not defined in config!");
        }
        
        
        
        directory = assetDirMapping.get().getDirectory();
        return directory;
    }


    private List<String> missingElements(List<String> a, List<String> b) {
        return a.stream()
                .filter(s->!b.contains(s))
                .collect(Collectors.toList());
    }
    
    private boolean isFileExtensionValidationEnabled() {
        return this.appConfiguration.getAssetMgtConfiguration().isFileExtensionValidationEnabled();
    }

    private boolean isModuleNameValidationEnabled() {
        return this.appConfiguration.getAssetMgtConfiguration().isModuleNameValidationEnabled();
    }

    private void validateFileExtension(Document asset) {

        if (asset == null || appConfiguration.getAssetMgtConfiguration() == null
                || appConfiguration.getAssetMgtConfiguration().getAssetDirMapping() == null
                || appConfiguration.getAssetMgtConfiguration().getAssetDirMapping().isEmpty()
                || !isFileExtensionValidationEnabled()) {
            return;
        }

        String fileName = asset.getFileName();
        String fileExtension = this.getFileExtension(fileName);
        List<String> validFileExtensions = this.getValidFileExtension();
        log.info("Checking valid file extention - fileName:{}, fileExtension:{}, validFileExtensions:{}", fileName,
                fileExtension, validFileExtensions);

        if (StringUtils.isBlank(fileName) || StringUtils.isBlank(fileExtension)) {
            throw new InvalidConfigurationException("Valid file name not provided!");
        }

        if (validFileExtensions.isEmpty()) {
            return;
        }

        boolean isValidExtension = validFileExtensions.contains(fileExtension);

        if (!isValidExtension) {
            throw new InvalidConfigurationException("Valid file type are '{" + validFileExtensions + "}', '{"
                    + fileExtension + "}' name not supported!");
        }

    }

    private void validateModules(Document asset) {

        if (asset == null || asset.getService() == null || asset.getService().isEmpty()) {
            throw new InvalidConfigurationException("Service module to save asset is not provided in request!");
        }

        List<String> validModules = getValidServiceModuleName();
        log.info("validModules:{} ", validModules);

        if (validModules == null || validModules.isEmpty() || !isModuleNameValidationEnabled()) {
            throw new InvalidConfigurationException("Service module not configured in system! ");
        }

        List<String> invalidModuleList = authUtil.findMissingElements(asset.getService(), validModules);
        log.info("invalidModuleList:{}", invalidModuleList);

        if (invalidModuleList != null && !invalidModuleList.isEmpty()) {
            throw new InvalidConfigurationException(
                    "Valid modules are '{" + validModules + "}', '{" + invalidModuleList + "}' not supported!");
        }
    }

    private void validateServiceDirectory(String assetFileName, String assetDir, List<String> serviceModules) {
        log.info("validate service directory details - assetFileName,:{}, assetDir:{}, serviceModules:{}", assetDir,
                assetFileName, serviceModules);
        StringBuilder invalidServiceDirList = new StringBuilder();
        StringBuilder missingMapping = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        
        for (String serviceName : serviceModules) {

            String serviceDirectory = this.getServiceDirectory(assetDir, serviceName);
            if (StringUtils.isBlank(serviceDirectory)) {
                missingMapping.append(serviceName);
            }

            // check if the asset directory exist
            boolean serviceDirectoryExist = isServiceDirectoryExist(serviceDirectory);
            if (!serviceDirectoryExist) {
                invalidServiceDirList.append(serviceName);
            }
        }

        log.debug("missingMapping:{}, invalidServiceDirList:{}", missingMapping, invalidServiceDirList);
        if (StringUtils.isNotBlank(missingMapping.toString())) {
            errorMsg.append("Cannot save asset as service directory mapping for [" + missingMapping + "] is null!");
        }

        if (StringUtils.isNotBlank(invalidServiceDirList.toString())) {
            errorMsg.append("Service directory to save asset [" + invalidServiceDirList + "] does not exist!");
        }

        log.info("errorMsg:{}", errorMsg);
        if (StringUtils.isNotBlank(errorMsg.toString())) {
            throw new InvalidConfigurationException(errorMsg.toString());
        }
    }

    private boolean isServiceDirectoryExist(String serviceDirectory) {
        File dir = new File(serviceDirectory);
        boolean serviceDirectoryExist = dir.exists();
        log.info("Check using File API serviceDirectory:{} - exist:{}", serviceDirectory, serviceDirectoryExist);
        return serviceDirectoryExist;
    }

    private InputStream readDocumentAsStream(String name, String assetContent) {
        log.debug("Asset name:{} assetContent: '{}'", name, assetContent);

        if (StringUtils.isBlank(assetContent)) {
            log.error("Asset file name '{}' is empty", name);
            return null;
        }
        return new ByteArrayInputStream(assetContent.getBytes());
    }

    private DocumentStoreType getDocumentStoreType() {
        return documentStoreService.getProviderType();
    }
    
    private AssetMgtConfiguration getAssetMgtConfiguration() {
        return this.appConfiguration.getAssetMgtConfiguration();
    }
    
    private List<AssetDirMapping> getAssetDirMapping() {
        List<AssetDirMapping> assetDirMapping = null;
        if(this.getAssetMgtConfiguration() == null) {
            return assetDirMapping;
        }
        return this.getAssetMgtConfiguration().getAssetDirMapping();
    }

}
