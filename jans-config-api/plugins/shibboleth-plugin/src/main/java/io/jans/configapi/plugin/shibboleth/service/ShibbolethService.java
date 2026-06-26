package io.jans.configapi.plugin.shibboleth.service;

import io.jans.agama.model.Flow;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.configapi.plugin.shibboleth.model.TrustRelationship;
import io.jans.configapi.plugin.shibboleth.model.EntityType;
import io.jans.configapi.plugin.shibboleth.model.Status;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.DefaultBatchOperation;
import io.jans.orm.model.ProcessBatchOperation;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;

import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidAttributeException;
import io.jans.util.exception.InvalidConfigurationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.stream.*;

import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class ShibbolethService {

    private static final String SHIBBOLETH_TR_CONFIG_DN = "inum=%s,ou=trustRelationship,%s";

    @Inject
    private Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    OrganizationService organizationService;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    ShibbolethConfigService shibbolethConfigService;

    @Inject
    ShibbolethDocumentService shibbolethDocumentService;

    @Inject
    ConfigHttpService configHttpService;

    public int getRecordMaxCount() {
        logger.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
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

    public String getDnForTrustRelationship(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=trustRelationship,%s", orgDn);
        }
        return String.format(SHIBBOLETH_TR_CONFIG_DN, inum, orgDn);
    }

    public boolean containsRelationship(String dn) {
        return persistenceEntryManager.contains(dn, TrustRelationship.class);
    }

    public TrustRelationship getTrustRelationshipByDn(String dn) {
        if (StringHelper.isNotEmpty(dn)) {
            try {
                return persistenceEntryManager.find(TrustRelationship.class, dn);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    public List<TrustRelationship> getAllTrustRelationships() {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null);
    }

    public List<TrustRelationship> getAllActiveTrustRelationships() {
        TrustRelationship trustRelationship = new TrustRelationship();
        trustRelationship.setBaseDn(getDnForTrustRelationship(null));
        trustRelationship.setStatus(Status.ACTIVE);
        return persistenceEntryManager.findEntries(trustRelationship);
    }

    public TrustRelationship getTrustRelationshipByInum(String inum) {
        TrustRelationship result = null;
        try {
            result = persistenceEntryManager.find(TrustRelationship.class, getDnForTrustRelationship(inum));
        } catch (Exception ex) {
            logger.error("Failed to load TrustRelationship entry", ex);
        }
        return result;
    }

    public List<TrustRelationship> getAllTrustRelationshipByDisplayName(String name) {
        logger.info(" \n\n New Search TrustRelationship with name:{}", name);

        Filter displayNameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, name);
        logger.error("Search TrustRelationship with displayNameFilter:{}", displayNameFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                displayNameFilter);
    }

    public List<TrustRelationship> searchTrustRelationship(String pattern, int sizeLimit) {

        logger.debug("Search TrustRelationship with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        logger.debug("Search TrustRelationship with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                searchFilter, sizeLimit);
    }

    public PagedResult<TrustRelationship> getTrustRelationship(SearchRequest searchRequest) {
        logger.error("\n\n\n Search TrustRelationship with searchRequest:{}, searchRequest.getFilterAssertionValue:{}", searchRequest, searchRequest.getFilterAssertionValue());

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {
            logger.error("\n\n\n Block 1");
            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                logger.error("\n\n\n Block 2 - assertionValue:{} ", assertionValue);
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

        logger.error("TrustRelationship pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            logger.error("\n\n\n Block 3");
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                logger.trace("TrustRelationship dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        logger.error("TrustRelationship searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                searchFilter, null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public Set<String> getFederationEntityId(String fedId) {
        logger.debug("Search TrustRelationship with fedId:{}", fedId);

        if (StringUtils.isBlank(fedId)) {
            throw new InvalidAttributeException("Federation ID cannot be null");
        }

        TrustRelationship trustRelationship = getTrustRelationshipByInum(fedId);
        if (trustRelationship == null) {
            throw new InvalidAttributeException("No Federation found by inum:{" + fedId + "}");
        }

        if (trustRelationship.getEntityType() != EntityType.AGGREGATE) {
            throw new InvalidAttributeException("{" + fedId + "} is not a Federation");
        }
        return trustRelationship.getEntityIds();
    }

    public List<TrustRelationship> getActiveChildren(TrustRelationship trustRelationship) {

        if (trustRelationship == null) {
            throw new InvalidAttributeException("TrustRelationship is null");
        }

        if (!EntityType.AGGREGATE.equals(trustRelationship.getEntityType())) {
            throw new InvalidAttributeException("TrustRelationship is not an AGGREGATE ");
        }

        // get active children
        Filter searchFilter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansContainerFed", trustRelationship.getJansContainerFedId()),
                Filter.createEqualityFilter("jansStatus", Status.ACTIVE));

        logger.debug("Search TrustRelationship with searchFilter:{}", searchFilter);
        List<TrustRelationship> trustRelationshipList = persistenceEntryManager
                .findEntries(getDnForTrustRelationship(null), TrustRelationship.class, searchFilter);
        logger.debug("Active children for trustRelationship.getJansContainerFedId():{} are trustRelationshipList:{}",
                trustRelationship.getJansContainerFedId(), trustRelationshipList);

        return trustRelationshipList;

    }

    public TrustRelationship addTrustRelationship(TrustRelationship trustRelationship) {
        trustRelationship.setInum(this.generateInumForNewRelationship());
        trustRelationship.setBaseDn(SHIBBOLETH_TR_CONFIG_DN);
        trustRelationship.setDn(this.getDnForTrustRelationship(trustRelationship.getInum()));
        return addTrustRelationship(trustRelationship, null);
    }

    public TrustRelationship addTrustRelationship(TrustRelationship trustRelationship, InputStream file) {
        logger.info("Add new trustRelationship:{}, file:{}", trustRelationship, file);

        setTrustRelationshipDefaultValue(trustRelationship, false);
        persistenceEntryManager.persist(trustRelationship);

        logger.info("After saving new trustRelationship:{}", trustRelationship);
        return getTrustRelationshipByInum(trustRelationship.getInum());
    }

    public TrustRelationship updateTrustRelationship(TrustRelationship trustRelationship) throws IOException {
        return updateTrustRelationship(trustRelationship, null);
    }

    public TrustRelationship updateTrustRelationship(TrustRelationship trustRelationship, InputStream file)
            throws IOException {
        logger.info("Update trustRelationship:{}, file:{}", trustRelationship, file);
        if (trustRelationship == null) {
            return trustRelationship;
        }
        setTrustRelationshipDefaultValue(trustRelationship, true);

        if (file != null && file.available() > 0) {
            saveSpMetaDataFileSourceTypeFile(trustRelationship, file);
        }
        persistenceEntryManager.merge(trustRelationship);
        logger.info("After updating trustRelationship:{}", trustRelationship);
        return getTrustRelationshipByInum(trustRelationship.getInum());

    }

    public void deleteTrustRelationship(String inum) {
        logger.info("TrustRelationship to remove is inum:{}", inum);

        // validate TrustRelationship is valid
        TrustRelationship trustRelationship = this.getTrustRelationshipByInum(inum);
        if (trustRelationship == null) {
            throw new InvalidAttributeException("No TrustRelationship exists with given ID '{" + inum + "}'");
        }

        // get active children
        List<TrustRelationship> trustRelationshipList = this.getActiveChildren(trustRelationship);
        logger.info("Active children for inum:{} are trustRelationshipList:{}", inum, trustRelationshipList);

        // Block if an AGGREGATE has ACTIVE children.
        if (Status.ACTIVE.equals(trustRelationship.getStatus())
                && EntityType.AGGREGATE.equals(trustRelationship.getEntityType())
                && (trustRelationshipList != null && trustRelationshipList.size() > 0)) {
            throw new InvalidAttributeException("TrustRelationship {'" + trustRelationship.getDisplayName()
                    + "}' is 'ACTIVE' and has associated Trust Relationship(s)  depending on it and cannot be deleted. Please disable the federation and try again.");
        }
        // Mark Children and AGGREGATE trustRelationship as INACTIVE for the Worker to
        // unpublish.
        markChildrenInactive(trustRelationship);
        markTrustRelationshipInactive(trustRelationship);
    }

    public boolean urlExists(String urlPath) {
        return this.configHttpService.urlExists(urlPath);
    }

    /* Helper methods */
    private List<TrustRelationship> markTrustRelationshipInactive(List<TrustRelationship> trustRelationshipList) {
        logger.info("TrustRelationships to be marked as INACTIVE are:{}", trustRelationshipList);

        if (trustRelationshipList == null || trustRelationshipList.isEmpty()) {
            throw new InvalidAttributeException("TrustRelationship is null");
        }
        for (TrustRelationship trustRelationship : trustRelationshipList) {
            markTrustRelationshipInactive(trustRelationship);
        }
        return trustRelationshipList;

    }

    private TrustRelationship markTrustRelationshipInactive(TrustRelationship trustRelationship) {
        logger.info("TrustRelationship to be marked as INACTIVE is:{}", trustRelationship);

        if (trustRelationship == null) {
            throw new InvalidAttributeException("TrustRelationship is null");
        }

        trustRelationship.setStatus(Status.INACTIVE);
        persistenceEntryManager.merge(trustRelationship);

        return trustRelationship;

    }

    private List<TrustRelationship> markChildrenInactive(TrustRelationship fedTrustRelationship) {
        if (fedTrustRelationship == null) {
            throw new InvalidAttributeException("TrustRelationship is null");
        }

        BatchOperation<TrustRelationship> updateBatchOperation = new ProcessBatchOperation<TrustRelationship>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<TrustRelationship> objects) {
                int currentProcessedCount = 0;
                for (TrustRelationship trustRelationship : objects) {
                    try {

                        trustRelationship.setStatus(Status.INACTIVE);
                        persistenceEntryManager.merge(trustRelationship);
                        processedCount++;
                        currentProcessedCount++;
                    } catch (EntryPersistenceException ex) {
                        logger.error("Failed to update entry", ex);
                    }
                }
                logger.info("Currnet batch count processed trustRelationship:{} ", currentProcessedCount);

                logger.info("Total processed trustRelationship:{} ", processedCount);
            }
        };

        final Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansContainerFed", fedTrustRelationship.getJansContainerFedId()),
                Filter.createEqualityFilter("jansStatus", Status.ACTIVE));

        List<TrustRelationship> trustRelationshipList = persistenceEntryManager.findEntries(
                getDnForTrustRelationship(null), TrustRelationship.class, filter, SearchScope.SUB, null,
                updateBatchOperation, 0, 0, 100);
        logger.info("Updated trustRelationshipList:{} ", trustRelationshipList);
        return trustRelationshipList;
    }

    private TrustRelationship setTrustRelationshipDefaultValue(TrustRelationship trustRelationship, boolean isUpdate) {
        logger.debug("trustRelationship:{}, isUpdate:{}", trustRelationship, isUpdate);

        if (!isUpdate) {
            trustRelationship.setStatus(Status.DRAFT); // Initial state
        }
        updateVersion(trustRelationship, isUpdate);

        return trustRelationship;
    }

    private TrustRelationship updateVersion(TrustRelationship trustRelationship, boolean isUpdate) {
        logger.debug("Update trustRelationship version - trustRelationship:{}, isUpdate:{}", trustRelationship,
                isUpdate);
        try {
            if (trustRelationship == null) {
                return trustRelationship;
            }

            int version = (trustRelationship.getVersion() == null ? 0 : trustRelationship.getVersion());
            logger.debug(" Current trustRelationship version is:{}", version);

            if (isUpdate) {
                version = version + 1;
            }
            trustRelationship.setVersion(version);
            logger.info("Updated trustRelationship revision to trustRelationship.getJansRevision():{}",
                    trustRelationship.getVersion());

        } catch (Exception ex) {
            logger.error("Exception while updating trustRelationship revision is - ", ex);
            return trustRelationship;
        }
        return trustRelationship;
    }

    private String getSpNewMetadataFileName(TrustRelationship trustRel) {
        return getSpNewMetadataFileName(trustRel.getInum());
    }

    private String getSpNewMetadataFileName(String inum) {
        logger.info("Generate SP Metadata FileName with inum:{}", inum);
        String relationshipInum = StringHelper.removePunctuation(inum);
        logger.info("inum after remove punctuation is:{}", relationshipInum);

        return String.format(shibbolethConfigService.getShibbolethMetadataFilePattern(), relationshipInum);
    }

    private String getSpMetadataFileName(TrustRelationship trustRel) {
        if (trustRel == null) {
            throw new InvalidAttributeException("TrustRelationship data us null!");
        }
        return getMetadataFileName(trustRel.getInum());
    }

    private String getMetadataFileName(String inum) {
        String relationshipInum = StringHelper.removePunctuation(inum);
        return String.format(shibbolethConfigService.getShibbolethMetadataFilePattern(), relationshipInum);
    }

    private String getShibbolethMetadataDir() {
        if (StringUtils.isBlank(shibbolethConfigService.getShibbolethMetadataDir())) {
            throw new InvalidConfigurationException("Failed to return SP metadata file path as undefined!");
        }
        return shibbolethConfigService.getShibbolethMetadataDir() + File.separator;
    }

    private boolean saveSpMetaDataFileSourceTypeFile(TrustRelationship trustRelationship, InputStream file) {

        logger.debug("saveSpMetadataFileSourceTypeFile(). trustRelationship: {} . file: {}", trustRelationship, file);

        final String spMetadataFileName = getSpNewMetadataFileName(trustRelationship);

        InputStream targetStream = file;
        final String metadataFilePath = shibbolethDocumentService.saveMetadataFile(
                shibbolethConfigService.getShibbolethMetadataDir(), spMetadataFileName, Constants.SP_MODULE,
                targetStream);
        logger.debug("targetStream: {}, spMetadataDir: {}, spMetadataFileName: {}", targetStream,
                shibbolethConfigService.getShibbolethMetadataDir(), spMetadataFileName);

        if (StringHelper.isNotEmpty(metadataFilePath)) {
            logger.debug("SP Metadata file ' {} ' saved.", spMetadataFileName);
            return true;
        } else {
            logger.error("Failed to save SP metadata file for TrustRelationship ' {} '", trustRelationship.getInum());
            return false;
        }
    }
}
