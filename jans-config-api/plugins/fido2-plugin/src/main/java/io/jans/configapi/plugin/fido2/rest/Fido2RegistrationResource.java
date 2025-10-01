package io.jans.configapi.plugin.fido2.rest;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2RegistrationService;
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
public class Fido2RegistrationResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    Fido2RegistrationService fido2RegistrationService;

    private class Fido2RegistrationEntryPagedResult extends PagedResult<Fido2RegistrationEntry> {
    };

    @Operation(summary = "Get a list of Fido2RegistrationEntry.", description = "Get a list of Fido2RegistrationEntry.", operationId = "search-fido2-registration-data", tags = {
            "Fido2 - Registration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.FIDO2_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2RegistrationEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/search-fido2-registration-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
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

        SearchRequest searchReq = createSearchRequest(
                fido2RegistrationService.getBaseDnForFido2RegistrationEntries(null), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, fido2RegistrationService.getRecordMaxCount(), fieldValuePair,
                Fido2RegistrationEntry.class);

        return Response.ok(this.doSearch(searchReq)).build();
    }

    @Operation(summary = "Fetch Fido2RegistrationEntry by Id.", description = "Fetch Fido2RegistrationEntry by Id.", operationId = "get-fido2-by-id", tags = {
            "Fido2 - Registration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.FIDO2_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2RegistrationEntry.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/get-fido2-by-id.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.JANSID_PATH + ApiConstants.JANSID_PATH_PARAM)
    public Response getFido2RegistrationEntryById(
            @Parameter(description = "Fido2Registration identifier") @PathParam(ApiConstants.JANSID) @NotNull String jansId) {
        if (logger.isInfoEnabled()) {
            logger.info("Fido2RegistrationEntry search by id:{}", escapeLog(jansId));
        }
        Fido2RegistrationEntry fido2RegistrationEntry = this.fido2RegistrationService
                .getFido2RegistrationEntryById(jansId);

        return Response.ok(fido2RegistrationEntry).build();
    }

    @Operation(summary = "Get details of connected FIDO2 devices registered to user", description = "Get details of connected FIDO2 devices registered to user", operationId = "get-registration-entries-fido2", tags = {
            "Fido2 - Registration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.FIDO2_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2RegistrationEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/search-fido2-registration-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.ENTRIES + ApiConstants.USERNAME_PATH)
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_READ_ACCESS })
    public Response findAllRegisteredByUsername(
            @Parameter(description = "User name") @PathParam("username") @NotNull String username) {
        logger.info("FIDO2 registration entries by username.");
        checkNotNull(username, "User name");
        PagedResult<Fido2RegistrationEntry> entries = fido2RegistrationService.getFido2RegisteredByUsername(username);
        logger.info("FIDO2 registration entries by entries:{}", entries);
        return Response.ok(entries).build();
    }

    @Operation(summary = "Delete Fido2 Device Data based on device UID", description = "Delete Fido2 Device Data based on device UID", operationId = "delete-fido2-data", tags = {
            "Fido2 - Registration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.FIDO2_CONFIG_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @DELETE
    @Path(ApiConstants.JANSID_PATH + ApiConstants.JANSID_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_DELETE_ACCESS })
    public Response deleteFido2Data(
            @Parameter(description = "Fido2Registration Unique identifier.") @PathParam(ApiConstants.JANSID) @NotNull String jansId) {
        if (logger.isInfoEnabled()) {
            logger.info("Request to delete Fido2 device identified by jansId:{}", escapeLog(jansId));
        }
        checkResourceNotNull(jansId, ApiConstants.JANSID);
        try {
            // delete device
            fido2RegistrationService.removeFido2RegistrationEntry(jansId);

            if (logger.isInfoEnabled()) {
                logger.info("Successfully deleted Fido2 with id:{}", escapeLog(jansId));
            }

        } catch (InvalidAttributeException iae) {
            throwNotFoundException("Not Found", iae.getMessage());
        } catch (Exception ex) {
            throwInternalServerException(ex);
        }
        return Response.noContent().build();
    }

    private Fido2RegistrationEntryPagedResult doSearch(SearchRequest searchReq) {
        if (logger.isInfoEnabled()) {
            logger.info("User search params - searchReq:{}", escapeLog(searchReq));
        }

        PagedResult<Fido2RegistrationEntry> pagedResult = fido2RegistrationService.searchFido2Registration(searchReq);
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
