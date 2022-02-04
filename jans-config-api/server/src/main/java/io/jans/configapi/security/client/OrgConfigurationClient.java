package io.jans.configapi.security.client;

import io.jans.as.model.common.Image;
import io.jans.as.persistence.model.GluuOrganization;
import io.jans.util.io.FileUploadWrapper;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    public GluuOrganization getOrg(@HeaderParam("Authorization") String authorization);

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public GluuOrganization updateOrg(@HeaderParam("Authorization") String authorization, GluuOrganization gluuOrganization);
    
    @PUT
    @Path("{imageType}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public GluuOrganization updateImageResource(@HeaderParam("Authorization") String authorization, @PathParam("imageType") Image image, FileUploadWrapper fileUploadWrapper);
    
}
