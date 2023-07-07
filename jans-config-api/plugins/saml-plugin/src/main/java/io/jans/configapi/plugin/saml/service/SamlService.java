/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.as.common.service.OrganizationService;
import io.jans.model.SearchRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.service.document.store.conf.DBDocumentStoreConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.JcaDocumentStoreConfiguration;
import io.jans.service.document.store.conf.LocalDocumentStoreConfiguration;
import io.jans.service.document.store.conf.WebDavDocumentStoreConfiguration;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

@ApplicationScoped
public class SamlService {

    private static final String SAML_DN_BASE = "ou=trustRelationships,o=jans";
    public static final String SHIB3_IDP_TEMPMETADATA_FOLDER = "temp_metadata";
    private static final String SHIB3_SP_METADATA_FILE_PATTERN = "%s-sp-metadata.xml";

    @Inject
    Logger log;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    OrganizationService organizationService;

    @Inject
    private transient InumService inumService;

    @Inject
    DocumentStoreConfiguration documentStoreConfiguration;

    @Inject
    SamlIdpService samlIdpService;

    public String baseDn() {
        // return staticConfiguration.getBaseDn().getTrustRelationshipDn();
        return SAML_DN_BASE;
    }

    public boolean containsRelationship(String dn) {
        return persistenceEntryManager.contains(dn, TrustRelationship.class);
    }

    public TrustRelationship getRelationshipByDn(String dn) {
        if (StringHelper.isNotEmpty(dn)) {
            try {
                return persistenceEntryManager.find(TrustRelationship.class, dn);
            } catch (Exception e) {
                log.info(e.getMessage());
            }

        }
        return null;
    }

    public TrustRelationship getTrustRelationshipByInum(String inum) {
        TrustRelationship result = null;
        try {
            result = persistenceEntryManager.find(TrustRelationship.class, getDnForTrustRelationship(inum));
        } catch (Exception ex) {
            log.error("Failed to load TrustRelationship entry", ex);
        }
        return result;
    }

