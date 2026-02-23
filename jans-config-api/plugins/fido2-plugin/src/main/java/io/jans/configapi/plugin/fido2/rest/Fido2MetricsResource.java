package io.jans.configapi.plugin.fido2.rest;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.fido2.model.metric.Fido2MetricsEntry;
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

    private class Fido2MetricsEntryPagedResult extends PagedResult<Fido2MetricsEntry> {
    };

    /**
     * Retrieve a paged list of FIDO2 metrics entries matching the provided search criteria.
     *
     * @param limit          maximum number of results to return
     * @param pattern        search pattern to match entry attributes
     * @param startIndex     1-based index of the first result to return
     * @param sortBy         attribute used to order results
     * @param sortOrder      order direction applied to {@code sortBy}; allowed values are "ascending" and "descending"
     * @param fieldValuePair comma-separated field=value pairs to further filter results (e.g. {@code mail=abc@mail.com,jansStatus=true})
     * @return               a Response containing a Fido2MetricsEntryPagedResult with the matching entries
     */
    @Operation(summary = "Get a list of Fido2MetricsEntry.", description = "Get a list of Fido2MetricsEntry.", operationId = "search-metrics-data", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2MetricsEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/search-fido2-registration-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
   // @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }, groupScopes = {
      //      Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                   // ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getFido2MetricsEntry(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Start date/time for the log entries report. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "start_date") String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "end_date") String endDate){

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Fido2MetricsEntry search param - limit:{}, startDate:{}, endDate:{}",
                    escapeLog(limit), escapeLog(startDate), escapeLog(endDate));
        }

       // SearchRequest searchReq = createSearchRequest(
         //       fido2MetricsService.getBaseDnForFido2RegistrationEntries(null), pattern, sortBy, sortOrder,
        //        startIndex, limit, null, null, fido2RegistrationService.getRecordMaxCount(), fieldValuePair,
         //       Fido2RegistrationEntry.class);
        SearchRequest searchReq = null;
        return Response.ok(this.doSearch(searchReq)).build();
    }
    
    private Fido2MetricsEntryPagedResult doSearch(SearchRequest searchReq) {
        if (logger.isInfoEnabled()) {
            logger.info("Fido2 Metrics search params - searchReq:{}", escapeLog(searchReq));
        }

        //PagedResult<Fido2RegistrationEntry> pagedResult = fido2RegistrationService.searchFido2Registration(searchReq);
        PagedResult<Fido2MetricsEntry> pagedResult =null;
        if (logger.isDebugEnabled()) {
            logger.debug("Fido2RegistrationEntry  - pagedResult:{}", pagedResult);
        }

        Fido2MetricsEntryPagedResult pagedFido2MetricsEntry = new Fido2MetricsEntryPagedResult();
        if (pagedResult != null) {
            logger.debug("Users fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
            pagedFido2MetricsEntry.setStart(pagedResult.getStart());
            pagedFido2MetricsEntry.setEntriesCount(pagedResult.getEntriesCount());
            pagedFido2MetricsEntry.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            pagedFido2MetricsEntry.setEntries(pagedResult.getEntries());
        }

        logger.info("Fido2MetricsEntry pagedFido2MetricsEntry:{}", pagedFido2MetricsEntry);
        return pagedFido2MetricsEntry;

    }
}