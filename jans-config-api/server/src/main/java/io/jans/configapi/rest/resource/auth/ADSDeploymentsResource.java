package io.jans.configapi.rest.resource.auth;

import io.jans.ads.model.Deployment;
import io.jans.orm.model.PagedResult;
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