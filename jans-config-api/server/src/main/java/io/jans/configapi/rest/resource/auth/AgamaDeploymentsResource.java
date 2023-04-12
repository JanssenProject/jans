package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    @Operation(summary = "Retrieve the list of projects deployed currently.", description = "Retrieve the list of projects deployed currently.", operationId = "get-agama-dev-prj", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_READ_ACCESS}))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Agama projects", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-dev-prj-get-all.json"))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS}, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("list")
    public Response getDeployments(@QueryParam("start") int start, @QueryParam("count") int count) {
        
        // this is NOT a search but a paged listing
        int maxcount = getMaxCount();
        PagedResult<Deployment> res = ads.list(start < 0 ? 0 : start, count > 0 ? count : maxcount, maxcount);
        res.getEntries().forEach(d -> d.getDetails().setFolders(null));
        return Response.ok(res).build();

    }

    @Operation(summary = "Fetches deployed Agama project based on name.", description = "Fetches deployed Agama project based on name.", operationId = "get-agama-dev-studio-prj-by-name", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_READ_ACCESS} ))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Agama project", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Deployment.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-dev-prj-get.json"))),
    @ApiResponse(responseCode = "204", description = "No Content"),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS}, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    @Path(ApiConstants.NAME_PARAM_PATH)
    public Response getDeployment(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) @NotNull String projectName) {
        
        if (projectName == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Project name missing in query string").build();
        }
        
        Deployment d = ads.getDeployment(projectName);
        
        if (d == null)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Unknown project " + projectName).build();

        if (d.getFinishedAt() == null)
            return Response.noContent().build();
        
        d.getDetails().setFolders(null);
        try {
            //Use own mapper so flows with no errors are effectively serialized
            return Response.ok(mapper.writeValueAsString(d)).build();
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return Response.serverError().build();
        }

    }

    @Operation(summary = "Deploy an Agama project.", description = "Deploy an Agama project.", operationId = "post-agama-dev-studio-prj", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS}))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "202", description = "Agama project accepted", content = @Content(mediaType = "application/zip", schema = @Schema(implementation = String.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-dev-prj-post.json"))),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "409", description = "Conflict"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Consumes("application/zip")
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, groupScopes = {},
            superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.NAME_PARAM_PATH)
    public Response deploy(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) @NotNull String projectName, byte[] gamaBinary) {
        
        if (projectName == null || gamaBinary == null)
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Project name or binary data missing").build();
        
        if (ads.createDeploymentTask(projectName, gamaBinary))
            return Response.accepted().entity("A deployment task for project " + projectName + 
                    " has been queued. Use the GET endpoint to poll status").build();

        return Response.status(Response.Status.CONFLICT)
                .entity("There is an active deployment task for " + projectName + 
                    ". Wait for an OK response from the GET endpoint").build();

    }

    @Operation(summary = "Delete a deployed Agama project.", description = "Delete a deployed Agama project.", operationId = "delete-agama-dev-studio-prj", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            ApiAccessConstants.AGAMA_DELETE_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "No Content"),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "409", description = "Conflict"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_DELETE_ACCESS }, groupScopes = {},
            superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.NAME_PARAM_PATH)
    public Response undeploy(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) @NotNull String projectName) {
        
        if (projectName == null)
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Project name missing in query string").build();
        
        Boolean result = ads.createUndeploymentTask(projectName);
        
        if (result == null)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Unknown project " + projectName).build();
 
        if (!result)
            return Response.status(Response.Status.CONFLICT)
                    .entity("Cannot undeploy project " + projectName + ": it is currently being deployed").build();
            
        return Response.noContent().build();
        
    }

    @Operation(summary = "Retrieve the list of configs based on name.", description = "Retrieve the list of configs based on name.", operationId = "get-agama-dev-prj-configs", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_READ_ACCESS}))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Agama projects configs", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-dev-prj-get-configs-all.json"))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.CONFIGS + ApiConstants.NAME_PARAM_PATH)
    public Response getConfigs(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) @NotNull String projectName)
            throws JsonProcessingException {
        
        Pair<Response, Set<String>> pair = projectFlows(projectName);
        Response resp = pair.getFirst();
        if (resp != null) return resp;

        Map<String, Map<String, Object>> configs = new HashMap<>();

        for (String qname : pair.getSecond()) {
            Map<String, Object> config = Optional.ofNullable(flowService.getFlowByName(qname))
                    .map(f -> f.getMetadata().getProperties()).orElse(null);

            if (config == null) {
                logger.warn("Flow {} does not exist or has no configuration properties defined", qname);
            } else {
                logger.debug("Adding flow properties of {}", qname);
                configs.put(qname, config);
            }
        }
        //Use own mapper so any empty maps that may be found inside flows configurations are not ignored 
        return Response.ok(mapper.writeValueAsString(configs)).build();

    }

    @Operation(summary = "Update an Agama project.", description = "Update an Agama project.", operationId = "put-agama-dev-studio-prj", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {ApiAccessConstants.AGAMA_WRITE_ACCESS}))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "202", description = "Agama project accepted", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-dev-prj-post.json"))),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "409", description = "Conflict"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, groupScopes = {},
            superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.CONFIGS + ApiConstants.NAME_PARAM_PATH)
    public Response setConfigs(@Parameter(description = "Agama project name") @PathParam(ApiConstants.NAME) @NotNull String projectName,
            Map<String, Map<String, Object>> flowsConfigs) {

        if (flowsConfigs == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Mapping of flows vs. configs not provided").build();
        }
                    
        Pair<Response, Set<String>> pair = projectFlows(projectName);
        Response resp = pair.getFirst(); 
        if (resp != null) return resp;
        
        Set<String> flowIds = pair.getSecond();
        Map<String, Boolean> results = new HashMap<>();

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
        return Response.ok(results).build();

    }    
 
    private Pair<Response, Set<String>> projectFlows(String projectName) {        

        Response res = getDeployment(projectName);
        if (res.getStatus() != Response.Status.OK.getStatusCode()) return new Pair<>(res, null);

        try {
            Deployment d = mapper.readValue(res.getEntity().toString(), Deployment.class);
            //Retrieve the flows this project contains
            return new Pair<>(null, d.getDetails().getFlowsError().keySet());
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return new Pair<>(Response.serverError().build(), null);
        }

    }

    @PostConstruct
    private void init() {
        mapper = new ObjectMapper();
    }

}
