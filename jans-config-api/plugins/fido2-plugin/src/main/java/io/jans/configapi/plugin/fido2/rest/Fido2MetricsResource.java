package io.jans.configapi.plugin.fido2.rest;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2MetricsService;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.util.exception.InvalidAttributeException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

@Path(Constants.REGISTRATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2MetricsResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    Fido2MetricsService fido2MetricsService;

    private class Fido2RegistrationEntryPagedResult extends PagedResult<Fido2RegistrationEntry> {
    };

    /**
     * Retrieve a paged list of FIDO2 registration entries matching the provided search criteria.
     *
     * @param limit          maximum number of results to return
     * @param pattern        search pattern to match entry attributes
     * @param startIndex     1-based index of the first result to return
     * @param sortBy         attribute used to order results
     * @param sortOrder      order direction applied to {@code sortBy}; allowed values are "ascending" and "descending"
     * @param fieldValuePair comma-separated field=value pairs to further filter results (e.g. {@code mail=abc@mail.com,jansStatus=true})
     * @return               a Response containing a Fido2RegistrationEntryPagedResult with the matching entries
     */
    @Operation(summary = "Get a list of Fido2RegistrationEntry.", description = "Get a list of Fido2RegistrationEntry.", operationId = "search-fido2-registration-data", tags = {
            "Fido2 - Registration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2RegistrationEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/search-fido2-registration-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getFido2RegistrationEntry(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Data whose value will be used to order the returned response") @DefaultValue(Constants.JANSID) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "mail=abc@mail.com,jansStatus=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair) {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Fido2RegistrationEntry search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair));
        }

       // SearchRequest searchReq = createSearchRequest(
         //       fido2MetricsService.getBaseDnForFido2RegistrationEntries(null), pattern, sortBy, sortOrder,
        //        startIndex, limit, null, null, fido2RegistrationService.getRecordMaxCount(), fieldValuePair,
         //       Fido2RegistrationEntry.class);
        SearchRequest searchReq = null;
        return Response.ok(this.doSearch(searchReq)).build();
    }
    
    private Fido2RegistrationEntryPagedResult doSearch(SearchRequest searchReq) {
        if (logger.isInfoEnabled()) {
            logger.info("User search params - searchReq:{}", escapeLog(searchReq));
        }

        //PagedResult<Fido2RegistrationEntry> pagedResult = fido2RegistrationService.searchFido2Registration(searchReq);
        PagedResult<Fido2RegistrationEntry> pagedResult =null;
        if (logger.isDebugEnabled()) {
            logger.debug("Fido2RegistrationEntry  - pagedResult:{}", pagedResult);
        }

        Fido2RegistrationEntryPagedResult pagedFido2Registration = new Fido2RegistrationEntryPagedResult();
        if (pagedResult != null) {
            logger.debug("Users fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
            pagedFido2Registration.setStart(pagedResult.getStart());
            pagedFido2Registration.setEntriesCount(pagedResult.getEntriesCount());
            pagedFido2Registration.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            pagedFido2Registration.setEntries(pagedResult.getEntries());
        }

        logger.info("Fido2RegistrationEntry pagedFido2Registration:{}", pagedFido2Registration);
        return pagedFido2Registration;

    }
}