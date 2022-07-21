/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

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
        log.error(" Agama Decoded flow name decodedFlowName:{}", decodedFlowName);
        Flow flow = findFlow(decodedFlowName,true);

        return Response.ok(flow).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response createFlow(@Valid Flow flow)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException      {
        log.error(" Flow to be added flow:{}, flow.getQName():{}, flow.getSource():{} ", flow, flow.getQname(),
                flow.getSource());


        // check if flow with same name already exists
        Flow existingFlow = findFlow(flow.getQname(),false);
        log.error(" existingFlow:{}", existingFlow);
        if (existingFlow != null) {
            thorwBadRequestException("Flow identified by name '" + flow.getQname() + "' already exist!");
        }
        
        // validate flow data
        validateAgamaFlowData(flow, true);
        flow.setRevision(-1);
        agamaFlowService.addAgamaFlow(flow);

        flow = findFlow(flow.getQname(), true);
        return Response.status(Response.Status.CREATED).entity(flow).build();
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response createFlowFromFile(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid String source)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException    {
        log.error(" Flow to be created flowName:{}, source:{}", flowName, source);

        String decodedFlowName = getURLDecodedValue(flowName);
        log.error(" Agama Decoded flow name for create is:{}", decodedFlowName);

        // check if flow with same name already exists
        Flow existingFlow = findFlow(decodedFlowName,false);
        log.error(" existingFlow:{}", existingFlow);
        if (existingFlow != null) {
            thorwBadRequestException("Flow identified by name '" + decodedFlowName + "' already exist!");
        }

        Flow flow = new Flow();
        flow.setQname(decodedFlowName);
        flow.setSource(source);
        flow.setEnabled(true);
        flow.setRevision(-1);

        // validate flow data
        validateAgamaFlowData(flow, true);
        agamaFlowService.addAgamaFlow(flow);

        flow = findFlow(flow.getQname(),true);
        return Response.status(Response.Status.CREATED).entity(flow).build();
    }

    @PUT
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS })
    public Response updateFlow(@PathParam(ApiConstants.QNAME) @NotNull String flowName, @Valid Flow flow)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException     {
        log.error(" Flow to update flowName:{}, flow:{}, flow.getQName():{}, flow.getSource():{} ", flowName, flow,
                flow.getQname(), flow.getSource());

        String decodedFlowName = getURLDecodedValue(flowName);
        log.error(" Agama Decoded flow name for update is:{}", decodedFlowName);

        // check if flow exists
        Flow existingFlow = findFlow(decodedFlowName, true);
 
        // set flow data
        flow.setQname(decodedFlowName);
        log.error("Flow revision check - flow.getRevision():{}, existingFlow.getRevision():{}", flow.getRevision(),
                existingFlow.getRevision());
        getRevision(flow,existingFlow);
        log.error("Flow revision after update - flow.getRevision():{}", flow.getRevision());

        // validate flow data
        validateAgamaFlowData(flow, false);
        log.error("Updating flow after validation");
        agamaFlowService.updateFlow(flow);

        flow = findFlow(decodedFlowName,true);
        return Response.status(Response.Status.OK).entity(flow).build();
    }

    @DELETE
    @Path(ApiConstants.QNAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_DELETE_ACCESS })
    public Response deleteAttribute(@PathParam(ApiConstants.QNAME) @NotNull String flowName) {
        log.error(" Flow to delete - flowName:{}", flowName);
        String decodedFlowName = getURLDecodedValue(flowName);
        log.error(" Agama Decoded flow name is:{}", decodedFlowName);

        // check if flow exists
        Flow flow = findFlow(decodedFlowName, true);

        agamaFlowService.removeAgamaFlow(flow);
        return Response.noContent().build();
    }
    
    private Flow findFlow(String flowName, boolean throwError) {
        Flow flow = null;
         try {
             flow = agamaFlowService.getFlowByName(flowName);
        } catch (EntryPersistenceException e) {
            log.error("No flow found with the name:{} ",flowName );
            if(throwError) {
                throw new NotFoundException(getNotFoundError("Flow - "+flowName+"!!!"));
            }
        }
         return flow;
    }

    private void validateAgamaFlowData(Flow flow, boolean checkNonMandatoryFields)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        log.error(" Validate Agama Flow data - flow:{}, checkNonMandatoryFields:{}", flow, checkNonMandatoryFields);
        if (flow == null) {
            return;
        }
        log.error("Agama Flow to be added flow:{}, flow.getQname():{}, flow.getSource():{} ", flow, flow.getQname(),
                flow.getSource());

        String validateMsg = agamaFlowService.validateFlowFields(flow, checkNonMandatoryFields);
        log.error("Agama Flow to be validation msg:{} ", validateMsg);
        if (StringUtils.isNotBlank(validateMsg)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Required feilds missing -> ");
            sb.append(validateMsg);
            thorwBadRequestException(sb.toString());
        }

        // validate no fields other than required exists
        // ??TO-do

        validateSyntax(flow);
    }

    private void validateAgamaFlowData2(Flow flow) {
        if (flow == null) {
            return;
        }

        log.error(" Validate Agama Flow to be created flow:{}, flow.getQname():{}, flow.getSource():{} ", flow,
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

        // validate no fields other than required exists
        // ??TO-do

        validateSyntax(flow);
    }

    private void validateSyntax(Flow flow) {
        log.error("Validate Flow Source Syntax - flow:{}", flow);
        if (flow == null) {
            return;
        }        
        //validate syntax
        try {
            Transpiler.runSyntaxCheck(flow.getQname(), flow.getSource());
        } catch (SyntaxException se) {
            log.error("Transpiler syntax check error", se);
           try {
               log.error("Throwing BadRequestException 400 :{} ", Jackson.asJson(se));
                thorwBadRequestException(Jackson.asJson(se));
            }catch(IOException io) {
                log.error("Agama Flow Transpiler syntax error parsing error", io);
                thorwBadRequestException("Transpiler syntax check error"+se);
            }
        } catch (TranspilerException te) {
            log.error("Agama Flow transpiler exception", te);
            thorwBadRequestException(te.toString());
        }
    }

    private String getURLDecodedValue(String pathParam) {
        log.error(" Decode pathParam():{} ", pathParam);
        try {
            return URLDecoder.decode(pathParam, UTF_8.name());
        } catch (UnsupportedEncodingException uee) {
            log.error("Agama Flow error while URL decoding pathParam:{}, is:{}", pathParam, uee);
        }
        return pathParam;
    }
    
    private Flow getRevision(Flow flow, Flow existingFlow) {
        log.error("Flow revision check - flow:{}, existingFlow:{}", flow, existingFlow);
        
        if(flow==null || existingFlow==null) {
            return flow;
        }
        
        log.error("Flow revision check - flow.getRevision():{}, existingFlow.getRevision():{}", flow.getRevision(),
                existingFlow.getRevision());
        
        if (flow.getSource()!=null && flow.getRevision() == 0) {
            if (existingFlow.getRevision() == 0 || existingFlow.getRevision() == -1) {
                flow.setRevision(1);
            } else {
                flow.setRevision(existingFlow.getRevision() + 1);
            }
        }
        log.error("Final flow revision to be updated to - flow.getRevision():{}", flow.getRevision());
        return flow;
    }
}
