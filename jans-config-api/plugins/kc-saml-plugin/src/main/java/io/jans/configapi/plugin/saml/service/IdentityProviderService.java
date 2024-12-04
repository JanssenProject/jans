/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

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

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.saml.model.IdentityProvider;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.model.GluuStatus;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
    SamlConfigService samlConfigService;

    @Inject
    SamlIdpService samlIdpService;

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
        log.info("Search IdentityProvider with name:{}", name);

        Filter nameFilter = Filter.createEqualityFilter("NAME", name);
        log.debug("Search IdentityProvider with displayNameFilter:{}", nameFilter);
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, nameFilter);
    }

    public IdentityProvider getIdentityProvider(IdentityProvider identityProvider) {
        return getIdentityProviderByDn(identityProvider.getDn());
    }

    public IdentityProvider getIdentityProvider(String dn) {
        return getIdentityProviderByDn(dn);
    }

    public List<IdentityProvider> searchIdentityProvider(String pattern, int sizeLimit) {

        log.info("Search IdentityProvider with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter nameFilter = Filter.createSubstringFilter("NAME", null, targetArray, null);
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(nameFilter, displayNameFilter, descriptionFilter, inumFilter);

        log.trace("Search IdentityProvider with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, searchFilter,
                sizeLimit);
    }

    public List<IdentityProvider> getAllIdentityProvider(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForIdentityProvider(null), IdentityProvider.class, null,
                sizeLimit);
    }

    public PagedResult<IdentityProvider> getIdentityProvider(SearchRequest searchRequest) {
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
                log.debug("IdentityProvider dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.debug("IdentityProvider searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForIdentityProvider(null), IdentityProvider.class,
                searchFilter, null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public IdentityProvider addSamlIdentityProvider(IdentityProvider identityProvider, InputStream file) throws IOException{
        log.info("Add new identityProvider:{}, file:{}", identityProvider, file);

        if (file != null && file.available() > 0) {
            log.info("Save IDP metadatfile on server");
            saveIdpMetaDataFileSourceTypeFile(identityProvider, file);
            log.info("After saving IDP metadatfile on server - identityProvider:{}", identityProvider);
        }else {
            identityProvider.setIdpMetaDataFN(null);
        }
        
        log.info("Persist IDP in DB identityProvider:{}", identityProvider);
        persistenceEntryManager.persist(identityProvider);
        log.info("After Persisting IDP");
        return getIdentityProviderByInum(identityProvider.getInum());
    }

    public IdentityProvider updateIdentityProvider(IdentityProvider identityProvider) throws IOException {
        return updateIdentityProvider(identityProvider, null);
    }

    public IdentityProvider updateIdentityProvider(IdentityProvider identityProvider, InputStream file)
            throws IOException {

        if (identityProvider == null) {
            return identityProvider;
        }
        if (file != null && file.available() > 0) {
            saveIdpMetaDataFileSourceTypeFile(identityProvider, file);
        }else {
            identityProvider.setIdpMetaDataFN(null);
        }

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

    private boolean saveIdpMetaDataFileSourceTypeFile(IdentityProvider identityProvider, InputStream file) {
        
        log.debug("Saving idp {} metadata file : {}",identityProvider.getInum(),file);

        if(identityProvider == null || file == null) {
            return false;
        }

        final String idpMetaDataFN = getIdpNewMetadataFileName(identityProvider);
        identityProvider.setIdpMetaDataFN(idpMetaDataFN);
        identityProvider.setIdpMetaDataLocation(getIdpMetadataTempDirFilePath());

        final InputStream targetStream = file;
        log.debug("targetStream: {}, idpMetaDataFN: {}", targetStream,idpMetaDataFN);

        final String result = samlIdpService.saveMetadataFile(getIdpMetadataTempDirFilePath(), idpMetaDataFN,
            Constants.IDP_MODULE, targetStream);
        log.debug("targetStream:{}, idpMetaDataFN:{}, result:{}", targetStream, idpMetaDataFN, result);

        if(StringHelper.isNotEmpty(result)) {
            log.info("IDP metadata file saved inum: {} , filename: {}",identityProvider.getInum(),idpMetaDataFN);
            return true;
        }else {
            log.error("Failed to save IDP metadata file for IdentityProvider {}. filename: {}",identityProvider.getInum(),idpMetaDataFN);
            return false;
        }
    }

    private String getIdpNewMetadataFileName(IdentityProvider identityProvider) {
        String idpMetaDataFN = null;
        if (identityProvider == null) {
            return idpMetaDataFN;
        }
        log.info("idpConfigService.getIdpMetadataFileName(identityProvider.getInum()):{}",
                getIdpMetadataFileName(identityProvider.getInum()));
        return getIdpMetadataFileName(identityProvider.getInum());
    }

    private String getIdpMetadataTempDirFilePath(String idpMetaDataFN) {
        log.debug("idpMetaDataFN:{}", idpMetaDataFN);
        if (StringUtils.isBlank(getIdpMetadataTempDirFilePath())) {
            throw new InvalidConfigurationException("Failed to return IDP metadata file path as undefined!");
        }

        return getIdpMetadataTempDirFilePath() + idpMetaDataFN;
    }

    private String getIdpMetadataFileName(String inum) {
        String id = StringHelper.removePunctuation(inum);
        return String.format(samlConfigService.getIdpMetadataFilePattern(), id);
    }

    private String getIdpMetadataTempDirFilePath() {
        return samlConfigService.getIdpMetadataTempDir();
    }

}
