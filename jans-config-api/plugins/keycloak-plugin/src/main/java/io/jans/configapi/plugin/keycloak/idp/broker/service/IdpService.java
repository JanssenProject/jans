/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.core.service.SamlIdpService;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.timer.SpMetadataValidationTimer;
import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpConfigService;


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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;

@ApplicationScoped
public class IdpService {

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
    SpMetadataValidationTimer spMetadataValidationTimer;
    
    @Inject
    SamlIdpService samlIdpService;
    
    @Inject
    IdpConfigService idpConfigService;

    public String getIdentityProviderDn() {
        return idpConfigService.getTrustedIdpDn();
    }

    public String getSpMetadataFilePattern() {
        return idpConfigService.getSpMetadataFilePattern();
    }

    public boolean containsRelationship(String dn) {
        return persistenceEntryManager.contains(dn, IdentityProvider.class);
    }

    public IdentityProvider getRelationshipByDn(String dn) {
        if (StringHelper.isNotEmpty(dn)) {
            try {
                return persistenceEntryManager.find(IdentityProvider.class, dn);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }
        return null;
    }

    public IdentityProvider getIdentityProviderByInum(String inum) {
        IdentityProvider result = null;
        try {
            result = persistenceEntryManager.find(IdentityProvider.class, getDnForIdentityProvider(inum));
        } catch (Exception ex) {
            log.error("Failed to load IdentityProvider entry", ex);
        }
        return result;
    }

    public List<IdentityProvider> getAllIdentityProviders() {
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, null);
    }

    public List<IdentityProvider> getAllActiveIdentityProviders() {
        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setBaseDn(getDnForIdentityProvider(null));
        identityProvider.setStatus(GluuStatus.ACTIVE);

        return persistenceEntryManager.findEntries(identityProvider);
    }

