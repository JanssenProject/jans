package io.jans.configapi.plugin.shibboleth.service;

import io.jans.agama.model.Flow;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiConstants;
import io.jans.kernel.DomainError;
import io.jans.kernel.Result;
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
import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.error.TrustRelationshipNotFound;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipEntry;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipEntryMapper;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipQuery;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipRepositoryImpl;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipSummaries;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipSummaryEntry;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipSummaryPage;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.dto.mapper.config.TrustRelationshipMapper;
import io.jans.kernel.Result;

import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidAttributeException;
import io.jans.util.exception.InvalidConfigurationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

import com.github.fge.jackson.JacksonUtils;
import com.google.common.base.Preconditions;

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
    private static final String BASE_DN = "ou=trustRelationships,o=jans";

    @Inject
    private Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    OrganizationService organizationService;

    @Inject
    ConfigHttpService configHttpService;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    public int getRecordMaxCount() {
        logger.trace("MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }

    private final TrustRelationshipRepositoryImpl trustRelationshipRepository = new TrustRelationshipRepositoryImpl(
            persistenceEntryManager, BASE_DN);

    public PagedResult<TrustRelationshipSummaryEntry> getTrustRelationship(TrustRelationshipQuery query) {
        PagedResult<TrustRelationshipSummaryEntry> pagedResult = new PagedResult<>();
        Result<TrustRelationshipSummaryPage> result = trustRelationshipRepository.list(query);
        pagedResult = readShibbolethObject(result, pagedResult.getClass(),  "Search TrustRelationship");
        logger.error("Search TrustRelationship pagedResult:{}", pagedResult);
        return pagedResult;
    }

    public TrustRelationship findById(Id id) {
        Result<TrustRelationship> result = trustRelationshipRepository.findById(id);
        TrustRelationship trustRelationship = readShibbolethObject(result, TrustRelationship.class, "Fetch TrustRelationshipRequest by id:{"+id+"}");
        logger.error("Fetch TrustRelationship by id:{}, trustRelationship:{}", id, trustRelationship);
        return trustRelationship;
    }

    public TrustRelationship addTrustRelationship(CreateTrustRelationshipRequest request) {
        logger.error(" Request to create TrustRelationship request:{}", request);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);
        TrustRelationship trustRelationship = readShibbolethObject(result, TrustRelationship.class, "CreateTrustRelationshipRequest");
        result = trustRelationshipRepository.save(trustRelationship);
        trustRelationship = readShibbolethObject(result, TrustRelationship.class, "Created TrustRelationship");
     
        logger.error("trustRelationship :{}", trustRelationship);
        return trustRelationship;

    }

    public Result<Void> delete(Id id) {
        return trustRelationshipRepository.delete(id);
    }

    private <T, U> U readShibbolethObject(Result<T> result, Class<U> returnObject, String message) {
        logger.error("ShibbolethObject result:{}", result);

        if (result == null) {
            throw new WebApplicationException(message, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        if (result.isFailure()) {
            DomainError domainError = result.getError();
            logger.error("Error while creating TrustRelationship is domainError:{}", domainError);

            throw new WebApplicationException(domainError.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        if(returnObject == null || result.getValue() == null) {
            return null;
        }
        
        if(result.getValue()!=null  && result.getValue().getClass().isInstance(returnObject)) {
                return returnObject.cast(result.getValue()); // Zero warnings, 100% type-safe
            }
            throw new ClassCastException("Cannot cast " + result.getValue().getClass().getName() + " to " + returnObject.getName());
     
    }

}
