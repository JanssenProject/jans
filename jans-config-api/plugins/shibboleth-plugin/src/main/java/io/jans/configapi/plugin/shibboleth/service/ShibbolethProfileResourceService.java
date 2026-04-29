package io.jans.configapi.plugin.shibboleth.service;

import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.shibboleth.model.TrustRelationship;

import io.jans.configapi.plugin.shibboleth.model.*;
import io.jans.configapi.plugin.shibboleth.model.profile.*;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidAttributeException;
import io.jans.util.exception.InvalidConfigurationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

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

@ApplicationScoped
public class ShibbolethProfileResourceService {

    @Inject
    private Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    OrganizationService organizationService;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

  
    public List<SAMLProfile> getAllProfileForTrustRelationships(String inum) {
        List<SAMLProfile> profiles = new ArrayList<>();
		//TO-DO
        return persistenceEntryManager.findEntries(profiles);
    }

}
