package io.jans.as.server.ws.rs.org;

import io.jans.as.common.service.OrganizationService;
import io.jans.util.io.FileUploadWrapper;
import io.jans.util.io.DownloadWrapper;
import io.jans.as.model.common.ComponentType;
import io.jans.as.model.common.Image;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    public static final String BASE_AUTH_LOGO_PATH = "/opt/jans/jetty/jans-auth/custom/static/logo/";

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private OrganizationService organizationService;

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getOrg(@HeaderParam("Authorization") String authorization) {
        log.error("\n\n OrganizationConfigWS::getOrg() - authorization:{}, request:{}", authorization, request);
        GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationConfigWS::getOrg() - gluuOrganization:{}", gluuOrganization);
        boolean hasSucceed = readCustomFavicon(response, gluuOrganization);
        log.error("\n\n OrganizationConfigWS::getOrg() - hasSucceed:{}", hasSucceed);
        if (!hasSucceed) {
            hasSucceed = readDefaultFavicon(response);
            log.error("\n\n OrganizationConfigWS::getOrg() - readDefaultFavicon:{}", hasSucceed);
        }
        return Response.ok(gluuOrganization).build();
    }

    @PUT
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response updateOrg(@HeaderParam("Authorization") String authorization, GluuOrganization gluuOrganization) {
        log.error("\n\n OrganizationConfigWS::updateOrg() - gluuOrganization:{}", gluuOrganization);
        organizationService.updateOrganization(gluuOrganization);
        gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationConfigWS::getOrg() - gluuOrganization:{}", gluuOrganization);
        return Response.ok(gluuOrganization).build();
    }

    @PUT
    @Path("{imageType}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response updateImageResource(@HeaderParam("Authorization") String authorization,
            @PathParam("imageType") Image image, FileUploadWrapper fileUploadWrapper) {
        log.error("\n\n OrganizationConfigWS::updateImageResource() - authorization:{}, image:{}, fileUploadWrapper:{}",
                authorization, image, fileUploadWrapper);
        GluuOrganization gluuOrganization = saveImage(fileUploadWrapper, image);
        log.error("\n\n OrganizationConfigWS::updateImageResource() - gluuOrganization:{} ", gluuOrganization);

        return Response.ok(gluuOrganization).build();
    }

    private GluuOrganization saveImage(FileUploadWrapper uploadedFile, Image image) {
        // Save file on server
        // Update GluuOrganization
        // Return GluuOrganization

        String basePath = getBasePath(image);
        log.error("\n\n OrganizationResource::saveImage() - basePath:{} ", basePath);
        if (StringUtils.isBlank(basePath)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR,
                    AuthorizeErrorResponseType.SERVER_ERROR, "Error while uploading image - base path blank.");
        }

        String fileName = saveFile(uploadedFile, basePath);
        log.error("\n\n OrganizationResource::saveImage() - fileName:{} ", fileName);

        final GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationResource::saveImage() - gluuOrganization:{} ", gluuOrganization);

        if (image == Image.FAVICON) {
            gluuOrganization.setJsFaviconPath(basePath + fileName);
        } else if (image == Image.LOGO) {
            gluuOrganization.setJsLogoPath(basePath + fileName);
        }

        organizationService.updateOrganization(gluuOrganization);
        return organizationService.getOrganization();

    }

    private String saveFile(FileUploadWrapper fileUploadWrapper, String basePath) {
        log.error("\n\n OrganizationResource::saveFile() - fileUploadWrapper:{} , basePath:{} ", fileUploadWrapper,
                basePath);

        String fileName = fileUploadWrapper.getFileName();
        log.error("\n\n OrganizationResource::saveFile() - fileName:{} ", fileName);
        if (StringUtils.isBlank(fileName)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR,
                    AuthorizeErrorResponseType.SERVER_ERROR, "Error while uploading image - file name blank.");
        }

        try {
            File file = new File(basePath, fileName);
            if (!file.exists()) {
                File dir = new File(basePath);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                file.createNewFile();
                file = new File(basePath, fileName);
            }
            Files.copy(fileUploadWrapper.getStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Error loading custom idp favicon", ex);
            return null;
        } finally {
            fileUploadWrapper = null;
        }
        return fileName;
    }

    private String getBasePath(Image image) {

        switch (image) {
        case FAVICON:
            return BASE_AUTH_FAVICON_PATH;
        case LOGO:
            return BASE_AUTH_LOGO_PATH;
        }
        return null;

    }

    private boolean readDefaultFavicon(HttpServletResponse response) {
        String defaultFaviconFileName = "/WEB-INF/static/favicon.ico";
        log.error("\n\n OrganizationConfigWS::readDefaultFavicon() - defaultFaviconFileName:{}",
                defaultFaviconFileName);
        try (InputStream in = request.getServletContext().getResourceAsStream(defaultFaviconFileName);
                OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading default favicon: " + e.getMessage());
            return false;
        }
    }

    private boolean readCustomFavicon(HttpServletResponse response, GluuOrganization organization) {
        log.error("\n\n OrganizationConfigWS::readCustomFavicon() - response:{}, organization:{}", response,
                organization);
        if (organization.getJsFaviconPath() == null || StringUtils.isEmpty(organization.getJsFaviconPath())) {
            return false;
        }

        File directory = new File(BASE_AUTH_FAVICON_PATH);
        log.error(
                "\n\n OrganizationConfigWS::readCustomFavicon() - directory.getName():{}, directory.getAbsolutePath():{}",
                directory.getName(), directory.getAbsolutePath());
        if (!directory.exists()) {
            directory.mkdir();
        }
        File faviconPath = new File(organization.getJsFaviconPath());
        log.error("\n\n OrganizationConfigWS::readCustomFavicon() - faviconPath:{}, faviconPath.exists():{} ",
                faviconPath, faviconPath.exists());
        if (!faviconPath.exists()) {
            log.error("\n\n OrganizationConfigWS::readCustomFavicon() - faviconPath.exists() block ");
            return false;
        }

        log.error("\n\n OrganizationConfigWS::readCustomFavicon() - Copy faviconPath  ");
        try (InputStream in = new FileInputStream(faviconPath); OutputStream out = response.getOutputStream()) {
            log.error("\n\n OrganizationConfigWS::readCustomFavicon() - IOUtils.copy block - start ");
            IOUtils.copy(in, out);
            log.error("\n\n OrganizationConfigWS::readCustomFavicon() - IOUtils.copy block - end ");
            return true;
        } catch (IOException e) {
            log.debug("Error loading custom favicon: " + e.getMessage());
            return false;
        }
    }

}
