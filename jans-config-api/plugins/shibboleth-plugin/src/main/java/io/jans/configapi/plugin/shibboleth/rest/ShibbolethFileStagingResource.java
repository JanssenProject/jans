package io.jans.configapi.plugin.shibboleth.rest;

import java.io.InputStream;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;

import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipQuery;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipSummaryEntry;
import io.jans.configapi.plugin.shibboleth.service.ShibbolethService;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

@Path(Constants.TRUST_RELATIONSHIP_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ShibbolethFileStagingResource extends BaseResource {

    private static final String DATA_NULL_CHK = "RESOURCE_IS_NULL";
    private static final String DATA_NULL_MSG = "`%s` should not be null!";
    private static final String METADATA_FILE = "METADATA_FILE";
    private static final String METADATA_FILE_ERR = "METADATA_FILE_ERR";
    
    
    @Inject
    private Logger logger;

    @Inject
    private ShibbolethService shibbolethService;
    
    
}
