/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.jans.agama.model.Flow;
import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.service.auth.AgamaFlowService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.List;
import static java.nio.charset.StandardCharsets.UTF_8;


import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.AGAMA)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgamaResource extends ConfigBaseResource {

    private static final String AGAMA_QNAME = "FlowName";
    private static final String AGAMA_SOURCE = "source";

    @Inject
    Logger log;

    @Inject
    AgamaFlowService agamaFlowService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS })
    public Response getFlows(@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit) {

        if (log.isDebugEnabled()) {
            log.error("Search Agama Flow with pattern:{}, sizeLimit:{}, ", escapeLog(pattern), escapeLog(limit));
        }

        List<Flow> flows = null;
        if (!pattern.isEmpty() && pattern.length() >= 2) {
            flows = agamaFlowService.searchAgamaFlows(pattern, limit);
        } else {
            flows = agamaFlowService.getAllAgamaFlows(limit);
        }

        return Response.ok(flows).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS })
    @Path(ApiConstants.QNAME_PATH)
    public Response getFlowByName(@PathParam(ApiConstants.QNAME) @NotNull String flowName) {
        if (log.isDebugEnabled()) {
            log.error("Search Agama with flowName:{}, ", escapeLog(flowName));
        }
        
        String decodedFlowName = getURLDecodedValue(flowName);
        log.error(" Agama Decoded flow name decodedFlowName:{}",decodedFlowName);
        Flow flow = agamaFlowService.getFlowByName(decodedFlowName);

        return Response.ok(flow).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response createFlow(@Valid Flow flow) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException  {
        log.error(" Flow to be added flow:{}, flow.getQName():{}, flow.getSource():{} ", flow, flow.getQname(),
                flow.getSource());

        // validate flow data
        validateAgamaFlowData(flow);
        agamaFlowService.addAgamaFlow(flow);

        flow = agamaFlowService.getFlowByName(flow.getQname());
        return Response.status(Response.Status.CREATED).entity(flow).build();
    }
    
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response updateFlow(@Valid Flow flow) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        log.error(" Flow to update flow:{}, flow.getQName():{}, flow.getSource():{} ", flow, flow.getQname(),
                flow.getSource());

        // check if flow exists
        Flow existingFlow = agamaFlowService.getFlowByName(flow.getQname());
        if(existingFlow == null) {
            thorwBadRequestException("Flow identified by "+flow.getQname()+" does not exist!" );
        }
        
        //validate flow data
        validateAgamaFlowData(flow);
        agamaFlowService.updateFlow(flow);

        flow = agamaFlowService.getFlowByName(flow.getQname());
        return Response.status(Response.Status.OK).entity(flow).build();
    }

    @DELETE
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_DELETE_ACCESS })
    public Response deleteAttribute(@PathParam(ApiConstants.QNAME) @NotNull String flowName) {
        log.error(" Flow to delete - flowName:{}", flowName);
        String decodedFlowName = getURLDecodedValue(flowName);
        log.error(" Agama Decoded flow name is:{}",decodedFlowName);
        
        // check if flow exists
        Flow flow = agamaFlowService.getFlowByName(decodedFlowName);
        if(flow == null) {
            thorwBadRequestException("Flow identified by "+flowName+" does not exist!" );
        }
        
        agamaFlowService.removeAgamaFlow(flow);
        return Response.noContent().build();
    }
    
    private void validateAgamaFlowData(Flow flow) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        log.error(" Validate Agama Flow data - flow:{}", flow);
        if (flow == null) {
            return;
        }
        log.error("Agama Flow to be added flow:{}, flow.getQname():{}, flow.getSource():{} ", flow,
                flow.getQname(), flow.getSource());
        
        String validateMsg = agamaFlowService.validateFlowFields(flow);
        log.error("Agama Flow to be validation msg:{} ", validateMsg);
        if(StringUtils.isNotBlank(validateMsg)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Required feilds missing -> ");
            sb.append(validateMsg);
            thorwBadRequestException(sb.toString());
        }
        
        validateSyntax(flow);
    }
    
    private void validateAgamaFlowData2(Flow flow) {
        if (flow == null) {
            return;
        }

        log.error(" Validate Agama Flow to be added flow:{}, flow.getQname():{}, flow.getSource():{} ", flow,
                flow.getQname(), flow.getSource());
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isBlank(flow.getQname())) {
            sb.append(AGAMA_QNAME).append(",");
        }

        if (StringUtils.isBlank(flow.getSource())) {
            sb.append(AGAMA_SOURCE).append(",");
        }

        log.error(" sb:{} ", sb);
        if (sb.length() > 0) {
            sb.insert(0, "Required feilds missing -> ");
            sb.replace(sb.lastIndexOf(","), sb.length(), "");
            thorwBadRequestException(sb.toString());
        }
        
        
        
        validateSyntax(flow);
    }
    
    private void validateSyntax(Flow flow) {
        log.error("Validate Syntax - flow:{}", flow);
        if (flow == null) {
            return;
        }        
        //validate syntax
        try {
            Transpiler.runSyntaxCheck(flow.getQname(), flow.getSource());
        } catch (SyntaxException se) {
            log.error("Transpiler syntax check error", se);
            try {
                thorwBadRequestException(Jackson.asJsonNode(se.getError()).toString());
            }catch(JsonProcessingException jpe) {
                log.error("Agama Flow Transpiler syntax error parsing error", jpe);
            }
        } catch (TranspilerException te) {
            log.error("Agama Flow transpiler exception", te);
            thorwBadRequestException(te.toString());
        }
    }
    
    private String getURLDecodedValue(String pathParam) {
        log.error(" Validate Agama Flow to be added pathParam():{} ", pathParam);
        try {
            return URLDecoder.decode(pathParam, UTF_8.name());
        } catch (UnsupportedEncodingException uee) {
            log.error("Agama Flow error while URL decoding pathParam:{}, is:{}", pathParam, uee);
        }
        return pathParam;
    }
}
