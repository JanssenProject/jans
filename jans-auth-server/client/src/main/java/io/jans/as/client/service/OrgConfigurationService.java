package io.jans.as.client.service;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.as.model.common.Image;
import io.jans.as.persistence.model.GluuOrganization;
import io.jans.util.io.DownloadWrapper;
import io.jans.util.io.FileUploadWrapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface OrgConfigurationService {    

    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public GluuOrganization getOrg(@HeaderParam("Authorization") String authorization);
    
    @GET
    @Path("{imageType}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public DownloadWrapper getImage(@HeaderParam("Authorization") String authorization, @PathParam("imageType") Image imageType);

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
