package io.jans.configapi.rest.resource.auth;

import io.jans.ads.model.Deployment;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.service.auth.ADSDeploymentsService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(ApiConstants.ADS_DEPLOYMENTS)
@Produces(MediaType.APPLICATION_JSON)
public class ADSDeploymentsResource extends ConfigBaseResource {

    @Inject
    private ADSDeploymentsService ads;

    @Operation(summary = "Retrieve the list of projects deployed currently.", description = "Retrieve the list of projects deployed currently.", operationId = "get-agama-dev-prj", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            ApiAccessConstants.AGAMA_READ_ACCESS, ApiAccessConstants.AGAMA_WRITE_ACCESS,
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Agama projects", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-dev-prj-get-all.json"))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("list")
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeployments(@QueryParam("start") int start, @QueryParam("count") int count) {
        
        // this is NOT a search but a paged listing
        int maxcount = getMaxCount();
        PagedResult<Deployment> res = ads.list(start > 0 ? start - 1 : 0, count > 0 ? count : maxcount, maxcount);
        res.getEntries().forEach(d -> d.getDetails().setFolders(null));
        res.setStart(start + 1);
        return Response.ok(res).build();

    }

    @Operation(summary = "Fetches deployed Agama project based on name.", description = "Fetches deployed Agama project based on name.", operationId = "get-agama-dev-studio-prj-by-name", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            ApiAccessConstants.AGAMA_READ_ACCESS, ApiAccessConstants.AGAMA_WRITE_ACCESS,
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Agama project", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Deployment.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-dev-prj-get.json"))),
    @ApiResponse(responseCode = "204", description = "No Content"),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeployment(@QueryParam("name") String projectName) {
        
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
        return Response.ok(d).build();

    }

    @Operation(summary = "Deploy an Agama project.", description = "Deploy an Agama project.", operationId = "post-agama-dev-studio-prj", tags = {
    "Agama - Developer Studio" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            ApiAccessConstants.AGAMA_READ_ACCESS, ApiAccessConstants.AGAMA_WRITE_ACCESS,
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "202", description = "Agama project accepted", content = @Content(mediaType = "application/zip", schema = @Schema(implementation = String.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-dev-prj-post.json"))),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "409", description = "Conflict"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Consumes("application/zip")
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, 
            superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response deploy(@QueryParam("name") String projectName, byte[] gamaBinary) {
        
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
            ApiAccessConstants.AGAMA_READ_ACCESS, ApiAccessConstants.AGAMA_WRITE_ACCESS,
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "No Content"),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "409", description = "Conflict"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_WRITE_ACCESS }, 
            superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response undeploy(@QueryParam("name") String projectName) {
        
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

}