package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.ads.model.Deployment;
import io.jans.agama.model.Flow;
import io.jans.as.model.util.Pair;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.service.auth.AgamaDeploymentsService;
import io.jans.configapi.service.auth.AgamaFlowService;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;

@Path(ApiConstants.AGAMA_DEPLOYMENTS)
@Produces(MediaType.APPLICATION_JSON)
public class AgamaDeploymentsResource extends ConfigBaseResource {

    @Inject
    private AgamaDeploymentsService ads;

    @Inject
    private AgamaFlowService flowService;
    
    private ObjectMapper mapper;

    @Operation(summary = "Retrieve the list of projects deployed currently.", description = "Retrieve the list of projects deployed currently.", operationId = "get-agama-prj", tags = {
    "Agama" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_READ_ACCESS}))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Agama projects", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-prj-get-all.json"))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS}, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeployments(@Parameter(description = "Start index for the result")  @QueryParam("start") int start, @Parameter(description = "Search size - count of the results to return") @QueryParam("count") int count) {
        
        // this is NOT a search but a paged listing
        int maxcount = getMaxCount();
        PagedResult<Deployment> res = ads.list(start < 0 ? 0 : start, count > 0 ? count : maxcount, maxcount);
        res.getEntries().forEach(this::minimize);
        return Response.ok(res).build();

    }
    
    @Operation(summary = "Fetches deployed Agama project based on name.", description = "Fetches deployed Agama project based on name.", operationId = "get-agama-prj-by-name", tags = {
    "Agama" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_READ_ACCESS} ))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Agama project", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Deployment.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-prj-get.json"))),
    @ApiResponse(responseCode = "204", description = "No Content"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS}, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    @Path(ApiConstants.NAME_PARAM_PATH)
    public Response getDeployment(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) String projectName) {
        if (logger.isInfoEnabled()) {
            logger.info("Get projectName:{}", escapeLog(projectName));
        }
        Pair<Boolean, Deployment> p = getDeploymentP(projectName);
        Deployment d = p.getSecond();
        
        if (d == null) return errorResponse(p.getFirst(), projectName);
        logger.debug("deployment:{}", d);
        return Response.ok(d).build();

    }

    @Operation(summary = "Deploy an Agama project.", description = "Deploy an Agama project.", operationId = "post-agama-prj", tags = {
    "Agama" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS}))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "202", description = "Agama project accepted", content = @Content(mediaType = "application/zip", schema = @Schema(implementation = String.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-prj-post.json"))),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "409", description = "Conflict"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Consumes("application/zip")
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, groupScopes = {},
            superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.NAME_PARAM_PATH)
    public Response deploy(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME)
                String projectName, @Parameter(description = "Boolean value to indicating to auto configure the project ") @QueryParam("autoconfigure") String autoconfigure, @Parameter(description = "Agama gama file") byte[] gamaBinary) {
        if (logger.isInfoEnabled()) {
            logger.info("Deploy projectName:{}, autoconfigure:{}, gamaBinary:{}", escapeLog(projectName), escapeLog(autoconfigure), escapeLog(gamaBinary));
        }
        if (gamaBinary == null)
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Project name or binary data missing").build();

        if (ads.createDeploymentTask(projectName, gamaBinary, Boolean.parseBoolean(autoconfigure)))
            return Response.accepted().entity("A deployment task for project " + projectName + 
                    " has been queued. Use the GET endpoint to poll status").build();

        return Response.status(Response.Status.CONFLICT)
                .entity("There is an active deployment task for " + projectName + 
                    ". Wait for an OK response from the GET endpoint").build();

    }

    @Operation(summary = "Delete a deployed Agama project.", description = "Delete a deployed Agama project.", operationId = "delete-agama-prj", tags = {
    "Agama" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            ApiAccessConstants.AGAMA_DELETE_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "No Content"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "409", description = "Conflict"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_DELETE_ACCESS }, groupScopes = {},
            superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.NAME_PARAM_PATH)
    public Response undeploy(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) String projectName) {
        if (logger.isInfoEnabled()) {
            logger.info("Undeploy projectName:{}", escapeLog(projectName));
        }
        Boolean result = ads.createUndeploymentTask(projectName);
        
        if (result == null)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Unknown project " + projectName).build();
 
        if (!result)
            return Response.status(Response.Status.CONFLICT)
                    .entity("Cannot undeploy project " + projectName + ": it is currently being deployed").build();
            
        return Response.noContent().build();
        
    }

    @Operation(summary = "Retrieve the list of configs based on name.", description = "Retrieve the list of configs based on name.", operationId = "get-agama-prj-configs", tags = {
    "Agama" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_READ_ACCESS}))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Agama projects configs", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-prj-get-configs-all.json"))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.CONFIGS + ApiConstants.NAME_PARAM_PATH)
    public Response getConfigs(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) String projectName) {
        if (logger.isInfoEnabled()) {
            logger.info("getConfigs projectName:{}", escapeLog(projectName));
        }
        Pair<Boolean, Deployment> p = getDeploymentP(projectName);
        Deployment d = p.getSecond();
        logger.debug("Deployment:{}", d);
        if (d == null) return errorResponse(p.getFirst(), projectName);
        
        Map<String, Map<String, Object>> configs = new HashMap<>();
        Set<String> flowIds = Optional.ofNullable(d.getDetails().getFlowsError())
                .map(Map::keySet).orElse(Collections.emptySet());
        logger.debug("flowIds:{}", flowIds);
        for (String qname : flowIds) {
            Map<String, Object> config = Optional.ofNullable(flowService.getFlowByName(qname))
                    .map(f -> f.getMetadata().getProperties()).orElse(null);

            if (config == null) {
                logger.warn("Flow {} does not exist or has no configuration properties defined", qname);
            } else {
                logger.debug("Adding flow properties of {}", qname);
                configs.put(qname, config);
            }
        }
        logger.debug("configs:{}", configs);
        //Use own mapper so any empty maps/nulls that may be found inside flows configurations are not
        //ignored. Using @JsonInclude(Include.ALWAYS) in FlowMetadata#properties did not help
        try {
            return Response.ok(mapper.writeValueAsString(configs)).build();
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return Response.serverError().build();
        }

    }

    @Operation(summary = "Update an Agama project.", description = "Update an Agama project.", operationId = "put-agama-prj", tags = {
    "Agama" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS}))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "202", description = "Agama project accepted", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-prj-post.json"))),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "409", description = "Conflict"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, groupScopes = {},
            superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.CONFIGS + ApiConstants.NAME_PARAM_PATH)
    public Response setConfigs(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) String projectName,
            @Parameter(description = "Agama flow config, key is `name` of config property and `value` is the property value. ") Map<String, Map<String, Object>> flowsConfigs) {
        if (logger.isInfoEnabled()) {
            logger.info("Set Agama project configs projectName:{}, flowsConfigs:{}", escapeLog(projectName), escapeLog(flowsConfigs));
        }
        if (flowsConfigs == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Mapping of flows vs. configs not provided").build();
        }

        Pair<Boolean, Deployment> p = getDeploymentP(projectName);
        Deployment d = p.getSecond();
        logger.debug("Set Agama project configs projectName:{}, Deployment:{}", projectName, d);
        if (d == null) return errorResponse(p.getFirst(), projectName);

        Map<String, Boolean> results = new HashMap<>();
        Set<String> flowIds = Optional.ofNullable(d.getDetails().getFlowsError())
                .map(Map::keySet).orElse(Collections.emptySet());
        logger.debug("Set Agama project configs projectName:{}, flowIds:{}", projectName, flowIds);
        for (String qname : flowsConfigs.keySet()) {
            if (qname != null && flowIds.contains(qname)) {

                Flow flow = flowService.getFlowByName(qname);
                boolean success = false;

                if (flow == null) {
                    logger.warn("Unable to retrieve flow {}", qname);
                } else {
                    try {
                        flow.getMetadata().setProperties(flowsConfigs.get(qname));
                        flowService.updateFlow(flow);
                        success = true;
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                results.put(qname, success);

            } else if (logger.isWarnEnabled()) {
                logger.warn("Flow {} is not part of project {}, config ignored", qname, 
                        projectName.replaceAll("[\n\r]", "_"));
            }
        } 
        logger.debug("Final Agama project configs projectName:{}, results:{}", projectName, results);
        return Response.ok(results).build();

    }

    private Pair<Boolean, Deployment> getDeploymentP(String projectName) {

        Deployment d = ads.getDeployment(projectName);

        if (d == null) return new Pair<>(false, null);

        if (d.getFinishedAt() == null) return new Pair<>(true, null);   
        
        return new Pair<>(true, minimize(d));
        
    }

    private Response errorResponse(boolean flag, String projectName) {
        
        if (flag) return Response.noContent().build();
        
        return Response.status(Response.Status.NOT_FOUND)
                .entity("Unknown project " + projectName).build();

    }

    private Deployment minimize(Deployment d) {
        //hides some deployment details 
        d.getDetails().setFolders(null);
        return d;
    }

    @PostConstruct
    private void init() {
        mapper = new ObjectMapper();
    }

}