    public List<TrustRelationship> getAllTrustRelationship() {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null);
    }

    public List<TrustRelationship> getAllTrustRelationshipByInum(String inum) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(inum), TrustRelationship.class, null);
    }

    public List<TrustRelationship> getAllTrustRelationshipByName(String name) {
        log.error("Search TrustRelationship with name:{}", name);

        String[] targetArray = new String[] { name };
        Filter displayNameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, targetArray);
        log.error("Search TrustRelationship with displayNameFilter:{}", displayNameFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                displayNameFilter);
    }

    public TrustRelationship getTrustContainerFederation(TrustRelationship trustRelationship) {
        TrustRelationship relationshipByDn = getRelationshipByDn(trustRelationship.getDn());
        return relationshipByDn;
    }

    public TrustRelationship getTrustContainerFederation(String dn) {
        TrustRelationship relationshipByDn = getRelationshipByDn(dn);
        return relationshipByDn;
    }

    public List<TrustRelationship> getAllTrustRelationships(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null,
                sizeLimit);
    }

    public List<TrustRelationship> searchTrustRelationship(String pattern, int sizeLimit) {

        log.error("Search TrustRelationship with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        log.error("Search TrustRelationship with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                searchFilter, sizeLimit);
    }

    public List<TrustRelationship> getAllTrustRelationship(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null,
                sizeLimit);
    }

    public PagedResult<Client> getTrustRelationship(SearchRequest searchRequest) {
        log.error("Search TrustRelationship with searchRequest:{}", searchRequest);

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

        log.trace("TrustRelationship pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("TrustRelationship dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.error("TrustRelationship searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForTrustRelationship(null), Client.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public TrustRelationship addTrustRelationship(TrustRelationship trustRelationship, InputStream file) throws IOException {
        log.error("Add new trustRelationship:{}, file:{}", trustRelationship, file);
        setTrustRelationship(trustRelationship, false);
        persistenceEntryManager.persist(trustRelationship);

        if (file != null) {
            saveSpMetaDataFileSourceTypeFile(trustRelationship, file);
        }

        return getTrustRelationshipByInum(trustRelationship.getInum());
    }

    public TrustRelationship addTrustRelationship(TrustRelationship trustRelationship) throws IOException {

        return addTrustRelationship(trustRelationship, null);
    }

    public void removeTrustRelationship(TrustRelationship trustRelationship) {
        persistenceEntryManager.removeRecursively(trustRelationship.getDn(), TrustRelationship.class);

    }

    public TrustRelationship updateTrustRelationship(TrustRelationship trustRelationship) {
        setTrustRelationship(trustRelationship, true);
        persistenceEntryManager.merge(trustRelationship);
        return getTrustRelationshipByInum(trustRelationship.getInum());
    }

    public TrustRelationship setTrustRelationship(TrustRelationship trustRelationship, boolean update) {
        return trustRelationship;
    }

    public String getDnForTrustRelationship(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=trustRelationships,%s", orgDn);
        }
        return String.format("inum=%s,ou=trustRelationships,%s", inum, orgDn);
    }

    public String generateInumForNewRelationship() {
        String newInum = null;
        String newDn = null;
        do {
            newInum = UUID.randomUUID().toString();
            newDn = getDnForTrustRelationship(newInum);
        } while (containsRelationship(newDn));

        return newInum;
    }

    private boolean saveMetaData(TrustRelationship trustRelationship, InputStream file) {
        log.error("Saving MetaData trustRelationship:{}, file:{}", trustRelationship, file);

        if (trustRelationship == null || trustRelationship.getSpMetaDataSourceType() == null) {
            return false;
        }

        switch (trustRelationship.getSpMetaDataSourceType()) {
        case FILE:
            try {
                if (saveSpMetaDataFileSourceTypeFile(trustRelationship, file)) {

                } else {
                    log.error("Failed to save SP meta-data file {}");
                    return false;
                }
            } catch (IOException ex) {
                log.error("Failed to download SP metadata", ex);
                return false;
            }
            return true;
        }
        return true;
    }

    private boolean saveSpMetaDataFileSourceTypeFile(TrustRelationship trustRelationship, InputStream file)
            throws IOException {
        
        log.error("TrustRelationship trustRelationship:{}", trustRelationship, file);
        // To-be-removed
        LocalDocumentStoreConfiguration localConfiguration = getDocumentStoreConfiguration();
        log.error("localConfiguration:{}", localConfiguration);
        
        String spMetadataFileName = trustRelationship.getSpMetaDataFN();
        boolean emptySpMetadataFileName = StringHelper.isEmpty(spMetadataFileName);
        log.error("emptySpMetadataFileName:{}", emptySpMetadataFileName);
        if ((file == null) ) {
            log.error("File is null");
            if (emptySpMetadataFileName) {
                log.debug("The trust relationship {} has an empty Metadata filename", trustRelationship.getInum());
                return false;
            }
            String filePath = samlIdpService.getSpMetadataFilePath(spMetadataFileName);
            log.error("filePath:{}",filePath);
            if (filePath == null) {
                log.debug("The trust relationship {} has an invalid Metadata file storage path",
                        trustRelationship.getInum());
                return false;
            }

            if (samlIdpService.isLocalDocumentStoreType()) {

                File newFile = new File(filePath);
                log.error("newFile:{}",newFile);
                if (!newFile.exists()) {
                    log.debug(
                            "The trust relationship {} metadata used local storage but the SP metadata file `{}` was not found",
                            trustRelationship.getInum(), filePath);
                    return false;
                }
            }
            return true;
        }
        if (emptySpMetadataFileName) {
            log.error("emptySpMetadataFileName:{}",emptySpMetadataFileName);
            spMetadataFileName = samlIdpService.getSpNewMetadataFileName(trustRelationship);
            log.error("spMetadataFileName:{}",spMetadataFileName);
            trustRelationship.setSpMetaDataFN(spMetadataFileName);
            /*
             * if (trustRelationship.getDn() == null) { String dn =
             * getDnForTrustRelationship(trustRelationship.getInum());
             * trustRelationship.setDn(dn); addTrustRelationship(trustRelationship); } else
             * { updateTrustRelationship(trustRelationship); }
             */
        }
        InputStream targetStream = file;
        log.error("targetStream:{}, spMetadataFileName:{}",targetStream, spMetadataFileName);
        String result = samlIdpService.saveSpMetadataFile(spMetadataFileName, targetStream);
        if (StringHelper.isNotEmpty(result)) {
            // metadataValidationTimer.queue(result);
        } else {
            log.error("Failed to save SP meta-data file. Please check if you provide correct file");
        }

       

        return false;

    }

    public String getSpNewMetadataFileName(TrustRelationship trustRel) {
        return getSpNewMetadataFileName(trustRel.getInum());
    }

    public String getSpNewMetadataFileName(String inum) {
        String relationshipInum = StringHelper.removePunctuation(inum);
        return String.format(SHIB3_SP_METADATA_FILE_PATTERN, relationshipInum);
    }

    private LocalDocumentStoreConfiguration getDocumentStoreConfiguration() {
        log.error("Getting DocumentStoreConfiguration documentStoreConfiguration:{}", documentStoreConfiguration);

        LocalDocumentStoreConfiguration localConfiguration = documentStoreConfiguration.getLocalConfiguration();
        log.error("Getting localConfiguration:{}", localConfiguration);

        JcaDocumentStoreConfiguration jcaConfiguration = documentStoreConfiguration.getJcaConfiguration();
        log.error("Getting jcaConfiguration:{}", jcaConfiguration);

        WebDavDocumentStoreConfiguration webDavConfiguration = documentStoreConfiguration.getWebDavConfiguration();
        log.error("Getting webDavConfiguration:{}", webDavConfiguration);

        DBDocumentStoreConfiguration dbConfiguration = documentStoreConfiguration.getDbConfiguration();
        log.error("Getting dbConfiguration:{}", dbConfiguration);

        return localConfiguration;
    }

}
