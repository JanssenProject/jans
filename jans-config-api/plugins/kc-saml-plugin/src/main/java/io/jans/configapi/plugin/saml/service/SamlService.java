/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.as.common.service.common.InumService;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.util.exception.InvalidConfigurationException;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.plugin.saml.model.TrustRelationship;

import io.jans.model.GluuStatus;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class SamlService {

    @Inject
    Logger log;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    OrganizationService organizationService;

    @Inject
    private InumService inumService;

    @Inject
    SamlConfigService samlConfigService;

    @Inject
    SamlIdpService samlIdpService;

    public String getTrustRelationshipDn() {
        return samlConfigService.getTrustRelationshipDn();
    }

    public String getSpMetadataFilePattern() {
        return samlConfigService.getSpMetadataFilePattern();
    }

    public boolean containsRelationship(String dn) {
        return persistenceEntryManager.contains(dn, TrustRelationship.class);
    }

    public TrustRelationship getRelationshipByDn(String dn) {
        if (StringHelper.isNotEmpty(dn)) {
            try {
                return persistenceEntryManager.find(TrustRelationship.class, dn);
            } catch (Exception e) {
                log.error(e.getMessage());
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

    public List<TrustRelationship> getAllTrustRelationships() {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null);
    }

    public List<TrustRelationship> getAllActiveTrustRelationships() {
        TrustRelationship trustRelationship = new TrustRelationship();
        trustRelationship.setBaseDn(getDnForTrustRelationship(null));
        trustRelationship.setStatus(GluuStatus.ACTIVE);

        return persistenceEntryManager.findEntries(trustRelationship);
    }

    public List<TrustRelationship> getAllTrustRelationshipByInum(String inum) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(inum), TrustRelationship.class, null);
    }

    public List<TrustRelationship> getAllTrustRelationshipByDisplayName(String name) {
        log.info("Search TrustRelationship with name:{}", name);

        String[] targetArray = new String[] { name };
        Filter displayNameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, targetArray);
        log.debug("Search TrustRelationship with displayNameFilter:{}", displayNameFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                displayNameFilter);
    }
    
    public List<TrustRelationship> getAllTrustRelationshipByName(String name) {
        log.info("Search TrustRelationship with name:{}", name);
        Filter nameFilter = Filter.createEqualityFilter("name", name);
        log.debug("Search TrustRelationship with nameFilter:{}", nameFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, nameFilter);
    }
    
    public TrustRelationship getTrustContainerFederation(TrustRelationship trustRelationship) {
        return getRelationshipByDn(trustRelationship.getDn());
    }

    public TrustRelationship getTrustContainerFederation(String dn) {
        return getRelationshipByDn(dn);
    }

    public List<TrustRelationship> getAllTrustRelationships(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null,
                sizeLimit);
    }

    public TrustRelationship getTrustByUnpunctuatedInum(String unpunctuated) {
        for (TrustRelationship trust : getAllTrustRelationships()) {
            if (StringHelper.removePunctuation(trust.getInum()).equals(unpunctuated)) {
                return trust;
            }
        }
        return null;
    }

    public List<TrustRelationship> searchTrustRelationship(String pattern, int sizeLimit) {

        log.info("Search TrustRelationship with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        log.info("Search TrustRelationship with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                searchFilter, sizeLimit);
    }

    public List<TrustRelationship> getAllTrustRelationship(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null,
                sizeLimit);
    }

    public PagedResult<TrustRelationship> getTrustRelationship(SearchRequest searchRequest) {
        log.info("Search TrustRelationship with searchRequest:{}", searchRequest);

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

        log.debug("TrustRelationship pattern searchFilter:{}", searchFilter);
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

        log.info("TrustRelationship searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForTrustRelationship(null), TrustRelationship.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public TrustRelationship addTrustRelationship(TrustRelationship trustRelationship) throws IOException {
        return addTrustRelationship(trustRelationship, null);
    }

    public TrustRelationship addTrustRelationship(TrustRelationship trustRelationship, InputStream file)
            throws IOException {
        log.info("Add new trustRelationship:{}, file:{}", trustRelationship, file);

        setTrustRelationshipDefaultValue(trustRelationship, false);
        persistenceEntryManager.persist(trustRelationship);

        if (file != null && file.available() > 0) {
            saveSpMetaDataFileSourceTypeFile(trustRelationship, file);
        }else {
            trustRelationship.setSpMetaDataFN(null);
        }
        
        persistenceEntryManager.merge(trustRelationship);
        log.info("After saving new trustRelationship:{}", trustRelationship);
        return getTrustRelationshipByInum(trustRelationship.getInum());
    }

    public TrustRelationship updateTrustRelationship(TrustRelationship trustRelationship) throws IOException {
        return updateTrustRelationship(trustRelationship, null);
    }

    public TrustRelationship updateTrustRelationship(TrustRelationship trustRelationship, InputStream file)
            throws IOException {
        log.info("Update trustRelationship:{}, file:{}", trustRelationship, file);
        setTrustRelationshipDefaultValue(trustRelationship, true);
        
        if (file != null && file.available() > 0) {
            saveSpMetaDataFileSourceTypeFile(trustRelationship, file);
        }
        
        persistenceEntryManager.merge(trustRelationship);
        log.info("After updating trustRelationship:{}", trustRelationship);
        return getTrustRelationshipByInum(trustRelationship.getInum());

    }

    public void removeTrustRelationship(TrustRelationship trustRelationship) {
        persistenceEntryManager.removeRecursively(trustRelationship.getDn(), TrustRelationship.class);

    }

    private TrustRelationship setTrustRelationshipDefaultValue(TrustRelationship trustRelationship, boolean update) {
        log.debug("trustRelationship:{}, update:{}",trustRelationship, update);
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

    private boolean saveSpMetaDataFileSourceTypeFile(TrustRelationship trustRelationship, InputStream file) {

        log.debug("saveSpMetadataFileSourceTypeFile(). trustRelationship: {} . file: {}",trustRelationship,file);
    
        final String spMetadataFileName = getSpNewMetadataFileName(trustRelationship);
        trustRelationship.setSpMetaDataFN(spMetadataFileName);
        InputStream targetStream = file;
        final String metadataFilePath  = samlIdpService.saveMetadataFile(
                samlConfigService.getSpMetadataDir(),spMetadataFileName,Constants.SP_MODULE,targetStream);
        log.debug("targetStream: {}, spMetadataDir: {}, spMetadataFileName: {}",targetStream,samlConfigService.getSpMetadataDir(),spMetadataFileName);
        if(StringHelper.isNotEmpty(metadataFilePath)) {
            trustRelationship.setSpMetaDataFN(metadataFilePath);
            log.debug("SP Metadata file ' {} ' saved.",spMetadataFileName);
            return true;
        }else {
            log.error("Failed to save SP metadata file for TrustRelationship ' {} '",trustRelationship.getInum());
            return false;
        }
    }
    
    public String getSpMetadataFilePath(String spMetaDataFN) {
        if (StringUtils.isBlank(getSpMetadataDir())) {
            throw new InvalidConfigurationException("Failed to return SP metadata file path as undefined!");
        }
        return getSpMetadataDir() + spMetaDataFN;
    }
    
    public String getSpMetadataDir() {
        if (StringUtils.isBlank(samlConfigService.getSpMetadataDir())) {
            throw new InvalidConfigurationException("Failed to return SP metadata file path as undefined!");
        }
        return samlConfigService.getSpMetadataDir() + File.separator;
    }
    
    public String getSpNewMetadataFileName(TrustRelationship trustRel) {
        return getSpNewMetadataFileName(trustRel.getInum());
    }

    public String getSpNewMetadataFileName(String inum) {
        log.info("Generate SP Metadata FileName with inum:{}",inum);
        String relationshipInum = StringHelper.removePunctuation(inum);
        log.info("inum after remove punctuation is:{}",relationshipInum);
        return String.format(samlConfigService.getSpMetadataFilePattern(), relationshipInum);
    }

    
    public InputStream getTrustRelationshipMetadataFile(TrustRelationship trustrelationship) {

        log.debug("Get trustrelationship metadata file");
        return samlIdpService.getFileFromDocumentStore(trustrelationship.getSpMetaDataFN());
    }

}