    public List<IdentityProvider> getAllIdentityProviderByInum(String inum) {
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(inum), IdentityProvider.class, null);
    }

    public List<IdentityProvider> getAllIdentityProviderByName(String name) {
        log.info("Search IdentityProvider with name:{}", name);

        String[] targetArray = new String[] { name };
        Filter displayNameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, targetArray);
        log.debug("Search IdentityProvider with displayNameFilter:{}", displayNameFilter);
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class,
                displayNameFilter);
    }

    public IdentityProvider getTrustContainerFederation(IdentityProvider IdentityProvider) {
        return getRelationshipByDn(IdentityProvider.getDn());
    }

    public IdentityProvider getTrustContainerFederation(String dn) {
        return getRelationshipByDn(dn);
    }

    public List<IdentityProvider> getAllIdentityProviders(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, null,
                sizeLimit);
    }

    public IdentityProvider getTrustByUnpunctuatedInum(String unpunctuated) {
        for (IdentityProvider trust : getAllIdentityProviders()) {
            if (StringHelper.removePunctuation(trust.getInum()).equals(unpunctuated)) {
                return trust;
            }
        }
        return null;
    }

    public List<IdentityProvider> searchIdentityProvider(String pattern, int sizeLimit) {

        log.info("Search IdentityProvider with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        log.info("Search IdentityProvider with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class,
                searchFilter, sizeLimit);
    }

    public List<IdentityProvider> getAllIdentityProvider(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, null,
                sizeLimit);
    }

    public PagedResult<Client> getIdentityProvider(SearchRequest searchRequest) {
        log.info("Search IdentityProvider with searchRequest:{}", searchRequest);

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

        log.debug("IdentityProvider pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("IdentityProvider dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.info("IdentityProvider searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForIdentityProvider(null), Client.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public IdentityProvider addIdentityProvider(IdentityProvider IdentityProvider) throws IOException {
        return addIdentityProvider(IdentityProvider, null);
    }

    public IdentityProvider addIdentityProvider(IdentityProvider IdentityProvider, InputStream file)
            throws IOException {
        log.info("Add new IdentityProvider:{}, file:{}", IdentityProvider, file);

        setIdentityProviderDefaultValue(IdentityProvider, false);
        persistenceEntryManager.persist(IdentityProvider);

        if (file != null && file.available() > 0) {
            saveSpMetaDataFileSourceTypeFile(IdentityProvider, file);
        }

        return getIdentityProviderByInum(IdentityProvider.getInum());
    }

    public IdentityProvider updateIdentityProvider(IdentityProvider IdentityProvider) throws IOException {
        return updateIdentityProvider(IdentityProvider, null);
    }

    public IdentityProvider updateIdentityProvider(IdentityProvider IdentityProvider, InputStream file)
            throws IOException {
        setIdentityProviderDefaultValue(IdentityProvider, true);
        persistenceEntryManager.merge(IdentityProvider);

        if (file != null && file.available() > 0) {
            saveSpMetaDataFileSourceTypeFile(IdentityProvider, file);
        }

        return getIdentityProviderByInum(IdentityProvider.getInum());

    }

    public void removeIdentityProvider(IdentityProvider IdentityProvider) {
        persistenceEntryManager.removeRecursively(IdentityProvider.getDn(), IdentityProvider.class);

    }

    private IdentityProvider setIdentityProviderDefaultValue(IdentityProvider IdentityProvider, boolean update) {
        return IdentityProvider;
    }

    public String getDnForIdentityProvider(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=IdentityProviders,%s", orgDn);
        }
        return String.format("inum=%s,ou=IdentityProviders,%s", inum, orgDn);
    }

    public String generateInumForNewRelationship() {
        String newInum = null;
        String newDn = null;
        do {
            newInum = UUID.randomUUID().toString();
            newDn = getDnForIdentityProvider(newInum);
        } while (containsRelationship(newDn));

        return newInum;
    }

    private boolean saveSpMetaDataFileSourceTypeFile(IdentityProvider identityProvider, InputStream file) {
        log.info("identityProvider:{}, file:{}", identityProvider, file);

        String spMetadataFileName = identityProvider.getSpMetaDataFN();
        boolean emptySpMetadataFileName = StringHelper.isEmpty(spMetadataFileName);
        log.debug("emptySpMetadataFileName:{}", emptySpMetadataFileName);
        if ((file == null)) {
            log.trace("File is null");
            if (emptySpMetadataFileName) {
                log.debug("The trust relationship {} has an empty Metadata filename", identityProvider.getInum());
                return false;
            }
            String filePath = samlIdpService.getSpMetadataFilePath(spMetadataFileName);
            log.debug("filePath:{}", filePath);

            if (filePath == null) {
                log.debug("The trust relationship {} has an invalid Metadata file storage path",
                        identityProvider.getInum());
                return false;
            }

            if (samlIdpService.isLocalDocumentStoreType()) {

                File newFile = new File(filePath);
                log.debug("newFile:{}", newFile);

                if (!newFile.exists()) {
                    log.debug(
                            "The trust relationship {} metadata used local storage but the SP metadata file `{}` was not found",
                            identityProvider.getInum(), filePath);
                    return false;
                }
            }
            return true;
        }
        if (emptySpMetadataFileName) {
            log.debug("emptySpMetadataFileName:{}", emptySpMetadataFileName);
            spMetadataFileName = samlIdpService.getSpNewMetadataFileName(identityProvider);
            log.debug("spMetadataFileName:{}", spMetadataFileName);
            identityProvider.setSpMetaDataFN(spMetadataFileName);

        }
        InputStream targetStream = file;
        log.debug("targetStream:{}, spMetadataFileName:{}", targetStream, spMetadataFileName);

        String result = samlIdpService.saveSpMetadataFile(spMetadataFileName, targetStream);
        log.debug("targetStream:{}, spMetadataFileName:{}", targetStream, spMetadataFileName);
        if (StringHelper.isNotEmpty(result)) {
            spMetadataValidationTimer.queue(result);
            //process files in temp that were not processed earlier
            processUnprocessedMetadataFiles();
        } else {
            log.error("Failed to save SP meta-data file. Please check if you provide correct file");
        }

        return false;

    }

    public void processUnprocessedMetadataFiles() {
        log.debug("processing unprocessed metadata files ");
        String directory = samlIdpService.getIdpMetadataTempDir();
        log.debug("directory:{}, Files.exists(Paths.get(directory):{}", directory, Files.exists(Paths.get(directory)));

        if (Files.exists(Paths.get(directory))) {
            log.debug("directory:{} does exists)", directory);
            File folder = new File(directory);
            File[] files = folder.listFiles();
            log.debug("files:{}", files);
            if (files != null && files.length > 0) {

                for (File file : files) {
                    log.debug("file:{}, file.getName():{}", file, file.getName());
                    spMetadataValidationTimer.queue(file.getName());
                }
            }

        }
    }

}
