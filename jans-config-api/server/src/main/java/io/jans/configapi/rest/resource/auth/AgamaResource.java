/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatch;

import io.jans.agama.model.Flow;
import io.jans.agama.model.FlowMetadata;
import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.service.auth.AgamaFlowService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.List;
import java.util.stream.Collectors;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.commons.lang.StringUtils;

@Path(ApiConstants.AGAMA)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgamaResource extends ConfigBaseResource {

    @Inject
    AgamaFlowService agamaFlowService;

    @Operation(summary = "Fetches all agama flow.", description = "Fetches all agama flow.", operationId = "get-agama-flows", tags = {
            "Configuration – Agama Flow" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_READ_ACCESS, ApiAccessConstants.AGAMA_WRITE_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agama Flows", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-get-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getFlows(@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @DefaultValue("agFlowQname") @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @DefaultValue("false") @QueryParam(value = ApiConstants.INCLUDE_SOURCE) boolean includeSource) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Search Agama Flow with pattern:{}, limit:{}, startIndex:{}, sortBy:{}, sortOrder:{}, includeSource:{}",
                    escapeLog(pattern), escapeLog(limit), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(includeSource));
        }

        SearchRequest searchReq = createSearchRequest(agamaFlowService.getAgamaFlowDn(null), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, this.getMaxCount());

        return Response.ok(doSearch(searchReq, includeSource)).build();
    }

    @Operation(summary = "Gets an agama flow based on Qname.", description = "Gets an agama flow based on Qname.", operationId = "get-agama-flow", tags = {
            "Configuration – Agama Flow" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_READ_ACCESS, ApiAccessConstants.AGAMA_WRITE_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agama Flow", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Flow.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.QNAME_PATH)
    public Response getFlowByName(@PathParam(ApiConstants.QNAME) @NotNull String flowName,
            @DefaultValue("false") @QueryParam(value = ApiConstants.INCLUDE_SOURCE) boolean includeSource) {
        if (logger.isDebugEnabled()) {
            logger.debug("Search Agama with flowName:{}, includeSource:{}", escapeLog(flowName),
                    escapeLog(includeSource));
        }

        String decodedFlowName = getURLDecodedValue(flowName);
        logger.trace(" Agama Decoded flow name decodedFlowName:{}", decodedFlowName);
        Flow flow = findFlow(decodedFlowName, true);

        return Response.ok(minimize(flow, includeSource)).build();
    }

    @Operation(summary = "Create a new agama flow", description = "Create a new agama flow", operationId = "post-agama-flow", tags = {
            "Configuration – Agama Flow" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_WRITE_ACCESS }))
    @RequestBody(description = "Agama Flow", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Flow.class), examples = @ExampleObject(name = "Request example", value = "example/agama/agama.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Flow.class), examples = @ExampleObject(name = "Response example", value = "example/agama/agama.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response createFlow(@Valid Flow flow)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        logger.debug(" Flow to be added flow:{}, flow.getQName():{}, flow.getSource():{} ", flow, flow.getQname(),
                flow.getSource());

        // check if flow with same name already exists
        Flow existingFlow = findFlow(flow.getQname(), false);
        logger.debug(" existingFlow:{}", existingFlow);
        if (existingFlow != null) {
            thorwBadRequestException("Flow identified by name '" + flow.getQname() + "' already exists!");
        }

        // validate flow data
        updateFlowDetails(flow, null, true);
        validateAgamaFlowData(flow, true);
        agamaFlowService.addAgamaFlow(flow);

        flow = findFlow(flow.getQname(), true);
        return Response.status(Response.Status.CREATED).entity(minimize(flow, false)).build();
    }

    @Operation(summary = "Create a new agama flow from source", description = "Create a new agama flow from source.", operationId = "post-agama-flow-from-source", tags = {
            "Configuration – Agama Flow" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_WRITE_ACCESS }))
    @RequestBody(description = "Agama Flow", content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class), examples = @ExampleObject(name = "Request example", value = "example/agama/test-agama-source.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Flow.class), examples = @ExampleObject(name = "Response example", value = "example/agama/agama.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response createFlowFromSource(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid String source)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        logger.debug(" Flow to be created flowName:{}, source:{}", flowName, source);

        String decodedFlowName = getURLDecodedValue(flowName);
        logger.trace(" Agama Decoded flow name for create is:{}", decodedFlowName);

        // check if flow with same name already exists
        Flow existingFlow = findFlow(decodedFlowName, false);
        logger.debug(" existing-flow:{}", existingFlow);
        if (existingFlow != null) {
            thorwBadRequestException("Flow identified by name '" + decodedFlowName + "' already exists!");
        }

        Flow flow = new Flow();
        flow.setQname(decodedFlowName);
        flow.setSource(source);
        flow.setEnabled(true);
        updateFlowDetails(flow, null, true);

        // validate flow data
        validateAgamaFlowData(flow, true);
        agamaFlowService.addAgamaFlow(flow);

        flow = findFlow(flow.getQname(), true);
        return Response.status(Response.Status.CREATED).entity(minimize(flow, false)).build();
    }

    @Operation(summary = "Update agama flow from source file", description = "Update agama flow from source file.", operationId = "put-agama-flow-from-source", tags = {
            "Configuration – Agama Flow" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class), examples = @ExampleObject(name = "Request example", value = "example/agama/test-agama-source.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Flow.class), examples = @ExampleObject(name = "Response example", value = "example/agama/agama.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Path(ApiConstants.SOURCE + ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateFlowSource(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid String source)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        logger.debug(" Flow to be updated flowName:{}, source:{}", flowName, source);

        String decodedFlowName = getURLDecodedValue(flowName);
        logger.trace(" Agama flow name for update is:{}", decodedFlowName);

        // check if flow with same name exists
        Flow existingFlow = findFlow(decodedFlowName, true);
        logger.debug(" Agama existingFlow:{}", existingFlow);

        existingFlow.setSource(source);
        updateFlowDetails(existingFlow, existingFlow, false);

        // validate flow data
        validateAgamaFlowData(existingFlow, false);
        logger.debug("Update flow after validation");
        agamaFlowService.updateFlow(existingFlow);

        existingFlow = findFlow(existingFlow.getQname(), true);
        return Response.status(Response.Status.OK).entity(minimize(existingFlow, false)).build();
    }

    @Operation(summary = "Partially modify a Agama Flow", description = "Partially modify a Agama Flow", operationId = "patch-agama-flow", tags = {
            "Configuration – Agama Flow" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_WRITE_ACCESS }))
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request example", value = "example/agama/agama-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patched Agama Flow", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Flow.class), examples = @ExampleObject(name = "Response example", value = "example/agama/agama.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchFlow(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @NotNull JsonPatch jsonPatch)
            throws JsonPatchException, IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("Flow details to be patched - flowName:{}, jsonPatch:{}", escapeLog(flowName),
                    escapeLog(jsonPatch));
        }

        String decodedFlowName = getURLDecodedValue(flowName);
        logger.debug(" Flow to be patched is name:{}", decodedFlowName);

        // check if flow exists
        Flow existingFlow = findFlow(decodedFlowName, false);
        logger.debug(" Flow to be patched:{}", existingFlow);

        existingFlow = Jackson.applyJsonPatch(jsonPatch, existingFlow);
        logger.debug(" After patch flow:{}", existingFlow);

        // validate flow data
        validateAgamaFlowData(existingFlow, false);
        logger.debug("Updating flow after validation");
        agamaFlowService.updateFlow(existingFlow);
        return Response.ok(minimize(existingFlow, false)).build();
    }

    @Operation(summary = "Deletes an agama flow based on Qname", description = "Deletes an agama flow based on Qname", operationId = "delete-agama-flow", tags = {
            "Configuration – Agama Flow" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_DELETE_ACCESS }, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response delete(@PathParam(ApiConstants.QNAME) @NotNull String flowName) {
        logger.debug(" Flow to delete - flowName:{}", flowName);
        String decodedFlowName = getURLDecodedValue(flowName);
        logger.trace(" Agama Decoded flow name is:{}", decodedFlowName);

        // check if flow exists
        findFlow(decodedFlowName, true);

        agamaFlowService.removeAgamaFlow(flowName);
        return Response.noContent().build();
    }

    private Flow findFlow(String flowName, boolean throwError) {
        Flow flow = null;
        try {
            flow = agamaFlowService.getFlowByName(flowName);
        } catch (EntryPersistenceException e) {
            logger.error("No flow found with the name:{} ", flowName);
            if (throwError) {
                throw new NotFoundException(getNotFoundError("Flow - '" + flowName + "'"));
            }
        }
        return flow;
    }

    private void validateAgamaFlowData(Flow flow, boolean checkNonMandatoryFields)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        logger.debug(" Validate Agama Flow data - flow:{}, checkNonMandatoryFields:{}", flow, checkNonMandatoryFields);
        if (flow == null) {
            return;
        }
        logger.debug("Agama Flow to be added flow:{}, flow.getQname():{}, flow.getSource():{} ", flow, flow.getQname(),
                flow.getSource());

        String validateMsg = agamaFlowService.validateFlowFields(flow, checkNonMandatoryFields);
        logger.debug("Agama Flow to be validation msg:{} ", validateMsg);
        if (StringUtils.isNotBlank(validateMsg)) {
            thorwBadRequestException(validateMsg);
        }

        // validate syntax
        validateSyntax(flow);
    }

    private void validateSyntax(Flow flow) {
        logger.debug("Validate Flow Source Syntax - flow:{}", flow);
        if (flow == null) {
            return;
        }
        // validate syntax
        try {
            Transpiler.runSyntaxCheck(flow.getQname(), flow.getSource());
        } catch (SyntaxException | TranspilerException e) {
            logger.error("Transpiler exception", e);
            e.setStackTrace(new StackTraceElement[0]);
            thorwBadRequestException(e);
        }
    }

    private String getURLDecodedValue(String pathParam) {
        logger.debug(" Decode pathParam():{} ", pathParam);
        try {
            return URLDecoder.decode(pathParam, UTF_8.name());
        } catch (UnsupportedEncodingException uee) {
            logger.error("Agama Flow error while URL decoding pathParam:{}, is:{}", pathParam, uee);
        }
        return pathParam;
    }

    private Flow updateFlowDetails(Flow flow, Flow existingFlow, boolean updateMetadata) {
        logger.debug("Update Flow details - flow:{}, existingFlow:{}, updateMetadata:{}", flow, existingFlow,
                updateMetadata);

        updateRevision(flow, existingFlow);
        if (updateMetadata) {
            updateMetadata(flow);
        }
        return flow;
    }

    private Flow updateRevision(Flow flow, Flow existingFlow) {
        logger.debug("Flow revision check - flow:{}, existingFlow:{}", flow, existingFlow);

        if (flow == null) {
            return flow;
        }

        if (existingFlow == null) {
            flow.setRevision(-1);
            return flow;
        }

        logger.trace("Flow revision before update - flow.getRevision():{}, existingFlow.getRevision():{}",
                flow.getRevision(), existingFlow.getRevision());

        if (flow.getSource() != null && (flow.getRevision() <= 0 || flow.getRevision() == existingFlow.getRevision())) {
            if (existingFlow.getRevision() <= 0) {
                flow.setRevision(1);
            } else {
                flow.setRevision(existingFlow.getRevision() + 1);
            }
        }
        logger.trace("Flow revision after update - flow.getRevision():{}", flow.getRevision());
        return flow;
    }

    private Flow updateMetadata(Flow flow) {
        logger.debug("Update Flow Metadata - flow:{}", flow);

        if (flow == null) {
            return flow;
        }

        FlowMetadata flowMetadata = flow.getMetadata();
        if (flowMetadata == null) {
            flowMetadata = new FlowMetadata();
        }

        logger.trace("Flow Metadata Timestamp before update - flowMetadata.getTimestamp():{}",
                flowMetadata.getTimestamp());
        flowMetadata.setTimestamp(System.currentTimeMillis());
        flow.setMetadata(flowMetadata);

        logger.trace("Flow Metadata Timestamp after update - flowMetadata.getTimestamp():{}",
                flowMetadata.getTimestamp());
        return flow;
    }

    private Flow minimize(Flow flow, boolean includeSource) {
        if (flow == null) {
            return flow;
        }
        flow.setTranspiled(null);
        flow.setTransHash(null);
        if (!includeSource) {
            flow.setSource(null);
        }

        return flow;

    }

    private PagedResult<Flow> doSearch(SearchRequest searchReq, boolean includeSource) {

        logger.debug("Filters for Searching Agama Flow  - searchReq:{}, includeSource:{} ", searchReq, includeSource);

        PagedResult<Flow> pagedResult = agamaFlowService.searchFlows(searchReq);

        logger.debug("Agama Flow PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "Agama Flow fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
            List<Flow> flows = pagedResult.getEntries();
            flows = flows.stream().map(f -> minimize(f, includeSource)).collect(Collectors.toList());
            pagedResult.setEntries(flows);
        }
        logger.debug("Agama Flow pagedResult:{} ", pagedResult);
        return pagedResult;
    }

}
