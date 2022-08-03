/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.agama.model.Flow;
import io.jans.agama.model.FlowMetadata;
import io.jans.as.common.model.registration.Client;
import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.service.auth.AgamaFlowService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.exception.EntryPersistenceException;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.github.fge.jsonpatch.JsonPatchException;

@Path(ApiConstants.AGAMA)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgamaResource extends ConfigBaseResource {

    @Inject
    Logger log;

    @Inject
    AgamaFlowService agamaFlowService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS })
    public Response getFlows(@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("false") @QueryParam(value = ApiConstants.INCLUDE_SOURCE) boolean includeSource) {

        if (log.isDebugEnabled()) {
            log.debug("Search Agama Flow with pattern:{}, sizeLimit:{}, includeSource:{}", escapeLog(pattern),
                    escapeLog(limit), escapeLog(includeSource));
        }

        List<Flow> flows = null;
        if (!pattern.isEmpty() && pattern.length() >= 2) {
            flows = agamaFlowService.searchAgamaFlows(pattern, limit);
        } else {
            flows = agamaFlowService.getAllAgamaFlows(limit);
        }

        // filter values
        getAgamaFlowDetails(flows, includeSource);
        return Response.ok(flows).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS })
    @Path(ApiConstants.QNAME_PATH)
    public Response getFlowByName(@PathParam(ApiConstants.QNAME) @NotNull String flowName,
            @DefaultValue("false") @QueryParam(value = ApiConstants.INCLUDE_SOURCE) boolean includeSource) {
        if (log.isDebugEnabled()) {
            log.debug("Search Agama with flowName:{}, includeSource:{}", escapeLog(flowName), escapeLog(includeSource));
        }

        String decodedFlowName = getURLDecodedValue(flowName);
        log.trace(" Agama Decoded flow name decodedFlowName:{}", decodedFlowName);
        Flow flow = findFlow(decodedFlowName, true, includeSource);

        return Response.ok(flow).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response createFlow(@Valid Flow flow)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        log.debug(" Flow to be added flow:{}, flow.getQName():{}, flow.getSource():{} ", flow, flow.getQname(),
                flow.getSource());

        // check if flow with same name already exists
        Flow existingFlow = findFlow(flow.getQname(), false, false);
        log.debug(" existingFlow:{}", existingFlow);
        if (existingFlow != null) {
            thorwBadRequestException("Flow identified by name '" + flow.getQname() + "' already exist!");
        }

        // validate flow data
        validateAgamaFlowData(flow, true);
        flow.setRevision(-1);
        FlowMetadata flowMetadata = new FlowMetadata();
        flowMetadata.setTimestamp(System.currentTimeMillis());
        flow.setMetadata(flowMetadata);
        agamaFlowService.addAgamaFlow(flow);

        flow = findFlow(flow.getQname(), true, false);
        return Response.status(Response.Status.CREATED).entity(flow).build();
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response createFlowFromFile(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid String source)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        log.debug(" Flow to be created flowName:{}, source:{}", flowName, source);

        String decodedFlowName = getURLDecodedValue(flowName);
        log.trace(" Agama Decoded flow name for create is:{}", decodedFlowName);

        // check if flow with same name already exists
        Flow existingFlow = findFlow(decodedFlowName, false, false);
        log.debug(" existing-flow:{}", existingFlow);
        if (existingFlow != null) {
            thorwBadRequestException("Flow identified by name '" + decodedFlowName + "' already exist!");
        }

        Flow flow = new Flow();
        flow.setQname(decodedFlowName);
        flow.setSource(source);
        flow.setEnabled(true);
        flow.setRevision(-1);
        FlowMetadata flowMetadata = new FlowMetadata();
        flowMetadata.setTimestamp(System.currentTimeMillis());
        flow.setMetadata(flowMetadata);

        // validate flow data
        validateAgamaFlowData(flow, true);
        agamaFlowService.addAgamaFlow(flow);

        flow = findFlow(flow.getQname(), true, false);
        return Response.status(Response.Status.CREATED).entity(flow).build();
    }

    @PUT
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response updateFlow(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid Flow flow)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        log.debug(" Flow to update flowName:{}, flow:{}, flow.getQName():{}, flow.getSource():{} ", flowName, flow,
                flow.getQname(), flow.getSource());

        String decodedFlowName = getURLDecodedValue(flowName);
        log.trace(" Agama Decoded flow name for update is:{}", decodedFlowName);

        // check if flow exists
        Flow existingFlow = findFlow(decodedFlowName, true, false);

        // set flow data
        flow.setQname(decodedFlowName);
        log.trace("Flow revision check - flow.getRevision():{}, existingFlow.getRevision():{}", flow.getRevision(),
                existingFlow.getRevision());
        getRevision(flow, existingFlow);
        log.debug("Flow revision after update - flow.getRevision():{}", flow.getRevision());

        // validate flow data
        validateAgamaFlowData(flow, false);
        log.debug("Updating flow after validation");
        agamaFlowService.updateFlow(flow);

        flow = findFlow(decodedFlowName, true, false);
        return Response.status(Response.Status.OK).entity(flow).build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response updateFlowFromFile(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid String source)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        log.debug(" Flow to be updated flowName:{}, source:{}", flowName, source);

        String decodedFlowName = getURLDecodedValue(flowName);
        log.trace(" Agama flow name for update is:{}", decodedFlowName);

        // check if flow with same name already exists
        Flow existingFlow = findFlow(decodedFlowName, false, false);
        log.debug(" Agama existingFlow:{}", existingFlow);

        // Update source and revision
        if (existingFlow != null) {
            existingFlow.setSource(source);

            getRevision(existingFlow, existingFlow);

            // validate flow data
            validateAgamaFlowData(existingFlow, false);
            log.debug("Update flow after validation");
            agamaFlowService.updateFlow(existingFlow);

            existingFlow = findFlow(existingFlow.getQname(), true, false);
        }
        return Response.status(Response.Status.OK).entity(existingFlow).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response patchFlow(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @NotNull String pathString)
            throws JsonPatchException, IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("Flow details to be patched - flowName:{}, pathString:{}", escapeLog(flowName),
                    escapeLog(pathString));
        }
        String decodedFlowName = getURLDecodedValue(flowName);
        log.trace(" Flow name for update is:{}", decodedFlowName);

        // check if flow exists
        Flow existingFlow = findFlow(decodedFlowName, false, true);
        log.debug(" existingFlow:{}", existingFlow);

        existingFlow = Jackson.applyPatch(pathString, existingFlow);
        getRevision(existingFlow, existingFlow);

        // validate flow data
        validateAgamaFlowData(existingFlow, false);
        log.debug("Updating flow after validation");
        agamaFlowService.updateFlow(existingFlow);
        return Response.ok(existingFlow).build();
    }

    @DELETE
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_DELETE_ACCESS })
    public Response deleteAttribute(@PathParam(ApiConstants.QNAME) @NotNull String flowName) {
        log.debug(" Flow to delete - flowName:{}", flowName);
        String decodedFlowName = getURLDecodedValue(flowName);
        log.trace(" Agama Decoded flow name is:{}", decodedFlowName);

        // check if flow exists
        Flow flow = findFlow(decodedFlowName, true, false);

        agamaFlowService.removeAgamaFlow(flow);
        return Response.noContent().build();
    }

    private Flow findFlow(String flowName, boolean throwError, boolean includeSource) {
        Flow flow = null;
        try {
            flow = agamaFlowService.getFlowByName(flowName);

            // filter values
            List<Flow> flows = Arrays.asList(flow);
            getAgamaFlowDetails(flows, includeSource);
            if (flows != null && !flows.isEmpty()) {
                flow = flows.get(0);
            }
        } catch (EntryPersistenceException e) {
            log.error("No flow found with the name:{} ", flowName);
            if (throwError) {
                throw new NotFoundException(getNotFoundError("Flow - " + flowName + "!!!"));
            }
        }
        return flow;
    }

    private void validateAgamaFlowData(Flow flow, boolean checkNonMandatoryFields)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        log.debug(" Validate Agama Flow data - flow:{}, checkNonMandatoryFields:{}", flow, checkNonMandatoryFields);
        if (flow == null) {
            return;
        }
        log.debug("Agama Flow to be added flow:{}, flow.getQname():{}, flow.getSource():{} ", flow, flow.getQname(),
                flow.getSource());

        String validateMsg = agamaFlowService.validateFlowFields(flow, checkNonMandatoryFields);
        log.debug("Agama Flow to be validation msg:{} ", validateMsg);
        if (StringUtils.isNotBlank(validateMsg)) {
            thorwBadRequestException(validateMsg);
        }

        // validate syntax
        validateSyntax(flow);
    }

    private void validateSyntax(Flow flow) {
        log.debug("Validate Flow Source Syntax - flow:{}", flow);
        if (flow == null) {
            return;
        }
        // validate syntax
        try {
            Transpiler.runSyntaxCheck(flow.getQname(), flow.getSource());
        } catch (SyntaxException se) {
            log.error("Transpiler syntax check error", se);
            try {
                log.debug("Throwing BadRequestException 400 :{} ", Jackson.asPrettyJson(se));
                thorwBadRequestException(Jackson.asJson(se));
            } catch (IOException io) {
                log.error("Agama Flow Transpiler syntax error parsing error", io);
                thorwBadRequestException("Transpiler syntax check error" + se);
            }
        } catch (TranspilerException te) {
            log.error("Agama Flow transpiler exception", te);
            thorwBadRequestException(te.toString());
        }
    }

    private String getURLDecodedValue(String pathParam) {
        log.debug(" Decode pathParam():{} ", pathParam);
        try {
            return URLDecoder.decode(pathParam, UTF_8.name());
        } catch (UnsupportedEncodingException uee) {
            log.error("Agama Flow error while URL decoding pathParam:{}, is:{}", pathParam, uee);
        }
        return pathParam;
    }

    private Flow getRevision(Flow flow, Flow existingFlow) {
        log.debug("Flow revision check - flow:{}, existingFlow:{}", flow, existingFlow);

        if (flow == null || existingFlow == null) {
            return flow;
        }

        log.debug("Flow revision check - flow.getRevision():{}, existingFlow.getRevision():{}", flow.getRevision(),
                existingFlow.getRevision());

        if (flow.getSource() != null && flow.getRevision() == 0) {
            if (existingFlow.getRevision() == 0 || existingFlow.getRevision() == -1) {
                flow.setRevision(1);
            } else {
                flow.setRevision(existingFlow.getRevision() + 1);
            }
        }
        log.debug("Final flow revision to be updated to - flow.getRevision():{}", flow.getRevision());
        return flow;
    }

    private List<Flow> getAgamaFlowDetails(List<Flow> flows, boolean includeSource) {

        log.debug("Flow data filter - flows:{}, includeSource:{}", flows, includeSource);
        if (flows == null || flows.isEmpty()) {
            return flows;
        }

        for (Flow flow : flows) {
            
            flow.setTranspiled(null);
            flow.setTransHash(null);
            
            if (!includeSource) {
                flow.setSource(null);
            }

        }
        return flows;

    }
}
