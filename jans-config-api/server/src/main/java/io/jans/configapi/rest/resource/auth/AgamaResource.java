/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

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
import java.util.stream.Collectors;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatch;

@Path(ApiConstants.AGAMA)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgamaResource extends ConfigBaseResource {

    @Inject
    AgamaFlowService agamaFlowService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS })
    public Response getFlows(@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("false") @QueryParam(value = ApiConstants.INCLUDE_SOURCE) boolean includeSource) {

        if (logger.isDebugEnabled()) {
            logger.debug("Search Agama Flow with pattern:{}, sizeLimit:{}, includeSource:{}", escapeLog(pattern),
                    escapeLog(limit), escapeLog(includeSource));
        }

        List<Flow> flows = null;
        if (!pattern.isEmpty() && pattern.length() >= 2) {
            flows = agamaFlowService.searchAgamaFlows(pattern, limit);
        } else {
            flows = agamaFlowService.getAllAgamaFlows(limit);
        }

        flows = flows.stream().map(f -> minimize(f, includeSource)).collect(Collectors.toList());
        return Response.ok(flows).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS })
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

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
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

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
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

    //TODO: determine if this endpoint is really needed in practice
    /*
    @PUT
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response updateFlow(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid Flow flow)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        logger.debug(" Flow to update flowName:{}, flow:{}, flow.getQName():{}, flow.getSource():{} ", flowName, flow,
                flow.getQname(), flow.getSource());

        String decodedFlowName = getURLDecodedValue(flowName);
        logger.trace(" Agama Decoded flow name for update is:{}", decodedFlowName);

        // check if flow exists
        Flow existingFlow = findFlow(decodedFlowName, true, false);

        // set flow data
        flow.setQname(decodedFlowName);
        updateFlowDetails(flow, existingFlow, false);
        logger.debug("Flow revision after update - flow.getRevision():{}", flow.getRevision());

        // validate flow data
        validateAgamaFlowData(flow, false);
        logger.debug("Updating flow after validation");
        agamaFlowService.updateFlow(flow);

        flow = findFlow(decodedFlowName, true, false);
        return Response.status(Response.Status.OK).entity(flow).build();
    }*/

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Path(ApiConstants.SOURCE + ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response updateFlowSource(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid String source)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        logger.debug(" Flow to be updated flowName:{}, source:{}", flowName, source);

        String decodedFlowName = getURLDecodedValue(flowName);
        logger.trace(" Agama flow name for update is:{}", decodedFlowName);

        // check if flow with same name exists
        Flow existingFlow = findFlow(decodedFlowName, true);
        logger.debug(" Agama existingFlow:{}", existingFlow);

        // Update source and revision
        if (existingFlow != null) {
            existingFlow.setSource(source);

            updateFlowDetails(existingFlow, existingFlow, false);

            // validate flow data
            validateAgamaFlowData(existingFlow, false);
            logger.debug("Update flow after validation");
            agamaFlowService.updateFlow(existingFlow);

            existingFlow = findFlow(existingFlow.getQname(), true);
        }
        return Response.status(Response.Status.OK).entity(minimize(existingFlow, false)).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
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

    @DELETE
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_DELETE_ACCESS })
    public Response delete(@PathParam(ApiConstants.QNAME) @NotNull String flowName) {
        logger.debug(" Flow to delete - flowName:{}", flowName);
        String decodedFlowName = getURLDecodedValue(flowName);
        logger.trace(" Agama Decoded flow name is:{}", decodedFlowName);

        // check if flow exists
        Flow flow = findFlow(decodedFlowName, true);

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
        logger.debug("Update Flow details - flow:{}, existingFlow:{}, updateMetadata:{}", flow, existingFlow, updateMetadata);

        updateRevision(flow, existingFlow);
        if(updateMetadata) {
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

        flow.setTranspiled(null);
        flow.setTransHash(null);
        flow.setCodeError(null);
        if (!includeSource) {
            flow.setSource(null);
        }
        return flow;

    }

}
