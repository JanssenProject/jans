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
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.timer.IdpMetadataValidationTimer;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class IdentityProviderService {

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
    IdpConfigService idpConfigService;

    @Inject
    SamlService samlService;

    @Inject
    IdpMetadataValidationTimer idpMetadataValidationTimer;

    public String getIdentityProviderDn() {
        return idpConfigService.getTrustedIdpDn();
    }

    public String getIdpMetadataFilePattern() {
        return idpConfigService.getIdpMetadataFilePattern();
    }

    public boolean containsIdentityProvider(String dn) {
        return persistenceEntryManager.contains(dn, IdentityProvider.class);
    }

    public IdentityProvider getIdentityProviderByDn(String dn) {
        if (StringHelper.isNotEmpty(dn)) {
            try {
                return persistenceEntryManager.find(IdentityProvider.class, dn);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }
        return null;
    }

    public List<IdentityProvider> getAllIdentityProviders() {
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, null);
    }

    public List<IdentityProvider> getAllIdentityProviders(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, null,
                sizeLimit);
    }

    public IdentityProvider getIdentityProviderByUnpunctuatedInum(String unpunctuated) {
        for (IdentityProvider idp : getAllIdentityProviders()) {
            if (StringHelper.removePunctuation(idp.getInum()).equals(unpunctuated)) {
                return idp;
            }
        }
        return null;
    }

    public List<IdentityProvider> getAllActiveIdentityProviders() {
        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setBaseDn(getDnForIdentityProvider(null));
        identityProvider.setStatus(GluuStatus.ACTIVE);

        return persistenceEntryManager.findEntries(identityProvider);
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

    public List<IdentityProvider> getIdentityProviderByName(String name) {
        log.error("Search IdentityProvider with name:{}", name);

        Filter nameFilter = Filter.createEqualityFilter("NAME", name);
        log.error("Search IdentityProvider with displayNameFilter:{}", nameFilter);
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class,
                nameFilter);
    }

    public IdentityProvider getIdentityProvider(IdentityProvider identityProvider) {
        return getIdentityProviderByDn(identityProvider.getDn());
    }

    public IdentityProvider getIdentityProvider(String dn) {
        return getIdentityProviderByDn(dn);
    }

    public List<IdentityProvider> searchIdentityProvider(String pattern, int sizeLimit) {

        log.error("Search IdentityProvider with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter nameFilter = Filter.createSubstringFilter("NAME", null, targetArray,
                null);
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(nameFilter, displayNameFilter, descriptionFilter, inumFilter);

        log.error("Search IdentityProvider with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, searchFilter,
                sizeLimit);
    }

    public List<IdentityProvider> getAllIdentityProvider(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, null,
                sizeLimit);
    }

    public PagedResult<IdentityProvider> getIdentityProvider(SearchRequest searchRequest) {
        log.error("Search IdentityProvider with searchRequest:{}", searchRequest);

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

        log.error("IdentityProvider pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.error("IdentityProvider dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.error("IdentityProvider searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForIdentityProvider(null), IdentityProvider.class,
                searchFilter, null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public IdentityProvider addSamlIdentityProvider(IdentityProvider identityProvider, InputStream file)
            throws IOException {
        log.error("Add new identityProvider:{}, file:{}", identityProvider, file);

        String inum = generateInumForIdentityProvider();
        identityProvider.setInum(inum);
        identityProvider.setDn(getDnForIdentityProvider(inum));

        if (file != null) {
            log.error("Save IDP metadatfile on server");
            saveIdpMetaDataFileSourceTypeFile(identityProvider, file);
        }

        // Set default Value for SAML IDP
        setSamlIdentityProviderDefaultValue(identityProvider, false);
        persistenceEntryManager.persist(identityProvider);

        return getIdentityProviderByInum(identityProvider.getInum());
    }

    public IdentityProvider updateIdentityProvider(IdentityProvider identityProvider) throws IOException {
        return updateIdentityProvider(identityProvider, null);
    }

    public IdentityProvider updateIdentityProvider(IdentityProvider identityProvider, InputStream file)
            throws IOException {

        if(identityProvider==null) {
            return identityProvider;
        }
        if (file != null && file.available() > 0) {
            saveIdpMetaDataFileSourceTypeFile(identityProvider, file);
        }

        // Set default Value for SAML IDP
        setSamlIdentityProviderDefaultValue(identityProvider, true);
        persistenceEntryManager.merge(identityProvider);

        return getIdentityProviderByInum(identityProvider.getInum());

    }

    public void removeIdentityProvider(IdentityProvider identityProvider) {
        persistenceEntryManager.removeRecursively(identityProvider.getDn(), IdentityProvider.class);

    }

    public String getDnForIdentityProvider(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=trusted-idp,%s", orgDn);
        }
        return String.format("inum=%s,ou=trusted-idp,%s", inum, orgDn);
    }

    public String generateInumForIdentityProvider() {
        String newInum = null;
        String newDn = null;
        do {
            newInum = UUID.randomUUID().toString();
            newDn = getDnForIdentityProvider(newInum);
        } while (this.containsIdentityProvider(newDn));

        return newInum;
    }

    public String generateInumForNewIdentityProvider() {
        String newInum = null;
        String newDn = null;
        int trycount = 0;
        do {
            if (trycount < InumService.MAX_IDGEN_TRY_COUNT) {
                newInum = inumService.generateId("idp");
                trycount++;
            } else {
                newInum = inumService.generateDefaultId();
            }
            newDn = getDnForIdentityProvider(newInum);
        } while (persistenceEntryManager.contains(newDn, Client.class));
        return newInum;
    }

    private IdentityProvider setSamlIdentityProviderDefaultValue(IdentityProvider identityProvider, boolean update) {
        log.error("setting default value for identityProvider:{}, update:{}", identityProvider, update);
        if (identityProvider == null) {
            return identityProvider;
        }
        if (!update) {
            identityProvider.setProviderId(Constants.SAML);
        }
        return identityProvider;
    }

    private boolean saveIdpMetaDataFileSourceTypeFile(IdentityProvider identityProvider, InputStream file) {
        log.error("Saving file identityProvider:{}, file:{}", identityProvider, file);

        String idpMetaDataFN = identityProvider.getIdpMetaDataFN();
        log.error("idpMetaDataFN:{}", idpMetaDataFN);

        boolean emptyidpMetaDataFN = StringHelper.isEmpty(idpMetaDataFN);
        log.error("emptyidpMetaDataFN:{}", emptyidpMetaDataFN);
        if ((file == null)) {
            log.error("File is null");
            if (emptyidpMetaDataFN) {
                log.error("The trust relationship {} has an empty Metadata filename", identityProvider.getInum());
                return false;
            }
            String filePath = idpConfigService.getIdpMetadataTempDirFilePath(idpMetaDataFN);
            log.error("filePath:{}", filePath);

            if (filePath == null) {
                log.error("The trust relationship {} has an invalid Metadata file storage path",
                        identityProvider.getInum());
                return false;
            }

            if (samlService.isLocalDocumentStoreType()) {

                File newFile = new File(filePath);
                log.error("newFile:{}", newFile);

                if (!newFile.exists()) {
                    log.error(
                            "The trust relationship {} metadata used local storage but the IDP metadata file `{}` was not found",
                            identityProvider.getInum(), filePath);
                    return false;
                }
            }
            return true;
        }
        if (emptyidpMetaDataFN) {
            log.error("File name is blank emptyidpMetaDataFN:{}", emptyidpMetaDataFN);
            idpMetaDataFN = getIdpNewMetadataFileName(identityProvider);
            log.error("Final idpMetaDataFN:{}", idpMetaDataFN);
            identityProvider.setIdpMetaDataFN(idpMetaDataFN);

        }
        InputStream targetStream = file;
        log.error("targetStream:{}, idpMetaDataFN:{}", targetStream, idpMetaDataFN);

        String result = samlService.saveMetadataFile(Constants.IDP_MODULE, idpConfigService.getIdpMetadataTempDir(),
                idpMetaDataFN, targetStream);
        log.error("targetStream:{}, idpMetaDataFN:{}", targetStream, idpMetaDataFN);
        if (StringHelper.isNotEmpty(result)) {
            idpMetadataValidationTimer.queue(result);
            // process files in temp that were not processed earlier
            processUnprocessedIdpMetadataFiles();
        } else {
            log.error("Failed to save IDP meta-data file. Please check if you provide correct file");
        }

        return false;

    }

    private String getIdpNewMetadataFileName(IdentityProvider identityProvider) {
        String idpMetaDataFN = null;
        if (identityProvider == null) {
            return idpMetaDataFN;
        }
        log.error("idpConfigService.getIdpMetadataFileName(identityProvider.getInum()):{}",
                idpConfigService.getIdpMetadataFileName(identityProvider.getInum()));
        return idpConfigService.getIdpMetadataFileName(identityProvider.getInum());
    }

    public void processUnprocessedIdpMetadataFiles() {
        log.error("processing unprocessed IDP metadata files ");
        String directory = idpConfigService.getIdpMetadataTempDir();
        log.error("directory:{}, Files.exists(Paths.get(directory):{}", directory, Files.exists(Paths.get(directory)));

        if (Files.exists(Paths.get(directory))) {
            log.error("directory:{} does exists)", directory);
            File folder = new File(directory);
            File[] files = folder.listFiles();
            log.error("files:{}", files);
            if (files != null && files.length > 0) {

                for (File file : files) {
                    log.error("file:{}, file.getName():{}", file, file.getName());
                    idpMetadataValidationTimer.queue(file.getName());
                }
            }

        }
    }

}
