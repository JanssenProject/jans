package io.jans.as.server.ws.rs.org;

import io.jans.as.common.service.OrganizationService;
import io.jans.util.io.FileUploadWrapper;
import io.jans.util.io.DownloadWrapper;
import io.jans.as.model.common.ComponentType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.persistence.model.GluuOrganization;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import net.agkn.hll.HLL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.jans.as.model.util.Util.escapeLog;


@ApplicationScoped
@Path("/internal/org")
public class OrganizationConfigWS {

    public static final String BASE_AUTH_FAVICON_PATH = "/opt/jans/jetty/jans-auth/custom/static/favicon/";

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private OrganizationService organizationService;

    @Context
    HttpServletRequest request;
    
    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getOrg(@HeaderParam("Authorization") String authorization) {
        log.error("\n\n OrganizationConfigWS::getOrg() - authorization:{}, request:{}", authorization, request);
        GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationConfigWS::getOrg() - gluuOrganization:{}", gluuOrganization);
        return Response.ok(gluuOrganization).build();
    }
    
    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateOrg(@HeaderParam("Authorization") String authorization, GluuOrganization gluuOrganization) {
        log.error("\n\n OrganizationConfigWS::updateOrg() - gluuOrganization:{}", gluuOrganization);
        organizationService.updateOrganization(gluuOrganization);
        gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationConfigWS::getOrg() - gluuOrganization:{}", gluuOrganization);
        return Response.ok(gluuOrganization).build();
    }
    
    @PUT
    @Path("{imageType}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateImageResource(@HeaderParam("Authorization") String authorization, @PathParam("imageType") String imageType, FileUploadWrapper fileUploadWrapper) {
        log.error("\n\n OrganizationConfigWS::updateImageResource() - authorization:{}, imageType:{}, fileUploadWrapper:{}", authorization, imageType, fileUploadWrapper);
        GluuOrganization gluuOrganization = organizationService.getOrganization();
        return Response.ok(gluuOrganization).build();
    }

    private boolean readDefaultFavicon(HttpServletResponse response) {
        String defaultFaviconFileName = "/WEB-INF/static/favicon.ico";
       
        try (InputStream in =  request.getServletContext().getResourceAsStream(defaultFaviconFileName);
             OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading default favicon: " + e.getMessage());
            return false;
        }
    }

    private boolean readCustomFavicon(HttpServletResponse response, GluuOrganization organization) {
        if (organization.getJsFaviconPath() == null || StringUtils.isEmpty(organization.getJsFaviconPath())) {
            return false;
        }

        File directory = new File(BASE_AUTH_FAVICON_PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File faviconPath = new File(organization.getJsFaviconPath());
        if (!faviconPath.exists()) {
            return false;
        }
        try (InputStream in = new FileInputStream(faviconPath); OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading custom favicon: " + e.getMessage());
            return false;
        }
    }
   
}
