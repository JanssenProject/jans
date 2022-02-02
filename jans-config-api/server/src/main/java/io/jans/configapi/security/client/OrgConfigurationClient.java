package io.jans.configapi.security.client;

import io.jans.as.persistence.model.GluuOrganization;
import io.jans.util.io.FileUploadWrapper;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@ApplicationScoped
public interface OrgConfigurationClient {

    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    Response getOrg(@HeaderParam("Authorization") String authorization);

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    Response updateOrg(@HeaderParam("Authorization") String authorization, GluuOrganization gluuOrganization);
    
    @PUT
    @Path("{imageType}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    Response updateImageResource(@HeaderParam("Authorization") String authorization, @PathParam("imageType") String rsid, FileUploadWrapper fileUploadWrapper);

}
